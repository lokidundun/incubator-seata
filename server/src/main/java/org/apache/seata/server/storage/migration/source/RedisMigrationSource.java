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
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.storage.migration.MigrationSource;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManagerFactory;
import org.apache.seata.server.store.TransactionStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis storage migration source
 * Reads sessions from Redis storage
 *
 */
public class RedisMigrationSource implements MigrationSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisMigrationSource.class);

    private final TransactionStoreManager storeManager;

    public RedisMigrationSource() {
        this.storeManager = RedisTransactionStoreManagerFactory.getInstance();
    }

    @Override
    public Iterable<GlobalSession> readAllGlobalSessions() {
        List<GlobalSession> sessions = new ArrayList<>();
        try {
            // Read all sessions using SessionCondition
            SessionCondition condition = new SessionCondition(
                    GlobalStatus.UnKnown,
                    GlobalStatus.Begin,
                    GlobalStatus.Committing,
                    GlobalStatus.CommitRetrying,
                    GlobalStatus.Rollbacking,
                    GlobalStatus.RollbackRetrying,
                    GlobalStatus.TimeoutRollbacking,
                    GlobalStatus.TimeoutRollbackRetrying,
                    GlobalStatus.AsyncCommitting,
                    GlobalStatus.StopRollbackOrRollbackRetry,
                    GlobalStatus.StopCommitOrCommitRetry,
                    GlobalStatus.Deleting);

            List<GlobalSession> globalSessions = storeManager.readSession(condition);
            sessions.addAll(globalSessions);

            LOGGER.info("Read {} sessions from Redis storage", sessions.size());
        } catch (Exception e) {
            LOGGER.error("Failed to read sessions from Redis storage", e);
            throw new RuntimeException("Failed to read from Redis storage", e);
        }
        return sessions;
    }

    @Override
    public StoreMode getSourceMode() {
        return StoreMode.REDIS;
    }

    @Override
    public void close() {
        if (storeManager != null) {
            storeManager.shutdown();
        }
    }
}
