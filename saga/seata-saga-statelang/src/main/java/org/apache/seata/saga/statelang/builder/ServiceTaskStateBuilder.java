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

import java.util.Arrays;
import java.util.Map;

/**
 * 对应类图的 ServiceTaskState 实现类
 * 继承自 TaskStateBuilder，复用公共属性
 */
public class ServiceTaskStateBuilder extends TaskStateBuilder {

    public ServiceTaskStateBuilder(StateMachineBuilder parent, String stateName, Map<String, Object> nodeConfig) {
        super(parent, stateName, nodeConfig);
    }

    // ========== ServiceTask 专属属性 ==========
    /**
     * ServiceName element
     */
    public ServiceTaskStateBuilder serviceName(String serviceName) {
        nodeConfig.put("ServiceName", serviceName);
        return this;
    }

    /**
     * ServiceMethod element
     */
    public ServiceTaskStateBuilder serviceMethod(String serviceMethod) {
        nodeConfig.put("ServiceMethod", serviceMethod);
        return this;
    }

    /**
     * CompensateState element
     */
    public ServiceTaskStateBuilder compensateState(String compensateStateName) {
        nodeConfig.put("CompensateState", compensateStateName);
        return this;
    }

    /**
     * Input element
     */
    public ServiceTaskStateBuilder input(Object... inputParams) {
        nodeConfig.put("Input", Arrays.asList(inputParams));
        return this;
    }

    /**
     * Output element
     */
    public ServiceTaskStateBuilder output(Map<String, Object> output) {
        nodeConfig.put("Output", output);
        return this;
    }

    /**
     * IsForUpdate element
     */
    public ServiceTaskStateBuilder forUpdate(boolean isForUpdate) {
        nodeConfig.put("IsForUpdate", isForUpdate);
        return this;
    }

    /**
     * parameterTypes element
     */
    public ServiceTaskStateBuilder parameterTypes(String... parameterTypes) {
        nodeConfig.put("ParameterTypes", Arrays.asList(parameterTypes));
        return this;
    }


    @Override
    public ServiceTaskStateBuilder next(String nextState) {
        BuilderHelper.setNext(parent.getStates(), stateName, nextState);
        return this;
    }
}
