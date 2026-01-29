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
package org.apache.seata.saga.statelang.builder;

import org.apache.seata.saga.statelang.domain.StateMachine;

/**
 * Builder interface for constructing StateMachine definitions.
 *
 */
public interface StateMachineBuilder {

    /**
     * Creates a new StateMachineBuilder.
     *
     * @return a new builder instance
     */
    static StateMachineBuilder stateMachineBuilder() {
        return new StateMachineBuilderImpl();
    }

    /**
     * Builds and returns the StateMachine definition.
     *
     * @return the configured StateMachine
     */
    StateMachine build();

    /**
     * Begins defining states for the state machine.
     *
     * @return a StatesConfigurer for chaining state definitions
     */
    StatesConfigurer withStates();

    /**
     * Sets the name of the state machine.
     *
     * @param name the name
     * @return this builder
     */
    StateMachineBuilder withName(String name);

    /**
     * Sets the version of the state machine.
     *
     * @param version the version string
     * @return this builder
     */
    StateMachineBuilder withVersion(String version);

    /**
     * Sets the start state of the state machine.
     *
     * @param startState the name of the start state
     * @return this builder
     */
    StateMachineBuilder withStartState(String startState);

    /**
     * Sets a comment for the state machine.
     *
     * @param comment the comment description
     * @return this builder
     */
    StateMachineBuilder withComment(String comment);

    /**
     * Sets the tenant ID for the state machine.
     *
     * @param tenantId the tenant identifier
     * @return this builder
     */
    StateMachineBuilder withTenantId(String tenantId);

    /**
     * Sets the recovery strategy when errors occur.
     *
     * @param recoverStrategy the recovery strategy (Forward or Compensation)
     * @return this builder
     */
    StateMachineBuilder withRecoverStrategy(org.apache.seata.saga.statelang.domain.RecoverStrategy recoverStrategy);

    /**
     * Sets whether to persist execution logs.
     *
     * @param persist true to persist execution logs
     * @return this builder
     */
    StateMachineBuilder withPersist(boolean persist);

    /**
     * Sets the retry persist mode.
     *
     * @param update true to update existing logs, false to append new logs
     * @return this builder
     */
    StateMachineBuilder withRetryPersistModeUpdate(boolean update);

    /**
     * Sets the compensate persist mode.
     *
     * @param update true to update existing logs, false to append new logs
     * @return this builder
     */
    StateMachineBuilder withCompensatePersistModeUpdate(boolean update);
}
