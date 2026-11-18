# ⚡ OJAS — Local-First Enterprise AI Agent & RAG Orchestrator

<div align="center">

```
   ██████╗      ██╗ █████╗ ███████╗
  ██╔═══██╗     ██║██╔══██╗██╔════╝
  ██║   ██║     ██║███████║███████╗
  ██║   ██║██   ██║██╔══██║╚════██║
  ╚██████╔╝╚█████╔╝██║  ██║███████║
   ╚═════╝  ╚════╝ ╚═╝  ╚═╝╚══════╝
```

**Local-First · Non-Wrapper · Autonomous · Enterprise-Grade**

*Built from scratch in Java — not another API wrapper.*

[![Java](https://img.shields.io/badge/Java-21+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.36.2-blue?style=flat-square)](https://docs.langchain4j.dev/)
[![Gemini](https://img.shields.io/badge/Google_Gemini-3.8_Flash-4285F4?style=flat-square&logo=google&logoColor=white)](https://ai.google.dev/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

</div>

---

## 🧠 What Makes Ojas Different

> **Ojas is NOT a chatbot wrapper.** Most "AI projects" on GitHub are thin HTTP wrappers around an LLM API — send a prompt, get a response, print it. That's not engineering.

Ojas is a **modular, local-first, enterprise-grade AI Agent** built from scratch in Java that orchestrates multiple autonomous subsystems:

| Feature | Wrapper Project | **Ojas** |
|:---|:---|:---|
| Architecture | Single file, single API call | 7+ modular classes across 7 packages |
| Embeddings | None, or calls a cloud API | **Local ONNX model** — runs on JVM, zero API cost |
| RAG | None | **Full pipeline**: file scan → chunk → embed → vector search |
| Tool Execution | None | **Autonomous @Tool methods**: file I/O, process execution |
| Memory | Stateless, or basic history | **FIFO sliding window** + cross-platform conversation ingestion |
| Security | API key hardcoded in source | **BYOK** via OS environment variables — zero-trust |
| Cross-LLM | Vendor locked | Ingests ChatGPT exports into Gemini context |

---

## 🏗️ System Architecture

```text
+-----------------------------------------------------------------------+
|                        USER INTERFACE / CLI                           |
|           xAI-Inspired Terminal UI (ANSI styled output)               |
+-----------------------------------+-----------------------------------+
                                    | (User Prompt / Slash Command)
                                    v
+-----------------------------------------------------------------------+
|                    OJAS ORCHESTRATION ENGINE (Java)                   |
|  +-----------------------------+  +--------------------------------+  |
|  |   Environment & Security    |  |     Agentic Memory Manager     |  |
|  |  (System.getenv / BYOK)     |  | (FIFO Sliding Window + Import) |  |
|  +-----------------------------+  +--------------------------------+  |
|                                   |                                   |
|         +-------------------------+-------------------------+         |
|         v                                                   v         |
|  +-----------------------------+  +--------------------------------+  |
|  |     Local RAG Pipeline      |  |    Autonomous Tool Router      |  |
|  | - Java NIO File Scanner     |  | - @Tool Annotated Methods      |  |
|  | - Recursive Text Chunking   |  | - File Read / Write / Patch    |  |
|  | - Local ONNX Embeddings     |  | - ProcessBuilder Execution     |  |
|  | - In-Memory Vector Store    |  | - Directory Search & Listing   |  |
|  | - Cosine Similarity Search  |  | - Compilation & Build Checks   |  |
|  +--------------+--------------+  +---------------+----------------+  |
+-----------------|-----------------------------------|-----------------+
                  | (Top-K Relevant Chunks)           | (JSON Tool Schema)
                  +-----------------+-----------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                GOOGLE GEMINI API GATEWAY (gemini-3.8-flash)           |
|           BYOK — Zero recurring cloud server costs                    |
+-----------------------------------------------------------------------+
```

---

## 📦 Project Structure

```
ojas/
├── pom.xml                              # Maven build — all dependencies declared
├── README.md                            # This file
└── src/main/java/org/ojas/
    ├── Main.java                        # Entry point — bootstraps everything
    ├── config/
    │   └── OjasConfig.java              # BYOK configuration from environment
    ├── agent/
    │   └── OjasAgent.java               # Declarative agent contract (AiServices)
    ├── cli/
    │   └── OjasConsole.java             # xAI-inspired terminal UI
    ├── tools/
    │   └── FileSystemTools.java         # @Tool methods for autonomous execution
    ├── rag/
    │   └── LocalRagPipeline.java        # Local RAG: scan → chunk → embed → store
    ├── memory/
    │   └── ConversationIngestor.java    # Cross-platform conversation import
    └── ingest/                          # (Reserved for future ingestion formats)
```

---

## 🛠️ Tech Stack

| Layer | Technology | Why |
|:---|:---|:---|
| **Language** | Java 21+ (LTS) | Enterprise standard, type-safe, production-ready |
| **Build** | Apache Maven | Declarative dependency management |
| **AI Orchestration** | [LangChain4j](https://docs.langchain4j.dev/) 0.36.2 | Decouples business logic from LLM providers |
| **LLM Engine** | Google Gemini (`gemini-2.5-flash`) | Free tier BYOK, massive context window |
| **Local Embeddings** | AllMiniLmL6V2 (ONNX) | Runs entirely on JVM — no API call, zero cost |
| **Vector Store** | InMemoryEmbeddingStore | Embedded, zero-config, cosine similarity |
| **Logging** | SLF4J Simple | Lightweight runtime telemetry |
| **JSON Parsing** | Gson | ChatGPT export / conversation ingestion |
| **Design Language** | xAI-Inspired | Dark terminal aesthetic with ANSI colors |

---

## 🚀 Getting Started

### Prerequisites

- **JDK 21+** installed ([OpenJDK](https://openjdk.org/) or any distribution)
- **Apache Maven** (bundled with IntelliJ IDEA, or install separately)
- **Google AI Studio API Key** — [Get one free](https://aistudio.google.com/)

### 1. Clone

```bash
git clone https://github.com/yaswanth1089/Ojas-agent-.git
cd Ojas-agent-
```

### 2. Set Your API Key

**Windows (PowerShell):**
```powershell
$env:GEMINI_API_KEY = "your_api_key_here"
```

**Windows (CMD):**
```cmd
set GEMINI_API_KEY=your_api_key_here
```

**Linux / macOS:**
```bash
export GEMINI_API_KEY="your_api_key_here"
```

**IntelliJ IDEA:**
Edit Run Configuration → Environment Variables → Add `GEMINI_API_KEY=your_key`

### 3. Build & Run

#### Quick Launch (Windows)
Double-click or run from PowerShell:
```powershell
.\run.ps1
```
Or from Command Prompt:
```cmd
run.bat
```

#### Run in IntelliJ IDEA
1. Open the project in IntelliJ IDEA.
2. Edit Run Configuration:
   - **Main class**: `org.ojas.Main` (or `org.example.Main`)
   - **VM options**: `--enable-native-access=ALL-UNNAMED -Djava.net.preferIPv4Stack=true`
   - **Environment variables**: `GEMINI_API_KEY=your_api_key_here`
3. Click **Run** (`Shift + F10`).

#### Run with Maven / Standalone JAR
```bash
mvn clean package -DskipTests
java --enable-native-access=ALL-UNNAMED -Djava.net.preferIPv4Stack=true -jar target/ojas-1.0-SNAPSHOT.jar
```

> **Note on Java 22+ / 27 Native Access:**
> Java 22+ restricts native code loading (`System.load`) in unnamed modules. Since the local embedding model (`AllMiniLmL6V2` via ONNX Runtime) runs in-process on the JVM, passing `--enable-native-access=ALL-UNNAMED` prevents restricted method warnings and ensures forward compatibility.

---

## 💻 Usage & Commands

Once Ojas is running, you'll see the boot splash and system status. Then interact:

```
  ▸ you › What is a HashMap in Java?
  ◆ ojas
    A HashMap is a hash-table-based implementation of the Map interface...
```

### Slash Commands

| Command | Description |
|:---|:---|
| `/rag <directory>` | Index a local directory for RAG-powered code search |
| `/ingest <file>` | Import a ChatGPT JSON export or text transcript into memory |
| `/model <name>` | Switch model (e.g., `gemini-3.8-flash`, `gemini-3.5-flash`) |
| `/status` | View system status, active model, and memory state |
| `/clear` | Clear conversation memory and start fresh |
| `/help` | Show available commands |
| `/exit` | Quit Ojas |

### Example Workflows

**1. Codebase Q&A (RAG Mode):**
```
  ▸ you › /rag C:\Projects\SchoolManagementSystem
  ✔ RAG pipeline active. Ask questions about your codebase.

  ▸ you › Where is exception handling implemented across the service classes?
  ◆ ojas
    Based on the indexed codebase, exception handling is found in:
    - StudentService.java (lines 45-62): try-catch around database operations...
```

**2. Cross-LLM Context Bridging:**
```
  ▸ you › /ingest C:\Downloads\chatgpt_export.json
  ✔ Ingested 24 messages into memory.

  ▸ you › Continue the discussion about the observer pattern from that session
```

**3. Autonomous Code Inspection:**
```
  ▸ you › Read the file C:\Projects\Calculator.java and check for bugs
  ◆ ojas
    [Uses @Tool readFile to access the file]
    Found 2 issues:
    1. Line 23: Division by zero not handled...
    2. Line 41: ArrayList should use generics...
```

---

## 🏛️ Core Architectural Concepts

### 1. Agentic Orchestration (LangChain4j AiServices)
```java
// Declarative — no HTTP boilerplate
OjasAgent ojas = AiServices.builder(OjasAgent.class)
    .chatLanguageModel(geminiModel)
    .chatMemory(slidingWindowMemory)
    .tools(new FileSystemTools())
    .contentRetriever(ragRetriever)
    .build();
```
The `AiServices` builder wires the LLM, memory, tools, and RAG into a single agent using **dynamic runtime proxies**. The interface contract (`OjasAgent`) defines _what_ the agent can do; the builder defines _how_.

### 2. Local RAG Pipeline (Non-Wrapper Architecture)
```
Directory Scan (java.nio) → Text Chunking → ONNX Embeddings (local JVM) → Vector Store → Cosine Search
```
Embeddings are computed by **AllMiniLmL6V2**, an ONNX model running entirely inside the JVM. No embedding API calls. No data leaves the machine. This is what makes Ojas a true local-first system.

### 3. Autonomous Tool Execution (@Tool)
```java
@Tool("Read the full contents of a file at the given path")
public String readFile(String filePath) { ... }

@Tool("Execute a terminal command and return its output")
public String executeCommand(String command) { ... }
```
LangChain4j's `@Tool` annotation exposes Java methods as callable functions to the LLM. Gemini outputs structured JSON function calls; the runtime executes them deterministically.

### 4. Stateful Memory (FIFO Sliding Window)
```java
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
```
Bounded sliding-window FIFO queue prevents token exhaustion. Combined with the `ConversationIngestor`, Ojas can merge external chat histories (ChatGPT exports) into its active context.

---

## 🎨 Design Language

Ojas's terminal UI is inspired by [xAI's design system](https://x.ai):

- **Near-black canvas** — the terminal's dark background IS the design surface
- **White ink** — high-contrast text for readability
- **ANSI accent colors** — sunset orange (prompts), dusk purple (system labels), cyan (agent responses)
- **Monospace uppercase eyebrows** — section headers styled after GeistMono tracked uppercase
- **Hairline dividers** — the only elevation cue, matching xAI's no-shadow philosophy
- **Pill-free, function-first** — no decorative UI; every element has a purpose

---

## 🔒 Security Model (BYOK)

Ojas uses a **Zero-Trust Bring Your Own Key** architecture:

1. API keys are **never hardcoded** in source code
2. Keys are loaded from OS environment variables at runtime (`System.getenv`)
3. The agent **fails fast** with a diagnostic error if the key is missing
4. `.env` files are in `.gitignore` — keys never enter version control
5. All RAG operations run locally — **no data leaves your machine** for embeddings

---

## 🗺️ Development Roadmap

- [x] **Phase 1:** Core Maven architecture, secure BYOK configuration, Gemini API integration
- [x] **Phase 2:** Stateful FIFO sliding-window ChatMemory & AiServices wiring
- [x] **Phase 3:** Interactive CLI with slash commands and xAI-inspired terminal UI
- [x] **Phase 4:** Local RAG pipeline — file scanning, chunking, ONNX embeddings, vector search
- [x] **Phase 5:** @Tool function calling — file I/O, process execution, directory search
- [x] **Phase 6:** Cross-platform conversation ingestion (ChatGPT JSON / text transcripts)
- [ ] **Phase 7:** Persistent vector store (SQLite-backed) for cross-session RAG
- [ ] **Phase 8:** Folder watcher (`WatchService`) for real-time file indexing
- [ ] **Phase 9:** REST API backend (Spring Boot) for web/mobile clients
- [ ] **Phase 10:** Standalone packaging & distribution

---

## 🎯 Real-World Use Cases

| Use Case | Mechanism | Value |
|:---|:---|:---|
| **Enterprise Codebase Assistant** | Local RAG over Java repositories + System Prompt | Socratic code reviewer — spots bugs without spoiling solutions |
| **Cross-LLM Context Harmonizer** | ChatGPT JSON parser + ChatMemory injection | Eliminates vendor lock-in at zero API cost |
| **Autonomous Code Review** | @Tool file reading + ProcessBuilder compilation | Inspect → patch → verify compilation autonomously |
| **Technical Documentation Generator** | File scanning + LLM summarization | Scans undocumented codebases, exports README/guides |
| **Local Build Automator** | @Tool process execution | Natural language → Maven builds, Git operations |

---

## 📄 Resume Blueprint

> **Ojas — Local-First Enterprise AI Agent & RAG Orchestrator** | *Java 21, Maven, LangChain4j, Google Gemini API*
> * **Architected a modular desktop AI agent in Java** (7+ classes, 7 packages), decoupling LLM orchestration from business logic using **LangChain4j AiServices** and the **Builder Design Pattern**.
> * **Engineered a local Retrieval-Augmented Generation (RAG) pipeline** using `java.nio` file scanning, recursive text chunking, and **in-process ONNX embeddings** (AllMiniLmL6V2) — zero external API calls for vector search.
> * **Implemented autonomous Function Calling** via LangChain4j `@Tool` annotations, enabling the agent to read/write files, execute terminal commands, and verify compilation via `ProcessBuilder`.
> * **Designed a zero-cost BYOK security architecture** with OS environment variable injection, cross-platform ChatGPT conversation ingestion, and an xAI-inspired terminal UI.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m 'feat: add my feature'`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

---

## 📜 License

This project is open source and available under the [MIT License](LICENSE).

---

<div align="center">

**Built with precision. Not a wrapper.**

*Ojas — from Sanskrit ओजस् — meaning vitality, strength, energy.*

</div>
