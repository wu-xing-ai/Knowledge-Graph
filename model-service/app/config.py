"""模型微服务配置：全部来自环境变量，密钥不落库。"""

import os


class Settings:
    def __init__(self):
        # ---- LLM ----
        # provider: modelscope（默认，OpenAI 兼容云端）| ollama（本地，http://localhost:11434/v1）
        self.llm_provider = os.getenv("LLM_PROVIDER", "modelscope")
        self.llm_base_url = os.getenv("LLM_BASE_URL", "https://api-inference.modelscope.cn/v1")
        self.llm_api_key = os.getenv("MODELSCOPE_API_KEY", "")
        self.llm_model = os.getenv("LLM_MODEL", "MiniMax/MiniMax-M1-80k")
        self.llm_timeout = float(os.getenv("LLM_TIMEOUT_SECONDS", "240"))

        # ---- OCR（云端 PaddleOCR / AI Studio）----
        self.ocr_job_url = os.getenv("OCR_JOB_URL", "https://paddleocr.aistudio-app.com/api/v2/ocr/jobs")
        self.ocr_token = os.getenv("PADDLE_OCR_TOKEN", "")
        self.ocr_model = os.getenv("OCR_MODEL", "PaddleOCR-VL-1.6")
        self.ocr_timeout = float(os.getenv("OCR_TIMEOUT_SECONDS", "360"))
        self.ocr_poll_interval = float(os.getenv("OCR_POLL_INTERVAL_SECONDS", "3"))


settings = Settings()