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

import org.apache.seata.server.session.GlobalSession;

/**
 * Interface for writing sessions to target storage
 *
 */
public interface MigrationTarget {

    /**
     * Writes a global session to the target storage
     *
     * @param session the global session to write
     * @return true if successful
     */
    boolean writeGlobalSession(GlobalSession session);

    /**
     * Writes multiple global sessions in batch
     *
     * @param sessions the sessions to write
     * @return number of successfully written sessions
     */
    int writeGlobalSessions(Iterable<GlobalSession> sessions);

    /**
     * Gets the target storage mode
     *
     * @return store mode
     */
    org.apache.seata.common.store.StoreMode getTargetMode();

    /**
     * Closes the target and releases resources
     */
    void close();
}
