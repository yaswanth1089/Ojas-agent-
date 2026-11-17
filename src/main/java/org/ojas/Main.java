package org.ojas;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.Scanner;

import org.ojas.agent.OjasAgent;
import org.ojas.cli.OjasConsole;
import org.ojas.config.OjasConfig;
import org.ojas.memory.ConversationIngestor;
import org.ojas.rag.LocalRagPipeline;
import org.ojas.tools.FileSystemTools;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  OJAS — MAIN ENTRY POINT
 *
 *  Bootstraps the entire agentic system:
 *   1. Loads BYOK configuration from environment
 *   2. Initializes the Gemini LLM via LangChain4j Builder Pattern
 *   3. Spins up the local embedding model (AllMiniLmL6V2 — runs on JVM)
 *   4. Wires ChatMemory, @Tool methods, and RAG pipeline into AiServices
 *   5. Launches the interactive CLI loop
 *
 *  This is the orchestration engine — not a wrapper. It coordinates:
 *   • LLM inference (Gemini API)
 *   • Local vector search (in-process embeddings)
 *   • Autonomous tool execution (filesystem, process builder)
 *   • Cross-platform memory ingestion
 * ═══════════════════════════════════════════════════════════════════════
 */
public class Main {

    // ── Mutable state for model hot-swap ────────────────────────────
    private static GoogleAiGeminiChatModel chatModel;
    private static OjasAgent agent;
    private static ChatMemory memory;
    private static EmbeddingModel embeddingModel;
    private static EmbeddingStore<TextSegment> embeddingStore;
    private static LocalRagPipeline ragPipeline;
    private static String currentModelName;
    private static OjasConfig config;

    public static void main(String[] args) {

        // ── 1. BOOT SPLASH ──────────────────────────────────────────
        OjasConsole.printSplash();

        // ── 2. LOAD CONFIGURATION ───────────────────────────────────
        try {
            config = OjasConfig.load();
        } catch (IllegalStateException e) {
            OjasConsole.printError(e.getMessage());
            return;
        }

        currentModelName = config.getModelName();

        // ── 3. INITIALIZE GEMINI LLM (Builder Pattern) ──────────────
        OjasConsole.printInfo("Initializing Gemini model: " + currentModelName + "...");
        chatModel = buildChatModel(config.getApiKey(), currentModelName);

        // ── 4. INITIALIZE LOCAL EMBEDDING MODEL ─────────────────────
        //    This is what makes Ojas a NON-WRAPPER:
        //    Embeddings are computed entirely on the JVM — no API call.
        OjasConsole.printInfo("Loading local embedding model (AllMiniLmL6V2)...");
        embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        embeddingStore = new InMemoryEmbeddingStore<>();
        ragPipeline = new LocalRagPipeline(embeddingModel, embeddingStore);

        // ── 5. INITIALIZE CHAT MEMORY (Sliding Window FIFO) ─────────
        memory = MessageWindowChatMemory.withMaxMessages(config.getMemoryWindow());

        // ── 6. WIRE EVERYTHING INTO THE AGENT ───────────────────────
        agent = buildAgent();

        // ── 7. PRINT BOOT STATUS ────────────────────────────────────
        OjasConsole.printBootStatus(currentModelName, config.getMemoryWindow());
        OjasConsole.printSuccess("Ojas agent is online. Type /help for commands.");
        System.out.println();

        // ── 8. INTERACTIVE CLI LOOP ─────────────────────────────────
        runChatLoop();
    }

    /**
     * Main interactive conversation loop.
     */
    private static void runChatLoop() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            OjasConsole.printPrompt();
            String input;
            try {
                input = scanner.nextLine().trim();
            } catch (Exception e) {
                break; // EOF or stream closed
            }

            if (input.isEmpty()) continue;

            // ── Slash Commands ──────────────────────────────────────
            if (input.startsWith("/")) {
                if (handleCommand(input, scanner)) continue;
                else break; // /exit was called
            }

