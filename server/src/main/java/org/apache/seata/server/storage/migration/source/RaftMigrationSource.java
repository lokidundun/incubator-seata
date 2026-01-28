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
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.file.ReloadableStore;
import org.apache.seata.server.storage.file.TransactionWriteStore;
import org.apache.seata.server.storage.file.store.FileTransactionStoreManager;
import org.apache.seata.server.storage.migration.MigrationSource;
import org.apache.seata.server.store.TransactionStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Raft storage migration source
 * Reads sessions from Raft-based storage
 * Note: Raft mode uses the same file-based storage as FILE mode,
 * but data is synchronized through Raft protocol
 *
 * @author xingfudeshi@gmail.com
 */
public class RaftMigrationSource implements MigrationSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaftMigrationSource.class);

    private final FileTransactionStoreManager storeManager;
    private final ReloadableStore reloadableStore;

    public RaftMigrationSource(String filePath) {
        try {
            // Raft mode uses file-based storage, read from the data directory
            this.storeManager = new FileTransactionStoreManager(filePath, null);
            this.reloadableStore = storeManager;
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Raft storage at: " + filePath, e);
        }
    }

    @Override
    public Iterable<GlobalSession> readAllGlobalSessions() {
        List<GlobalSession> sessions = new ArrayList<>();
        try {
            int readSize = 500;
            boolean hasRemaining = true;
            boolean isHistory = false;

            while (hasRemaining) {
                List<TransactionWriteStore> stores = reloadableStore.readWriteStore(readSize, isHistory);
                if (stores == null || stores.isEmpty()) {
                    if (!isHistory) {
                        isHistory = true;
                        continue;
                    }
                    break;
                }

                for (TransactionWriteStore store : stores) {
                    try {
                        // Only process GLOBAL type operations
                        if (store.getOperate() == TransactionStoreManager.LogOperation.GLOBAL_ADD
                                || store.getOperate() == TransactionStoreManager.LogOperation.GLOBAL_UPDATE
                                || store.getOperate() == TransactionStoreManager.LogOperation.GLOBAL_REMOVE) {
                            GlobalSession session = (GlobalSession) store.getSessionRequest();
                            if (isValidSession(session)) {
                                sessions.add(session);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to process session from Raft storage: {}", e.getMessage());
                    }
                }

                hasRemaining = reloadableStore.hasRemaining(isHistory);
            }
            LOGGER.info("Read {} sessions from Raft storage", sessions.size());
        } catch (Exception e) {
            LOGGER.error("Failed to read sessions from Raft storage", e);
            throw new RuntimeException("Failed to read from Raft storage", e);
        }
        return sessions;
    }

    private boolean isValidSession(GlobalSession session) {
        return session.getXid() != null && session.getTransactionId() > 0;
    }

    @Override
    public StoreMode getSourceMode() {
        return StoreMode.RAFT;
    }

    @Override
    public void close() {
        if (storeManager != null) {
            storeManager.shutdown();
        }
    }
}
