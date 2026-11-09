"""模型微服务入口（FastAPI）。

统一暴露模型能力，供 Java 后端调用：
- POST /v1/chat/completions   OpenAI 兼容对话（非流式 JSON / 流式 SSE 透传）
- POST /v1/ocr                文档/图片 OCR，返回 Markdown 文本
- GET  /health                健康检查
"""

import logging

from fastapi import FastAPI, File, HTTPException, Request, UploadFile
from fastapi.responses import JSONResponse, StreamingResponse

from . import llm, ocr

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
log = logging.getLogger("model-service")

app = FastAPI(title="Zhi Model Service", version="1.0.0", description="知联汇学模型微服务：LLM / OCR 统一网关")
app.state.ocr_client = ocr.PaddleOcrClient()

_MAX_UPLOAD_BYTES = 10 * 1024 * 1024


@app.get("/health")
async def health():
    return {"status": "ok", "provider": llm.settings.llm_provider, "model": llm.settings.llm_model}


@app.post("/v1/chat/completions")
async def chat_completions(request: Request):
    try:
        body = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="请求体不是合法 JSON")
    if not body.get("messages"):
        raise HTTPException(status_code=400, detail="messages 不能为空")

    if body.get("stream"):
        return StreamingResponse(
            llm.forward_chat_stream(body),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
        )
    result = await llm.forward_chat(body)
    return JSONResponse(result)


@app.post("/v1/ocr")
async def ocr_endpoint(file: UploadFile = File(...)):
    data = await file.read()
    if not data:
        raise HTTPException(status_code=400, detail="文件内容为空")
    if len(data) > _MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="文件超过 10MB 限制")
    filename = file.filename or "file"
    try:
        text = await app.state.ocr_client.recognize(filename, data)
    except HTTPException:
        raise
    except Exception as e:
        log.exception("OCR 失败")
        raise HTTPException(status_code=502, detail=f"OCR 失败: {e}")
    return {"text": text}
