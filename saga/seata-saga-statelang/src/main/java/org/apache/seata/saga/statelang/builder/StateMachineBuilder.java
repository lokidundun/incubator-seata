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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.seata.saga.statelang.domain.ChoiceState;
import org.apache.seata.saga.statelang.domain.State;
import org.apache.seata.saga.statelang.domain.StateType;
import org.apache.seata.saga.statelang.domain.impl.*;

import java.util.*;

/**
 * A fluent builder that builds a state machine as a Map/JSON.
 */
public class StateMachineBuilder {

    private final Map<String, Object> root = new LinkedHashMap<>();
    private final Map<String, Map<String,Object>> states = new LinkedHashMap<>();
    private String startState;

    private StateMachineBuilder(String name, String version) {
        root.put("Name", name);
        if (version != null) {
            root.put("Version", version);
        }
        root.put("States", states);
    }


    public static StateMachineBuilder create(String name, String version) {
        return new StateMachineBuilder(name, version);
    }


    // first node status
    public StateMachineBuilder startAt(String stateName) {
        this.startState = stateName;
        root.put("StartState", stateName);
        return this;
    }


    public StateMachineBuilder comment(String commmet) {
        root.put("Comment", commmet);
        return this;
    }


    // start a serviceTask state builder
    public ServiceTaskStateBuilder serviceTask(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", "SERVICE_TASK");
        states.put(name, node);
        return new ServiceTaskStateBuilder(this, name, node);
    }


    // start a choice state builder
    public ChoiceStateBuilder choice(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", "CHOICE");
        // TODO 为什么下面这种会报错，解决一下
//        node.put("Type", StateType.CHOICE.getValue());
        states.put(name, node);
        return new ChoiceStateBuilder(this, name, node);
    }


    public StateMachineBuilder compensationTrigger(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", "COMPENSATION_TRIGGER");
        states.put(name, node);
        return this;
    }

    public StateMachineBuilder compensationTrigger(String name, String nextState) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", "COMPENSATION_TRIGGER");
        node.put("Next", nextState);
        states.put(name, node);
        return this;
    }

    public StateMachineBuilder succeed(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", "SUCCEED");
        states.put(name, node);
        return this;
    }

    public StateMachineBuilder fail(String name, String errorCode, String message) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", "FAIL");
        node.put("ErrorCode", errorCode);
        node.put("Message", message);
        states.put(name, node);
        return this;
    }

    // generate json format
    // TODO 这里还要看一下，应该要用统一后的json序列化
    public String buildJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 关闭默认类型信息输出（关键）
        objectMapper.disableDefaultTyping();
        // 美化输出（和目标JSON格式一致）
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build JSON", e);
        }
    }


    // build domain model
    public StateMachineImpl buildModel() {
        StateMachineImpl stateMachine = new StateMachineImpl();
        // 根节点属性赋值 - 标准Setter
        stateMachine.setName((String) root.get("Name"));
        stateMachine.setVersion((String) root.get("Version"));
        stateMachine.setComment((String) root.get("Comment"));
        stateMachine.setStartState((String) root.get("StartState"));

        Map<String, State> stateMap = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : states.entrySet()) {
            String stateName = entry.getKey();
            Map<String, Object> stateConfig = entry.getValue();
            String stateType = (String) stateConfig.get("Type");

            // 根据类型创建实体类实例，全部使用无参构造 + Setter赋值
            State state = buildStateInstance(stateName, stateType, stateConfig);
            if (state != null) {
                stateMap.put(stateName, state);
            }
        }
        stateMachine.setStates(stateMap);
        return stateMachine;
    }

    /**
     * buildStateInstance
     */
    private State buildStateInstance(String stateName, String stateType, Map<String, Object> config) {
        StateType type = StateType.valueOf(stateType);
        switch (type) {
            case SERVICE_TASK:
                return buildServiceTaskState(stateName, config);
            case CHOICE:
                return buildChoiceState(stateName, config);
            case COMPENSATION_TRIGGER:
                return buildCompensationTriggerState(stateName, config);
            case SUCCEED:
                return buildSucceedState(stateName);
            case FAIL:
                return buildFailState(stateName, config);
            default:
                return null;
        }
    }

    /**
     * 适配用户提供的 ServiceTaskStateImpl 完整源码
     * 继承自AbstractTaskState，全部使用标准Setter赋值
     */
    private ServiceTaskStateImpl buildServiceTaskState(String name, Map<String, Object> config) {
        ServiceTaskStateImpl state = new ServiceTaskStateImpl();
        // BaseState 公共属性 Setter
        state.setName(name);
        state.setNext((String) config.get("Next"));
        state.setCompensateState((String) config.get("CompensateState"));
        state.setInput((List<Object>) config.get("Input"));
        state.setOutput((Map<String, Object>) config.get("Output"));
        state.setStatus((Map<String, String>) config.get("Status"));
        state.setServiceName((String) config.get("ServiceName"));
        state.setServiceMethod((String) config.get("ServiceMethod"));
        return state;
    }

    /**
     * 适配用户提供的 ChoiceStateImpl 完整源码【核心重点】
     * 1. 内部类 ChoiceImpl 实例化
     * 2. JSON的List<Map> 转为 实体类要求的 List<ChoiceState.Choice>
     * 3. 匹配专属方法 setDefaultChoice 而非 setDefault
     */
    private ChoiceStateImpl buildChoiceState(String name, Map<String, Object> config) {
        ChoiceStateImpl state = new ChoiceStateImpl();
        // BaseState 公共属性 Setter
        state.setName(name);
        state.setNext((String) config.get("Next"));

        // 核心转换：JSON Choices -> List<ChoiceImpl>
        List<ChoiceState.Choice> choiceList = new ArrayList<>();
        List<Map<String, Object>> jsonChoices = (List<Map<String, Object>>) config.get("Choices");
        if (Objects.nonNull(jsonChoices) && !jsonChoices.isEmpty()) {
            for (Map<String, Object> jsonChoice : jsonChoices) {
                ChoiceStateImpl.ChoiceImpl choice = new ChoiceStateImpl.ChoiceImpl();
                choice.setExpression((String) jsonChoice.get("Expression"));
                choice.setNext((String) jsonChoice.get("Next"));
                choiceList.add(choice);
            }
        }
        // ChoiceStateImpl 自身属性 Setter
        state.setChoices(choiceList);
        state.setDefaultChoice((String) config.get("Default"));
        return state;
    }

    /**
     * 适配用户提供的 CompensationTriggerStateImpl 完整源码
     */
    private CompensationTriggerStateImpl buildCompensationTriggerState(String name, Map<String, Object> config) {
        CompensationTriggerStateImpl state = new CompensationTriggerStateImpl();
        // BaseState 公共属性 Setter
        state.setName(name);
        state.setNext((String) config.get("Next"));
        return state;
    }

    /**
     * buildSucceedState
     */
    private SucceedEndStateImpl buildSucceedState(String name) {
        SucceedEndStateImpl state = new SucceedEndStateImpl();
        state.setName(name);
        return state;
    }

    /**
     * buildFailState
     */
    private FailEndStateImpl buildFailState(String name, Map<String, Object> config) {
        FailEndStateImpl state = new FailEndStateImpl();
        state.setName(name);
        state.setErrorCode((String) config.get("ErrorCode"));
        state.setMessage((String) config.get("Message"));
        return state;
    }

    public Map<String, Map<String, Object>> getStates() {
        return states;
    }
}
