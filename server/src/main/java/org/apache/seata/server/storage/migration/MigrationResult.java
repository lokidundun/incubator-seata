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
 * Migration result containing statistics and status
 *
 */
public class MigrationResult {
    private final int successCount;
    private final int failCount;
    private final int skippedCount;
    private final long elapsedMillis;
    private final boolean success;

    public MigrationResult(int successCount, int failCount, int skippedCount, long elapsedMillis, boolean success) {
        this.successCount = successCount;
        this.failCount = failCount;
        this.skippedCount = skippedCount;
        this.elapsedMillis = elapsedMillis;
        this.success = success;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return String.format("MigrationResult{success=%d, failed=%d, skipped=%d, elapsed=%dms}",
                successCount, failCount, skippedCount, elapsedMillis);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int successCount;
        private int failCount;
        private int skippedCount;
        private long elapsedMillis;
        private boolean success = true;

        public Builder successCount(int successCount) {
            this.successCount = successCount;
            return this;
        }

        public Builder failCount(int failCount) {
            this.failCount = failCount;
            return this;
        }

        public Builder skippedCount(int skippedCount) {
            this.skippedCount = skippedCount;
            return this;
        }

        public Builder elapsedMillis(long elapsedMillis) {
            this.elapsedMillis = elapsedMillis;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public MigrationResult build() {
            return new MigrationResult(successCount, failCount, skippedCount, elapsedMillis, success);
        }
    }
}
