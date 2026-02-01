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

/**
 * Storage Migration Package
 *
 * <p>This package provides functionality for migrating transaction data between
 * different storage modes in Seata. The following storage modes are supported:</p>
 * <ul>
 *   <li>FILE - File-based storage</li>
 *   <li>DB - Database storage</li>
 *   <li>REDIS - Redis storage</li>
 *   <li>RAFT - Raft cluster storage (not yet supported for migration)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Get the migration service
 * StorageMigrationService migrationService = StorageMigrationService.getInstance();
 *
 * // Start migration from DB to Redis
 * MigrationProgress progress = migrationService.migrate(StoreMode.DB, StoreMode.REDIS);
 *
 * // Check progress
 * if (progress.getStatus() == MigrationProgress.Status.COMPLETED) {
 *     System.out.println("Migration completed successfully!");
 * }
 *
 * // Pause migration (if needed)
 * migrationService.pause();
 *
 * // Resume migration
 * migrationService.resume();
 *
 * // Rollback migration (if needed)
 * migrationService.rollback();
 * }</pre>
 *
 * <h2>Configuration</h2>
 * <p>The following configuration keys can be used to tune migration performance:</p>
 * <ul>
 *   <li>store.migration.batchSize - Number of sessions to process in each batch (default: 100)</li>
 *   <li>store.migration.parallelThreads - Number of parallel threads for migration (default: 4)</li>
 *   <li>store.migration.checkpointInterval - Sessions between progress checkpoints (default: 1000)</li>
 * </ul>
 *
 * @author xingfudeshi@gmail.com
 */
package org.apache.seata.server.storage.migration;
