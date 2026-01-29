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

import org.apache.seata.saga.statelang.domain.ServiceTaskState;
import org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating ServiceTaskState definitions.
 * ServiceTask is used to invoke a service method.
 *
 */
public class ServiceTaskStateBuilder extends AbstractTaskStateBuilder<ServiceTaskState, ServiceTaskStateBuilder> {

    private String serviceType;
    private String serviceName;
    private String serviceMethod;
    private List<String> parameterTypes;

    public ServiceTaskStateBuilder(String name, StatesConfigurer statesConfigurer) {
        super(name, statesConfigurer);
    }

    /**
     * Sets the service type (e.g., "spring", "script").
     *
     * @param serviceType the service type
     * @return this builder
     */
    public ServiceTaskStateBuilder withServiceType(String serviceType) {
        this.serviceType = serviceType;
        return this;
    }

    /**
     * Sets the name of the service to invoke.
     *
     * @param serviceName the service bean name
     * @return this builder
     */
    public ServiceTaskStateBuilder withServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    /**
     * Sets the method name to invoke on the service.
     *
     * @param method the method name
     * @return this builder
     */
    public ServiceTaskStateBuilder withServiceMethod(String method) {
        this.serviceMethod = method;
        return this;
    }

    /**
     * Sets the parameter types for the service method.
     *
     * @param parameterTypes the parameter class types
     * @return this builder
     */
    public ServiceTaskStateBuilder withParameterTypes(Class<?>... parameterTypes) {
        List<String> typeNames = new ArrayList<>();
        for (Class<?> type : parameterTypes) {
            typeNames.add(type.getName());
        }
        this.parameterTypes = typeNames;
        return this;
    }

    @Override
    public ServiceTaskState build() {
        ServiceTaskStateImpl state = new ServiceTaskStateImpl();
        applyCommonProperties(state);
        state.setServiceType(serviceType);
        state.setServiceName(serviceName);
        state.setServiceMethod(serviceMethod);
        if (parameterTypes != null) {
            state.setParameterTypes(parameterTypes);
        }
        return state;
    }
}
