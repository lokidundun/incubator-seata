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

public class ChoiceStateBuilder {

    private final StateMachineBuilder parent;
    private final String name;
    private final Map<String, Object> node;

    public ChoiceStateBuilder(StateMachineBuilder parent, String name, Map<String, Object> node) {
        this.parent = parent;
        this.name = name;
        this.node = node;
    }

    public ChoiceStateBuilder choiceItem(String expression, String nextState) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) node.getOrDefault("Choices", new ArrayList<>());
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("Expression", expression);
        choice.put("Next", nextState);
        choices.add(choice);
        node.put("Choices", choices);
        return this;
    }

    public ChoiceStateBuilder defaultChoice(String nextState) {
        node.put("Default", nextState);
        return this;
    }

    public StateMachineBuilder endStateBuilder() {
        return parent;
    }
}
