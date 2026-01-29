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

import org.apache.seata.saga.statelang.domain.ChoiceState;
import org.apache.seata.saga.statelang.domain.impl.ChoiceStateImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating ChoiceState definitions.
 * Choice is used for conditional branching.
 *
 */
public class ChoiceStateBuilder implements StateBuilder<ChoiceState> {

    private final String name;
    private final StatesConfigurer statesConfigurer;
    private String comment;
    private String next;
    private final List<ChoiceState.Choice> choices = new ArrayList<>();
    private String defaultNextState;

    public ChoiceStateBuilder(String name, StatesConfigurer statesConfigurer) {
        this.name = name;
        this.statesConfigurer = statesConfigurer;
    }

    /**
     * Sets the name of this state.
     *
     * @param name the state name
     * @return this builder
     */
    public ChoiceStateBuilder withName(String name) {
        return this;
    }

    /**
     * Sets a comment for this state.
     *
     * @param comment the comment
     * @return this builder
     */
    public ChoiceStateBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Sets the next state to transition to after this state completes.
     *
     * @param nextState the name of the next state
     * @return this builder
     */
    public ChoiceStateBuilder withNext(String nextState) {
        this.next = nextState;
        return this;
    }

    /**
     * Adds a choice condition.
     *
     * @param expression the SpEL expression to evaluate
     * @param nextState the state to transition to if the expression evaluates to true
     * @return this builder
     */
    public ChoiceStateBuilder withChoice(String expression, String nextState) {
        ChoiceStateImpl.ChoiceImpl choice = new ChoiceStateImpl.ChoiceImpl();
        choice.setExpression(expression);
        choice.setNext(nextState);
        choices.add(choice);
        return this;
    }

    /**
     * Adds a choice condition (alias for withChoice).
     *
     * @param expression the SpEL expression to evaluate
     * @param nextState the state to transition to if the expression evaluates to true
     * @return this builder
     */
    public ChoiceStateBuilder when(String expression, String nextState) {
        return withChoice(expression, nextState);
    }

    /**
     * Sets the default next state when no choices match.
     *
     * @param nextState the default state name
     * @return this builder
     */
    public ChoiceStateBuilder withDefault(String nextState) {
        this.defaultNextState = nextState;
        return this;
    }

    /**
     * Sets the default next state when no choices match (alias for withDefault).
     *
     * @param nextState the default state name
     * @return this builder
     */
    public ChoiceStateBuilder defaultNext(String nextState) {
        return withDefault(nextState);
    }

    @Override
    public ChoiceState build() {
        ChoiceStateImpl state = new ChoiceStateImpl();
        state.setName(name);
        state.setComment(comment);
        state.setNext(next);
        state.setChoices(new ArrayList<>(choices));
        if (defaultNextState != null) {
            state.setDefaultChoice(defaultNextState);
        }
        return state;
    }

    @Override
    public StatesConfigurer and() {
        return statesConfigurer;
    }
}
