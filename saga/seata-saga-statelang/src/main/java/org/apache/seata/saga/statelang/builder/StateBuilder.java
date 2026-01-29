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

import org.apache.seata.saga.statelang.domain.State;

/**
 * Base interface for all state builders.
 * Each state builder must provide a way to build the state
 * and return to the StatesConfigurer.
 *
 * @param <T> the type of State this builder creates
 */
public interface StateBuilder<T extends State> {

    /**
     * Builds and returns the State definition.
     *
     * @return the configured State
     */
    T build();

    /**
     * Completes this state configuration and returns to the StatesConfigurer.
     *
     * @return the parent StatesConfigurer
     */
    StatesConfigurer and();
}
