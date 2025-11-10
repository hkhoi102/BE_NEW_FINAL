# Smart Retail AI Service (Python)

Minimal FastAPI microservice providing:
- RAG over documents (Chroma + OpenAI embeddings)
- SQL Agent over MySQL (LangChain)

## Setup

1) Python 3.11+

2) Install deps:
```bash
pip install -r requirements.txt
```

3) Export environment variables:
```bash
# OpenAI
set OPENAI_API_KEY=sk-xxxxx
set MODEL_NAME=gpt-4o-mini

# MySQL (read-only user)
set MYSQL_URL=mysql+pymysql://reader:reader@localhost:3306/retail

# RAG paths
set DOCS_DIR=./data/docs
set CHROMA_DIR=./data/chroma
```

4) Run service:
```bash
uvicorn app.main:app --reload --port 8000
```

## Endpoints

- POST `/ingest` body `{ "paths": ["./docs/policies"] }` or omit to use `DOCS_DIR`.
- POST `/chat` body `{ "question": "Sản phẩm nào bán chạy nhất tháng trước?" }`.
- GET `/health`

## Docker
```bash
docker build -t smart-retail-ai .
docker run -p 8000:8000 -e OPENAI_API_KEY=sk-xxxxx -e MYSQL_URL="mysql+pymysql://reader:reader@host:3306/retail" smart-retail-ai
```

## Integrate from Spring Boot
Call `POST http://ai-service:8000/chat` from your controller and return the JSON payload.

