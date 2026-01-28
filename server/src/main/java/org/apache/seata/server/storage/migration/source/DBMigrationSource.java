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
package org.apache.seata.server.storage.migration.source;

import org.apache.seata.common.store.StoreMode;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.db.store.DataBaseTransactionStoreManager;
import org.apache.seata.server.storage.migration.MigrationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Database storage migration source
 * Reads sessions from database storage
 *
 */
public class DBMigrationSource implements MigrationSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBMigrationSource.class);

    private final DataBaseTransactionStoreManager storeManager;

    public DBMigrationSource() {
        this.storeManager = DataBaseTransactionStoreManager.getInstance();
    }

    @Override
    public Iterable<GlobalSession> readAllGlobalSessions() {
        List<GlobalSession> sessions = new ArrayList<>();
        try {
            // Read all active and in-progress transactions
            GlobalStatus[] statuses = {
                    GlobalStatus.Begin,
                    GlobalStatus.Committing,
                    GlobalStatus.CommitRetrying,
                    GlobalStatus.Rollbacking,
                    GlobalStatus.TimeoutRollbacking,
                    GlobalStatus.RollbackRetrying,
                    GlobalStatus.TimeoutRollbackRetrying,
                    GlobalStatus.AsyncCommitting
            };

            List<GlobalSession> globalSessions = storeManager.readSession(statuses, true);
            sessions.addAll(globalSessions);

            // Also read completed transactions that haven't been cleaned up (within 24 hours)
            GlobalStatus[] finishedStatuses = {GlobalStatus.Finished, GlobalStatus.Committed, GlobalStatus.Rollbacked};
            List<GlobalSession> finishedSessions = storeManager.readSession(finishedStatuses, true);
            sessions.addAll(finishedSessions);

            LOGGER.info("Read {} sessions from database storage", sessions.size());
        } catch (Exception e) {
            LOGGER.error("Failed to read sessions from database storage", e);
            throw new RuntimeException("Failed to read from database storage", e);
        }
        return sessions;
    }

    @Override
    public StoreMode getSourceMode() {
        return StoreMode.DB;
    }

    @Override
    public void close() {
        // Database connection pool is managed by singleton, no need to close
    }
}
