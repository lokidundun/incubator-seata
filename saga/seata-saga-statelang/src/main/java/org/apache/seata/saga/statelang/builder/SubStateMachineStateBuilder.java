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

import org.apache.seata.saga.statelang.domain.SubStateMachine;
import org.apache.seata.saga.statelang.domain.impl.SubStateMachineImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builder for creating SubStateMachine definitions.
 * SubStateMachine is used to invoke a child state machine.
 *
 */
public class SubStateMachineStateBuilder
        extends AbstractTaskStateBuilder<SubStateMachine, SubStateMachineStateBuilder> {

    private String stateMachineName;

    public SubStateMachineStateBuilder(String name, StatesConfigurer statesConfigurer) {
        super(name, statesConfigurer);
    }

    /**
     * Sets the name of the sub state machine to invoke.
     *
     * @param stateMachineName the state machine name
     * @return this builder
     */
    public SubStateMachineStateBuilder withStateMachineName(String stateMachineName) {
        this.stateMachineName = stateMachineName;
        return this;
    }

    /**
     * Sets the input parameters for the sub state machine.
     *
     * @param input the input parameters
     * @return this builder
     */
    public SubStateMachineStateBuilder withInput(Map<String, Object> input) {
        if (input != null) {
            List<Object> inputList = new ArrayList<>();
            inputList.add(input);
            super.withInput(inputList.toArray());
        }
        return this;
    }

    @Override
    public SubStateMachine build() {
        SubStateMachineImpl state = new SubStateMachineImpl();
        applyCommonProperties(state);
        state.setStateMachineName(stateMachineName);
        return state;
    }
}
