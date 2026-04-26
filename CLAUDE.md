# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./mvnw compile          # compile only
./mvnw spring-boot:run  # run locally (requires ANTHROPIC_API_KEY or application-local.yml)
./mvnw test             # run tests
./mvnw package          # build fat jar → target/JavaAISandbox-0.0.1-SNAPSHOT.jar
```

The app runs on `http://localhost:8080` by default.

**API key**: set via env var `ANTHROPIC_API_KEY`, or override in `src/main/resources/application-local.yml` (gitignored — do not commit keys).

## Architecture

Single-feature Spring Boot 4.0.6 sandbox demonstrating Claude extended thinking over SSE.

```
ThinkingChatController  – GET /api/chat/stream  (SSE, reactive Flux)
ChatUIController        – GET /chat             (serves the HTML UI)
CorsConfig              – global CORS filter for /api/**
static/thinking-stream-ui.html  – self-contained vanilla JS SSE client
```

**Streaming flow**: the `/api/chat/stream` endpoint builds an `AnthropicChatOptions` with `.thinkingEnabled(budgetTokens)`, wraps it in a `Prompt`, and calls `chatModel.stream(prompt)`. Each `ChatResponse` is mapped to a named SSE event:

- `thinking` — chunk where `output.getMetadata().get("thinking") == Boolean.TRUE`; text is in `output.getText()`
- `text` — regular answer chunk
- `done` / `error` — terminal events

## Key Spring AI 2.x notes

This project uses **Spring AI 2.0.0-M4**, which replaced the old `spring-ai-anthropic` internal HTTP layer with the official Anthropic Java SDK (`com.anthropic`). Important API differences from 1.x:

- `AnthropicApi` / `org.springframework.ai.anthropic.api` **does not exist** — do not import it.
- Thinking is configured via `AnthropicChatOptions.Builder.thinkingEnabled(long budgetTokens)` (or `.thinking(ThinkingConfigParam)`).
- There is no `.budgetTokens()` builder method — pass the budget directly to `.thinkingEnabled()`.
- `JsonMapper` (from `tools.jackson.databind.json.JsonMapper`, the Anthropic SDK's bundled Jackson) is used instead of `com.fasterxml.jackson.databind.ObjectMapper` — Spring Boot 4 does not auto-register `ObjectMapper` as a bean in this setup.
- `budgetTokens` must be ≥ 1024; `maxTokens` must exceed `budgetTokens`.
