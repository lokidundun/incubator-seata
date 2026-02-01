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
package org.apache.seata.server.console.vo;

/**
 * Migration Result VO
 *
 * @author xingfudeshi@gmail.com
 */
public class MigrationResultVO {

    private boolean migrating;
    private boolean paused;
    private long totalSessions;
    private long migratedSessions;
    private long failedSessions;
    private double progressPercentage;
    private String currentXid;
    private long elapsedTime;
    private String status;
    private String errorMessage;
    private String sourceMode;
    private String targetMode;

    public MigrationResultVO() {
    }

    public boolean isMigrating() {
        return migrating;
    }

    public void setMigrating(boolean migrating) {
        this.migrating = migrating;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
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

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getCurrentXid() {
        return currentXid;
    }

    public void setCurrentXid(String currentXid) {
        this.currentXid = currentXid;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSourceMode() {
        return sourceMode;
    }

    public void setSourceMode(String sourceMode) {
        this.sourceMode = sourceMode;
    }

    public String getTargetMode() {
        return targetMode;
    }

    public void setTargetMode(String targetMode) {
        this.targetMode = targetMode;
    }

    @Override
    public String toString() {
        return "MigrationResultVO{" +
                "migrating=" + migrating +
                ", paused=" + paused +
                ", totalSessions=" + totalSessions +
                ", migratedSessions=" + migratedSessions +
                ", failedSessions=" + failedSessions +
                ", progressPercentage=" + progressPercentage +
                ", status='" + status + '\'' +
                '}';
    }
}
