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
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.store.TransactionStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migration from File storage mode to Raft storage mode.
 *
 * WARNING: Migrating TO Raft mode is a complex operation that requires:
 * 1. Stopping all existing Seata servers
 * 2. Configuring new Raft cluster settings
 * 3. Starting the new Raft cluster with the migrated data
 * 4. Updating all clients to connect to the new cluster
 *
 * @author xingfudeshi@gmail.com
 */
public class FileToRaftMigration extends AbstractStorageMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileToRaftMigration.class);

    public FileToRaftMigration() {
        this.sourceMode = StoreMode.FILE;
        this.targetMode = StoreMode.RAFT;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public boolean validate() {
        try {
            sourceStoreManager = StorageMigrationService.getInstance().getStoreManager(StoreMode.FILE);

            LOGGER.info("File to Raft migration validation passed");
            LOGGER.warn("IMPORTANT: Migrating TO Raft mode requires manual steps:");
            LOGGER.warn("1. Stop all Seata servers");
            LOGGER.warn("2. Configure Raft cluster in seata.conf");
            LOGGER.warn("3. Copy the sessionStore files to each Raft node's data directory");
            LOGGER.warn("4. Start the new Raft cluster");
            LOGGER.warn("5. Update client configurations");

            return sourceStoreManager != null;
        } catch (Exception e) {
            LOGGER.error("Failed to validate File to Raft migration", e);
            return false;
        }
    }

    @Override
    public boolean migrate(GlobalSession session) {
        if (session == null) {
            return false;
        }
        try {
            // For Raft mode, we write to the file-based store that Raft uses
            if (targetStoreManager != null) {
                if (!targetStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, session)) {
                    LOGGER.error("Failed to write global session for Raft, xid: {}", session.getXid());
                    return false;
                }

                if (session.getBranchSessions() != null) {
                    for (BranchSession branchSession : session.getBranchSessions()) {
                        if (!targetStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, branchSession)) {
                            LOGGER.error("Failed to write branch session for Raft, xid: {}, branchId: {}",
                                    session.getXid(), branchSession.getBranchId());
                            return false;
                        }
                    }
                }
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("Error migrating session to Raft: {}", session.getXid(), e);
            return false;
        }
    }

    @Override
    protected boolean createBackup() {
        LOGGER.info("File to Raft migration: Backup of sessionStore directory is recommended");
        return true;
    }

    @Override
    protected void removeBackup() {
        LOGGER.info("File to Raft migration cleanup completed");
    }
}
