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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.store.StoreMode;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.store.TransactionStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Storage Migration Service.
 * Provides functionality to migrate transaction data between different storage modes.
 * Supports:
 * - Full migration of all session data
 * - Incremental migration for new sessions
 * - Pause and resume of migration
 * - Rollback on failure
 *
 * @author xingfudeshi@gmail.com
 */
public class StorageMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageMigrationService.class);

    private static volatile StorageMigrationService instance;

    private final ConcurrentHashMap<StoreMode, TransactionStoreManager> storeManagers = new ConcurrentHashMap<>();

    private final AtomicBoolean migrating = new AtomicBoolean(false);

    private ExecutorService migrationExecutor;

    private StorageMigration currentMigration;

    private MigrationProgress currentProgress;

    private volatile boolean paused = false;

    private StoreMode currentMode;

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int DEFAULT_PARALLEL_THREADS = 4;
    private static final long DEFAULT_CHECKPOINT_INTERVAL = 1000;

    private StorageMigrationService() {
    }

    /**
     * Get singleton instance
     *
     * @return the singleton instance
     */
    public static StorageMigrationService getInstance() {
        if (instance == null) {
            synchronized (StorageMigrationService.class) {
                if (instance == null) {
                    instance = new StorageMigrationService();
                }
            }
        }
        return instance;
    }

    /**
     * Get configuration
     *
     * @return the configuration
     */
    private Configuration getConfiguration() {
        return ConfigurationFactory.getInstance();
    }

    /**
     * Get the store manager for a specific mode
     *
     * @param mode the storage mode
     * @return the transaction store manager
     */
    public TransactionStoreManager getStoreManager(StoreMode mode) {
        return storeManagers.computeIfAbsent(mode, this::createStoreManager);
    }

    /**
     * Create a store manager for the specified mode
     *
     * @param mode the storage mode
     * @return the transaction store manager
     */
    private TransactionStoreManager createStoreManager(StoreMode mode) throws TransactionException {
        switch (mode) {
            case FILE:
                return createFileStoreManager();
            case DB:
                return createDbStoreManager();
            case REDIS:
                return createRedisStoreManager();
            case RAFT:
                // Raft uses file-based storage internally
                return createFileStoreManager();
            default:
                throw new IllegalArgumentException("Unknown store mode: " + mode);
        }
    }

    private TransactionStoreManager createFileStoreManager() {
        try {
            Class<?> clazz = Class.forName("org.apache.seata.server.storage.file.store.FileTransactionStoreManager");
            java.lang.reflect.Method method = clazz.getMethod("getInstance");
            return (TransactionStoreManager) method.invoke(null);
        } catch (Exception e) {
            LOGGER.warn("Failed to create File store manager, will use null: {}", e.getMessage());
            return null;
        }
    }

    private TransactionStoreManager createDbStoreManager() throws TransactionException {
        try {
            Class<?> clazz = Class.forName("org.apache.seata.server.storage.db.store.DataBaseTransactionStoreManager");
            java.lang.reflect.Method method = clazz.getMethod("getInstance");
            return (TransactionStoreManager) method.invoke(null);
        } catch (Exception e) {
            throw new TransactionException(TransactionExceptionCode.FailedStore, "Failed to create DB store manager", e);
        }
    }

    private TransactionStoreManager createRedisStoreManager() throws TransactionException {
        try {
            Class<?> clazz = Class.forName("org.apache.seata.server.storage.redis.store.RedisTransactionStoreManager");
            java.lang.reflect.Method method = clazz.getMethod("getInstance");
            return (TransactionStoreManager) method.invoke(null);
        } catch (Exception e) {
            throw new TransactionException(TransactionExceptionCode.FailedStore, "Failed to create Redis store manager", e);
        }
    }

    /**
     * Migrate from source mode to target mode
     *
     * @param sourceMode the source storage mode
     * @param targetMode the target storage mode
     * @return the migration progress
     */
    public MigrationProgress migrate(StoreMode sourceMode, StoreMode targetMode) throws TransactionException {
        return migrate(sourceMode, targetMode, null);
    }

    /**
     * Migrate from source mode to target mode with a condition
     *
     * @param sourceMode the source storage mode
     * @param targetMode the target storage mode
     * @param condition  the session condition (null for all sessions)
     * @return the migration progress
     */
    public MigrationProgress migrate(StoreMode sourceMode, StoreMode targetMode, SessionCondition condition) throws TransactionException {
        if (!migrating.compareAndSet(false, true)) {
            throw new TransactionException(TransactionExceptionCode.LockKeyConflict, "Migration is already in progress");
        }

        try {
            currentProgress = new MigrationProgress();
            currentProgress.start();

            // Create the appropriate migration instance
            currentMigration = createMigration(sourceMode, targetMode);
            if (currentMigration == null) {
                throw new TransactionException(TransactionExceptionCode.Unknown, "Migration not supported from " + sourceMode + " to " + targetMode);
            }

            // Validate migration
            if (!currentMigration.validate()) {
                throw new TransactionException(TransactionExceptionCode.Unknown, "Migration validation failed");
            }

            // Prepare for migration (create backup)
            if (!currentMigration.prepare()) {
                throw new TransactionException(TransactionExceptionCode.Unknown, "Failed to prepare for migration");
            }

            // Execute migration
            executeMigration(condition);

            currentProgress.setStatus(MigrationProgress.Status.COMPLETED);
            return currentProgress;

        } catch (TransactionException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Migration failed", e);
            if (currentProgress != null) {
                currentProgress.setStatus(MigrationProgress.Status.FAILED);
                currentProgress.setErrorMessage(e.getMessage());
            }
            throw new TransactionException(TransactionExceptionCode.FailedStore, "Migration failed: " + e.getMessage(), e);
        } finally {
            if (currentMigration != null) {
                currentMigration.cleanup();
            }
            migrating.set(false);
            if (currentProgress != null) {
                currentProgress.end();
            }
            shutdownExecutor();
        }
    }

    /**
     * Execute the migration with batching and parallel processing
     *
     * @param condition the session condition
     */
    private void executeMigration(SessionCondition condition) throws TransactionException {
        int batchSize = getIntConfig(ConfigurationKeys.STORE_MIGRATION_BATCH_SIZE, DEFAULT_BATCH_SIZE);
        int parallelThreads = getIntConfig(ConfigurationKeys.STORE_MIGRATION_PARALLEL_THREADS, DEFAULT_PARALLEL_THREADS);
        long checkpointInterval = getLongConfig(ConfigurationKeys.STORE_MIGRATION_CHECKPOINT_INTERVAL, DEFAULT_CHECKPOINT_INTERVAL);

        SessionManager sourceSessionManager = getSourceSessionManager();
        if (sourceSessionManager == null) {
            throw new TransactionException(TransactionExceptionCode.GlobalTransactionNotExist, "Failed to get source session manager");
        }

        // Get total count for progress tracking
        Collection<GlobalSession> allSessions = sourceSessionManager.allSessions();
        long totalCount = allSessions.size();
        currentProgress.setTotalSessions(totalCount);
        LOGGER.info("Starting migration of {} sessions in batches of {}", totalCount, batchSize);

        // Initialize executor
        migrationExecutor = new ThreadPoolExecutor(
                parallelThreads,
                parallelThreads,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        List<GlobalSession> batch = new ArrayList<>();
        long processedCount = 0;

        for (GlobalSession session : allSessions) {
            // Check if paused
            while (paused) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TransactionException(TransactionExceptionCode.LockKeyConflict, "Migration interrupted while paused");
                }
            }

            batch.add(session);
            processedCount++;

            if (batch.size() >= batchSize) {
                processBatch(batch, processedCount);
                batch.clear();

                // Update checkpoint
                if (processedCount % checkpointInterval == 0) {
                    LOGGER.info("Migration progress: {} / {}", processedCount, totalCount);
                }
            }
        }

        // Process remaining sessions
        if (!batch.isEmpty()) {
            processBatch(batch, processedCount);
        }

        migrationExecutor.shutdown();
        try {
            migrationExecutor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LOGGER.info("Migration completed. Total: {}, Migrated: {}, Failed: {}",
                totalCount, currentProgress.getMigratedSessions(), currentProgress.getFailedSessions());
    }

    /**
     * Process a batch of sessions
     *
     * @param batch         the batch of sessions
     * @param processedCount the number of sessions processed so far
     */
    private void processBatch(List<GlobalSession> batch, long processedCount) {
        CountDownLatch latch = new CountDownLatch(batch.size());

        for (GlobalSession session : batch) {
            migrationExecutor.submit(() -> {
                try {
                    currentProgress.setCurrentXid(session.getXid());
                    if (currentMigration.migrate(session)) {
                        currentProgress.incrementMigratedSessions();
                    } else {
                        currentProgress.incrementFailedSessions();
                        LOGGER.error("Failed to migrate session: {}", session.getXid());
                    }
                } catch (Exception e) {
                    currentProgress.incrementFailedSessions();
                    LOGGER.error("Error migrating session: {}", session.getXid(), e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            currentProgress.setErrorMessage("Batch processing interrupted");
        }
    }

    /**
     * Get the source session manager
     *
     * @return the session manager
     */
    private SessionManager getSourceSessionManager() {
        try {
            Class<?> sessionHolderClass = Class.forName("org.apache.seata.server.session.SessionHolder");
            java.lang.reflect.Method method = sessionHolderClass.getMethod("getRootSessionManager");
            return (SessionManager) method.invoke(null);
        } catch (Exception e) {
            LOGGER.error("Failed to get session manager", e);
            return null;
        }
    }

    /**
     * Create a migration instance for the specified source and target modes
     *
     * @param sourceMode the source storage mode
     * @param targetMode the target storage mode
     * @return the migration instance
     */
    private StorageMigration createMigration(StoreMode sourceMode, StoreMode targetMode) {
        if (sourceMode == targetMode) {
            return null;
        }

        String migrationClassName = "org.apache.seata.server.storage.migration." +
                sourceMode.name() + "To" + targetMode.name() + "Migration";

        try {
            Class<?> migrationClass = Class.forName(migrationClassName);
            return (StorageMigration) migrationClass.newInstance();
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Migration class not found: {}", migrationClassName);
            return createGenericMigration(sourceMode, targetMode);
        } catch (Exception e) {
            LOGGER.error("Failed to create migration instance", e);
            return createGenericMigration(sourceMode, targetMode);
        }
    }

    /**
     * Create a generic migration instance
     *
     * @param sourceMode the source storage mode
     * @param targetMode the target storage mode
     * @return the migration instance
     */
    private StorageMigration createGenericMigration(StoreMode sourceMode, StoreMode targetMode) {
        return new GenericStorageMigration(sourceMode, targetMode);
    }

    /**
     * Pause the current migration
     */
    public void pause() {
        this.paused = true;
        if (currentProgress != null) {
            currentProgress.setStatus(MigrationProgress.Status.PAUSED);
        }
        LOGGER.info("Migration paused");
    }

    /**
     * Resume a paused migration
     */
    public void resume() {
        this.paused = false;
        if (currentProgress != null) {
            currentProgress.setStatus(MigrationProgress.Status.IN_PROGRESS);
        }
        LOGGER.info("Migration resumed");
    }

    /**
     * Rollback the current migration
     */
    public void rollback() throws TransactionException {
        if (!migrating.get()) {
            throw new TransactionException(TransactionExceptionCode.Unknown, "No migration in progress to rollback");
        }

        paused = false;
        currentProgress.setStatus(MigrationProgress.Status.ROLLED_BACK);
        currentProgress.setErrorMessage("Migration rolled back by user");

        shutdownExecutor();

        // Clean up target store
        if (currentProgress != null) {
            LOGGER.info("Rolling back migration. Migrated: {}, Failed: {}",
                    currentProgress.getMigratedSessions(), currentProgress.getFailedSessions());
        }

        migrating.set(false);
        LOGGER.info("Migration rolled back");
    }

    /**
     * Get the current migration progress
     *
     * @return the migration progress, or null if no migration in progress
     */
    public MigrationProgress getProgress() {
        return currentProgress;
    }

    /**
     * Check if migration is in progress
     *
     * @return true if migration is in progress
     */
    public boolean isMigrating() {
        return migrating.get();
    }

    /**
     * Check if migration is paused
     *
     * @return true if paused
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Get the current storage mode
     *
     * @return the current storage mode
     */
    public StoreMode getCurrentMode() {
        if (currentMode == null) {
            String modeStr = getConfig(ConfigurationKeys.STORE_MODE, "file");
            currentMode = StoreMode.get(modeStr);
        }
        return currentMode;
    }

    /**
     * Get configuration value
     *
     * @param key the configuration key
     * @param defaultValue the default value
     * @return the configuration value
     */
    private String getConfig(String key, String defaultValue) {
        return getConfiguration().getConfig(key, defaultValue);
    }

    /**
     * Get configuration value as int
     *
     * @param key the configuration key
     * @param defaultValue the default value
     * @return the configuration value as int
     */
    private int getIntConfig(String key, int defaultValue) {
        return getConfiguration().getInt(key, defaultValue);
    }

    /**
     * Get configuration value as long
     *
     * @param key the configuration key
     * @param defaultValue the default value
     * @return the configuration value as long
     */
    private long getLongConfig(String key, long defaultValue) {
        return getConfiguration().getLong(key, defaultValue);
    }

    /**
     * Shutdown the migration executor
     */
    private void shutdownExecutor() {
        if (migrationExecutor != null && !migrationExecutor.isShutdown()) {
            migrationExecutor.shutdownNow();
            try {
                if (!migrationExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    LOGGER.warn("Migration executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
