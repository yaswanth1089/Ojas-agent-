package org.ojas.memory;

import com.google.gson.*;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  CONVERSATION INGESTION ENGINE
 *
 *  Cross-Platform AI Bridging: Ingest exported conversation logs from
 *  external platforms (ChatGPT JSON exports, plain text transcripts)
 *  and merge them into Ojas's active ChatMemory.
 *
 *  This eliminates vendor lock-in by letting users carry context from
 *  one LLM into Gemini's context window at zero cost.
 *
 *  Supported formats:
 *   • ChatGPT JSON export (conversations.json)
 *   • Plain text transcript (alternating User:/Assistant: lines)
 *   • Markdown conversation files
 * ═══════════════════════════════════════════════════════════════════════
 */
public class ConversationIngestor {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Ingest a ChatGPT JSON export file and add messages to the given ChatMemory.
     * ChatGPT exports use this structure:
     * [ { "title": "...", "mapping": { ... } } ]
     *
     * We extract user/assistant message pairs from the conversation.
     */
    public static int ingestChatGptExport(String filePath, ChatMemory memory) {
        try {
            String json = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
            JsonArray conversations = JsonParser.parseString(json).getAsJsonArray();
            int messageCount = 0;

            for (JsonElement convElem : conversations) {
                JsonObject conv = convElem.getAsJsonObject();
                if (!conv.has("mapping")) continue;

                JsonObject mapping = conv.getAsJsonObject("mapping");
                for (var entry : mapping.entrySet()) {
                    JsonObject node = entry.getValue().getAsJsonObject();
                    if (!node.has("message") || node.get("message").isJsonNull()) continue;

                    JsonObject message = node.getAsJsonObject("message");
                    if (!message.has("author") || !message.has("content")) continue;

                    JsonObject author = message.getAsJsonObject("author");
                    String role = author.has("role") ? author.get("role").getAsString() : "";
                    JsonObject content = message.getAsJsonObject("content");

                    if (!content.has("parts")) continue;
                    JsonArray parts = content.getAsJsonArray("parts");
                    StringBuilder text = new StringBuilder();
                    for (JsonElement part : parts) {
                        if (part.isJsonPrimitive()) {
                            text.append(part.getAsString());
                        }
                    }

                    String msgText = text.toString().trim();
                    if (msgText.isEmpty()) continue;

                    // Truncate very long messages to avoid token overflow
                    if (msgText.length() > 2000) {
                        msgText = msgText.substring(0, 2000) + "\n... [truncated]";
                    }

                    switch (role) {
                        case "user" -> {
                            memory.add(UserMessage.from(msgText));
                            messageCount++;
                        }
                        case "assistant" -> {
                            memory.add(AiMessage.from(msgText));
                            messageCount++;
                        }
                        // Skip "system" and "tool" roles
                    }
                }
            }
            return messageCount;
        } catch (IOException e) {
            System.err.println("ERROR: Failed to read ChatGPT export — " + e.getMessage());
            return 0;
        } catch (JsonSyntaxException e) {
            System.err.println("ERROR: Invalid JSON in ChatGPT export — " + e.getMessage());
            return 0;
        }
    }

    /**
     * Ingest a plain text conversation transcript.
     * Expected format:
     *   User: hello
     *   Assistant: hi there
     *   User: how are you?
     *   Assistant: I'm doing well.
     */
    public static int ingestTextTranscript(String filePath, ChatMemory memory) {
        try {
            List<String> lines = Files.readAllLines(Path.of(filePath), StandardCharsets.UTF_8);
            int messageCount = 0;
            StringBuilder currentMessage = new StringBuilder();
            String currentRole = null;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.toLowerCase().startsWith("user:")) {
                    if (currentRole != null && !currentMessage.isEmpty()) {
                        addMessage(memory, currentRole, currentMessage.toString().trim());
                        messageCount++;
                    }
                    currentRole = "user";
                    currentMessage = new StringBuilder(trimmed.substring(5).trim());
                } else if (trimmed.toLowerCase().startsWith("assistant:")) {
                    if (currentRole != null && !currentMessage.isEmpty()) {
                        addMessage(memory, currentRole, currentMessage.toString().trim());
                        messageCount++;
                    }
                    currentRole = "assistant";
                    currentMessage = new StringBuilder(trimmed.substring(10).trim());
                } else if (currentRole != null) {
                    currentMessage.append("\n").append(trimmed);
                }
            }

            // Don't forget the last message
            if (currentRole != null && !currentMessage.isEmpty()) {
                addMessage(memory, currentRole, currentMessage.toString().trim());
                messageCount++;
            }

            return messageCount;
        } catch (IOException e) {
            System.err.println("ERROR: Failed to read transcript — " + e.getMessage());
            return 0;
        }
    }

    /**
     * Auto-detect format and ingest a conversation file.
     */
    public static int ingest(String filePath, ChatMemory memory) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".json")) {
            return ingestChatGptExport(filePath, memory);
        } else {
            return ingestTextTranscript(filePath, memory);
        }
    }

    private static void addMessage(ChatMemory memory, String role, String text) {
        if (text.length() > 2000) {
            text = text.substring(0, 2000) + "\n... [truncated]";
        }
        switch (role) {
            case "user" -> memory.add(UserMessage.from(text));
            case "assistant" -> memory.add(AiMessage.from(text));
        }
    }
}
