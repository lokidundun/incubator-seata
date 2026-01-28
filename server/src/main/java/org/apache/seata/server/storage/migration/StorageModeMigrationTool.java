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
package org.apache.seata.server.storage.migration;

import org.apache.seata.common.store.StoreMode;
import org.apache.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 * Command-line tool for storage mode migration
 *
 * Usage:
 * java StorageModeMigrationTool <source-mode> <target-mode> [source-file-path] [target-file-path]
 *
 * Examples:
 * java StorageModeMigrationTool file db /path/to/store /tmp/target
 * java StorageModeMigrationTool db redis
 * java StorageModeMigrationTool redis db
 * java StorageModeMigrationTool db raft /tmp/target
 * java StorageModeMigrationTool raft db /path/to/store
 *
 * Note: For FILE and RAFT modes, the file path must be specified
 *
 */
public class StorageModeMigrationTool {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageModeMigrationTool.class);

    private static final String USAGE =
            "Usage: java StorageModeMigrationTool <source-mode> <target-mode> [source-file-path] [target-file-path]\n\n" +
            "Supported modes: file, db, redis, raft\n\n" +
            "Examples:\n" +
            "    java StorageModeMigrationTool file db /path/to/store /tmp/target\n" +
            "    java StorageModeMigrationTool db redis\n" +
            "    java StorageModeMigrationTool redis db\n" +
            "    java StorageModeMigrationTool db raft /tmp/target\n" +
            "    java StorageModeMigrationTool raft db /path/to/store\n\n" +
            "Note: FILE and RAFT modes require specifying the file path.";

    public static void main(String[] args) {
        try {
            // Parse arguments
            if (args.length < 2) {
                System.out.println(USAGE);
                return;
            }

            StoreMode sourceMode = parseMode(args[0]);
            StoreMode targetMode = parseMode(args[1]);

            String sourceFilePath = args.length > 2 ? args[2] : null;
            String targetFilePath = args.length > 3 ? args[3] : null;

            // Validate migration
            if (!StorageModeMigrationExecutor.isMigrationSupported(sourceMode, targetMode)) {
                System.err.println("Error: Migration from " + sourceMode + " to " + targetMode + " is not supported.");
                return;
            }

            // Validate file paths for file-based modes (FILE and RAFT)
            if ((sourceMode == StoreMode.FILE || sourceMode == StoreMode.RAFT) && StringUtils.isBlank(sourceFilePath)) {
                System.err.println("Error: source-file-path is required for " + sourceMode + " mode.");
                return;
            }
            if ((targetMode == StoreMode.FILE || targetMode == StoreMode.RAFT) && StringUtils.isBlank(targetFilePath)) {
                System.err.println("Error: target-file-path is required for " + targetMode + " mode.");
                return;
            }

            // Print migration info
            System.out.println("=============================================");
            System.out.println("  Seata Storage Mode Migration Tool");
            System.out.println("=============================================");
            System.out.println("Source Mode:     " + sourceMode);
            System.out.println("Target Mode:     " + targetMode);
            System.out.println("Source File:     " + (sourceFilePath != null ? sourceFilePath : "N/A"));
            System.out.println("Target File:     " + (targetFilePath != null ? targetFilePath : "N/A"));
            System.out.println("=============================================");
            System.out.println("WARNING: This migration will overwrite target storage data.");
            System.out.println("Please ensure that the Seata server is stopped before migration.");
            System.out.println("=============================================");

            // Ask for confirmation
            System.out.print("Do you want to continue? (yes/no): ");
            Scanner scanner = new Scanner(System.in);
            String confirm = scanner.nextLine().trim().toLowerCase();

            if (!"yes".equals(confirm) && !"y".equals(confirm)) {
                System.out.println("Migration cancelled.");
                return;
            }

            // Execute migration
            System.out.println("\nStarting migration...\n");

            StorageModeMigrationExecutor executor = StorageModeMigrationExecutor.builder()
                    .sourceMode(sourceMode)
                    .targetMode(targetMode)
                    .sourceFilePath(sourceFilePath)
                    .targetFilePath(targetFilePath)
                    .build();

            MigrationResult result = executor.execute();

            // Print result
            System.out.println("\n=============================================");
            System.out.println("  Migration Result");
            System.out.println("=============================================");
            System.out.println("Status:      " + (result.isSuccess() ? "SUCCESS" : "FAILED"));
            System.out.println("Success:     " + result.getSuccessCount());
            System.out.println("Failed:      " + result.getFailCount());
            System.out.println("Skipped:     " + result.getSkippedCount());
            System.out.println("Elapsed:     " + result.getElapsedMillis() + "ms");
            System.out.println("=============================================");

        } catch (Exception e) {
            LOGGER.error("Migration failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static StoreMode parseMode(String mode) {
        try {
            return StoreMode.get(mode.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown mode: " + mode + ". Supported modes: file, db, redis, raft");
        }
    }
}
