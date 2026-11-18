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
        // ── Ensure UTF-8 and IPv4 networking defaults ───────────────
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");
        System.setProperty("java.net.preferIPv4Stack", "true");

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

            // ── Exit commands ───────────────────────────────────────
            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                break;
            }

            // ── Slash Commands ──────────────────────────────────────
            if (input.startsWith("/")) {
                if (handleCommand(input, scanner)) continue;
                else break; // /exit was called
            }

            // ── Send to Agent with Intelligent Resilience ──────────
            try {
                String response = executeChatWithResilience(input);
                OjasConsole.printResponse(response);
            } catch (Exception e) {
                OjasConsole.printError("Agent error: " + e.getMessage());
            }
        }

        scanner.close();
        OjasConsole.printShutdown();
    }

    /**
     * Executes chat with automatic retry and model failover:
     *  1. Catches transient socket / DNS handshake delays and retries.
     *  2. Catches 503 high-demand spikes and auto-failovers to alternate model.
     *  3. Catches 429 rate-limit errors and applies exponential backoff.
     */
    private static String executeChatWithResilience(String input) throws Exception {
        try {
            return agent.chat(input);
        } catch (Exception e) {
            String chain = getFullErrorChain(e);

            // 1. Transient socket / DNS resolution hiccups on Windows startup
            if (errorMatches(chain, "UnresolvedAddressException", "ConnectException", "SocketTimeoutException")) {
                OjasConsole.printInfo("Network handshake initializing. Retrying...");
                sleep(1500);
                try {
                    return agent.chat(input);
                } catch (Exception retryEx) {
                    chain = getFullErrorChain(retryEx);
                }
            }

            // 2. High demand spikes (503 UNAVAILABLE)
            if (errorMatches(chain, "503", "UNAVAILABLE", "high demand")) {
                OjasConsole.printInfo("Model " + currentModelName + " reported high demand (503). Retrying in 2s...");
                sleep(2000);
                try {
                    return agent.chat(input);
                } catch (Exception retryEx) {
                    // Auto-failover to alternative model
                    String fallback = currentModelName.equalsIgnoreCase(OjasConfig.DEFAULT_MODEL_NAME)
                            ? OjasConfig.FALLBACK_MODEL_NAME
                            : OjasConfig.DEFAULT_MODEL_NAME;
                    OjasConsole.printInfo("Auto-failover: temporarily switching to " + fallback + "...");
                    if (switchModel(fallback)) {
                        try {
                            return agent.chat(input);
                        } catch (Exception fallbackEx) {
                            throw new RuntimeException("Failover to " + fallback + " also failed: " + getFullErrorChain(fallbackEx), fallbackEx);
                        }
                    }
                    throw retryEx;
                }
            }

            // 3. Rate limits (429 RESOURCE_EXHAUSTED)
            if (errorMatches(chain, "429", "RESOURCE_EXHAUSTED", "rate limit")) {
                OjasConsole.printInfo("Rate limit reached. Backing off for 6 seconds...");
                sleep(6000);
                return agent.chat(input);
            }

            throw new RuntimeException(chain.isEmpty() ? e.getClass().getSimpleName() : chain, e);
        }
    }

    /**
     * Switch active LLM model dynamically at runtime.
     */
    private static boolean switchModel(String modelName) {
        try {
            chatModel = buildChatModel(config.getApiKey(), modelName);
            currentModelName = modelName;
            agent = buildAgent();
            return true;
        } catch (Exception e) {
            OjasConsole.printError("Failed to switch model: " + e.getMessage());
            return false;
        }
    }

    private static String getFullErrorChain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable cur = t;
        while (cur != null) {
            if (cur.getMessage() != null && !cur.getMessage().isBlank()) {
                if (!sb.isEmpty()) sb.append(" -> ");
                sb.append(cur.getMessage());
            }
            cur = cur.getCause();
        }
        return sb.toString();
    }

    private static boolean errorMatches(String chain, String... keywords) {
        String lower = chain.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) return true;
        }
        return false;
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
            case "/model", "/models" -> {
                if (arg.isEmpty()) {
                    OjasConsole.printInfo("Current model: " + currentModelName);
                    OjasConsole.printInfo("Verified models: gemini-3.8-flash (default), gemini-3.5-flash (fast), gemini-3.5-flash-lite");
                    OjasConsole.printInfo("Usage: /model <model-name>");
                } else {
                    OjasConsole.printInfo("Switching to model: " + arg);
                    if (switchModel(arg)) {
                        OjasConsole.printSuccess("Now using model: " + arg);
                    }
                }
            }
            case "/status" -> {
                OjasConsole.printBootStatus(currentModelName, config.getMemoryWindow());
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
