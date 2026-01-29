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

import org.apache.seata.saga.statelang.domain.SucceedEndState;
import org.apache.seata.saga.statelang.domain.impl.SucceedEndStateImpl;

/**
 * Builder for creating SucceedEndState definitions.
 * SucceedEnd represents normal termination of the state machine.
 *
 */
public class SucceedEndStateBuilder implements StateBuilder<SucceedEndState> {

    private final String name;
    private final StatesConfigurer statesConfigurer;
    private String comment;

    public SucceedEndStateBuilder(String name, StatesConfigurer statesConfigurer) {
        this.name = name;
        this.statesConfigurer = statesConfigurer;
    }

    /**
     * Sets a comment for this state.
     *
     * @param comment the comment
     * @return this builder
     */
    public SucceedEndStateBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    @Override
    public SucceedEndState build() {
        SucceedEndStateImpl state = new SucceedEndStateImpl();
        state.setName(name);
        state.setComment(comment);
        return state;
    }

    @Override
    public StatesConfigurer and() {
        return statesConfigurer;
    }
}
