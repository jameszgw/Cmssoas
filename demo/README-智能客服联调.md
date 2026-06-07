# 智能客服联调演示(通用 · 不绑定厂商)

智能客服后端 `OpenAiCompatProvider` 走**统一 OpenAI 兼容接口** `POST {base-url}/chat/completions`,
因此"接入大模型"= 把 `app.ai.base-url/api-key/model` 指向任意兼容端点,**后端代码一行不改**。

下面给出三种联调方式:本地模拟(零依赖)、本地 Ollama(离线)、云端厂商(DeepSeek 等)。

## 0. 前置
```bash
cd server/license-platform && mvn -q -DskipTests package   # 产出 target/license-platform-1.0.1.jar
```

## 方式 A:本地模拟模型(零联网、零 GPU,最快)
仓库自带一个最小 OpenAI 兼容模拟服务 `demo/ai-mock-server.py`,实现与真实厂商一致的
`/v1/chat/completions`(stream:true)SSE 协议,用于快速验证整条链路。

```bash
# 1) 启动模拟模型(监听 :8770)
python3 demo/ai-mock-server.py 8770 &

# 2) 启动后端并指向它
cd server/license-platform
AI_ENABLED=true AI_BASE_URL=http://127.0.0.1:8770/v1 AI_MODEL=demo-llm AI_API_KEY=demo-key \
  java -jar target/license-platform-1.0.1.jar &

# 3) 验证(应返回 ready:true 与逐字 SSE)
TOK=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
      -d '{"username":"admin","password":"8888"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
curl -s localhost:8080/api/cs/status -H "Authorization: Bearer $TOK"; echo
curl -N -X POST localhost:8080/api/cs/chat -H "Authorization: Bearer $TOK" \
     -H 'Content-Type: application/json' -d '{"question":"License 授权是怎么签发的？"}'
```
预期:
```
{"ready":true,"kbSize":12,"model":"demo-llm"}
event:meta  data:{"conversationId":1}
event:delta data:您好，根据知识库为您解答：...
event:done  data:{}
```

## 方式 B:本地 Ollama(完全内网离线,不出公网)
```bash
ollama pull qwen2.5          # 或 llama3.1 / deepseek-r1 等
ollama serve                 # 默认 :11434
AI_ENABLED=true AI_BASE_URL=http://127.0.0.1:11434/v1 AI_MODEL=qwen2.5 \
  java -jar target/license-platform-1.0.1.jar
# Ollama 无需 api-key;数据全程留在内网。
```

## 方式 C:云端厂商(DeepSeek 示例,通义/Kimi/智谱/OpenAI 同理)
```bash
AI_ENABLED=true \
AI_BASE_URL=https://api.deepseek.com/v1 \
AI_API_KEY=sk-你的key \
AI_MODEL=deepseek-chat \
  java -jar target/license-platform-1.0.1.jar
```
> 生产中 `AI_API_KEY` 经 env/Nacos/KMS 注入,不落明文;CentOS 网络策略需放行所配 `base-url` 域名。

## 截图复现(功能截图)
后端(任一方式)+ 前端已 `npm run build` 后:
```bash
cp demo/shoot-features.mjs web/console/   # 或调整脚本内 dist/shots 路径
cd web/console && node shoot-features.mjs  # 产出 shots/feat-*.png
```
脚本无需常驻前端服务:Playwright 直接从 `dist/` 提供页面,并把 `/api`、`/pub` 代理到后端 :8080。

## 自动化测试
```bash
cd server/license-platform && mvn test
```
覆盖:`KnowledgeBaseTest`(知识库召回)、`FeaturesIntegrationTest`(须知/授权、客服降级、合同签署→自动出账全链路)等。
