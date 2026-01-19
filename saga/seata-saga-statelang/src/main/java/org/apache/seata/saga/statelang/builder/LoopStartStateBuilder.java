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

import java.util.Map;

/**
 * 对应类图的 LoopStartState 实现类
 */
public class LoopStartStateBuilder implements SubStateBuilder {
    private final StateMachineBuilder parent;
    private final String stateName;
    private final Map<String, Object> nodeConfig;

    public LoopStartStateBuilder(StateMachineBuilder parent, String stateName, Map<String, Object> nodeConfig) {
        this.parent = parent;
        this.stateName = stateName;
        this.nodeConfig = nodeConfig;
    }

    // ========== LoopStartState 专属属性 ==========
    public LoopStartStateBuilder loopCondition(String loopCondition) {
        nodeConfig.put("LoopCondition", loopCondition);
        return this;
    }

    public LoopStartStateBuilder maxLoopTimes(int maxLoopTimes) {
        nodeConfig.put("MaxLoopTimes", maxLoopTimes);
        return this;
    }

    public LoopStartStateBuilder endState(String endState) {
        nodeConfig.put("EndState", endState);
        return this;
    }

    // ========== 实现 SubStateBuilder 接口 ==========
    @Override
    public StateMachineBuilder end() {
        return parent;
    }

    @Override
    public LoopStartStateBuilder next(String nextState) {
        BuilderHelper.setNext(parent.getStates(), stateName, nextState);
        return this;
    }
}
