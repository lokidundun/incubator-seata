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

/**
 * Configuration for storage mode migration
 *
 */
public class MigrationConfig {

    /**
     * Whether to skip completed transactions during migration
     * Default: true (skip transactions older than 24 hours that are already completed)
     */
    private boolean skipCompletedTransactions = true;

    /**
     * Whether to use batch insert for database migration
     * Default: true
     */
    private boolean useBatchInsert = true;

    /**
     * Batch size for batch insert operations
     * Default: 100
     */
    private int batchSize = 100;

    /**
     * Whether to validate data integrity after migration
     * Default: true
     */
    private boolean validateAfterMigration = true;

    /**
     * Number of threads for parallel migration
     * Default: 1 (sequential)
     */
    private int threadCount = 1;

    /**
     * Source file path (for FILE mode)
     */
    private String sourceFilePath;

    /**
     * Target file path (for FILE mode)
     */
    private String targetFilePath;

    public boolean isSkipCompletedTransactions() {
        return skipCompletedTransactions;
    }

    public void setSkipCompletedTransactions(boolean skipCompletedTransactions) {
        this.skipCompletedTransactions = skipCompletedTransactions;
    }

    public boolean isUseBatchInsert() {
        return useBatchInsert;
    }

    public void setUseBatchInsert(boolean useBatchInsert) {
        this.useBatchInsert = useBatchInsert;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isValidateAfterMigration() {
        return validateAfterMigration;
    }

    public void setValidateAfterMigration(boolean validateAfterMigration) {
        this.validateAfterMigration = validateAfterMigration;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final MigrationConfig config = new MigrationConfig();

        public Builder skipCompletedTransactions(boolean skipCompletedTransactions) {
            config.skipCompletedTransactions = skipCompletedTransactions;
            return this;
        }

        public Builder useBatchInsert(boolean useBatchInsert) {
            config.useBatchInsert = useBatchInsert;
            return this;
        }

        public Builder batchSize(int batchSize) {
            config.batchSize = batchSize;
            return this;
        }

        public Builder validateAfterMigration(boolean validateAfterMigration) {
            config.validateAfterMigration = validateAfterMigration;
            return this;
        }

        public Builder threadCount(int threadCount) {
            config.threadCount = threadCount;
            return this;
        }

        public Builder sourceFilePath(String sourceFilePath) {
            config.sourceFilePath = sourceFilePath;
            return this;
        }

        public Builder targetFilePath(String targetFilePath) {
            config.targetFilePath = targetFilePath;
            return this;
        }

        public MigrationConfig build() {
            return config;
        }
    }
}
