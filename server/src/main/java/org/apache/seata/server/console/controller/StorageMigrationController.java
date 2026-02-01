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
package org.apache.seata.server.console.controller;

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.common.store.StoreMode;
import org.apache.seata.server.console.entity.vo.MigrationProgressVO;
import org.apache.seata.server.storage.migration.MigrationProgress;
import org.apache.seata.server.storage.migration.StorageMigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * Storage Migration Controller
 * Provides REST API for storage mode migration operations
 *
 * @author xingfudeshi@gmail.com
 */
@RestController
@RequestMapping("/api/v1/console/storageMigration")
public class StorageMigrationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageMigrationController.class);

    /**
     * Start storage mode migration
     *
     * @param sourceMode the source storage mode (file, db, redis)
     * @param targetMode the target storage mode (file, db, redis)
     * @return the migration progress
     */
    @PostMapping("start")
    public SingleResult<MigrationProgressVO> startMigration(
            @RequestParam String sourceMode,
            @RequestParam String targetMode) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting storage migration from {} to {}", sourceMode, targetMode);
        }

        try {
            StoreMode source = StoreMode.get(sourceMode);
            StoreMode target = StoreMode.get(targetMode);

            StorageMigrationService migrationService = StorageMigrationService.getInstance();
            MigrationProgress progress = migrationService.migrate(source, target);

            return SingleResult.success(convertToVO(progress));
        } catch (Exception e) {
            LOGGER.error("Failed to start storage migration", e);
            return SingleResult.fail("Migration failed: " + e.getMessage());
        }
    }

    /**
     * Get current migration progress
     *
     * @return the migration progress
     */
    @GetMapping("progress")
    public SingleResult<MigrationProgressVO> getProgress() {
        StorageMigrationService migrationService = StorageMigrationService.getInstance();
        MigrationProgress progress = migrationService.getProgress();

        if (progress == null) {
            return SingleResult.success(null);
        }

        return SingleResult.success(convertToVO(progress));
    }

    /**
     * Pause the current migration
     *
     * @return success or failure
     */
    @PostMapping("pause")
    public SingleResult<Void> pauseMigration() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Pausing storage migration");
        }

        try {
            StorageMigrationService.getInstance().pause();
            return SingleResult.success();
        } catch (Exception e) {
            LOGGER.error("Failed to pause migration", e);
            return SingleResult.fail("Failed to pause: " + e.getMessage());
        }
    }

    /**
     * Resume a paused migration
     *
     * @return success or failure
     */
    @PostMapping("resume")
    public SingleResult<Void> resumeMigration() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Resuming storage migration");
        }

        try {
            StorageMigrationService.getInstance().resume();
            return SingleResult.success();
        } catch (Exception e) {
            LOGGER.error("Failed to resume migration", e);
            return SingleResult.fail("Failed to resume: " + e.getMessage());
        }
    }

    /**
     * Rollback the current migration
     *
     * @return success or failure
     */
    @PostMapping("rollback")
    public SingleResult<Void> rollbackMigration() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Rolling back storage migration");
        }

        try {
            StorageMigrationService.getInstance().rollback();
            return SingleResult.success();
        } catch (Exception e) {
            LOGGER.error("Failed to rollback migration", e);
            return SingleResult.fail("Failed to rollback: " + e.getMessage());
        }
    }

    /**
     * Get current storage mode
     *
     * @return the current storage mode
     */
    @GetMapping("currentMode")
    public SingleResult<String> getCurrentMode() {
        StoreMode mode = StorageMigrationService.getInstance().getCurrentMode();
        return SingleResult.success(mode.getName());
    }

    /**
     * Check if migration is in progress
     *
     * @return true if migrating
     */
    @GetMapping("status")
    public SingleResult<String> getMigrationStatus() {
        StorageMigrationService migrationService = StorageMigrationService.getInstance();
        if (migrationService.isMigrating()) {
            if (migrationService.isPaused()) {
                return SingleResult.success("PAUSED");
            }
            return SingleResult.success("MIGRATING");
        }
        return SingleResult.success("IDLE");
    }

    /**
     * Convert MigrationProgress to MigrationProgressVO
     *
     * @param progress the migration progress
     * @return the VO
     */
    private MigrationProgressVO convertToVO(MigrationProgress progress) {
        if (progress == null) {
            return null;
        }
        MigrationProgressVO vo = new MigrationProgressVO();
        vo.setTotalSessions(progress.getTotalSessions());
        vo.setMigratedSessions(progress.getMigratedSessions());
        vo.setFailedSessions(progress.getFailedSessions());
        vo.setStatus(progress.getStatus().name());
        vo.setCurrentXid(progress.getCurrentXid());
        vo.setErrorMessage(progress.getErrorMessage());
        vo.setProgressPercentage(progress.getProgressPercentage());
        vo.setElapsedTime(progress.getElapsedTime());
        return vo;
    }
}
