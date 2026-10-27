package org.example;

// 1. Core LangChain4j Imports
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;

public class Main {

    // 2. The Agent Contract (Interface)
    // Defines WHAT Ojas can do without writing boilerplate code
    interface OjasAgent {
        String chat(String userMessage);
    }

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   BOOTING OJAS STATEFUL AGENT ENGINE    ");
        System.out.println("=========================================");

        // 3. Securely fetch the API key from the OS Environment
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("CRITICAL ERROR: GEMINI_API_KEY environment variable not set!");
            return;
        }

        // 4. Initialize the AI Brain (Gemini Model)
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-3.8-flash")
                .build();

        // 5. Initialize the 4-Message FIFO Sliding Window Memory
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(4);

        // 6. Wire the Brain and Memory together using AiServices
        OjasAgent ojas = AiServices.builder(OjasAgent.class)
                .chatLanguageModel(model)
                .chatMemory(memory)
                .build();

        System.out.println("Ojas Brain + Memory Wired Successfully!");
        System.out.println("=========================================");
    }
}
