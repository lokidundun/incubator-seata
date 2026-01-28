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
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.migration.source.DBMigrationSource;
import org.apache.seata.server.storage.migration.source.FileMigrationSource;
import org.apache.seata.server.storage.migration.source.RaftMigrationSource;
import org.apache.seata.server.storage.migration.source.RedisMigrationSource;
import org.apache.seata.server.storage.migration.target.DBMigrationTarget;
import org.apache.seata.server.storage.migration.target.FileMigrationTarget;
import org.apache.seata.server.storage.migration.target.RaftMigrationTarget;
import org.apache.seata.server.storage.migration.target.RedisMigrationTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Storage mode migration executor
 * Orchestrates the migration process between different storage modes
 *
 * Supported migrations:
 * - FILE -> DB, REDIS, RAFT
 * - DB -> FILE, REDIS, RAFT
 * - REDIS -> FILE, DB, RAFT
 * - RAFT -> FILE, DB, REDIS
 *
 * Note: RAFT mode migration requires specifying the file path for data directory
 *
 */
public class StorageModeMigrationExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageModeMigrationExecutor.class);

    private MigrationSource source;
    private MigrationTarget target;
    private final String sourceFilePath;
    private final String targetFilePath;

    /**
     * Creates a migration executor
     *
     * @param sourceMode     source storage mode
     * @param targetMode     target storage mode
     * @param sourceFilePath file path for file-based source (FILE or RAFT mode)
     * @param targetFilePath file path for file-based target (FILE or RAFT mode)
     */
    public StorageModeMigrationExecutor(StoreMode sourceMode, StoreMode targetMode,
                                         String sourceFilePath, String targetFilePath) {
        this.sourceFilePath = sourceFilePath;
        this.targetFilePath = targetFilePath;
        initializeSource(sourceMode);
        initializeTarget(targetMode);
    }

    private void initializeSource(StoreMode mode) {
        switch (mode) {
            case FILE:
                this.source = new FileMigrationSource(sourceFilePath);
                break;
            case DB:
                this.source = new DBMigrationSource();
                break;
            case REDIS:
                this.source = new RedisMigrationSource();
                break;
            case RAFT:
                this.source = new RaftMigrationSource(sourceFilePath);
                break;
            default:
                throw new IllegalArgumentException("Unknown source mode: " + mode);
        }
        LOGGER.info("Initialized migration source: {}", mode);
    }

    private void initializeTarget(StoreMode mode) {
        switch (mode) {
            case FILE:
                this.target = new FileMigrationTarget(targetFilePath);
                break;
            case DB:
                this.target = new DBMigrationTarget();
                break;
            case REDIS:
                this.target = new RedisMigrationTarget();
                break;
            case RAFT:
                this.target = new RaftMigrationTarget(targetFilePath);
                break;
            default:
                throw new IllegalArgumentException("Unknown target mode: " + mode);
        }
        LOGGER.info("Initialized migration target: {}", mode);
    }

    /**
     * Executes the migration
     *
     * @return migration result
     */
    public MigrationResult execute() {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Starting migration from {} to {}",
                source.getSourceMode(), target.getTargetMode());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        try {
            for (GlobalSession session : source.readAllGlobalSessions()) {
                try {
                    if (isMigratable(session)) {
                        if (target.writeGlobalSession(session)) {
                            successCount.incrementAndGet();
                            if (successCount.get() % 1000 == 0) {
                                LOGGER.info("Migrated {} sessions...", successCount.get());
                            }
                        } else {
                            failCount.incrementAndGet();
                            LOGGER.warn("Failed to migrate session: xid={}", session.getXid());
                        }
                    } else {
                        skippedCount.incrementAndGet();
                        LOGGER.debug("Skipped session: xid={}", session.getXid());
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    LOGGER.error("Error migrating session: xid={}", session.getXid(), e);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("Migration completed. Success: {}, Failed: {}, Skipped: {}, Elapsed: {}ms",
                    successCount.get(), failCount.get(), skippedCount.get(), elapsed);

            return MigrationResult.builder()
                    .successCount(successCount.get())
                    .failCount(failCount.get())
                    .skippedCount(skippedCount.get())
                    .elapsedMillis(elapsed)
                    .success(failCount.get() == 0)
                    .build();

        } finally {
            close();
        }
    }

    private boolean isMigratable(GlobalSession session) {
        // Skip sessions that are already finished and have been cleaned up
        if (session.getStatus() == org.apache.seata.core.model.GlobalStatus.Finished
                || session.getStatus() == org.apache.seata.core.model.GlobalStatus.Committed
                || session.getStatus() == org.apache.seata.core.model.GlobalStatus.Rollbacked) {
            // Check if it's an old completed session that should be skipped
            long currentTime = System.currentTimeMillis();
            if (currentTime - session.getBeginTime() > 24 * 60 * 60 * 1000) {
                // Skip sessions older than 24 hours that are already completed
                return false;
            }
        }
        return true;
    }

    /**
     * Closes the migration resources
     */
    public void close() {
        if (source != null) {
            source.close();
        }
        if (target != null) {
            target.close();
        }
    }

    /**
     * Validates that the migration path is supported
     *
     * @param sourceMode source mode
     * @param targetMode target mode
     * @return true if supported
     */
    public static boolean isMigrationSupported(StoreMode sourceMode, StoreMode targetMode) {
        if (sourceMode == targetMode) {
            return false; // Same mode, no migration needed
        }
        // All modes (FILE, DB, REDIS, RAFT) are now supported
        return true;
    }

    /**
     * Creates a builder for migration configuration
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private StoreMode sourceMode;
        private StoreMode targetMode;
        private String sourceFilePath;
        private String targetFilePath;

        public Builder sourceMode(StoreMode sourceMode) {
            this.sourceMode = sourceMode;
            return this;
        }

        public Builder targetMode(StoreMode targetMode) {
            this.targetMode = targetMode;
            return this;
        }

        public Builder sourceFilePath(String sourceFilePath) {
            this.sourceFilePath = sourceFilePath;
            return this;
        }

        public Builder targetFilePath(String targetFilePath) {
            this.targetFilePath = targetFilePath;
            return this;
        }

        public StorageModeMigrationExecutor build() {
            if (sourceMode == null || targetMode == null) {
                throw new IllegalArgumentException("Source and target modes must be specified");
            }
            if (!isMigrationSupported(sourceMode, targetMode)) {
                throw new IllegalArgumentException(
                        String.format("Migration from %s to %s is not supported", sourceMode, targetMode));
            }
            // FILE and RAFT modes require file path
            if ((sourceMode == StoreMode.FILE || sourceMode == StoreMode.RAFT) && sourceFilePath == null) {
                throw new IllegalArgumentException("sourceFilePath is required for FILE or RAFT mode");
            }
            if ((targetMode == StoreMode.FILE || targetMode == StoreMode.RAFT) && targetFilePath == null) {
                throw new IllegalArgumentException("targetFilePath is required for FILE or RAFT mode");
            }
            return new StorageModeMigrationExecutor(sourceMode, targetMode, sourceFilePath, targetFilePath);
        }
    }
}
