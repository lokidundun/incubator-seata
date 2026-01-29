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

import org.apache.seata.saga.statelang.domain.ScriptTaskState;
import org.apache.seata.saga.statelang.domain.impl.ScriptTaskStateImpl;

/**
 * Builder for creating ScriptTaskState definitions.
 * ScriptTask is used to execute a script (e.g., Groovy).
 *
 */
public class ScriptTaskStateBuilder extends AbstractTaskStateBuilder<ScriptTaskState, ScriptTaskStateBuilder> {

    private String scriptType;
    private String scriptContent;

    public ScriptTaskStateBuilder(String name, StatesConfigurer statesConfigurer) {
        super(name, statesConfigurer);
    }

    /**
     * Sets the script type (e.g., "groovy").
     *
     * @param scriptType the script type
     * @return this builder
     */
    public ScriptTaskStateBuilder withScriptType(String scriptType) {
        this.scriptType = scriptType;
        return this;
    }

    /**
     * Sets the script content.
     *
     * @param scriptContent the script code
     * @return this builder
     */
    public ScriptTaskStateBuilder withScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
        return this;
    }

    @Override
    public ScriptTaskState build() {
        ScriptTaskStateImpl state = new ScriptTaskStateImpl();
        applyCommonProperties(state);
        state.setScriptType(scriptType);
        state.setScriptContent(scriptContent);
        return state;
    }
}
