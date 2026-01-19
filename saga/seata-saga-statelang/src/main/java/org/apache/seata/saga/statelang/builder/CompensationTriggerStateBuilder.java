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

import java.util.LinkedHashMap;
import java.util.Map;

public class CompensationTriggerStateBuilder implements SubStateBuilder {
    private final StateMachineBuilder parent;
    private final String stateName;
    private final Map<String, Object> nodeConfig;

    public CompensationTriggerStateBuilder(StateMachineBuilder parent, String stateName) {
        this.parent = parent;
        this.stateName = stateName;
        this.nodeConfig = new LinkedHashMap<>();
        nodeConfig.put("Type", "COMPENSATION_TRIGGER");
        parent.getStates().put(stateName, nodeConfig);
    }

    // ========== CompensationTriggerState 专属配置 ==========
    /**
     * 设置补偿触发后的下一个状态
     */
    @Override
    public CompensationTriggerStateBuilder next(String nextState) {
        BuilderHelper.setNext(parent.getStates(), stateName, nextState);
        return this;
    }

    /**
     * 可选：设置补偿触发的条件表达式
     */
    public CompensationTriggerStateBuilder condition(String condition) {
        nodeConfig.put("Condition", condition);
        return this;
    }


    // ========== 实现 SubStateBuilder 接口（标准规范） ==========
    @Override
    public StateMachineBuilder end() {
        return parent;
    }

    @Override
    public StateMachineBuilder endStateBuilder() {
        return end();
    }
}
