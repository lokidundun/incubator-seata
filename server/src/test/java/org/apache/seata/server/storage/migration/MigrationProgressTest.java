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
import org.apache.seata.server.store.TransactionStoreManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MigrationProgress
 */
@ExtendWith(MockitoExtension.class)
public class MigrationProgressTest {

    private MigrationProgress progress;

    @BeforeEach
    void setUp() {
        progress = new MigrationProgress();
    }

    @Test
    void testInitialStatus() {
        assertEquals(MigrationProgress.Status.NOT_STARTED, progress.getStatus());
    }

    @Test
    void testSetTotalSessions() {
        progress.setTotalSessions(100);
        assertEquals(100, progress.getTotalSessions());
    }

    @Test
    void testIncrementMigratedSessions() {
        assertEquals(1, progress.incrementMigratedSessions());
        assertEquals(2, progress.incrementMigratedSessions());
        assertEquals(3, progress.incrementMigratedSessions());
        assertEquals(3, progress.getMigratedSessions());
    }

    @Test
    void testIncrementFailedSessions() {
        assertEquals(1, progress.incrementFailedSessions());
        assertEquals(2, progress.incrementFailedSessions());
        assertEquals(2, progress.getFailedSessions());
    }

    @Test
    void testSetStatus() {
        progress.setStatus(MigrationProgress.Status.IN_PROGRESS);
        assertEquals(MigrationProgress.Status.IN_PROGRESS, progress.getStatus());

        progress.setStatus(MigrationProgress.Status.PAUSED);
        assertEquals(MigrationProgress.Status.PAUSED, progress.getStatus());
    }

    @Test
    void testSetCurrentXid() {
        progress.setCurrentXid("test-xid-123");
        assertEquals("test-xid-123", progress.getCurrentXid());
    }

    @Test
    void testSetErrorMessage() {
        progress.setErrorMessage("Test error message");
        assertEquals("Test error message", progress.getErrorMessage());
    }

    @Test
    void testProgressPercentage() {
        progress.setTotalSessions(100);
        assertEquals(0.0, progress.getProgressPercentage());

        progress.incrementMigratedSessions();
        assertEquals(1.0, progress.getProgressPercentage());

        for (int i = 0; i < 49; i++) {
            progress.incrementMigratedSessions();
        }
        assertEquals(50.0, progress.getProgressPercentage());
    }

    @Test
    void testProgressPercentageWithZeroTotal() {
        assertEquals(0.0, progress.getProgressPercentage());
    }

    @Test
    void testStartAndEnd() {
        progress.start();
        assertTrue(progress.getElapsedTime() >= 0);

        progress.end();
        assertTrue(progress.getElapsedTime() > 0);
    }

    @Test
    void testToString() {
        progress.setTotalSessions(100);
        progress.incrementMigratedSessions();
        progress.incrementFailedSessions();

        String result = progress.toString();
        assertTrue(result.contains("total=100"));
        assertTrue(result.contains("migrated=1"));
        assertTrue(result.contains("failed=1"));
    }
}
