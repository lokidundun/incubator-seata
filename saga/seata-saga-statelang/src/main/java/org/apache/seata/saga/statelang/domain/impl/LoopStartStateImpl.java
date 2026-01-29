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
package org.apache.seata.saga.statelang.domain.impl;

import org.apache.seata.saga.statelang.domain.LoopStartState;
import org.apache.seata.saga.statelang.domain.StateType;

/**
 * Start the "loop" execution for the state with loop attribute
 *
 */
public class LoopStartStateImpl extends BaseState implements LoopStartState {

    private String collection;
    private String elementVariableName;
    private String elementIndexName;
    private String completionCondition;
    private int parallel = 1;

    public LoopStartStateImpl() {
        setType(StateType.LOOP_START);
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getElementVariableName() {
        return elementVariableName;
    }

    public void setElementVariableName(String elementVariableName) {
        this.elementVariableName = elementVariableName;
    }

    public String getElementIndexName() {
        return elementIndexName;
    }

    public void setElementIndexName(String elementIndexName) {
        this.elementIndexName = elementIndexName;
    }

    public String getCompletionCondition() {
        return completionCondition;
    }

    public void setCompletionCondition(String completionCondition) {
        this.completionCondition = completionCondition;
    }

    public int getParallel() {
        return parallel;
    }

    public void setParallel(int parallel) {
        this.parallel = parallel;
    }
}
