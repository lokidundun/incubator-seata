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

import org.apache.seata.saga.statelang.domain.FailEndState;
import org.apache.seata.saga.statelang.domain.impl.FailEndStateImpl;

/**
 * Builder for creating FailEndState definitions.
 * FailEnd represents abnormal termination of the state machine.
 *
 */
public class FailEndStateBuilder implements StateBuilder<FailEndState> {

    private final String name;
    private final StatesConfigurer statesConfigurer;
    private String comment;
    private String errorCode;
    private String message;

    public FailEndStateBuilder(String name, StatesConfigurer statesConfigurer) {
        this.name = name;
        this.statesConfigurer = statesConfigurer;
    }

    /**
     * Sets a comment for this state.
     *
     * @param comment the comment
     * @return this builder
     */
    public FailEndStateBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Sets the error code for this failure.
     *
     * @param errorCode the error code
     * @return this builder
     */
    public FailEndStateBuilder withErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    /**
     * Sets the error message for this failure.
     *
     * @param message the error message
     * @return this builder
     */
    public FailEndStateBuilder withMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public FailEndState build() {
        FailEndStateImpl state = new FailEndStateImpl();
        state.setName(name);
        state.setComment(comment);
        state.setErrorCode(errorCode);
        state.setMessage(message);
        return state;
    }

    @Override
    public StatesConfigurer and() {
        return statesConfigurer;
    }
}
