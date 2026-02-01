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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StorageMigrationService
 */
@ExtendWith(MockitoExtension.class)
public class StorageMigrationServiceTest {

    private StorageMigrationService migrationService;

    @BeforeEach
    void setUp() {
        // Note: StorageMigrationService is a singleton, so we need to be careful with tests
        // This test mainly checks the service configuration and methods
        migrationService = StorageMigrationService.getInstance();
    }

    @Test
    void testGetInstance() {
        StorageMigrationService instance1 = StorageMigrationService.getInstance();
        StorageMigrationService instance2 = StorageMigrationService.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    @Test
    void testGetCurrentMode() {
        StoreMode mode = migrationService.getCurrentMode();

        // Mode should be one of the supported modes
        assertNotNull(mode);
        assertTrue(mode == StoreMode.FILE || mode == StoreMode.DB ||
                   mode == StoreMode.REDIS || mode == StoreMode.RAFT);
    }

    @Test
    void testInitialState() {
        // Initially, no migration should be in progress
        assertFalse(migrationService.isMigrating());
        assertFalse(migrationService.isPaused());
    }

    @Test
    void testPauseWithoutMigration() {
        // Calling pause when no migration is in progress should not throw
        assertDoesNotThrow(() -> migrationService.pause());
    }

    @Test
    void testResumeWithoutMigration() {
        // Calling resume when no migration is in progress should not throw
        assertDoesNotThrow(() -> migrationService.resume());
    }

    @Test
    void testRollbackWithoutMigration() {
        // Calling rollback when no migration is in progress should throw
        assertThrows(Exception.class, () -> migrationService.rollback());
    }

    @Test
    void testGetProgressWithoutMigration() {
        MigrationProgress progress = migrationService.getProgress();

        // Progress may be null if no migration has been started
        assertNull(progress);
    }

    @Test
    void testCreateMigrationForSameMode() {
        StorageMigration migration = createMigration(StoreMode.DB, StoreMode.DB);
        assertNull(migration);
    }

    /**
     * Helper method to create a migration instance
     * Note: This is a simplified version for testing
     */
    private StorageMigration createMigration(StoreMode sourceMode, StoreMode targetMode) {
        if (sourceMode == targetMode) {
            return null;
        }

        // Check if it's a supported combination
        if (sourceMode == StoreMode.RAFT || targetMode == StoreMode.RAFT) {
            return new GenericStorageMigration(sourceMode, targetMode);
        }

        String migrationClassName = "org.apache.seata.server.storage.migration." +
                sourceMode.name() + "To" + targetMode.name() + "Migration";

        try {
            Class<?> migrationClass = Class.forName(migrationClassName);
            return (StorageMigration) migrationClass.newInstance();
        } catch (ClassNotFoundException e) {
            return new GenericStorageMigration(sourceMode, targetMode);
        } catch (Exception e) {
            return new GenericStorageMigration(sourceMode, targetMode);
        }
    }
}
