package org.ojas.config;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  OJAS ENVIRONMENT CONFIGURATION
 *  Zero-Trust BYOK (Bring Your Own Key) credential management.
 *  API keys are NEVER hardcoded — they come from OS environment variables.
 * ═══════════════════════════════════════════════════════════════════════
 */
public final class OjasConfig {

    // ── Model Defaults ──────────────────────────────────────────────────
    public static final String DEFAULT_MODEL_NAME = "gemini-2.5-flash";
    public static final int    DEFAULT_MEMORY_WINDOW = 20;
    public static final int    DEFAULT_RAG_TOP_K = 5;
    public static final int    DEFAULT_RAG_CHUNK_SIZE = 500;
    public static final int    DEFAULT_RAG_CHUNK_OVERLAP = 100;
    public static final double DEFAULT_TEMPERATURE = 0.7;
    public static final int    DEFAULT_MAX_OUTPUT_TOKENS = 8192;

    // ── Environment Variable Keys ───────────────────────────────────────
    private static final String ENV_API_KEY     = "GEMINI_API_KEY";
    private static final String ENV_MODEL_NAME  = "OJAS_MODEL";
    private static final String ENV_MEMORY_SIZE = "OJAS_MEMORY_WINDOW";

    private final String apiKey;
    private final String modelName;
    private final int    memoryWindow;

    private OjasConfig(String apiKey, String modelName, int memoryWindow) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.memoryWindow = memoryWindow;
    }

    /**
     * Load configuration from environment variables.
     * Falls back to hardcoded defaults for non-secret values.
     * Fails fast if the API key is missing.
     */
    public static OjasConfig load() {
        String apiKey = System.getenv(ENV_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "\n╔══════════════════════════════════════════════════════════════╗\n" +
                "║  CRITICAL: GEMINI_API_KEY environment variable not set!     ║\n" +
                "║  Set it in your OS or IDE Run Configuration:                ║\n" +
                "║    export GEMINI_API_KEY=your_key_here                      ║\n" +
                "╚══════════════════════════════════════════════════════════════╝"
            );
        }

        String modelName = System.getenv(ENV_MODEL_NAME);
        if (modelName == null || modelName.isBlank()) {
            modelName = DEFAULT_MODEL_NAME;
        }

        int memoryWindow = DEFAULT_MEMORY_WINDOW;
        String memEnv = System.getenv(ENV_MEMORY_SIZE);
        if (memEnv != null && !memEnv.isBlank()) {
            try { memoryWindow = Integer.parseInt(memEnv); }
            catch (NumberFormatException ignored) { /* use default */ }
        }

        return new OjasConfig(apiKey, modelName, memoryWindow);
    }

    /**
     * Create config with an explicitly provided API key (for programmatic use).
     */
    public static OjasConfig withApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be null or blank.");
        }
        return new OjasConfig(apiKey, DEFAULT_MODEL_NAME, DEFAULT_MEMORY_WINDOW);
    }

    public String getApiKey()      { return apiKey; }
    public String getModelName()   { return modelName; }
    public int    getMemoryWindow(){ return memoryWindow; }
}
