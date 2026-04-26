package com.sandbox.javaaisandbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")  // lock down in production
public class ThinkingChatController {

    private final AnthropicChatModel chatModel;
    private final ObjectMapper objectMapper;

    public ThinkingChatController(AnthropicChatModel chatModel,
                                  ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
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
                .thinking(AnthropicApi.ThinkingMode.ENABLED)
                .budgetTokens(budgetTokens)
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
            var output   = response.getResult().getOutput();
            var metadata = output.getMetadata();

            // Spring AI Anthropic places the thinking delta under "thinking" key.
            // For text deltas the content() string is non-empty.
            String thinkingChunk = (String) metadata.getOrDefault("thinking", "");
            String textChunk     = output.getText() != null ? output.getText() : "";

            // Thinking block takes priority in this chunk
            if (!thinkingChunk.isBlank()) {
                return sseEvent("thinking", Map.of("chunk", thinkingChunk));
            }
            if (!textChunk.isBlank()) {
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
                    .data(objectMapper.writeValueAsString(payload))
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
                    .data(objectMapper.writeValueAsString(Map.of("message", message)))
                    .build();
        } catch (Exception ex) {
            return ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"message\":\"unknown error\"}")
                    .build();
        }
    }
}
