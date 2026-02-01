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

import org.apache.seata.common.store.StoreMode;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.store.TransactionStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic storage migration implementation.
 * Uses TransactionStoreManager to read from source and write to target.
 * This is a fallback implementation when specific migration classes are not available.
 *
 * @author xingfudeshi@gmail.com
 */
public class GenericStorageMigration extends AbstractStorageMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericStorageMigration.class);

    public GenericStorageMigration(StoreMode sourceMode, StoreMode targetMode) {
        this.sourceMode = sourceMode;
        this.targetMode = targetMode;
    }

    @Override
    public boolean isSupported() {
        // Generic migration now supports all combinations
        // Raft mode is handled by treating it as file-based source or requiring special setup for target
        return true;
    }

    @Override
    public boolean validate() {
        // For Raft mode, we use FILE store manager since Raft internally uses file storage
        StoreMode actualSource = this.sourceMode == StoreMode.RAFT ? StoreMode.FILE : this.sourceMode;
        StoreMode actualTarget = this.targetMode == StoreMode.RAFT ? StoreMode.FILE : this.targetMode;

        if (actualSource == StoreMode.FILE || actualTarget == StoreMode.FILE) {
            LOGGER.warn("File mode requires special initialization, migration may not work correctly");
        }

        // For Raft target, we cannot validate fully without proper Raft configuration
        if (this.targetMode == StoreMode.RAFT) {
            LOGGER.warn("Migrating TO Raft mode requires additional manual configuration steps");
        }

        if (sourceStoreManager == null) {
            sourceStoreManager = StorageMigrationService.getInstance().getStoreManager(actualSource);
        }
        if (targetStoreManager == null) {
            targetStoreManager = StorageMigrationService.getInstance().getStoreManager(actualTarget);
        }

        // For Raft target, we only require source store manager
        if (this.targetMode == StoreMode.RAFT) {
            return sourceStoreManager != null;
        }

        return sourceStoreManager != null && targetStoreManager != null;
    }

    @Override
    protected boolean createBackup() {
        // For generic migration, we don't create a formal backup
        // The target store is assumed to be empty or the user is aware of potential duplication
        LOGGER.info("Generic migration does not create formal backup. Target store may contain duplicate data.");
        return true;
    }

    @Override
    protected void removeBackup() {
        // No backup to remove for generic migration
        LOGGER.info("Generic migration cleanup completed");
    }
}
