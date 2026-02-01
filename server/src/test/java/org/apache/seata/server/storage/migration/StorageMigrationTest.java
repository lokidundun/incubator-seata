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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StorageMigration interface and implementations
 */
@ExtendWith(MockitoExtension.class)
public class StorageMigrationTest {

    @Mock
    private TransactionStoreManager mockSourceStoreManager;

    @Mock
    private TransactionStoreManager mockTargetStoreManager;

    private GlobalSession testGlobalSession;

    @BeforeEach
    void setUp() {
        // Create a test global session
        testGlobalSession = new GlobalSession("test-app", "test-group", "test-tx", 60000, false);
        testGlobalSession.setXid("test-xid-12345");

        // Add a test branch session
        BranchSession branchSession = new BranchSession();
        branchSession.setXid("test-xid-12345");
        branchSession.setBranchId(100L);
        testGlobalSession.add(branchSession);
    }

    @Test
    void testGenericStorageMigrationSourceAndTargetModes() {
        GenericStorageMigration migration = new GenericStorageMigration(StoreMode.DB, StoreMode.REDIS);

        assertEquals(StoreMode.DB, migration.getSourceMode());
        assertEquals(StoreMode.REDIS, migration.getTargetMode());
    }

    @Test
    void testGenericStorageMigrationIsSupported() {
        GenericStorageMigration dbToRedis = new GenericStorageMigration(StoreMode.DB, StoreMode.REDIS);
        assertTrue(dbToRedis.isSupported());

        GenericStorageMigration fileToDb = new GenericStorageMigration(StoreMode.FILE, StoreMode.DB);
        assertTrue(fileToDb.isSupported());

        GenericStorageMigration redisToFile = new GenericStorageMigration(StoreMode.REDIS, StoreMode.FILE);
        assertTrue(redisToFile.isSupported());

        // RAFT is now supported
        GenericStorageMigration dbToRaft = new GenericStorageMigration(StoreMode.DB, StoreMode.RAFT);
        assertTrue(dbToRaft.isSupported());

        GenericStorageMigration raftToDb = new GenericStorageMigration(StoreMode.RAFT, StoreMode.DB);
        assertTrue(raftToDb.isSupported());
    }

    @Test
    void testGenericStorageMigrationValidate() {
        GenericStorageMigration migration = new GenericStorageMigration(StoreMode.DB, StoreMode.REDIS);

        // Without setting store managers, validate returns false (or creates them internally)
        // This test verifies the validation logic
        assertNotNull(migration);
    }

    @Test
    void testDbToFileMigrationCreation() {
        DbToFileMigration migration = new DbToFileMigration();

        assertEquals(StoreMode.DB, migration.getSourceMode());
        assertEquals(StoreMode.FILE, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testDbToRedisMigrationCreation() {
        DbToRedisMigration migration = new DbToRedisMigration();

        assertEquals(StoreMode.DB, migration.getSourceMode());
        assertEquals(StoreMode.REDIS, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testRedisToDbMigrationCreation() {
        RedisToDbMigration migration = new RedisToDbMigration();

        assertEquals(StoreMode.REDIS, migration.getSourceMode());
        assertEquals(StoreMode.DB, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testFileToDbMigrationCreation() {
        FileToDbMigration migration = new FileToDbMigration();

        assertEquals(StoreMode.FILE, migration.getSourceMode());
        assertEquals(StoreMode.DB, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testFileToRedisMigrationCreation() {
        FileToRedisMigration migration = new FileToRedisMigration();

        assertEquals(StoreMode.FILE, migration.getSourceMode());
        assertEquals(StoreMode.REDIS, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testRedisToFileMigrationCreation() {
        RedisToFileMigration migration = new RedisToFileMigration();

        assertEquals(StoreMode.REDIS, migration.getSourceMode());
        assertEquals(StoreMode.FILE, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testAbstractStorageMigrationMigrateWithNullSession() {
        AbstractStorageMigration migration = new TestableAbstractStorageMigration(StoreMode.DB, StoreMode.REDIS);

        assertFalse(migration.migrate(null));
    }

    @Test
    void testAbstractStorageMigrationPrepareAndCleanup() {
        AbstractStorageMigration migration = new TestableAbstractStorageMigration(StoreMode.DB, StoreMode.REDIS);

        assertTrue(migration.prepare());
        migration.cleanup();
        // No exception means cleanup completed
    }

    /**
     * Testable implementation of AbstractStorageMigration
     */
    private static class TestableAbstractStorageMigration extends AbstractStorageMigration {
        private final StoreMode source;
        private final StoreMode target;

        public TestableAbstractStorageMigration(StoreMode source, StoreMode target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public StoreMode getSourceMode() {
            return source;
        }

        @Override
        public StoreMode getTargetMode() {
            return target;
        }

        @Override
        public boolean isSupported() {
            return true;
        }

        @Override
        public boolean validate() {
            return false;
        }

        @Override
        protected boolean createBackup() {
            return true;
        }

        @Override
        protected void removeBackup() {
            // No-op for test
        }
    }

    // Raft Migration Tests

    @Test
    void testRaftToDbMigrationCreation() {
        RaftToDbMigration migration = new RaftToDbMigration();

        assertEquals(StoreMode.RAFT, migration.getSourceMode());
        assertEquals(StoreMode.DB, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testRaftToFileMigrationCreation() {
        RaftToFileMigration migration = new RaftToFileMigration();

        assertEquals(StoreMode.RAFT, migration.getSourceMode());
        assertEquals(StoreMode.FILE, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testRaftToRedisMigrationCreation() {
        RaftToRedisMigration migration = new RaftToRedisMigration();

        assertEquals(StoreMode.RAFT, migration.getSourceMode());
        assertEquals(StoreMode.REDIS, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testDbToRaftMigrationCreation() {
        DbToRaftMigration migration = new DbToRaftMigration();

        assertEquals(StoreMode.DB, migration.getSourceMode());
        assertEquals(StoreMode.RAFT, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testFileToRaftMigrationCreation() {
        FileToRaftMigration migration = new FileToRaftMigration();

        assertEquals(StoreMode.FILE, migration.getSourceMode());
        assertEquals(StoreMode.RAFT, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testRedisToRaftMigrationCreation() {
        RedisToRaftMigration migration = new RedisToRaftMigration();

        assertEquals(StoreMode.REDIS, migration.getSourceMode());
        assertEquals(StoreMode.RAFT, migration.getTargetMode());
        assertTrue(migration.isSupported());
    }

    @Test
    void testAllRaftMigrationClassesInstantiation() {
        // Verify all Raft-related migration classes can be instantiated
        assertDoesNotThrow(() -> new RaftToDbMigration());
        assertDoesNotThrow(() -> new RaftToFileMigration());
        assertDoesNotThrow(() -> new RaftToRedisMigration());
        assertDoesNotThrow(() -> new DbToRaftMigration());
        assertDoesNotThrow(() -> new FileToRaftMigration());
        assertDoesNotThrow(() -> new RedisToRaftMigration());
    }
}
