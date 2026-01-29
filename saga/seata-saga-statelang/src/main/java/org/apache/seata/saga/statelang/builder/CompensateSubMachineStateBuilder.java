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

import org.apache.seata.saga.statelang.domain.CompensateSubStateMachineState;
import org.apache.seata.saga.statelang.domain.impl.CompensateSubStateMachineStateImpl;

/**
 * Builder for creating CompensateSubStateMachineState definitions.
 * CompensateSubStateMachineState is used to compensate a child state machine.
 *
 */
public class CompensateSubMachineStateBuilder
        extends AbstractTaskStateBuilder<CompensateSubStateMachineState, CompensateSubMachineStateBuilder> {

    private String serviceName;
    private String serviceMethod;

    public CompensateSubMachineStateBuilder(String name, StatesConfigurer statesConfigurer) {
        super(name, statesConfigurer);
    }

    /**
     * Sets the name of the service to invoke.
     *
     * @param serviceName the service bean name
     * @return this builder
     */
    public CompensateSubMachineStateBuilder withServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    /**
     * Sets the method name to invoke on the service.
     *
     * @param method the method name
     * @return this builder
     */
    public CompensateSubMachineStateBuilder withServiceMethod(String method) {
        this.serviceMethod = method;
        return this;
    }

    @Override
    public CompensateSubStateMachineState build() {
        CompensateSubStateMachineStateImpl state = new CompensateSubStateMachineStateImpl();
        applyCommonProperties(state);
        state.setServiceName(serviceName);
        state.setServiceMethod(serviceMethod);
        return state;
    }
}
