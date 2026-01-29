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

import org.apache.seata.saga.statelang.domain.*;
import org.apache.seata.saga.statelang.domain.impl.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of StateMachineBuilder.
 * Provides a fluent API for constructing StateMachine definitions.
 *
 */
public class StateMachineBuilderImpl implements StateMachineBuilder, StatesConfigurer {

    private String name;
    private String version = "0.0.1";
    private String startState;
    private String comment;
    private String tenantId;
    private RecoverStrategy recoverStrategy = RecoverStrategy.Forward;
    private boolean isPersist = true;
    private Boolean retryPersistModeUpdate;
    private Boolean compensatePersistModeUpdate;

    private final Map<String, StateBuilder<?>> stateBuilders = new LinkedHashMap<>();
    private final Map<String, State> states = new LinkedHashMap<>();

    public StateMachineBuilderImpl() {}

    @Override
    public StateMachine build() {
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("StateMachine name is required");
        }
        if (startState == null || startState.isEmpty()) {
            throw new IllegalStateException("Start state is required");
        }

        if (!states.containsKey(startState)) {
            throw new IllegalStateException("Start state '" + startState + "' not found in states");
        }

        StateMachineImpl stateMachine = new StateMachineImpl();
        stateMachine.setName(name);
        stateMachine.setVersion(version);
        stateMachine.setStartState(startState);
        stateMachine.setComment(comment);
        stateMachine.setTenantId(tenantId);
        stateMachine.setRecoverStrategy(recoverStrategy);
        stateMachine.setPersist(isPersist);
        if (retryPersistModeUpdate != null) {
            stateMachine.setRetryPersistModeUpdate(retryPersistModeUpdate);
        }
        if (compensatePersistModeUpdate != null) {
            stateMachine.setCompensatePersistModeUpdate(compensatePersistModeUpdate);
        }

        // Set state machine reference for all states
        for (State state : states.values()) {
            if (state instanceof BaseState) {
                ((BaseState) state).setStateMachine(stateMachine);
            }
        }
        stateMachine.setStates(states);

        return stateMachine;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B extends StateBuilder<?>> B build(Class<B> clazz) {
        StateBuilder<?> builder;
        String stateName = "State" + (stateBuilders.size() + 1);

        if (clazz == ServiceTaskStateBuilder.class) {
            builder = new ServiceTaskStateBuilder(stateName, this);
        } else if (clazz == ScriptTaskStateBuilder.class) {
            builder = new ScriptTaskStateBuilder(stateName, this);
        } else if (clazz == ChoiceStateBuilder.class) {
            builder = new ChoiceStateBuilder(stateName, this);
        } else if (clazz == SucceedEndStateBuilder.class) {
            builder = new SucceedEndStateBuilder(stateName, this);
        } else if (clazz == FailEndStateBuilder.class) {
            builder = new FailEndStateBuilder(stateName, this);
        } else if (clazz == CompensationTriggerStateBuilder.class) {
            builder = new CompensationTriggerStateBuilder(stateName, this);
        } else if (clazz == SubStateMachineStateBuilder.class) {
            builder = new SubStateMachineStateBuilder(stateName, this);
        } else if (clazz == CompensateSubMachineStateBuilder.class) {
            builder = new CompensateSubMachineStateBuilder(stateName, this);
        } else if (clazz == LoopStartStateBuilder.class) {
            builder = new LoopStartStateBuilder(stateName, this);
        } else {
            throw new IllegalArgumentException("Unknown StateBuilder class: " + clazz.getName());
        }

        stateBuilders.put(stateName, builder);
        return (B) builder;
    }

    @Override
    public ServiceTaskStateBuilder newServiceTask(String stateName) {
        ServiceTaskStateBuilder builder = new ServiceTaskStateBuilder(stateName, this);
        stateBuilders.put(stateName, builder);
        return builder;
    }

    @Override
    public ScriptTaskStateBuilder newScriptTask(String stateName) {
        ScriptTaskStateBuilder builder = new ScriptTaskStateBuilder(stateName, this);
        stateBuilders.put(stateName, builder);
        return builder;
    }

    @Override
    public ChoiceStateBuilder newChoice(String stateName) {
        ChoiceStateBuilder builder = new ChoiceStateBuilder(stateName, this);
        stateBuilders.put(stateName, builder);
        return builder;
    }

    @Override
    public StatesConfigurer succeedEnd(String stateName) {
        SucceedEndStateImpl state = new SucceedEndStateImpl();
        state.setName(stateName);
        states.put(stateName, state);
        return this;
    }

    @Override
    public StatesConfigurer failEnd(String stateName) {
        FailEndStateImpl state = new FailEndStateImpl();
        state.setName(stateName);
        states.put(stateName, state);
        return this;
    }

    @Override
    public StatesConfigurer compensationTrigger(String stateName) {
        CompensationTriggerStateImpl state = new CompensationTriggerStateImpl();
        state.setName(stateName);
        states.put(stateName, state);
        return this;
    }

    @Override
    public LoopStartStateBuilder newLoopStart(String stateName) {
        LoopStartStateBuilder builder = new LoopStartStateBuilder(stateName, this);
        stateBuilders.put(stateName, builder);
        return builder;
    }

    @Override
    public SubStateMachineStateBuilder newSubStateMachine(String stateName) {
        SubStateMachineStateBuilder builder = new SubStateMachineStateBuilder(stateName, this);
        stateBuilders.put(stateName, builder);
        return builder;
    }

    @Override
    public CompensateSubMachineStateBuilder newCompensateSubMachine(String stateName) {
        CompensateSubMachineStateBuilder builder = new CompensateSubMachineStateBuilder(stateName, this);
        stateBuilders.put(stateName, builder);
        return builder;
    }

    @Override
    public StateMachineBuilder configure() {
        // Build all state builders now that configuration is complete
        for (StateBuilder<?> builder : stateBuilders.values()) {
            states.put(builder.build().getName(), builder.build());
        }
        return this;
    }

    @Override
    public StatesConfigurer withStates() {
        return this;
    }

    @Override
    public StateMachineBuilder withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public StateMachineBuilder withVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public StateMachineBuilder withStartState(String startState) {
        this.startState = startState;
        return this;
    }

    @Override
    public StateMachineBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    @Override
    public StateMachineBuilder withTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public StateMachineBuilder withRecoverStrategy(RecoverStrategy recoverStrategy) {
        this.recoverStrategy = recoverStrategy;
        return this;
    }

    @Override
    public StateMachineBuilder withPersist(boolean persist) {
        this.isPersist = persist;
        return this;
    }

    @Override
    public StateMachineBuilder withRetryPersistModeUpdate(boolean update) {
        this.retryPersistModeUpdate = update;
        return this;
    }

    @Override
    public StateMachineBuilder withCompensatePersistModeUpdate(boolean update) {
        this.compensatePersistModeUpdate = update;
        return this;
    }
}
