package com.sandbox.javaaisandbox;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")  // lock down in production
public class ThinkingChatController {

    private final AnthropicChatModel chatModel;
    private final JsonMapper jsonMapper;

    public ThinkingChatController(AnthropicChatModel chatModel,
                                  JsonMapper jsonMapper) {
        this.chatModel = chatModel;
        this.jsonMapper = jsonMapper;
    }

    /**
     * SSE endpoint — emits three event types:
     *   event: thinking   →  partial thinking chunk
     *   event: text       →  partial answer chunk
     *   event: done       →  stream complete signal
     *   event: error      →  stream error signal
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestParam String message,
                                                @RequestParam(defaultValue = "8000") int budgetTokens) {
        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model("claude-sonnet-4-5-20250929")
                .thinkingEnabled(budgetTokens)
                .maxTokens(budgetTokens + 4000) // must exceed budgetTokens
                .build();

        Prompt prompt = new Prompt(List.of(new UserMessage(message)), options);

        return chatModel.stream(prompt)
                .map(this::toSseEvent)
                .onErrorResume(ex -> Flux.just(errorEvent(ex.getMessage())))
                .concatWith(Flux.just(doneEvent()));
    }

    // ── SSE event builders ──────────────────────────────────────────────────

    private ServerSentEvent<String> toSseEvent(ChatResponse response) {
        try {
            var result = response.getResult();
            if (result == null) {
                return ServerSentEvent.<String>builder().comment("ping").build();
            }
            var output   = result.getOutput();
            var metadata = output.getMetadata();

            /*
                In Spring AI 2.x the thinking delta arrives with metadata "thinking"=Boolean.TRUE
                the actual thinking text is in output.getText().
             */
            boolean isThinking = Boolean.TRUE.equals(metadata.get("thinking"));
            String textChunk = output.getText();

            if (isThinking && textChunk != null && !textChunk.isBlank()) {
                return sseEvent("thinking", Map.of("chunk", textChunk));
            }
            if (!isThinking && textChunk != null && !textChunk.isBlank()) {
                return sseEvent("text", Map.of("chunk", textChunk));
            }

            // Empty/heartbeat chunk — send a no-op ping so the connection stays alive
            return ServerSentEvent.<String>builder()
                    .comment("ping")
                    .build();

        } catch (Exception e) {
            return errorEvent(e.getMessage());
        }
    }

    private ServerSentEvent<String> sseEvent(String eventType, Map<String, ?> payload) {
        try {
            return ServerSentEvent.<String>builder()
                    .event(eventType)
                    .data(jsonMapper.writeValueAsString(payload))
                    .build();
        } catch (Exception e) {
            return errorEvent(e.getMessage());
        }
    }

    private ServerSentEvent<String> doneEvent() {
        return ServerSentEvent.<String>builder()
                .event("done")
                .data("{}")
                .build();
    }

    private ServerSentEvent<String> errorEvent(String message) {
        try {
            return ServerSentEvent.<String>builder()
                    .event("error")
                    .data(jsonMapper.writeValueAsString(Map.of("message", message)))
                    .build();
        } catch (Exception ex) {
            return ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"message\":\"unknown error\"}")
                    .build();
        }
    }
}
