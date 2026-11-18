package org.example;

/**
 * ═══════════════════════════════════════════════════════════════════════
 *  OJAS — Application Entry Point & IntelliJ Run Compatibility Bridge
 *
 *  Provides backwards-compatibility with existing IDE Run Configurations
 *  targeting `org.example.Main`, delegating immediately to the enterprise
 *  agent orchestrator in {@link org.ojas.Main}.
 * ═══════════════════════════════════════════════════════════════════════
 */
public class Main {
    public static void main(String[] args) {
        org.ojas.Main.main(args);
    }
}
