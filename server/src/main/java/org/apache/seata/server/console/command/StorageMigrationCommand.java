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
package org.apache.seata.server.console.command;

import org.apache.seata.common.store.StoreMode;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.server.console.service.StorageMigrationConsoleService;
import org.apache.seata.server.console.vo.MigrationResultVO;
import org.apache.seata.server.console.vo.StoreModeVO;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;

/**
 * Storage Migration Console Commands
 *
 * Usage:
 *   storage-mode show                    # Show current storage mode
 *   storage-mode list                    # List all supported storage modes
 *   storage-mode status                  # Show migration status
 *   storage-mode migrate --from DB --to REDIS   # Start migration
 *   storage-mode pause                   # Pause migration
 *   storage-mode resume                  # Resume migration
 *   storage-mode rollback                # Rollback migration
 *
 * @author xingfudeshi@gmail.com
 */
@ShellComponent
public class StorageMigrationCommand {

    private final StorageMigrationConsoleService consoleService;

    public StorageMigrationCommand(StorageMigrationConsoleService consoleService) {
        this.consoleService = consoleService;
    }

    /**
     * Show current storage mode
     */
    @ShellMethod(key = {"storage-mode show", "storage-mode current"}, value = "Show current storage mode")
    public String showCurrentMode() {
        StoreModeVO currentMode = consoleService.getCurrentMode();
        return String.format("Current Storage Mode: %s", currentMode.getMode());
    }

    /**
     * List all supported storage modes
     */
    @ShellMethod(key = {"storage-mode list", "storage-mode modes"}, value = "List all supported storage modes")
    public String listSupportedModes() {
        List<StoreModeVO> modes = consoleService.listSupportedModes();
        StringBuilder sb = new StringBuilder("Supported Storage Modes:\n");
        for (StoreModeVO mode : modes) {
            sb.append(String.format("  - %s: %s\n", mode.getMode(), mode.getDescription()));
        }
        return sb.toString();
    }

    /**
     * Show migration status
     */
    @ShellMethod(key = {"storage-mode status", "storage-mode progress"}, value = "Show migration status and progress")
    public String showMigrationStatus() {
        MigrationResultVO status = consoleService.getMigrationStatus();
        StringBuilder sb = new StringBuilder();
        sb.append("=== Storage Migration Status ===\n\n");

        if (status.isMigrating()) {
            sb.append(String.format("Status: %s\n", status.isPaused() ? "PAUSED" : "MIGRATING"));
            sb.append(String.format("Progress: %.2f%% (%d/%d)\n",
                    status.getProgressPercentage(),
                    status.getMigratedSessions(),
                    status.getTotalSessions()));
            sb.append(String.format("Success: %d\n", status.getMigratedSessions()));
            sb.append(String.format("Failed: %d\n", status.getFailedSessions()));
            if (status.getCurrentXid() != null) {
                sb.append(String.format("Current: %s\n", status.getCurrentXid()));
            }
            if (status.getElapsedTime() > 0) {
                sb.append(String.format("Elapsed: %d seconds\n", status.getElapsedTime() / 1000));
            }
        } else {
            sb.append("Status: IDLE (No migration in progress)\n");
        }

        return sb.toString();
    }

    /**
     * Start migration
     */
    @ShellMethod(key = {"storage-mode migrate", "storage-mode start"}, value = "Start storage mode migration")
    public String startMigration(
            @ShellOption(value = {"--from", "-f"}, help = "Source storage mode (file, db, redis, raft)") String fromMode,
            @ShellOption(value = {"--to", "-t"}, help = "Target storage mode (file, db, redis, raft)") String toMode,
            @ShellOption(defaultValue = "false", help = "Skip confirmation prompt") boolean force) {

        StoreMode source = StoreMode.get(fromMode);
        StoreMode target = StoreMode.get(toMode);

        if (source == null || target == null) {
            return "Error: Invalid storage mode. Use: file, db, redis, or raft";
        }

        if (source == target) {
            return "Error: Source and target modes cannot be the same";
        }

        // Show warning
        StringBuilder sb = new StringBuilder();
        sb.append("=== Storage Migration Warning ===\n\n");
        sb.append(String.format("You are about to migrate from [%s] to [%s]\n\n", source.getName(), target.getName()));

        if (!force) {
            sb.append("WARNING: \n");
            sb.append("1. Please BACKUP your data before migration\n");
            sb.append("2. It is recommended to STOP new transactions during migration\n");
            sb.append("3. Migration may take a long time depending on data size\n\n");
            sb.append("Do you want to continue? (Type 'yes' to confirm): ");
            return sb.toString();
        }

        // Execute migration
        return executeMigration(source, target);
    }

