/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.console;

import org.apache.seata.common.store.StoreMode;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.server.console.service.StorageMigrationConsoleService;
import org.apache.seata.server.console.vo.MigrationResultVO;
import org.apache.seata.server.console.vo.StoreModeVO;
import org.apache.seata.server.storage.migration.MigrationProgress;
import org.apache.seata.server.storage.migration.StorageMigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Seata Storage Migration Interactive Console
 *
 * A command-line interface for storage mode migration operations.
 *
 * Usage:
 *   java -jar seata-server.jar --store-migration
 *
 * Features:
 *   - Interactive command prompts
 *   - Real-time progress monitoring
 *   - Migration control (pause/resume/rollback)
 *   - Status display
 *
 * @author xingfudeshi@gmail.com
 */
public class SeataStorageMigrationConsole {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataStorageMigrationConsole.class);

    private static final String BANNER = """
            =========================================================================
                           Seata Storage Migration Console
            =========================================================================

            Commands:
              show                    Show current storage mode
              list                    List all supported storage modes
              status                  Show migration status
              migrate <from> <to>     Start migration (e.g., migrate DB REDIS)
              pause                   Pause current migration
              resume                  Resume paused migration
              rollback                Rollback migration
              health                  Check system health
              help                    Show this help
              exit                    Exit console

            Examples:
              migrate DB REDIS        Migrate from Database to Redis
              migrate FILE DB         Migrate from File to Database
              migrate REDIS FILE      Migrate from Redis to File
              migrate DB RAFT         Migrate from Database to Raft

            =========================================================================
            """;

    private final StorageMigrationConsoleService consoleService;
    private final Scanner scanner;
    private volatile boolean running = true;

    public SeataStorageMigrationConsole() {
        this.consoleService = new StorageMigrationConsoleService(StorageMigrationService.getInstance());
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        System.out.println(BANNER);

        SeataStorageMigrationConsole console = new SeataStorageMigrationConsole();
        console.run();
    }

    public void run() {
        printWelcome();

        while (running) {
            try {
                printPrompt();
                String command = scanner.nextLine().trim().toLowerCase();

                if (command.isEmpty()) {
                    continue;
                }

                processCommand(command);

            } catch (Exception e) {
                System.out.println("\nError: " + e.getMessage());
                LOGGER.error("Console error", e);
            }
        }

        System.out.println("\nGoodbye!");
    }

    private void printWelcome() {
        System.out.println("\nWelcome to Seata Storage Migration Console!");
        System.out.println("Current storage mode: " + consoleService.getCurrentMode().getMode());
        System.out.println("\nType 'help' for available commands.\n");
    }

    private void printPrompt() {
        System.out.print("\n[seata-migration] ");
        System.out.flush();
    }

    private void processCommand(String command) {
        String[] parts = command.split("\\s+");
        String action = parts[0];

        switch (action) {
            case "show":
                cmdShow();
                break;
            case "list":
            case "modes":
                cmdList();
                break;
            case "status":
            case "progress":
                cmdStatus();
                break;
            case "migrate":
            case "start":
                if (parts.length >= 3) {
                    cmdMigrate(parts[1], parts[2]);
                } else {
                    System.out.println("Usage: migrate <from> <to>");
                    System.out.println("Example: migrate DB REDIS");
                }
                break;
            case "pause":
            case "stop":
                cmdPause();
                break;
            case "resume":
            case "continue":
                cmdResume();
                break;
            case "rollback":
            case "undo":
                cmdRollback();
                break;
            case "health":
            case "check":
                cmdHealth();
                break;
            case "help":
            case "--help":
            case "-h":
                cmdHelp();
                break;
            case "exit":
            case "quit":
            case "q":
                running = false;
                break;
            default:
                System.out.println("Unknown command: " + action);
                System.out.println("Type 'help' for available commands.");
        }
    }

    private void cmdShow() {
        StoreModeVO current = consoleService.getCurrentMode();
        System.out.println("\n=== Current Storage Mode ===");
        System.out.println("Mode: " + current.getMode());
        System.out.println("Name: " + current.getName());
        System.out.println("Description: " + current.getDescription());
    }

    private void cmdList() {
        List<StoreModeVO> modes = consoleService.listSupportedModes();
        System.out.println("\n=== Supported Storage Modes ===");
        for (StoreModeVO mode : modes) {
            String status = mode.isAvailable() ? "[OK]" : "[Not Configured]";
            System.out.println(String.format("  %-8s %-20s %s %s",
                    mode.getMode(),
                    mode.getName(),
                    status,
                    mode.getDescription()));
        }
    }

    private void cmdStatus() {
        MigrationResultVO status = consoleService.getMigrationStatus();

        System.out.println("\n=== Migration Status ===");

        if (status.isMigrating()) {
            System.out.println("Status: " + (status.isPaused() ? "PAUSED" : "MIGRATING"));
            System.out.println("Progress: " + String.format("%.2f%%", status.getProgressPercentage()));
            System.out.println("Sessions: " + status.getMigratedSessions() + "/" + status.getTotalSessions());
            System.out.println("Success: " + status.getMigratedSessions());
            System.out.println("Failed: " + status.getFailedSessions());

            if (status.getCurrentXid() != null) {
                System.out.println("Current: " + status.getCurrentXid());
            }
            if (status.getElapsedTime() > 0) {
                System.out.println("Elapsed: " + (status.getElapsedTime() / 1000) + "s");
            }
        } else {
            System.out.println("Status: IDLE (No migration in progress)");
        }

        if (status.getErrorMessage() != null) {
            System.out.println("Error: " + status.getErrorMessage());
        }
    }

    private void cmdMigrate(String fromMode, String toMode) {
        StoreMode source = StoreMode.get(fromMode.toUpperCase());
        StoreMode target = StoreMode.get(toMode.toUpperCase());

        if (source == null) {
            System.out.println("Error: Unknown source mode: " + fromMode);
            System.out.println("Available modes: FILE, DB, REDIS, RAFT");
            return;
        }

        if (target == null) {
            System.out.println("Error: Unknown target mode: " + toMode);
            System.out.println("Available modes: FILE, DB, REDIS, RAFT");
            return;
        }

        if (source == target) {
            System.out.println("Error: Source and target modes cannot be the same");
            return;
        }

        // Show warning
        System.out.println("\n=== Migration Warning ===");
        System.out.println("You are about to migrate from [" + source.getName() + "] to [" + target.getName() + "]");
        System.out.println("\nPlease ensure:");
        System.out.println("  1. Data has been backed up");
        System.out.println("  2. Target store is properly configured");
        System.out.println("  3. New transactions are stopped (recommended)");
        System.out.print("\nContinue? (yes/no): ");

        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!"yes".equals(confirm) && !"y".equals(confirm)) {
            System.out.println("Migration cancelled.");
            return;
        }

        // Execute migration
        try {
            System.out.println("\nStarting migration...");
            MigrationResultVO result = consoleService.migrate(source, target);

            if (result.isMigrating() || "IN_PROGRESS".equals(result.getStatus())) {
                System.out.println("Migration started successfully!");
                System.out.println("Total sessions to migrate: " + result.getTotalSessions());
                System.out.println("\nMonitoring progress (Press 's' to check status, 'p' to pause):");

                monitorMigrationInteractive();
            } else {
                System.out.println("Migration completed with status: " + result.getStatus());
            }

        } catch (TransactionException e) {
            System.out.println("Migration failed: " + e.getMessage());
            LOGGER.error("Migration error", e);
        }
    }

    private void monitorMigrationInteractive() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);
                    if (!running) break;

                    MigrationResultVO status = consoleService.getMigrationStatus();

                    if (!status.isMigrating()) {
                        System.out.println("\n=== Migration Completed ===");
                        System.out.println("Status: " + status.getStatus());
                        System.out.println("Success: " + status.getMigratedSessions());
                        System.out.println("Failed: " + status.getFailedSessions());
                        break;
                    }

                    if (!status.isPaused()) {
                        System.out.printf("\rProgress: %.2f%% (%d/%d) | Success: %d | Failed: %d",
                                status.getProgressPercentage(),
                                status.getMigratedSessions() + status.getFailedSessions(),
                                status.getTotalSessions(),
                                status.getMigratedSessions(),
                                status.getFailedSessions());
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();

        // Wait for user input
        while (running) {
            try {
                if (System.in.available() > 0) {
                    String input = scanner.nextLine().trim().toLowerCase();
                    if ("p".equals(input) || "pause".equals(input)) {
                        consoleService.pause();
                        System.out.println("\nMigration paused. Type 'resume' to continue.");
                    } else if ("s".equals(input) || "status".equals(input)) {
                        cmdStatus();
                    }
                }
                Thread.sleep(100);
            } catch (IOException | InterruptedException e) {
                break;
            }
        }
    }

    private void cmdPause() {
        try {
            consoleService.pause();
            System.out.println("Migration paused.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void cmdResume() {
        try {
            consoleService.resume();
            System.out.println("Migration resumed.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void cmdRollback() {
        System.out.print("Are you sure you want to rollback? This will delete migrated data. (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if ("yes".equals(confirm) || "y".equals(confirm)) {
            try {
                consoleService.rollback();
                System.out.println("Migration rolled back successfully.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else {
            System.out.println("Rollback cancelled.");
        }
    }

    private void cmdHealth() {
        System.out.println("\n=== Health Check ===");

        // Check current mode
        StoreModeVO current = consoleService.getCurrentMode();
        System.out.println("Current Mode: " + current.getMode() + " [OK]");

        // Check target stores
        for (StoreMode mode : StoreMode.values()) {
            if (mode == consoleService.getCurrentStoreMode()) {
                continue;
            }
            boolean available = consoleService.isStoreAvailable(mode);
            String status = available ? "[OK]" : "[WARN]";
            System.out.println(String.format("%s Store: %s %s",
                    mode.getName(),
                    available ? "Available" : "Not Configured",
                    status));
        }

        // Check migration status
        MigrationResultVO status = consoleService.getMigrationStatus();
        System.out.println("\nMigration Status: " + (status.isMigrating() ? "Running" : "Idle"));
    }

    private void cmdHelp() {
        System.out.println("""
                =========================================================================
                                   Help - Storage Migration Commands
                =========================================================================

                Show Commands:
                  show                    Show current storage mode
                  list                    List all supported storage modes
                  status                  Show migration status and progress

                Migration Commands:
                  migrate <from> <to>     Start migration (e.g., migrate DB REDIS)
                  migrate FILE DB         Migrate from File to Database
                  migrate DB REDIS        Migrate from Database to Redis

                Control Commands:
                  pause                   Pause current migration
                  resume                  Resume paused migration
                  rollback                Rollback migration

                Utility Commands:
                  health                  Check system health
                  help                    Show this help
                  exit                    Exit console

                Supported Modes: FILE, DB, REDIS, RAFT

                =========================================================================
                Pre-Migration Checklist:
                  [ ] Backup source data
                  [ ] Verify target store configuration
                  [ ] Stop new transactions (recommended)
                  [ ] Notify stakeholders

                During Migration:
                  - Use 'pause' to temporarily stop
                  - Use 'status' to check progress
                  - Use 'rollback' if issues occur

                Post-Migration:
                  - Update store.mode configuration
                  - Restart Seata Server
                  - Verify application functionality

                =========================================================================
                """);
    }
}
