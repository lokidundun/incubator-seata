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
 * Exception thrown when storage mode migration fails
 *
 */
public class StorageModeMigrationException extends RuntimeException {

    private final String sourceMode;
    private final String targetMode;
    private final int failedSessionCount;

    public StorageModeMigrationException(String message) {
        super(message);
        this.sourceMode = null;
        this.targetMode = null;
        this.failedSessionCount = 0;
    }

    public StorageModeMigrationException(String message, Throwable cause) {
        super(message, cause);
        this.sourceMode = null;
        this.targetMode = null;
        this.failedSessionCount = 0;
    }

    public StorageModeMigrationException(String sourceMode, String targetMode, String message, Throwable cause) {
        super(message, cause);
        this.sourceMode = sourceMode;
        this.targetMode = targetMode;
        this.failedSessionCount = 0;
    }

    public StorageModeMigrationException(String sourceMode, String targetMode, int failedSessionCount, String message) {
        super(message);
        this.sourceMode = sourceMode;
        this.targetMode = targetMode;
        this.failedSessionCount = failedSessionCount;
    }

    public String getSourceMode() {
        return sourceMode;
    }

    public String getTargetMode() {
        return targetMode;
    }

    public int getFailedSessionCount() {
        return failedSessionCount;
    }
}
