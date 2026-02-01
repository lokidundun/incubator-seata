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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents the progress of a storage migration operation.
 * Tracks total sessions, migrated sessions, failed sessions, and current status.
 *
 * @author xingfudeshi@gmail.com
 */
public class MigrationProgress {

    /**
     * Migration status
     */
    public enum Status {
        NOT_STARTED,
        IN_PROGRESS,
        PAUSED,
        COMPLETED,
        FAILED,
        ROLLED_BACK
    }

    private final AtomicLong totalSessions = new AtomicLong(0);
    private final AtomicLong migratedSessions = new AtomicLong(0);
    private final AtomicLong failedSessions = new AtomicLong(0);
    private volatile Status status = Status.NOT_STARTED;
    private volatile String currentXid;
    private volatile String errorMessage;
    private volatile long startTime;
    private volatile long endTime;
    private volatile long lastUpdateTime;

    public MigrationProgress() {
    }

    /**
     * Set total number of sessions to migrate
     *
     * @param count the total count
     */
    public void setTotalSessions(long count) {
        this.totalSessions.set(count);
    }

    /**
     * Get total number of sessions
     *
     * @return the total count
     */
    public long getTotalSessions() {
        return totalSessions.get();
    }

    /**
     * Increment migrated sessions count
     *
     * @return the new count
     */
    public long incrementMigratedSessions() {
        lastUpdateTime = System.currentTimeMillis();
        return migratedSessions.incrementAndGet();
    }

    /**
     * Get migrated sessions count
     *
     * @return the migrated count
     */
    public long getMigratedSessions() {
        return migratedSessions.get();
    }

    /**
     * Increment failed sessions count
     *
     * @return the new count
     */
    public long incrementFailedSessions() {
        lastUpdateTime = System.currentTimeMillis();
        return failedSessions.incrementAndGet();
    }

    /**
     * Get failed sessions count
     *
     * @return the failed count
     */
    public long getFailedSessions() {
        return failedSessions.get();
    }

    /**
     * Set current status
     *
     * @param status the new status
     */
    public void setStatus(Status status) {
        this.status = status;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Get current status
     *
     * @return the current status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Set current Xid being processed
     *
     * @param xid the current xid
     */
    public void setCurrentXid(String xid) {
        this.currentXid = xid;
    }

    /**
     * Get current Xid
     *
     * @return the current xid
     */
    public String getCurrentXid() {
        return currentXid;
    }

    /**
     * Set error message
     *
     * @param message the error message
     */
    public void setErrorMessage(String message) {
        this.errorMessage = message;
    }

    /**
     * Get error message
     *
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Start the migration timer
     */
    public void start() {
        this.startTime = System.currentTimeMillis();
        this.status = Status.IN_PROGRESS;
    }

    /**
     * End the migration timer
     */
    public void end() {
        this.endTime = System.currentTimeMillis();
    }

    /**
     * Get elapsed time in milliseconds
     *
     * @return the elapsed time
     */
    public long getElapsedTime() {
        if (startTime == 0) {
            return 0;
        }
        if (endTime == 0) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }

    /**
     * Get progress percentage
     *
     * @return the progress percentage (0-100)
     */
    public double getProgressPercentage() {
        long total = totalSessions.get();
        if (total == 0) {
            return 0;
        }
        return (migratedSessions.get() * 100.0) / total;
    }

    /**
     * Get the last update time
     *
     * @return the last update time
     */
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public String toString() {
        return String.format(
                "MigrationProgress{total=%d, migrated=%d, failed=%d, status=%s, progress=%.2f%%}",
                totalSessions.get(), migratedSessions.get(), failedSessions.get(), status, getProgressPercentage());
    }
}
