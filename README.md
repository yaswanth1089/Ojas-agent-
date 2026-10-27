# ⚡ Ojas — Enterprise Java Agentic AI Core

**Ojas** is a stateful, production-structured AI desktop agent backend engineered from scratch in **Java 25 (LTS)** using **Maven**, **LangChain4j**, and the **Google Gemini API** (`gemini-3.8-flash`).

Designed with enterprise backend architecture principles, Ojas moves beyond simple stateless API wrappers by implementing bounded sliding-window conversational memory, declarative AI service abstractions, and decoupled environment security.

---

## 🏗️ System Architecture & Key Backend Features

* **Declarative AI Service Orchestration (`AiServices`):** Uses Java interface contracts (`OjasAgent`) and dynamic runtime proxies to decouple business logic from low-level HTTP/JSON payload construction.
* **Bounded FIFO Sliding-Window Memory (`MessageWindowChatMemory`):** Prevents context-window token exhaustion and `OutOfMemoryError` bottlenecks by enforcing a strict First-In, First-Out (FIFO) message eviction queue.
* **Resilient Network & Error Handling:** Designed with defensive exception handling (`try-catch` + exponential backoff via `Thread.sleep`) to gracefully manage HTTP `503` server spikes and `429` rate-limit thresholds.
* **Zero-Trust Credential Management:** Strictly isolates API keys from source code using OS-level environment variables (`System.getenv("GEMINI_API_KEY")`).

---

## 🛠️ Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language & Runtime** | Java 25 (LTS) |
| **Build & Dependency Management** | Apache Maven (`pom.xml`) |
| **AI Orchestration Framework** | [LangChain4j](https://docs.langchain4j.dev/) (`0.36.2`) |
| **LLM Engine** | Google AI Gemini (`gemini-3.8-flash`) |
| **IDE** | IntelliJ IDEA |

---

## 🚀 Getting Started (Local Setup)

### 1. Prerequisites
* **JDK 21 or 25 (LTS)** installed.
* **Apache Maven** (bundled with IntelliJ IDEA).
* A valid **Google AI Studio API Key**.

### 2. Clone the Repository
```bash
git clone https://github.com/yaswanth1089/Ojas-agent-.git
cd ojas
```

### 3. Configure Your Environment Variable
Set the `GEMINI_API_KEY` environment variable in your OS or IntelliJ Run Configuration:
```text
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

### 4. Build and Run
Compile and execute `src/main/java/org/example/Main.java` directly from IntelliJ IDEA or via Maven.

---

## 🗺️ Development Roadmap
- [x] **Milestone 1:** Core Maven Architecture, Secure Env Config & Gemini API Integration
- [x] **Milestone 2:** Stateful FIFO Sliding-Window `ChatMemory` & `AiServices` Wiring
- [ ] **Milestone 2.5:** Interactive CLI Conversation Loop (`java.util.Scanner`)
- [ ] **Milestone 3:** Local File System RAG (Retrieval-Augmented Generation) & Vector Indexing
- [ ] **Milestone 4:** Autonomous Tool Execution & Function Calling
