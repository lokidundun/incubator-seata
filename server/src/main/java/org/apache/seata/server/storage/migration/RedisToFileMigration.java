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

/**
 * Migration from Redis storage mode to File storage mode.
 *
 * @author xingfudeshi@gmail.com
 */
public class RedisToFileMigration extends AbstractStorageMigration {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RedisToFileMigration.class);

    public RedisToFileMigration() {
        this.sourceMode = StoreMode.REDIS;
        this.targetMode = StoreMode.FILE;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public boolean validate() {
        try {
            sourceStoreManager = StorageMigrationService.getInstance().getStoreManager(StoreMode.REDIS);

            LOGGER.info("Redis to File migration: File store manager will be initialized during migration");

            return sourceStoreManager != null;
        } catch (Exception e) {
            LOGGER.error("Failed to validate Redis to File migration", e);
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
            if (!targetStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, session)) {
                LOGGER.error("Failed to write global session to file store, xid: {}", session.getXid());
                return false;
            }

            // Write branch sessions to file store
            if (session.getBranchSessions() != null) {
                for (BranchSession branchSession : session.getBranchSessions()) {
                    if (!targetStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, branchSession)) {
                        LOGGER.error("Failed to write branch session to file store, xid: {}, branchId: {}",
                                session.getXid(), branchSession.getBranchId());
                        return false;
                    }
                }
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("Error migrating session to file store: {}", session.getXid(), e);
            return false;
        }
    }

    @Override
    protected boolean createBackup() {
        LOGGER.info("Redis to File migration: Creating backup of source Redis data is recommended");
        return true;
    }

    @Override
    protected void removeBackup() {
        LOGGER.info("Redis to File migration cleanup completed");
    }
}