    /**
     * Execute migration internally
     */
    private String executeMigration(StoreMode source, StoreMode target) {
        try {
            MigrationResultVO result = consoleService.migrate(source, target);

            StringBuilder sb = new StringBuilder();
            sb.append("=== Migration Started ===\n\n");
            sb.append(String.format("Source: %s\n", source.getName()));
            sb.append(String.format("Target: %s\n", target.getName()));
            sb.append(String.format("Total Sessions: %d\n\n", result.getTotalSessions()));

            // Show real-time progress
            sb.append("Monitoring progress (Ctrl+C to pause)...\n");

            return sb.toString();

        } catch (TransactionException e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Pause migration
     */
    @ShellMethod(key = {"storage-mode pause", "storage-mode stop"}, value = "Pause current migration")
    public String pauseMigration() {
        try {
            consoleService.pause();
            return "Migration paused. Use 'storage-mode resume' to continue.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Resume migration
     */
    @ShellMethod(key = {"storage-mode resume", "storage-mode continue"}, value = "Resume paused migration")
    public String resumeMigration() {
        try {
            consoleService.resume();
            return "Migration resumed.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Rollback migration
     */
    @ShellMethod(key = {"storage-mode rollback", "storage-mode undo"}, value = "Rollback current migration")
    public String rollbackMigration() {
        try {
            consoleService.rollback();
            return "Migration rolled back. Target store data has been cleaned up.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Check migration health
     */
    @ShellMethod(key = {"storage-mode health", "storage-mode check"}, value = "Check migration health and prerequisites")
    public String checkHealth() {
        try {
            StringBuilder sb = new StringBuilder("=== Migration Health Check ===\n\n");

            // Check current mode
            StoreModeVO currentMode = consoleService.getCurrentMode();
            sb.append(String.format("Current Mode: %s [OK]\n", currentMode.getMode()));

            // Check target stores availability
            for (StoreMode mode : StoreMode.values()) {
                if (mode == consoleService.getCurrentMode().getStoreMode()) {
                    continue;
                }
                boolean available = consoleService.isStoreAvailable(mode);
                sb.append(String.format("%s Store: %s [%s]\n",
                        mode.getName(),
                        available ? "Available" : "Not Configured",
                        available ? "OK" : "WARN"));
            }

            return sb.toString();
        } catch (Exception e) {
            return "Health check failed: " + e.getMessage();
        }
    }

    /**
     * Show migration help
     */
    @ShellMethod(key = {"storage-mode help", "storage-mode --help"}, value = "Show migration help")
    public String showHelp() {
        return """
            === Storage Migration Commands ===

            Show Commands:
              storage-mode show              Show current storage mode
              storage-mode list              List all supported storage modes
              storage-mode status            Show migration status and progress

            Migration Commands:
              storage-mode migrate --from FILE --to DB  Start migration
              storage-mode pause              Pause current migration
              storage-mode resume             Resume paused migration
              storage-mode rollback           Rollback migration

            Utility Commands:
              storage-mode health             Check migration prerequisites
              storage-mode help               Show this help

            Examples:
              storage-mode migrate --from DB --to REDIS
              storage-mode migrate --from FILE --to DB --force
              storage-mode status
              storage-mode pause
              storage-mode resume

            Supported Modes: file, db, redis, raft

            Note: Before migration, please:
              1. Backup your data
              2. Stop new transactions (recommended)
              3. Verify target store connectivity
            """;
    }
}
