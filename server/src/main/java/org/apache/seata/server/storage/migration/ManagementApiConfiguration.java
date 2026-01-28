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
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for storage migration management API
 * Enables an additional management port for migration operations
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
@ConditionalOnProperty(name = "seata.management.enabled", havingValue = "true")
public class ManagementApiConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementApiConfiguration.class);

    private static final String MANAGEMENT_PORT_CONFIG = "seata.management.port";
    private static final int DEFAULT_MANAGEMENT_PORT = 7091;

    /**
     * Gets the management port from configuration
     */
    public static int getManagementPort() {
        try {
            String portStr = ConfigurationFactory.getInstance().getConfig(MANAGEMENT_PORT_CONFIG);
            return Integer.parseInt(portStr);
        } catch (Exception e) {
            return DEFAULT_MANAGEMENT_PORT;
        }
    }

    /**
     * Configure ObjectMapper for JSON serialization
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.build();
    }

    /**
     * Configure XML message converter
     */
    @Bean
    public MappingJackson2XmlHttpMessageConverter xmlHttpMessageConverter(Jackson2ObjectMapperBuilder builder) {
        return new MappingJackson2XmlHttpMessageConverter(builder.build());
    }
}
