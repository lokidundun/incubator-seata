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
 * File storage migration source
 * Reads sessions from file-based storage
 *
 */
public class FileMigrationSource implements MigrationSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileMigrationSource.class);

    private final FileTransactionStoreManager storeManager;
    private final ReloadableStore reloadableStore;

    public FileMigrationSource(String filePath) {
        try {
            // Use null sessionManager since we're only reading data
            this.storeManager = new FileTransactionStoreManager(filePath, null);
            this.reloadableStore = storeManager;
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize file storage at: " + filePath, e);
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
                        LOGGER.warn("Failed to process session from file store: {}", e.getMessage());
                    }
                }

                hasRemaining = reloadableStore.hasRemaining(isHistory);
            }
            LOGGER.info("Read {} sessions from file storage", sessions.size());
        } catch (Exception e) {
            LOGGER.error("Failed to read sessions from file storage", e);
            throw new RuntimeException("Failed to read from file storage", e);
        }
        return sessions;
    }

    private boolean isValidSession(GlobalSession session) {
        return session.getXid() != null && session.getTransactionId() > 0;
    }

    @Override
    public StoreMode getSourceMode() {
        return StoreMode.FILE;
    }

    @Override
    public void close() {
        if (storeManager != null) {
            storeManager.shutdown();
        }
    }
}
