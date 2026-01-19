package org.apache.seata.saga.statelang.builder;

import java.util.Arrays;
import java.util.Map;

/**
 * 对应类图的 ScriptTaskState 实现类
 * 继承自 TaskStateBuilder，复用公共属性
 */
public class ScriptTaskStateBuilder extends TaskStateBuilder {

    public ScriptTaskStateBuilder(StateMachineBuilder parent, String stateName, Map<String, Object> nodeConfig) {
        super(parent, stateName, nodeConfig);
    }

    // ========== ScriptTask 专属属性 ==========
    public ScriptTaskStateBuilder scriptType(String scriptType) {
        nodeConfig.put("ScriptType", scriptType);
        return this;
    }

    public ScriptTaskStateBuilder scriptContent(String scriptContent) {
        nodeConfig.put("ScriptContent", scriptContent);
        return this;
    }

    public ScriptTaskStateBuilder scriptResource(String scriptResource) {
        nodeConfig.put("ScriptResource", scriptResource);
        return this;
    }

    public ScriptTaskStateBuilder compensateState(String compensateStateName) {
        nodeConfig.put("CompensateState", compensateStateName);
        return this;
    }

    public ScriptTaskStateBuilder input(Object... inputParams) {
        nodeConfig.put("Input", Arrays.asList(inputParams));
        return this;
    }

    public ScriptTaskStateBuilder output(Map<String, Object> output) {
        nodeConfig.put("Output", output);
        return this;
    }

    @Override
    public ScriptTaskStateBuilder next(String nextState) {
        BuilderHelper.setNext(parent.getStates(), stateName, nextState);
        return this;
    }
}
