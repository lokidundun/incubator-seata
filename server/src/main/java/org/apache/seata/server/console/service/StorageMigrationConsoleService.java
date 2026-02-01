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
package org.apache.seata.server.console.service;

import org.apache.seata.common.store.StoreMode;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.server.console.vo.MigrationResultVO;
import org.apache.seata.server.console.vo.StoreModeVO;
import org.apache.seata.server.storage.migration.MigrationProgress;
import org.apache.seata.server.storage.migration.StorageMigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Storage Migration Console Service
 *
 * @author xingfudeshi@gmail.com
 */
@Service
public class StorageMigrationConsoleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageMigrationConsoleService.class);

    private final StorageMigrationService migrationService;

    private ScheduledExecutorService scheduler;

    public StorageMigrationConsoleService(StorageMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    /**
     * Get current storage mode
     *
     * @return current store mode
     */
    public StoreModeVO getCurrentMode() {
        StoreMode mode = migrationService.getCurrentMode();
        return new StoreModeVO(
                mode.getName(),
                getDisplayName(mode),
                getDescription(mode),
                true
        );
    }

    /**
     * Get current store mode as enum
     *
     * @return StoreMode enum
     */
    public StoreMode getCurrentStoreMode() {
        return migrationService.getCurrentMode();
    }

    /**
     * List all supported storage modes
     *
     * @return list of store modes
     */
    public List<StoreModeVO> listSupportedModes() {
        List<StoreModeVO> modes = new ArrayList<>();
        for (StoreMode mode : StoreMode.values()) {
            modes.add(new StoreModeVO(
                    mode.getName(),
                    getDisplayName(mode),
                    getDescription(mode),
                    isStoreConfigured(mode)
            ));
        }
        return modes;
    }

    /**
     * Check if a store mode is available/configured
     *
     * @param mode the store mode
     * @return true if configured
     */
    public boolean isStoreAvailable(StoreMode mode) {
        // Check if the store manager can be created
        try {
            return migrationService.getStoreManager(mode) != null || mode == migrationService.getCurrentMode();
        } catch (Exception e) {
            LOGGER.warn("Failed to check store availability for {}: {}", mode, e.getMessage());
            return mode == migrationService.getCurrentMode();
        }
    }

    /**
     * Start migration
     *
     * @param sourceMode source storage mode
     * @param targetMode target storage mode
     * @return migration result
     */
    public MigrationResultVO migrate(StoreMode sourceMode, StoreMode targetMode) throws TransactionException {
        LOGGER.info("Starting migration from {} to {}", sourceMode, targetMode);

        // Start migration
        MigrationProgress progress = migrationService.migrate(sourceMode, targetMode);

        // Convert to VO
        return convertToVO(progress);
    }

    /**
     * Pause current migration
     */
    public void pause() {
        migrationService.pause();
    }

    /**
     * Resume paused migration
     */
    public void resume() {
        migrationService.resume();
    }

    /**
     * Rollback current migration
     */
    public void rollback() {
        migrationService.rollback();
    }

    /**
     * Get migration status
     *
     * @return migration result
     */
    public MigrationResultVO getMigrationStatus() {
        MigrationProgress progress = migrationService.getProgress();
        if (progress == null) {
            // No migration in progress
            MigrationResultVO vo = new MigrationResultVO();
            vo.setMigrating(false);
            vo.setPaused(false);
            vo.setStatus("IDLE");
            return vo;
        }
        return convertToVO(progress);
    }

    /**
     * Start monitoring migration progress
     *
     * @param callback callback to receive progress updates
     * @return scheduled future for stopping
     */
    public java.util.concurrent.ScheduledFuture<?> startMonitoring(java.util.function.Consumer<MigrationResultVO> callback) {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "migration-monitor");
                t.setDaemon(true);
                return t;
            });
        }

        return scheduler.scheduleAtFixedRate(() -> {
            MigrationResultVO status = getMigrationStatus();
            if (status.isMigrating()) {
                callback.accept(status);
            } else {
                // Migration finished
                callback.accept(status);
                stopMonitoring();
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * Convert MigrationProgress to VO
     *
     * @param progress the migration progress
     * @return the VO
     */
    private MigrationResultVO convertToVO(MigrationProgress progress) {
        if (progress == null) {
            MigrationResultVO vo = new MigrationResultVO();
            vo.setMigrating(false);
            vo.setStatus("IDLE");
            return vo;
        }

        MigrationResultVO vo = new MigrationResultVO();
        vo.setMigrating(migrationService.isMigrating());
        vo.setPaused(migrationService.isPaused());
        vo.setTotalSessions(progress.getTotalSessions());
        vo.setMigratedSessions(progress.getMigratedSessions());
        vo.setFailedSessions(progress.getFailedSessions());
        vo.setProgressPercentage(progress.getProgressPercentage());
        vo.setCurrentXid(progress.getCurrentXid());
        vo.setElapsedTime(progress.getElapsedTime());
        vo.setStatus(progress.getStatus().name());
        vo.setErrorMessage(progress.getErrorMessage());
        return vo;
    }

    /**
     * Get display name for store mode
     */
    private String getDisplayName(StoreMode mode) {
        switch (mode) {
            case FILE:
                return "File Storage";
            case DB:
                return "Database Storage";
            case REDIS:
                return "Redis Storage";
            case RAFT:
                return "Raft Cluster";
            default:
                return mode.getName();
        }
    }

    /**
     * Get description for store mode
     */
    private String getDescription(StoreMode mode) {
        switch (mode) {
            case FILE:
                return "Local file-based storage. Simple but not suitable for multi-node deployment.";
            case DB:
                return "Relational database storage. Suitable for production environments.";
            case REDIS:
                return "Redis storage. High performance, suitable for scenarios requiring high throughput.";
            case RAFT:
                return "Raft cluster storage. Supports high availability and data consistency.";
            default:
                return "";
        }
    }

    /**
     * Check if store is configured
     */
    private boolean isStoreConfigured(StoreMode mode) {
        // Check if configuration exists for the mode
        // This is a simplified check
        return mode == migrationService.getCurrentMode() || isStoreAvailable(mode);
    }
}
