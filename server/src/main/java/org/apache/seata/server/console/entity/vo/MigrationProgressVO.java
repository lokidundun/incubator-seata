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
package org.apache.seata.server.console.entity.vo;

/**
 * Migration Progress VO
 * Represents the progress of a storage migration operation
 *
 * @author xingfudeshi@gmail.com
 */
public class MigrationProgressVO {

    /**
     * Total number of sessions to migrate
     */
    private long totalSessions;

    /**
     * Number of sessions successfully migrated
     */
    private long migratedSessions;

    /**
     * Number of sessions that failed to migrate
     */
    private long failedSessions;

    /**
     * Current migration status (NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED, FAILED, ROLLED_BACK)
     */
    private String status;

    /**
     * Current XID being processed
     */
    private String currentXid;

    /**
     * Error message if migration failed
     */
    private String errorMessage;

    /**
     * Progress percentage (0-100)
     */
    private double progressPercentage;

    /**
     * Elapsed time in milliseconds
     */
    private long elapsedTime;

    public MigrationProgressVO() {
    }

    public long getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(long totalSessions) {
        this.totalSessions = totalSessions;
    }

    public long getMigratedSessions() {
        return migratedSessions;
    }

    public void setMigratedSessions(long migratedSessions) {
        this.migratedSessions = migratedSessions;
    }

    public long getFailedSessions() {
        return failedSessions;
    }

    public void setFailedSessions(long failedSessions) {
        this.failedSessions = failedSessions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentXid() {
        return currentXid;
    }

    public void setCurrentXid(String currentXid) {
        this.currentXid = currentXid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    @Override
    public String toString() {
        return "MigrationProgressVO{" +
                "totalSessions=" + totalSessions +
                ", migratedSessions=" + migratedSessions +
                ", failedSessions=" + failedSessions +
                ", status='" + status + '\'' +
                ", currentXid='" + currentXid + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", progressPercentage=" + progressPercentage +
                ", elapsedTime=" + elapsedTime +
                '}';
    }
}
