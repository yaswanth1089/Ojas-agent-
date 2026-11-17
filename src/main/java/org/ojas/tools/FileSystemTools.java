package org.ojas.tools;

import dev.langchain4j.agent.tool.Tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  OJAS FILE SYSTEM TOOLS — @Tool annotated methods exposed to the LLM.
 *
 *  These are NOT wrapper functions that just call an API.
 *  They give the LLM autonomous access to the user's local filesystem
 *  and OS process execution — the defining feature of an agentic system.
 *
 *  LangChain4j's @Tool annotation emits a JSON function schema to the LLM.
 *  When Gemini determines a tool call is needed, it returns structured JSON,
 *  and the runtime invokes these methods deterministically.
 * ═══════════════════════════════════════════════════════════════════════
 */
public class FileSystemTools {

    // ═══════════════════════════════════════════════════════════════
    //  FILE READING
    // ═══════════════════════════════════════════════════════════════

    @Tool("Read the full contents of a file at the given absolute path. Returns the file text or an error message.")
    public String readFile(String filePath) {
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                return "ERROR: File not found — " + filePath;
            }
            if (!Files.isRegularFile(path)) {
                return "ERROR: Path is not a regular file — " + filePath;
            }
            long size = Files.size(path);
            if (size > 100_000) {
                return "ERROR: File too large (" + size + " bytes). Maximum is 100KB for safety.";
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "ERROR reading file: " + e.getMessage();
        }
    }

    @Tool("Read specific lines from a file. Provide the absolute file path, the 1-based start line, and the 1-based end line (inclusive).")
    public String readFileLines(String filePath, int startLine, int endLine) {
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) return "ERROR: File not found — " + filePath;

            List<String> allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int start = Math.max(1, startLine) - 1;
            int end = Math.min(allLines.size(), endLine);
            if (start >= end) return "ERROR: Invalid line range.";

            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(String.format("%4d | %s%n", i + 1, allLines.get(i)));
            }
            return sb.toString();
        } catch (IOException e) {
            return "ERROR reading file lines: " + e.getMessage();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  FILE WRITING
    // ═══════════════════════════════════════════════════════════════

    @Tool("Write content to a file at the given absolute path. Creates parent directories if needed. Overwrites existing content.")
    public String writeFile(String filePath, String content) {
        try {
            Path path = Path.of(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return "SUCCESS: File written — " + filePath + " (" + content.length() + " chars)";
        } catch (IOException e) {
            return "ERROR writing file: " + e.getMessage();
        }
    }

    @Tool("Append content to the end of an existing file, or create it if it does not exist.")
    public String appendToFile(String filePath, String content) {
        try {
            Path path = Path.of(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return "SUCCESS: Content appended to — " + filePath;
        } catch (IOException e) {
            return "ERROR appending to file: " + e.getMessage();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DIRECTORY LISTING
    // ═══════════════════════════════════════════════════════════════

    @Tool("List all files and directories inside the given directory path. Returns names with [DIR] or [FILE] labels.")
    public String listDirectory(String directoryPath) {
        try {
            Path dir = Path.of(directoryPath);
            if (!Files.isDirectory(dir)) {
                return "ERROR: Not a directory — " + directoryPath;
            }
            try (Stream<Path> entries = Files.list(dir)) {
                return entries.map(p -> {
                    String label = Files.isDirectory(p) ? "[DIR]  " : "[FILE] ";
                    return label + p.getFileName().toString();
                }).collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            return "ERROR listing directory: " + e.getMessage();
        }
    }

    @Tool("Search for files matching a glob pattern (e.g., '*.java') recursively inside the given directory. Returns up to 50 matching paths.")
    public String findFiles(String directoryPath, String globPattern) {
        try {
            Path dir = Path.of(directoryPath);
            if (!Files.isDirectory(dir)) return "ERROR: Not a directory — " + directoryPath;

            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
            try (Stream<Path> walk = Files.walk(dir, 10)) {
                String results = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> matcher.matches(p.getFileName()))
                        .limit(50)
                        .map(Path::toString)
                        .collect(Collectors.joining("\n"));
                return results.isEmpty() ? "No files found matching pattern: " + globPattern : results;
            }
        } catch (IOException e) {
            return "ERROR searching files: " + e.getMessage();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  PROCESS EXECUTION (Sandboxed)
    // ═══════════════════════════════════════════════════════════════

    @Tool("Execute a terminal command and return its output. Use for safe operations like compilation (javac, mvn), git status, directory listing. NEVER use for destructive commands.")
    public String executeCommand(String command) {
        // Safety: block obviously destructive commands
        String lower = command.toLowerCase().trim();
        if (lower.startsWith("rm -rf") || lower.startsWith("del /s") ||
            lower.startsWith("format") || lower.contains("shutdown") ||
            lower.contains("rmdir /s")) {
            return "BLOCKED: Destructive command refused for safety — " + command;
        }

        try {
            ProcessBuilder pb;
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("sh", "-c", command);
            }
            pb.redirectErrorStream(true);

            Process proc = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && lineCount < 200) {
                    output.append(line).append("\n");
                    lineCount++;
                }
            }

            int exitCode = proc.waitFor();
            return "Exit Code: " + exitCode + "\n" + output;
        } catch (IOException | InterruptedException e) {
            return "ERROR executing command: " + e.getMessage();
        }
    }
}
