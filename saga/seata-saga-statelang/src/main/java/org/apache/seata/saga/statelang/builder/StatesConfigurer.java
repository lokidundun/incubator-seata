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

/**
 * Interface for configuring states in a StateMachine.
 * Provides a generic method to build any type of StateBuilder,
 * as well as convenience methods for common state types.
 *
 * @author xingfudeshi@gmail.com
 */
public interface StatesConfigurer {

    /**
     * Creates and configures a new state using the specified builder type.
     *
     * @param clazz the StateBuilder implementation class
     * @param <B> the type of StateBuilder
     * @return the configured StateBuilder for further configuration
     */
    <B extends StateBuilder<?>> B build(Class<B> clazz);

    /**
     * Creates a new ServiceTask state builder.
     *
     * @param stateName the name of the state
     * @return the ServiceTaskStateBuilder for configuration
     */
    ServiceTaskStateBuilder newServiceTask(String stateName);

    /**
     * Creates a new ScriptTask state builder.
     *
     * @param stateName the name of the state
     * @return the ScriptTaskStateBuilder for configuration
     */
    ScriptTaskStateBuilder newScriptTask(String stateName);

    /**
     * Creates a new Choice state builder.
     *
     * @param stateName the name of the state
     * @return the ChoiceStateBuilder for configuration
     */
    ChoiceStateBuilder newChoice(String stateName);

    /**
     * Adds a SucceedEnd state.
     *
     * @param stateName the name of the end state
     * @return this configurer for chaining
     */
    StatesConfigurer succeedEnd(String stateName);

    /**
     * Adds a FailEnd state.
     *
     * @param stateName the name of the end state
     * @return this configurer for chaining
     */
    StatesConfigurer failEnd(String stateName);

    /**
     * Adds a CompensationTrigger state.
     *
     * @param stateName the name of the compensation trigger state
     * @return this configurer for chaining
     */
    StatesConfigurer compensationTrigger(String stateName);

    /**
     * Completes state configuration and returns to the parent StateMachineBuilder.
     *
     * @return the parent StateMachineBuilder
     */
    StateMachineBuilder configure();
}
