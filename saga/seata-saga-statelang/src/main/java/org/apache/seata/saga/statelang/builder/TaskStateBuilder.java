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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 抽象父构建器：对应类图的 TaskState 接口
 * ServiceTaskStateBuilder 和 ScriptTaskStateBuilder 的父类，复用公共配置
 */
public abstract class TaskStateBuilder implements SubStateBuilder {
    protected final StateMachineBuilder parent;
    protected final String stateName;
    protected final Map<String, Object> nodeConfig;

    public TaskStateBuilder(StateMachineBuilder parent, String stateName, Map<String, Object> nodeConfig) {
        this.parent = parent;
        this.stateName = stateName;
        this.nodeConfig = nodeConfig;
    }

    // ========== TaskState 公共属性（类图提到的 Retry/Loop/Async） ==========
    /**
     * Retry element
     */
    public TaskStateBuilder addRetry(Map<String, Object> retryConfig) {
        List<Map<String, Object>> retryList = (List<Map<String, Object>>) nodeConfig.getOrDefault("Retry", new ArrayList<>());
        retryList.add(retryConfig);
        nodeConfig.put("Retry", retryList);
        return this;
    }

    /**
     * Loop element
     * 这里先创建一些默认的，用户如果要改则覆盖默认的
     */
    public TaskStateBuilder loop(Map<String, Object> loopConfig) {
        Map<String, Object> loop = new LinkedHashMap<>();
        loop.put("Parallel", 1);
        loop.put("ElementVariableName", "loopElement");
        loop.put("ElementIndexName", "loopCounter");
        loop.put("CompletionCondition", "[nrOfInstances] == [nrOfCompletedInstances]");
        loop.putAll(loopConfig);
        nodeConfig.put("Loop", loopConfig);
        return this;
    }

    /**
     * IsAsync element
     */
    public TaskStateBuilder async(boolean isAsync) {
        nodeConfig.put("IsAsync", isAsync);
        return this;
    }

    /**
     * IsPersist element
     */
    public TaskStateBuilder persist(boolean isPersist) {
        nodeConfig.put("IsPersist", isPersist);
        return this;
    }

    /**
     * IsRetryPersistModeUpdate element
     */
    public TaskStateBuilder retryPersistModeUpdate(boolean isRetryPersistModeUpdate) {
        nodeConfig.put("IsRetryPersistModeUpdate", isRetryPersistModeUpdate);
        return this;
    }

    /**
     * IsCompensatePersistModeUpdate element
     */
    public TaskStateBuilder compensatePersistModeUpdate(boolean isCompensatePersistModeUpdate) {
        nodeConfig.put("IsCompensatePersistModeUpdate", isCompensatePersistModeUpdate);
        return this;
    }

    /**
     * Status element
     */
    public TaskStateBuilder status(Map<String, String> statusMap) {
        nodeConfig.put("Status", statusMap);
        return this;
    }

    /**
     * intervalSeconds element 重试事件间隔
     */
    public TaskStateBuilder retryIntervalSeconds(double intervalSeconds) {
        Map<String, Object> lastRetry = getLastRetryConfig();
        if(lastRetry != null){
            lastRetry.put("IntervalSeconds", intervalSeconds);
        }
        return this;
    }

    // 私有方法：获取最后一个重试规则，用于单独配置属性
    private Map<String, Object> getLastRetryConfig() {
        List<Map<String, Object>> retryList = (List<Map<String, Object>>) nodeConfig.getOrDefault("Retry", new ArrayList<>());
        if(retryList.isEmpty()){
            return null;
        }
        return retryList.get(retryList.size()-1);
    }

    // ========== 实现 SubStateBuilder 接口 ==========
    @Override
    public StateMachineBuilder end() {
        return parent;
    }

    @Override
    public StateMachineBuilder endStateBuilder() {
        return end();
    }
}
