"""云端 PaddleOCR（AI Studio）调用：提交任务 -> 轮询状态 -> 拉取 JSONL -> 拼接 Markdown。"""

import asyncio
import json
import logging
import time
import uuid

import httpx
from fastapi import HTTPException

from .config import settings

log = logging.getLogger("model-service.ocr")

OCR_OPTIONAL_PAYLOAD = (
    '{"useDocOrientationClassify":false,"useDocUnwarping":false,"useChartRecognition":false}'
)


def sanitize(filename: str) -> str:
    if not filename:
        return "file"
    return filename.replace("\r", "_").replace("\n", "_").replace('"', "_")


def markdown_from_jsonl(jsonl_text: str) -> str:
    """从 OCR 结果 JSONL 中提取各页 layoutParsingResults 的 markdown 文本并拼接。"""
    parts = []
    for line in jsonl_text.splitlines():
        line = line.strip()
        if not line:
            continue
        try:
            node = json.loads(line)
        except json.JSONDecodeError:
            continue
        results = (node.get("result") or {}).get("layoutParsingResults") or []
        for r in results:
            text = ((r.get("markdown") or {}).get("text")) or ""
            if text:
                parts.append(text)
    return "\n\n".join(parts).strip()


class PaddleOcrClient:
    def __init__(
        self,
        job_url: str = settings.ocr_job_url,
        token: str = settings.ocr_token,
        model: str = settings.ocr_model,
        timeout: float = settings.ocr_timeout,
        poll_interval: float = settings.ocr_poll_interval,
    ):
        self.job_url = job_url.rstrip("/")
        self.token = token
        self.model = model
        self.timeout = timeout
        self.poll_interval = poll_interval

    def _headers(self) -> dict:
        if not self.token:
            raise HTTPException(status_code=503, detail="未配置 PADDLE_OCR_TOKEN，无法调用云端 OCR")
        return {"Authorization": "bearer " + self.token}

    async def recognize(self, filename: str, data: bytes) -> str:
        job_id = await self._submit(filename, data)
        jsonl_url = await self._poll(job_id)
        return await self._fetch_markdown(jsonl_url)

    async def _submit(self, filename: str, data: bytes) -> str:
        boundary = "----ZhiPlatformOcr" + uuid.uuid4().hex
        multipart = (
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="model"\r\n\r\n{self.model}\r\n'
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="optionalPayload"\r\n\r\n{OCR_OPTIONAL_PAYLOAD}\r\n'
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="file"; filename="{sanitize(filename)}"\r\n'
            f"Content-Type: application/octet-stream\r\n\r\n"
        )
        body = multipart.encode("utf-8") + data + f"\r\n--{boundary}--\r\n".encode("utf-8")
        headers = self._headers()
        headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
        async with httpx.AsyncClient(timeout=120.0) as client:
            resp = await client.post(self.job_url, content=body, headers=headers)
        job_id = (resp.json().get("data") or {}).get("jobId") or ""
        if not job_id:
            raise HTTPException(status_code=502, detail=f"OCR 提交失败: {resp.text[:500]}")
        return job_id

    async def _poll(self, job_id: str) -> str:
        deadline = time.monotonic() + self.timeout
        async with httpx.AsyncClient(timeout=60.0) as client:
            while time.monotonic() < deadline:
                resp = await client.get(self.job_url + "/" + job_id, headers=self._headers())
                data = (resp.json().get("data")) or {}
                state = data.get("state") or ""
                if state == "done":
                    return (data.get("resultUrl") or {}).get("jsonUrl") or ""
                if state == "failed":
                    msg = data.get("errorMsg") or "未知错误"
                    raise HTTPException(status_code=502, detail=f"OCR 任务失败: {msg}")
                await asyncio.sleep(self.poll_interval)
        raise HTTPException(status_code=504, detail="OCR 任务超时")

    async def _fetch_markdown(self, jsonl_url: str) -> str:
        async with httpx.AsyncClient(timeout=120.0) as client:
            resp = await client.get(jsonl_url)
        return markdown_from_jsonl(resp.text)
