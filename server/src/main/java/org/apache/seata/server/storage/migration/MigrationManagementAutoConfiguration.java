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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for storage migration management API
 *
 * To enable the management API, add the following to your application.yml:
 *
 * seata:
 *   management:
 *     enabled: true
 *     port: 7091
 *
 * Or use system property:
 * -Dseata.management.enabled=true -Dseata.management.port=7091
 *
 */
@Configuration
public class MigrationManagementAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationManagementAutoConfiguration.class);

    /**
     * Creates and starts the management API server when enabled
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "seata.management.enabled", havingValue = "true", matchIfMissing = false)
    public ManagementApiServer managementApiServer() {
        LOGGER.info("Starting storage migration management API server...");
        ManagementApiServer server = new ManagementApiServer();
        if (server.isEnabled()) {
            LOGGER.info("Storage migration management API is enabled on port {}", server.getPort());
        }
        return server;
    }

    /**
     * Creates the migration controller as a bean for dependency injection
     */
    @Bean
    public StorageModeMigrationController storageModeMigrationController() {
        return new StorageModeMigrationController();
    }
}
