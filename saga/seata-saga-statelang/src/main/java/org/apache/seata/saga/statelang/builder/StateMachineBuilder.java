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
public class StateMachineBuilder implements StateBuilder<StateMachineImpl> {

    private final Map<String, Object> root = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> states = new LinkedHashMap<>();

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
        root.put("StartState", stateName);
        return this;
    }

    public StateMachineBuilder comment(String comment) {
        root.put("Comment", comment);
        return this;
    }

    public ServiceTaskStateBuilder serviceTask(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", StateType.SERVICE_TASK.name());
        states.put(name, node);
        return new ServiceTaskStateBuilder(this, name, node);
    }

    public ChoiceStateBuilder choice(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", StateType.CHOICE.name());
        states.put(name, node);
        return new ChoiceStateBuilder(this, name, node);
    }

    public StateMachineBuilder compensationTrigger(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", StateType.COMPENSATION_TRIGGER.name());
        states.put(name, node);
        return this;
    }

    public StateMachineBuilder compensationTrigger(String name, String nextState) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", StateType.COMPENSATION_TRIGGER.name());
        node.put("Next", nextState);
        states.put(name, node);
        return this;
    }

    // 在 StateMachineBuilder 类中新增以下方法
    public ScriptTaskStateBuilder scriptTask(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", StateType.SCRIPT_TASK.name());
        states.put(name, node);
        return new ScriptTaskStateBuilder(this, name, node);
    }

    public LoopStartStateBuilder loopStart(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", StateType.LOOP_START.name());
        states.put(name, node);
        return new LoopStartStateBuilder(this, name, node);
    }

    public CompensateSubStateMachineStateBuilder compensateSubStateMachine(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", StateType.SUB_MACHINE_COMPENSATION.name());
        states.put(name, node);
        return new CompensateSubStateMachineStateBuilder(this, name, node);
    }


    // TODO 这个可以考虑拆出来，和原本的代码格式对其
    public StateMachineBuilder succeed(String name) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", StateType.SUCCEED.name());
        states.put(name, node);
        return this;
    }

    public StateMachineBuilder fail(String name, String errorCode, String message) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Type", StateType.FAIL.name());
        node.put("ErrorCode", errorCode);
        node.put("Message", message);
        states.put(name, node);
        return this;
    }

    // ========== 原有方法：生成JSON配置 【完全保留，一行不改】 ==========
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

    // ========== 原有方法：生成领域模型 【完全保留，适配你的实体类，零编译错误】 ==========
    public StateMachineImpl buildModel() {
        StateMachineImpl stateMachine = new StateMachineImpl();
        stateMachine.setName((String) root.get("Name"));
        stateMachine.setVersion((String) root.get("Version"));
        stateMachine.setComment((String) root.get("Comment"));
        stateMachine.setStartState((String) root.get("StartState"));

        Map<String, State> stateMap = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : states.entrySet()) {
            String stateName = entry.getKey();
            Map<String, Object> stateConfig = entry.getValue();
            String stateType = (String) stateConfig.get("Type");
            State state = buildStateInstance(stateName, stateType, stateConfig);
            if (state != null) {
                stateMap.put(stateName, state);
            }
        }
        stateMachine.setStates(stateMap);
        return stateMachine;
    }

    // ========== 【Java标准Builder模式核心】实现StateBuilder接口的build()方法 ==========
    // ✅ 这是Java标准Builder的规范方法，和StringBuilder/build()完全一致
    // ✅ 底层调用buildModel()，兼容原有逻辑，业务层可自由选择build()或buildModel()
    @Override
    public StateMachineImpl build() {
        return buildModel();
    }

    // ========== 构建具体状态实例 【完全保留，适配你的所有StateImpl，零修改】 ==========
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

    private ServiceTaskStateImpl buildServiceTaskState(String name, Map<String, Object> config) {
        ServiceTaskStateImpl state = new ServiceTaskStateImpl();
        state.setName(name);
        state.setNext((String) config.get("Next"));
        state.setCompensateState((String) config.get("CompensateState"));
        state.setInput((List<Object>) config.get("Input"));
        state.setOutput((Map<String, Object>) config.get("Output"));
        state.setStatus((Map<String, String>) config.get("Status"));
        state.setServiceName((String) config.get("ServiceName"));
        state.setServiceMethod((String) config.get("ServiceMethod"));
        // 高级属性赋值 - 你的实体类已有的IsAsync
        if (config.containsKey("IsAsync")) {
            state.setAsync((boolean) config.get("IsAsync"));
        }
        return state;
    }

    private ChoiceStateImpl buildChoiceState(String name, Map<String, Object> config) {
        ChoiceStateImpl state = new ChoiceStateImpl();
        state.setName(name);
        state.setNext((String) config.get("Next"));
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
        state.setChoices(choiceList);
        state.setDefaultChoice((String) config.get("Default"));
        return state;
    }

    private CompensationTriggerStateImpl buildCompensationTriggerState(String name, Map<String, Object> config) {
        CompensationTriggerStateImpl state = new CompensationTriggerStateImpl();
        state.setName(name);
        state.setNext((String) config.get("Next"));
        return state;
    }

    private SucceedEndStateImpl buildSucceedState(String name) {
        SucceedEndStateImpl state = new SucceedEndStateImpl();
        state.setName(name);
        return state;
    }

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
