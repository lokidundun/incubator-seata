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

import org.apache.seata.saga.statelang.domain.LoopStartState;
import org.apache.seata.saga.statelang.domain.impl.LoopStartStateImpl;

/**
 * Builder for creating LoopStartState definitions.
 * LoopStartState is used to define the start of a loop/iteration.
 *
 * @author xingfudeshi@gmail.com
 */
public class LoopStartStateBuilder implements StateBuilder<LoopStartState> {

    private final String name;
    private final StatesConfigurer statesConfigurer;
    private String comment;
    private String next;
    private String collection;
    private String elementVariableName;
    private String elementIndexName;
    private String completionCondition;
    private int parallel = 1;

    public LoopStartStateBuilder(String name, StatesConfigurer statesConfigurer) {
        this.name = name;
        this.statesConfigurer = statesConfigurer;
    }

    /**
     * Sets a comment for this state.
     *
     * @param comment the comment
     * @return this builder
     */
    public LoopStartStateBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Sets the next state to transition to after this state completes.
     *
     * @param nextState the name of the next state
     * @return this builder
     */
    public LoopStartStateBuilder withNext(String nextState) {
        this.next = nextState;
        return this;
    }

    /**
     * Sets the collection object name to iterate over.
     *
     * @param collection the collection object name
     * @return this builder
     */
    public LoopStartStateBuilder withCollection(String collection) {
        this.collection = collection;
        return this;
    }

    /**
     * Sets the variable name for each element in the collection.
     *
     * @param elementVariableName the element variable name
     * @return this builder
     */
    public LoopStartStateBuilder withElementVariableName(String elementVariableName) {
        this.elementVariableName = elementVariableName;
        return this;
    }

    /**
     * Sets the variable name for the loop counter index.
     *
     * @param elementIndexName the element index name (default: loopCounter)
     * @return this builder
     */
    public LoopStartStateBuilder withElementIndexName(String elementIndexName) {
        this.elementIndexName = elementIndexName;
        return this;
    }

    /**
     * Sets the completion condition expression.
     *
     * @param completionCondition the SpEL expression for completion condition
     * @return this builder
     */
    public LoopStartStateBuilder withCompletionCondition(String completionCondition) {
        this.completionCondition = completionCondition;
        return this;
    }

    /**
     * Sets the number of parallel iterations.
     *
     * @param parallel the number of parallel threads (default: 1)
     * @return this builder
     */
    public LoopStartStateBuilder withParallel(int parallel) {
        this.parallel = parallel;
        return this;
    }

    @Override
    public LoopStartState build() {
        LoopStartStateImpl state = new LoopStartStateImpl();
        state.setName(name);
        state.setComment(comment);
        state.setNext(next);
        state.setCollection(collection);
        state.setElementVariableName(elementVariableName);
        state.setElementIndexName(elementIndexName);
        state.setCompletionCondition(completionCondition);
        state.setParallel(parallel);
        return state;
    }

    @Override
    public StatesConfigurer and() {
        return statesConfigurer;
    }
}
