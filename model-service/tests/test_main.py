from app import llm, ocr
from app.main import app
from fastapi.testclient import TestClient


def test_health():
    client = TestClient(app)
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"


def test_ocr_markdown_from_jsonl():
    jsonl = (
        '{"result":{"layoutParsingResults":[{"markdown":{"text":"第一页"}}]}}\n'
        '{"result":{"layoutParsingResults":[{"markdown":{"text":"第二页"}}]}}\n'
        "not-json-line\n"
        '{"result":{"layoutParsingResults":[]}}\n'
    )
    assert ocr.markdown_from_jsonl(jsonl) == "第一页\n\n第二页"


def test_ocr_empty_jsonl():
    assert ocr.markdown_from_jsonl("") == ""
    assert ocr.markdown_from_jsonl("garbage\n") == ""


def test_ocr_endpoint_with_mock_client(monkeypatch):
    class FakeClient:
        async def recognize(self, filename, data):
            assert data == b"fake-bytes"
            return "识别文本"

    monkeypatch.setattr(app.state, "ocr_client", FakeClient())
    client = TestClient(app)
    resp = client.post("/v1/ocr", files={"file": ("扫描件.png", b"fake-bytes", "image/png")})
    assert resp.status_code == 200
    assert resp.json() == {"text": "识别文本"}


def test_ocr_endpoint_empty_file(monkeypatch):
    monkeypatch.setattr(app.state, "ocr_client", object())
    client = TestClient(app)
    resp = client.post("/v1/ocr", files={"file": ("空.txt", b"", "text/plain")})
    assert resp.status_code == 400


def test_llm_resolve_endpoint():
    assert llm.resolve_endpoint("ollama", "") == "http://localhost:11434/v1"
    assert llm.resolve_endpoint("ollama", "http://localhost:11434/v1") == "http://localhost:11434/v1"
    assert llm.resolve_endpoint("modelscope", "https://x/v1") == "https://x/v1"


def test_llm_headers_without_key():
    import pytest

    with pytest.raises(Exception):
        llm.build_headers("modelscope", "")
    assert llm.build_headers("ollama", "") == {"Content-Type": "application/json"}


def test_chat_completions_missing_messages():
    client = TestClient(app)
    resp = client.post("/v1/chat/completions", json={"stream": False})
    assert resp.status_code == 400
