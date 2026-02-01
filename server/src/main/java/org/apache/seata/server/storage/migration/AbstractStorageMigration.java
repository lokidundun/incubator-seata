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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.store.StoreMode;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.store.TransactionStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for storage migration implementations.
 * Provides common functionality for all storage mode migrations.
 *
 * @author xingfudeshi@gmail.com
 */
public abstract class AbstractStorageMigration implements StorageMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageMigration.class);

    protected static Configuration getConfiguration() {
        return ConfigurationFactory.getInstance();
    }

    protected TransactionStoreManager sourceStoreManager;

    protected TransactionStoreManager targetStoreManager;

    protected volatile boolean backupCreated = false;

    protected StoreMode sourceMode;

    protected StoreMode targetMode;

    @Override
    public StoreMode getSourceMode() {
        return sourceMode;
    }

    @Override
    public StoreMode getTargetMode() {
        return targetMode;
    }

    @Override
    public boolean migrate(GlobalSession session) {
        if (session == null) {
            return false;
        }
        try {
            // Write global session to target
            if (!targetStoreManager.writeSession(TransactionStoreManager.LogOperation.GLOBAL_ADD, session)) {
                LOGGER.error("Failed to write global session to target store, xid: {}", session.getXid());
                return false;
            }

            // Write all branch sessions to target
            if (session.getBranchSessions() != null) {
                for (org.apache.seata.server.session.BranchSession branchSession : session.getBranchSessions()) {
                    if (!targetStoreManager.writeSession(TransactionStoreManager.LogOperation.BRANCH_ADD, branchSession)) {
                        LOGGER.error("Failed to write branch session to target store, xid: {}, branchId: {}",
                                session.getXid(), branchSession.getBranchId());
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Error migrating session: {}", session.getXid(), e);
            return false;
        }
    }

    @Override
    public boolean prepare() {
        try {
            // Create backup of source data
            backupCreated = createBackup();
            return backupCreated;
        } catch (Exception e) {
            LOGGER.error("Failed to create backup before migration", e);
            return false;
        }
    }

    @Override
    public void cleanup() {
        if (backupCreated) {
            try {
                removeBackup();
            } catch (Exception e) {
                LOGGER.error("Failed to remove backup after migration", e);
            }
        }
    }

    /**
     * Create a backup of the source data before migration
     *
     * @return true if backup created successfully
     */
    protected abstract boolean createBackup();

    /**
     * Remove the backup data
     */
    protected abstract void removeBackup();

    /**
     * Get the configuration value for a key
     *
     * @param key the configuration key
     * @param defaultValue the default value
     * @return the configuration value
     */
    protected String getConfig(String key, String defaultValue) {
        return getConfiguration().getConfig(key, defaultValue);
    }

    /**
     * Get the configuration value for a key
     *
     * @param key the configuration key
     * @return the configuration value
     */
    protected String getConfig(String key) {
        return getConfiguration().getConfig(key);
    }

    /**
     * Get the configuration value as int
     *
     * @param key the configuration key
     * @param defaultValue the default value
     * @return the configuration value as int
     */
    protected int getIntConfig(String key, int defaultValue) {
        return getConfiguration().getInt(key, defaultValue);
    }

    /**
     * Set the configuration value
     *
     * @param key the configuration key
     * @param value the value to set
     */
    protected void setConfig(String key, String value) {
        getConfiguration().putConfig(key, value);
    }
}
