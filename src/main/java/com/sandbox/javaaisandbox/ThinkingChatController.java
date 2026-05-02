package com.sandbox.javaaisandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ThinkingChatController.class);

    private final AnthropicChatModel chatModel;
    private final JsonMapper jsonMapper;

    public ThinkingChatController(AnthropicChatModel chatModel,
                                  JsonMapper jsonMapper) {
        this.chatModel = chatModel;
        this.jsonMapper = jsonMapper;
    }

    /**
     * SSE endpoint — emits three event types:
     *   event: thinking → partial thinking chunk
     *   event: text → partial answer chunk
     *   event: done → stream complete signal
     *   event: error → stream error signal
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestParam String message,
                                                @RequestParam(defaultValue = "8000") int budgetTokens) {
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        if (budgetTokens < 1024) {
            throw new IllegalArgumentException("budgetTokens must be >= 1024");
        }

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(chatModel.getOptions().getModel())
                .thinkingAdaptive()
                .thinkingEnabled(budgetTokens)
                .maxTokens(chatModel.getOptions().getMaxTokens())
                .build();

        Prompt prompt = new Prompt(List.of(new UserMessage(message)), options);

        // onErrorResume is placed after concatWith so that stream errors suppress the done event
        // and only emit a single terminal event (error), while normal completion emits done.
        return chatModel.stream(prompt)
                .map(this::toSseEvent)
                .concatWith(Flux.just(doneEvent()))
                .onErrorResume(ex -> {
                    log.error("SSE stream error", ex);
                    return Flux.just(errorEvent(clientMessage(ex)));
                });
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
            log.error("Error building SSE event from response", e);
            return errorEvent("Event processing error");
        }
    }

    private ServerSentEvent<String> sseEvent(String eventType, Map<String, ?> payload) {
        try {
            return ServerSentEvent.<String>builder()
                    .event(eventType)
                    .data(jsonMapper.writeValueAsString(payload))
                    .build();
        } catch (Exception e) {
            log.error("JSON serialization error for event type '{}'", eventType, e);
            return errorEvent("Serialization error");
        }
    }

    private ServerSentEvent<String> doneEvent() {
        return ServerSentEvent.<String>builder()
                .event("done")
                .data("{}")
                .build();
    }

    private String clientMessage(Throwable ex) {
        if (ex instanceof IllegalArgumentException) {
            return ex.getMessage();
        }
        return "Stream error — see server logs for details";
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
