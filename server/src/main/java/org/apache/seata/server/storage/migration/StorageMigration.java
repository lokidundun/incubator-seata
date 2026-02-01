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

/**
 * Interface for storage mode migration.
 * Provides methods to migrate transaction data between different storage modes.
 *
 * @author xingfudeshi@gmail.com
 */
public interface StorageMigration {

    /**
     * Get the source storage mode
     *
     * @return the source storage mode
     */
    StoreMode getSourceMode();

    /**
     * Get the target storage mode
     *
     * @return the target storage mode
     */
    StoreMode getTargetMode();

    /**
     * Check if migration is supported from source mode to target mode
     *
     * @return true if supported
     */
    boolean isSupported();

    /**
     * Execute migration for a single global session
     *
     * @param session the global session to migrate
     * @return true if migration successful
     */
    boolean migrate(GlobalSession session);

    /**
     * Validate that the source and target storage configurations are valid
     *
     * @return true if valid
     */
    boolean validate();

    /**
     * Prepare for migration, e.g., create backup
     *
     * @return true if preparation successful
     */
    boolean prepare();

    /**
     * Clean up after migration, e.g., remove backup
     */
    void cleanup();
}
