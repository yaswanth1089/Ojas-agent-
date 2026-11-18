package org.ojas.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.ojas.config.OjasConfig;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  LOCAL RAG PIPELINE — The core that makes Ojas a non-wrapper project.
 *
 *  This pipeline:
 *   1. Scans a local directory using Java NIO (java.nio.file)
 *   2. Reads supported source files (.java, .md, .txt, .json, .xml, .yaml, .py, .js, .ts)
 *   3. Splits them into overlapping semantic chunks (configurable size/overlap)
 *   4. Computes vector embeddings LOCALLY using AllMiniLmL6V2 (runs on JVM, no API call)
 *   5. Stores vectors in an in-memory EmbeddingStore for cosine similarity search
 *
 *  The embedding model runs entirely on the user's machine.
 *  No data ever leaves the local system for RAG operations.
 * ═══════════════════════════════════════════════════════════════════════
 */
public class LocalRagPipeline {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            ".java", ".md", ".txt", ".json", ".xml", ".yaml", ".yml",
            ".py", ".js", ".ts", ".html", ".css", ".sql", ".properties",
            ".gradle", ".kt", ".scala", ".rs", ".go", ".c", ".cpp", ".h",
            ".sh", ".bat", ".cmd", ".csv", ".log", ".cfg", ".toml"
    );

    private static final long MAX_FILE_SIZE = 200_000; // 200KB per file

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final int chunkSize;
    private final int chunkOverlap;

    public LocalRagPipeline(EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore) {
        this(embeddingModel, embeddingStore,
                OjasConfig.DEFAULT_RAG_CHUNK_SIZE,
                OjasConfig.DEFAULT_RAG_CHUNK_OVERLAP);
    }

    public LocalRagPipeline(EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore,
                            int chunkSize, int chunkOverlap) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * Ingest all supported files from a directory tree into the vector store.
     *
     * @param directoryPath root directory to scan
     * @return number of files successfully ingested
     */
    public int ingestDirectory(String directoryPath) {
        Path root = Path.of(directoryPath);
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Not a directory: " + directoryPath);
        }

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(chunkSize, chunkOverlap))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        AtomicInteger count = new AtomicInteger(0);

        try (Stream<Path> walk = Files.walk(root, 15)) {
            walk.filter(Files::isRegularFile)
                .filter(this::isNotInIgnoredDir)
                .filter(this::isSupportedFile)
                .filter(this::isNotTooLarge)
                .forEach(file -> {
                    try {
                        String content = Files.readString(file, StandardCharsets.UTF_8);
                        if (content.isBlank()) return;

                        Metadata metadata = Metadata.from("source", file.toAbsolutePath().toString());
                        metadata.put("filename", file.getFileName().toString());
                        metadata.put("extension", getExtension(file));

                        Document doc = Document.from(content, metadata);
                        ingestor.ingest(doc);
                        count.incrementAndGet();
                    } catch (IOException e) {
                        System.err.println("  ⚠ Skipped (IO error): " + file + " — " + e.getMessage());
                    } catch (Exception e) {
                        System.err.println("  ⚠ Skipped (parse error): " + file + " — " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory: " + directoryPath, e);
        }

        return count.get();
    }

    /**
     * Ingest a single file into the vector store.
     */
    public boolean ingestFile(String filePath) {
        Path file = Path.of(filePath);
        if (!Files.isRegularFile(file)) return false;

        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (content.isBlank()) return false;

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(chunkSize, chunkOverlap))
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            Metadata metadata = Metadata.from("source", file.toAbsolutePath().toString());
            metadata.put("filename", file.getFileName().toString());
            Document doc = Document.from(content, metadata);
            ingestor.ingest(doc);
            return true;
        } catch (Exception e) {
            System.err.println("  ⚠ Failed to ingest: " + filePath + " — " + e.getMessage());
            return false;
        }
    }

    private boolean isNotInIgnoredDir(Path path) {
        for (Path part : path) {
            String p = part.toString().toLowerCase();
            if (p.equals(".git") || p.equals("target") || p.equals(".idea") ||
                p.equals("node_modules") || p.equals(".gradle") || p.equals("bin")) {
                return false;
            }
        }
        return true;
    }

    private boolean isSupportedFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private boolean isNotTooLarge(Path file) {
        try { return Files.size(file) <= MAX_FILE_SIZE; }
        catch (IOException e) { return false; }
    }

    private String getExtension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot) : "";
    }
}
