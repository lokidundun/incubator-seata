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

import org.apache.seata.common.result.Result;
import org.apache.seata.common.store.StoreMode;
import org.apache.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST API Controller for storage mode migration
 * Provides endpoints to trigger and monitor storage migration
 *
 * API Endpoints:
 * - POST /api/v1/storage/migration/start  - Start migration
 * - GET  /api/v1/storage/migration/status - Get migration status
 * - GET  /api/v1/storage/migration/modes  - Get available modes
 * - GET  /api/v1/storage/migration/check?sourceMode=FILE&targetMode=DB - Check if migration is supported
 *
 * Note: This controller should be used when the Seata server is running
 * and transactions are not active
 *
 */
@RestController
@RequestMapping("/api/v1/storage/migration")
public class StorageModeMigrationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageModeMigrationController.class);

    private volatile MigrationResult lastMigrationResult;
    private volatile String migrationStatus = "IDLE"; // IDLE, RUNNING, COMPLETED, FAILED
    private volatile StoreMode lastSourceMode;
    private volatile StoreMode lastTargetMode;
    private final ExecutorService migrationExecutor = Executors.newSingleThreadExecutor();

    /**
     * Request body for migration
     */
    public static class MigrationRequest {
        private StoreMode sourceMode;
        private StoreMode targetMode;
        private String sourceFilePath;
        private String targetFilePath;

        public StoreMode getSourceMode() {
            return sourceMode;
        }

        public void setSourceMode(StoreMode sourceMode) {
            this.sourceMode = sourceMode;
        }

        public StoreMode getTargetMode() {
            return targetMode;
        }

        public void setTargetMode(StoreMode targetMode) {
            this.targetMode = targetMode;
        }

        public String getSourceFilePath() {
            return sourceFilePath;
        }

        public void setSourceFilePath(String sourceFilePath) {
            this.sourceFilePath = sourceFilePath;
        }

        public String getTargetFilePath() {
            return targetFilePath;
        }

        public void setTargetFilePath(String targetFilePath) {
            this.targetFilePath = targetFilePath;
        }
    }

    /**
     * Response body for migration status
     */
    public static class MigrationStatusResponse {
        private String status;
        private StoreMode sourceMode;
        private StoreMode targetMode;
        private MigrationResult result;

        public MigrationStatusResponse(String status, StoreMode sourceMode, StoreMode targetMode, MigrationResult result) {
            this.status = status;
            this.sourceMode = sourceMode;
            this.targetMode = targetMode;
            this.result = result;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public StoreMode getSourceMode() {
            return sourceMode;
        }

        public void setSourceMode(StoreMode sourceMode) {
            this.sourceMode = sourceMode;
        }

        public StoreMode getTargetMode() {
            return targetMode;
        }

        public void setTargetMode(StoreMode targetMode) {
            this.targetMode = targetMode;
        }

        public MigrationResult getResult() {
            return result;
        }

        public void setResult(MigrationResult result) {
            this.result = result;
        }
    }

    /**
     * Starts a storage mode migration asynchronously
     *
     * POST /api/v1/storage/migration/start
     *
     * Request body:
     * {
     *   "sourceMode": "FILE",
     *   "targetMode": "DB",
     *   "sourceFilePath": "/data/seata/store",
     *   "targetFilePath": null
     * }
     *
     * @param request the migration request
     * @return result indicating whether the migration was started successfully
     */
    @PostMapping("/start")
    public Result<MigrationStatusResponse> startMigration(@RequestBody MigrationRequest request) {
        // Validate request
        if (request.getSourceMode() == null || request.getTargetMode() == null) {
            return buildFailedResult("Source and target modes must be specified");
        }

        if (!StorageModeMigrationExecutor.isMigrationSupported(request.getSourceMode(), request.getTargetMode())) {
            return buildFailedResult("Migration from " + request.getSourceMode() + " to "
                    + request.getTargetMode() + " is not supported");
        }

        if ((request.getSourceMode() == StoreMode.FILE || request.getSourceMode() == StoreMode.RAFT)
                && StringUtils.isBlank(request.getSourceFilePath())) {
            return buildFailedResult("sourceFilePath is required for FILE or RAFT mode");
        }
        if ((request.getTargetMode() == StoreMode.FILE || request.getTargetMode() == StoreMode.RAFT)
                && StringUtils.isBlank(request.getTargetFilePath())) {
            return buildFailedResult("targetFilePath is required for FILE or RAFT mode");
        }

        // Check if migration is already running
        if ("RUNNING".equals(migrationStatus)) {
            return buildFailedResult("Migration is already running");
        }

        // Start migration asynchronously
        migrationStatus = "RUNNING";
        lastSourceMode = request.getSourceMode();
        lastTargetMode = request.getTargetMode();
        lastMigrationResult = null;

        migrationExecutor.submit(() -> {
            try {
                LOGGER.info("Starting storage mode migration from {} to {}",
                        request.getSourceMode(), request.getTargetMode());

                StorageModeMigrationExecutor executor = StorageModeMigrationExecutor.builder()
                        .sourceMode(request.getSourceMode())
                        .targetMode(request.getTargetMode())
                        .sourceFilePath(request.getSourceFilePath())
                        .targetFilePath(request.getTargetFilePath())
                        .build();

                lastMigrationResult = executor.execute();
                migrationStatus = "COMPLETED";

                LOGGER.info("Storage mode migration completed. Result: {}", lastMigrationResult);

            } catch (Exception e) {
                LOGGER.error("Storage mode migration failed", e);
                lastMigrationResult = MigrationResult.builder()
                        .successCount(0)
                        .failCount(0)
                        .skippedCount(0)
                        .elapsedMillis(0)
                        .success(false)
                        .build();
                migrationStatus = "FAILED";
            }
        });

        return buildSucceedResult(new MigrationStatusResponse(migrationStatus, lastSourceMode, lastTargetMode, null));
    }

    /**
     * Gets the current migration status
     *
     * GET /api/v1/storage/migration/status
     *
     * @return current migration status
     */
    @GetMapping("/status")
    public Result<MigrationStatusResponse> getMigrationStatus() {
        return buildSucceedResult(new MigrationStatusResponse(
                migrationStatus, lastSourceMode, lastTargetMode, lastMigrationResult));
    }

    /**
     * Checks if migration is supported between two modes
     *
     * GET /api/v1/storage/migration/check?sourceMode=FILE&targetMode=DB
     *
     * @param sourceMode source mode
     * @param targetMode target mode
     * @return true if supported
     */
    @GetMapping("/check")
    public Result<Boolean> checkMigrationSupported(
            @RequestParam StoreMode sourceMode,
            @RequestParam StoreMode targetMode) {
        return buildSucceedResult(StorageModeMigrationExecutor.isMigrationSupported(sourceMode, targetMode));
    }

    /**
     * Gets available storage modes
     *
     * GET /api/v1/storage/migration/modes
     *
     * @return array of available modes
     */
    @GetMapping("/modes")
    public Result<StoreMode[]> getAvailableModes() {
        return buildSucceedResult(new StoreMode[]{StoreMode.FILE, StoreMode.DB, StoreMode.REDIS, StoreMode.RAFT});
    }

    /**
     * Creates a successful result
     */
    private <T> Result<T> buildSucceedResult(T data) {
        Result<T> result = new Result<>();
        result.setCode(Result.SUCCESS_CODE);
        result.setMessage(Result.SUCCESS_MSG);
        return result;
    }

    /**
     * Creates a failed result
     */
    private <T> Result<T> buildFailedResult(String message) {
        Result<T> result = new Result<>();
        result.setCode(Result.FAIL_CODE);
        result.setMessage(message);
        return result;
    }
}