            // ── Send to Agent ───────────────────────────────────────
            try {
                String response = agent.chat(input);
                OjasConsole.printResponse(response);
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg != null && (msg.contains("429") || msg.contains("rate"))) {
                    OjasConsole.printError("Rate limit hit. Waiting 10 seconds...");
                    sleep(10_000);
                    OjasConsole.printInfo("Retrying...");
                    try {
                        String response = agent.chat(input);
                        OjasConsole.printResponse(response);
                    } catch (Exception e2) {
                        OjasConsole.printError("Retry failed: " + e2.getMessage());
                    }
                } else if (msg != null && msg.contains("503")) {
                    OjasConsole.printError("Gemini server overloaded (503). Retrying in 5s...");
                    sleep(5_000);
                    try {
                        String response = agent.chat(input);
                        OjasConsole.printResponse(response);
                    } catch (Exception e2) {
                        OjasConsole.printError("Retry failed: " + e2.getMessage());
                    }
                } else {
                    OjasConsole.printError("Agent error: " + (msg != null ? msg : e.getClass().getSimpleName()));
                }
            }
        }

        scanner.close();
        OjasConsole.printShutdown();
    }

    /**
     * Handle slash commands. Returns true to continue the loop, false to exit.
     */
    private static boolean handleCommand(String input, Scanner scanner) {
        String[] parts = input.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case "/exit", "/quit", "/q" -> {
                return false;
            }
            case "/help", "/h" -> {
                OjasConsole.printHelp();
            }
            case "/clear" -> {
                memory = MessageWindowChatMemory.withMaxMessages(config.getMemoryWindow());
                agent = buildAgent();
                OjasConsole.printSuccess("Memory cleared. Fresh context started.");
            }
            case "/rag" -> {
                if (arg.isEmpty()) {
                    OjasConsole.printError("Usage: /rag <directory-path>");
                } else {
                    OjasConsole.printInfo("Indexing directory for RAG: " + arg);
                    long start = System.currentTimeMillis();
                    try {
                        int count = ragPipeline.ingestDirectory(arg);
                        long elapsed = System.currentTimeMillis() - start;
                        OjasConsole.printRagStatus(arg, count, elapsed);
                        // Rebuild agent with RAG content retriever
                        agent = buildAgent();
                        OjasConsole.printSuccess("RAG pipeline active. Ask questions about your codebase.");
                    } catch (Exception e) {
                        OjasConsole.printError("RAG indexing failed: " + e.getMessage());
                    }
                }
            }
            case "/ingest" -> {
                if (arg.isEmpty()) {
                    OjasConsole.printError("Usage: /ingest <file-path> (ChatGPT JSON or text transcript)");
                } else {
                    OjasConsole.printInfo("Ingesting conversation: " + arg);
                    int count = ConversationIngestor.ingest(arg, memory);
                    if (count > 0) {
                        agent = buildAgent();
                        OjasConsole.printSuccess("Ingested " + count + " messages into memory.");
                    } else {
                        OjasConsole.printError("No messages found or parse error.");
                    }
                }
            }
            case "/model" -> {
                if (arg.isEmpty()) {
                    OjasConsole.printInfo("Current model: " + currentModelName);
                    OjasConsole.printInfo("Usage: /model <model-name>  (e.g., gemini-2.5-flash)");
                } else {
                    OjasConsole.printInfo("Switching to model: " + arg);
                    try {
                        chatModel = buildChatModel(config.getApiKey(), arg);
                        currentModelName = arg;
                        agent = buildAgent();
                        OjasConsole.printSuccess("Now using model: " + arg);
                    } catch (Exception e) {
                        OjasConsole.printError("Failed to switch model: " + e.getMessage());
                    }
                }
            }
            default -> {
                OjasConsole.printError("Unknown command: " + cmd + " — type /help for options.");
            }
        }
        return true;
    }

    /**
     * Build the Gemini chat model using the Fluent Builder Pattern.
     */
    private static GoogleAiGeminiChatModel buildChatModel(String apiKey, String modelName) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(OjasConfig.DEFAULT_TEMPERATURE)
                .maxOutputTokens(OjasConfig.DEFAULT_MAX_OUTPUT_TOKENS)
                .build();
    }

    /**
     * Wire all subsystems into the AiServices agent.
     * This is the central orchestration point where:
     *  - LLM brain (Gemini)
     *  - Memory (sliding window)
     *  - Tools (filesystem, process execution)
     *  - RAG (content retriever from vector store)
     * ...are all fused into a single coherent agent.
     */
    private static OjasAgent buildAgent() {
        var builder = AiServices.builder(OjasAgent.class)
                .chatLanguageModel(chatModel)
                .chatMemory(memory)
                .tools(new FileSystemTools());

        // Attach RAG content retriever if the embedding store has data
        try {
            var retriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .maxResults(OjasConfig.DEFAULT_RAG_TOP_K)
                    .build();
            builder.contentRetriever(retriever);
        } catch (Exception e) {
            // RAG not ready yet — that's fine, run without it
        }

        return builder.build();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
