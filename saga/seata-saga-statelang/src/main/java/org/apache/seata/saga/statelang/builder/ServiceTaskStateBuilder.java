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

import java.util.*;

public class ServiceTaskStateBuilder {

    private final StateMachineBuilder parent;
    private final String name;
    private final Map<String, Object> node;

    public ServiceTaskStateBuilder(StateMachineBuilder parent, String name, Map<String, Object> node) {
        this.parent = parent;
        this.name = name;
        this.node = node;
    }

    public ServiceTaskStateBuilder serviceName(String serviceName) {
        node.put("ServiceName", serviceName);
        return this;
    }

    public ServiceTaskStateBuilder serviceMethod(String serviceMethod) {
        node.put("ServiceMethod", serviceMethod);
        return this;
    }

    public ServiceTaskStateBuilder compensateState(String compensateStateName) {
        node.put("CompensateState", compensateStateName);
        return this;
    }

    public ServiceTaskStateBuilder input(Object... inputParams) {
        node.put("Input", Arrays.asList(inputParams));
        return this;
    }

    public ServiceTaskStateBuilder output(Map<String, Object> output) {
        node.put("Output", output);
        return this;
    }

    public ServiceTaskStateBuilder status(Map<String, String> statusMap) {
        node.put("Status", statusMap);
        return this;
    }

    public ServiceTaskStateBuilder catchExceptions(List<String> exceptions, String nextState) {
        List<Map<String, Object>> catchList = (List<Map<String, Object>>) node.getOrDefault("Catch", new ArrayList<>());
        Map<String, Object> catchItem = new LinkedHashMap<>();
        catchItem.put("Exceptions", exceptions);
        catchItem.put("Next", nextState);
        catchList.add(catchItem);
        node.put("Catch", catchList);
        return this;
    }

    public ServiceTaskStateBuilder next(String nextState) {
        BuilderHelper.setNext(parent.getStates(), name, nextState);
        return this;
    }

    // 1. 基础布尔属性
    public ServiceTaskStateBuilder forUpdate(boolean isForUpdate) {
        node.put("IsForUpdate", isForUpdate);
        return this;
    }

    public ServiceTaskStateBuilder persist(boolean isPersist) {
        node.put("IsPersist", isPersist);
        return this;
    }

    public ServiceTaskStateBuilder async(boolean isAsync) {
        node.put("IsAsync", isAsync);
        return this;
    }

    public ServiceTaskStateBuilder retryPersistModeUpdate(boolean isRetryPersistModeUpdate) {
        node.put("IsRetryPersistModeUpdate", isRetryPersistModeUpdate);
        return this;
    }

    public ServiceTaskStateBuilder compensatePersistModeUpdate(boolean isCompensatePersistModeUpdate) {
        node.put("IsCompensatePersistModeUpdate", isCompensatePersistModeUpdate);
        return this;
    }

    // 2. 重试配置（Retry）
    public ServiceTaskStateBuilder retry(Map<String, Object> retryConfig) {
        node.put("Retry", retryConfig);
        return this;
    }

    // 3. 循环配置（Loop）
    public ServiceTaskStateBuilder loop(Map<String, Object> loopConfig) {
        node.put("Loop", loopConfig);
        return this;
    }

    public StateMachineBuilder endStateBuilder() {
        return parent;
    }


}
