"""LLM 网关：OpenAI 兼容的 /v1/chat/completions 转发。

支持两种 provider：
- modelscope：转发魔搭 ModelScope（默认，请求头带 Bearer key）
- ollama：转发本地 Ollama（http://localhost:11434/v1，无需鉴权）

非流式：透传上游 JSON；流式：逐行透传上游 SSE，Java 客户端无需改动解析逻辑。
"""

import logging

import httpx
from fastapi import HTTPException

from .config import settings

log = logging.getLogger("model-service.llm")

_DEFAULT_OLLAMA_URL = "http://localhost:11434/v1"
_LLM_TIMEOUT = max(settings.llm_timeout, 30.0)


def resolve_endpoint(provider: str, base_url: str) -> str:
    """provider=ollama 且未显式配置 base_url 时，落到本地默认地址。"""
    if provider == "ollama" and (not base_url or base_url == "https://api-inference.modelscope.cn/v1"):
        return _DEFAULT_OLLAMA_URL
    return base_url.rstrip("/")


def build_headers(provider: str, api_key: str) -> dict[str, str]:
    headers = {"Content-Type": "application/json"}
    if provider == "ollama":
        return headers
    if not api_key:
        raise HTTPException(status_code=503, detail="未配置 MODELSCOPE_API_KEY，无法调用云端 LLM")
    headers["Authorization"] = "Bearer " + api_key
    return headers


def _build_request_body(body: dict) -> dict:
    """把客户端请求整理为完整 OpenAI 兼容体：模型名默认取配置，补齐缺省参数。"""
    payload = dict(body)
    payload.setdefault("model", settings.llm_model)
    payload.setdefault("temperature", 0.3)
    payload.setdefault("max_tokens", 4096)
    payload["messages"] = payload.get("messages") or []
    return payload


async def forward_chat(body: dict) -> dict:
    """非流式：转发并返回上游 JSON。"""
    payload = _build_request_body(body)
    endpoint = resolve_endpoint(settings.llm_provider, settings.llm_base_url)
    headers = build_headers(settings.llm_provider, settings.llm_api_key)
    url = endpoint + "/chat/completions"
    async with httpx.AsyncClient(timeout=_LLM_TIMEOUT) as client:
        resp = await client.post(url, json=payload, headers=headers)
        if resp.status_code != 200:
            raise HTTPException(
                status_code=502,
                detail=f"LLM 调用失败 (HTTP {resp.status_code}): {resp.text[:500]}",
            )
        return resp.json()


async def forward_chat_stream(body: dict):
    """流式：逐行透传上游 SSE（保持 data: 前缀与 JSON 格式不变）。"""
    payload = _build_request_body(body)
    endpoint = resolve_endpoint(settings.llm_provider, settings.llm_base_url)
    headers = build_headers(settings.llm_provider, settings.llm_api_key)
    url = endpoint + "/chat/completions"
    async with httpx.AsyncClient(timeout=_LLM_TIMEOUT) as client:
        async with client.stream("POST", url, json=payload, headers=headers) as resp:
            if resp.status_code != 200:
                raw = (await resp.aread()).decode("utf-8", "replace")
                raise HTTPException(
                    status_code=502,
                    detail=f"LLM 流式调用失败 (HTTP {resp.status_code}): {raw[:500]}",
                )
            async for line in resp.aiter_lines():
                if line.strip():
                    yield line + "\n"
            yield "data: [DONE]\n"