
# AI Retail Assistant (All-in-one)

Spring Boot 3 + H2 + Swagger + modern static UI + uploads with auto-RAG indexing + Llama 3 integration (Ollama/OpenAI-style).

## Run
```bash
./gradlew bootRun
```
- Dashboard: http://localhost:8080/
- Swagger:   http://localhost:8080/swagger-ui.html
- H2:        http://localhost:8080/h2-console  (JDBC: jdbc:h2:mem:retaidai, user: sa, pass: blank)

## Uploads & RAG
- Uploads saved to `uploads/` (auto-created). Verify: `GET /api/files/{filename}`
- `POST /api/ingest/file` with `type=inventory|products|sales|rag_policies|rag_catalog`
  - `rag_policies`: splits Markdown and indexes into RAG
  - `rag_catalog`: reads JSONL and indexes into RAG

## Chat
`POST /api/chat/ask` with `{ "tenant_id":"demo", "q":"..." }`
- Routes to **inventory tool** for availability
- Falls back to **RAG retrieval** and uses **Llama 3** for answer synthesis

### LLM config (application.yml)
- Default uses **Ollama** at `http://localhost:11434` with `llama3:8b-instruct`
- For OpenAI-style providers, set:
```
app.llm.provider: openai
app.llm.model: <provider-model-name>
app.llm.base-url: <provider-base-url>/openai/v1
app.llm.api-key: <key>
```

## Seed data
Startup loads `mock/inventory.csv` and `mock/sales_events.jsonl` into H2.
