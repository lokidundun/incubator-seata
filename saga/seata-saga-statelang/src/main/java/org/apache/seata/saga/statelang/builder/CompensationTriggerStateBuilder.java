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

import org.apache.seata.saga.statelang.domain.CompensationTriggerState;
import org.apache.seata.saga.statelang.domain.impl.CompensationTriggerStateImpl;

/**
 * Builder for creating CompensationTriggerState definitions.
 * CompensationTrigger is used to trigger the compensation process.
 *
 */
public class CompensationTriggerStateBuilder implements StateBuilder<CompensationTriggerState> {

    private final String name;
    private final StatesConfigurer statesConfigurer;
    private String comment;
    private String next;

    public CompensationTriggerStateBuilder(String name, StatesConfigurer statesConfigurer) {
        this.name = name;
        this.statesConfigurer = statesConfigurer;
    }

    /**
     * Sets a comment for this state.
     *
     * @param comment the comment
     * @return this builder
     */
    public CompensationTriggerStateBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Sets the next state to transition to after compensation completes.
     *
     * @param nextState the name of the next state
     * @return this builder
     */
    public CompensationTriggerStateBuilder withNext(String nextState) {
        this.next = nextState;
        return this;
    }

    @Override
    public CompensationTriggerState build() {
        CompensationTriggerStateImpl state = new CompensationTriggerStateImpl();
        state.setName(name);
        state.setComment(comment);
        state.setNext(next);
        return state;
    }

    @Override
    public StatesConfigurer and() {
        return statesConfigurer;
    }
}
