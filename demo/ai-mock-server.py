#!/usr/bin/env python3
"""
最小 OpenAI 兼容 Chat Completions 模拟服务，用于"智能客服"联调演示(无需联网/无需 GPU)。

它实现了与 OpenAI / DeepSeek / 通义 / Ollama 同一套 `POST /v1/chat/completions`(stream:true)
SSE 协议——正因为后端 OpenAiCompatProvider 不绑定厂商，把 app.ai.base-url 指到这里即可"接入大模型"。
真实环境把 base-url 换成 https://api.deepseek.com/v1 或 http://127.0.0.1:11434/v1(Ollama) 即可，
后端代码一行不改。

用法:  python3 demo/ai-mock-server.py 8770
"""
import json
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


def reply_for(messages):
    """根据最后一个 user 问题与 system 中注入的知识库片段，编一段合理的流式回复。"""
    user = next((m.get("content", "") for m in reversed(messages) if m.get("role") == "user"), "")
    system = next((m.get("content", "") for m in messages if m.get("role") == "system"), "")
    has_kb = "【参考资料】" in system or "参考资料" in system
    head = "您好，我是智能客服。" if not has_kb else "您好，根据知识库为您解答："
    body = f"关于「{user.strip()[:40]}」，"
    if "license" in user.lower() or "授权" in user or "签发" in user:
        body += "请在「License 授权」页创建授权，选择产品/套餐并绑定客户与到期时间，系统会用服务端私钥签名生成 .lic 文件；支持 Ed25519 与国密 SM2。"
    elif "账单" in user or "开票" in user or "计费" in user:
        body += "订阅或套餐变更会自动出账，在「计费账单」先收款再开票即可生成发票号。"
    elif "合同" in user or "签约" in user:
        body += "可在「合同签约」基于模板生成合同并发起多方电子签署，全部签署后哈希存证归档。"
    else:
        body += "这是本平台支持的功能，您可在对应菜单中完成操作；如需人工协助可点击「转人工」。"
    tail = "（本回复由 OpenAI 兼容模拟模型生成，用于联调演示）"
    return head + body + tail


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass  # 静默

    def do_GET(self):
        # 便于探活：GET /v1/models
        self._json(200, {"object": "list", "data": [{"id": "demo-llm", "object": "model"}]})

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(length) if length else b"{}"
        try:
            req = json.loads(raw or b"{}")
        except Exception:
            req = {}
        messages = req.get("messages", [])
        model = req.get("model", "demo-llm")
        stream = req.get("stream", False)
        text = reply_for(messages)

        if not stream:
            self._json(200, {
                "id": "chatcmpl-demo", "object": "chat.completion", "model": model,
                "choices": [{"index": 0, "message": {"role": "assistant", "content": text},
                             "finish_reason": "stop"}],
            })
            return

        # 流式：逐"词"发 SSE，完全对齐 OpenAI delta 协议
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream; charset=utf-8")
        self.send_header("Cache-Control", "no-cache")
        self.send_header("Connection", "keep-alive")
        self.end_headers()
        # 按句读号/逗号切片，模拟逐字输出
        import re
        chunks = re.findall(r"[^，。：（）]+[，。：（）]?", text)
        for ch in chunks:
            frame = {"choices": [{"index": 0, "delta": {"content": ch}, "finish_reason": None}]}
            self.wfile.write(f"data: {json.dumps(frame, ensure_ascii=False)}\n\n".encode("utf-8"))
            self.wfile.flush()
            time.sleep(0.05)
        self.wfile.write(b"data: [DONE]\n\n")
        self.wfile.flush()

    def _json(self, code, obj):
        data = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8770
    print(f"[ai-mock] OpenAI 兼容模拟模型监听 http://127.0.0.1:{port}/v1  (Ctrl-C 退出)")
    ThreadingHTTPServer(("127.0.0.1", port), Handler).serve_forever()
