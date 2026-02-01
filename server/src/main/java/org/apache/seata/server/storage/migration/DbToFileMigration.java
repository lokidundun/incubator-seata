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
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.store.TransactionStoreManager;
import org.apache.seata.server.storage.SessionConverter;
import org.apache.seata.core.store.GlobalTransactionDO;

import java.util.List;

/**
 * Migration from DB storage mode to File storage mode.
 *
 * @author xingfudeshi@gmail.com
 */
public class DbToFileMigration extends AbstractStorageMigration {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DbToFileMigration.class);

    public DbToFileMigration() {
        this.sourceMode = StoreMode.DB;
        this.targetMode = StoreMode.FILE;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public boolean validate() {
        try {
            sourceStoreManager = StorageMigrationService.getInstance().getStoreManager(StoreMode.DB);

            // For file mode, we need to create a FileTransactionStoreManager
            // This requires special initialization
            LOGGER.info("DB to File migration: File store manager will be initialized during migration");

            return sourceStoreManager != null;
        } catch (Exception e) {
            LOGGER.error("Failed to validate DB to File migration", e);
            return false;
        }
    }

    @Override
    public boolean migrate(GlobalSession session) {
        if (session == null) {
            return false;
        }
        try {
            // Write global session to file store
            if (targetStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, session)) {
                // Write branch sessions
                if (session.getBranchSessions() != null) {
                    for (org.apache.seata.server.session.BranchSession branchSession : session.getBranchSessions()) {
                        if (!targetStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, branchSession)) {
                            LOGGER.error("Failed to write branch session to file store, xid: {}, branchId: {}",
                                    session.getXid(), branchSession.getBranchId());
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("Error migrating session to file store: {}", session.getXid(), e);
            return false;
        }
    }

    @Override
    protected boolean createBackup() {
        // For DB to File migration, we don't create a formal backup
        // The file store will contain the migrated data
        LOGGER.info("DB to File migration: Target file store will contain migrated data");
        return true;
    }

    @Override
    protected void removeBackup() {
        LOGGER.info("DB to File migration cleanup completed");
    }
}
