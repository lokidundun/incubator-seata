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
 * 对应类图的 CompensateSubStateMachineState 实现类
 */
public class CompensateSubStateMachineStateBuilder implements SubStateBuilder {
    private final StateMachineBuilder parent;
    private final String stateName;
    private final Map<String, Object> nodeConfig;

    public CompensateSubStateMachineStateBuilder(StateMachineBuilder parent, String stateName, Map<String, Object> nodeConfig) {
        this.parent = parent;
        this.stateName = stateName;
        this.nodeConfig = nodeConfig;
    }

    // ========== CompensateSubStateMachine 专属属性 ==========
    public CompensateSubStateMachineStateBuilder subStateMachineName(String subStateMachineName) {
        nodeConfig.put("SubStateMachineName", subStateMachineName);
        return this;
    }

    // 子状态机名称【核心必配】，官网明确要求
    public CompensateSubStateMachineStateBuilder stateMachineName(String stateMachineName) {
        nodeConfig.put("StateMachineName", stateMachineName);
        return this;
    }

    public CompensateSubStateMachineStateBuilder subStateMachineVersion(String subStateMachineVersion) {
        nodeConfig.put("SubStateMachineVersion", subStateMachineVersion);
        return this;
    }

    public CompensateSubStateMachineStateBuilder input(Object... inputParams) {
        nodeConfig.put("Input", Arrays.asList(inputParams));
        return this;
    }

    // 补偿子状态机的输出映射
    public CompensateSubStateMachineStateBuilder output(Map<String, Object> output) {
        nodeConfig.put("Output", output);
        return this;
    }

    // 补偿子状态机的状态判断规则
    public CompensateSubStateMachineStateBuilder status(Map<String, String> statusMap) {
        nodeConfig.put("Status", statusMap);
        return this;
    }

    @Override
    public CompensateSubStateMachineStateBuilder next(String nextState) {
        BuilderHelper.setNext(parent.getStates(), stateName, nextState);
        return this;
    }

    // ========== 实现 SubStateBuilder 接口 ==========
    @Override
    public StateMachineBuilder end() {
        return parent;
    }
}
