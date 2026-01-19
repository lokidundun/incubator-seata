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

import org.apache.seata.saga.statelang.domain.impl.StateMachineImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class test {
    @Test
    public void test() {
        // 构建你指定的 reduceInventoryAndBalance 状态机
        StateMachineBuilder builder = StateMachineBuilder.create("reduceInventoryAndBalance", "0.0.1")
                .comment("reduce inventory then reduce balance in a transaction")
                .startAt("ReduceInventory");

        // 1. ReduceInventory 节点
        Map<String, String> invStatus = new LinkedHashMap<>();
        invStatus.put("#root == true", "SU");
        invStatus.put("#root == false", "FA");
        invStatus.put("$Exception{java.lang.Throwable}", "UN");
        // 替换 Map.of() 为 Java 8 的 LinkedHashMap
        Map<String, Object> invOutput = new LinkedHashMap<>();
        invOutput.put("reduceInventoryResult", "$.#root");
        builder.serviceTask("ReduceInventory")
                .serviceName("inventoryAction")
                .serviceMethod("reduce")
                .compensateState("CompensateReduceInventory")
                .input("$.[businessKey]", "$.[count]")
                .output(invOutput)
                .status(invStatus)
                .next("ChoiceState")
                .endStateBuilder();

        // 2. ChoiceState 分支节点
        builder.choice("ChoiceState")
                .choiceItem("[reduceInventoryResult] == true", "ReduceBalance")
                .defaultChoice("Fail")
                .endStateBuilder();

        // 3. ReduceBalance 节点 (含异常捕获)
        Map<String, String> balanceStatus = new LinkedHashMap<>();
        balanceStatus.put("#root == true", "SU");
        balanceStatus.put("#root == false", "FA");
        balanceStatus.put("$Exception{java.lang.Throwable}", "UN");
        // 替换 Map.of() 为 Java 8 的 LinkedHashMap
        Map<String, Object> mockException = new LinkedHashMap<>();
        mockException.put("throwException", "$.[mockReduceBalanceFail]");
        // 替换 Map.of() 为 Java 8 的 LinkedHashMap
        Map<String, Object> balanceOutput = new LinkedHashMap<>();
        balanceOutput.put("compensateReduceBalanceResult", "$.#root");
        // 替换 List.of() 为 Java 8 的 ArrayList
        List<String> exceptions = new ArrayList<>();
        exceptions.add("java.lang.Throwable");
        builder.serviceTask("ReduceBalance")
                .serviceName("balanceAction")
                .serviceMethod("reduce")
                .compensateState("CompensateReduceBalance")
                .input("$.[businessKey]", "$.[amount]", mockException)
                .output(balanceOutput)
                .status(balanceStatus)
//                .catchExceptions(exceptions, "CompensationTrigger")
                .next("Succeed")
                .endStateBuilder();

        // 4. 补偿节点
        builder.serviceTask("CompensateReduceInventory")
                .serviceName("inventoryAction")
                .serviceMethod("compensateReduce")
                .input("$.[businessKey]")
                .endStateBuilder();

        builder.serviceTask("CompensateReduceBalance")
                .serviceName("balanceAction")
                .serviceMethod("compensateReduce")
                .input("$.[businessKey]")
                .endStateBuilder();

        // 5. 补偿触发+成功/失败节点
        builder.compensationTrigger("CompensationTrigger", "Fail");
        builder.succeed("Succeed");
        builder.fail("Fail", "PURCHASE_FAILED", "purchase failed");

        // ========== 生成目标JSON (和你示例完全一致) ==========
        String targetJson = builder.buildJson();
        System.out.println("✅ 生成标准Saga JSON：\n" + targetJson);

        // ========== 生成无编译错误的领域模型 ==========
        StateMachineImpl stateMachine = builder.buildModel();
        System.out.println("build model successfully!");
        System.out.println("status name: " + stateMachine.getName());
        System.out.println("first node status: " + stateMachine.getStartState());
        System.out.println("all nodes: " + stateMachine.getStates().keySet());
    }

    @Test
    public void test2() {
        StateMachineBuilder builder = StateMachineBuilder.create("reduceInventoryAndBalance", "0.0.1")
                .comment("reduce inventory then reduce balance in a transaction")
                .startAt("ReduceBalance");

        // 配置官网示例中的 Loop 循环
        Map<String, Object> loopConfig = new LinkedHashMap<>();
        loopConfig.put("Parallel", 3);
        loopConfig.put("Collection", "$.[collection]");
        loopConfig.put("ElementVariableName", "element");
        loopConfig.put("ElementIndexName", "loopCounter");
        loopConfig.put("CompletionCondition", "[nrOfCompletedInstances] / [nrOfInstances] >= 0.6");

        // 配置 Input 嵌套 Map（和官网截图完全一致）
        Map<String, Object> nestedInput = new LinkedHashMap<>();
        nestedInput.put("loopCounter", "$.[loopCounter]");
        nestedInput.put("element", "$.[element]");
        nestedInput.put("throwException", "$.[mockReduceBalanceFail]");

        // 配置重试
        Map<String, Object> retryConfig = new LinkedHashMap<>();
        retryConfig.put("Interval", 1000);
        retryConfig.put("MaxAttempts", 3);
        retryConfig.put("BackoffRate", 2.0);
        Map<String, Object> balanceOutput = new LinkedHashMap<>();
        balanceOutput.put("compensateReduceBalanceResult", "$.#root");

        // 构建 ServiceTask 节点（包含所有高级属性）
        builder.serviceTask("ReduceBalance")
                .serviceName("balanceAction")
                .serviceMethod("reduce")
                .compensateState("CompensateReduceBalance")
                // 嵌套 Input Map
                .input("$.[businessKey]", "$.[amount]", nestedInput)
                .output(balanceOutput)
                // 新增高级属性
                .forUpdate(true)
                .persist(true)
                .async(false)
                .retryPersistModeUpdate(false)
                .compensatePersistModeUpdate(false)
//                .retry(retryConfig)
                .loop(loopConfig)
                .next("Succeed")
                .endStateBuilder();

        // 其他节点配置...
        builder.succeed("Succeed");
        builder.fail("Fail", "PURCHASE_FAILED", "purchase failed");

        // 生成 JSON（和官网格式完全一致）
        String targetJson = builder.buildJson();
        System.out.println("✅ 生成标准Saga JSON：\n" + targetJson);
    }

    @Test
    public void test3() {
        StateMachineBuilder builder = StateMachineBuilder.create("fullStateMachine", "1.0")
                .startAt("ReduceInventory");

        // 1. ServiceTaskState（已有）
        builder.serviceTask("ReduceInventory")
                .serviceName("inventoryAction")
                .serviceMethod("reduce")
                .async(true)
//                .loop(Map.of("Parallel", 3))
                .next("ScriptCheck")
                .end();

        // 2. ScriptTaskState（新增）
        builder.scriptTask("ScriptCheck")
                .scriptType("groovy")
                .scriptContent("return amount > 0;")
//                .retry(Map.of("MaxAttempts", 3))
                .next("LoopStart")
                .end();

        // 3. LoopStartState（新增）
        builder.loopStart("LoopStart")
                .loopCondition("$.[loopCount] < 5")
                .maxLoopTimes(5)
                .next("CompensateSubMachine")
                .endState("Succeed")
                .end();

        // 4. CompensateSubStateMachineState（新增）
        builder.compensateSubStateMachine("CompensateSubMachine")
                .subStateMachineName("compensateInventory")
                .subStateMachineVersion("1.0")
                .input("$.[businessKey]")
                .next("Succeed")
                .end();

        // 5. ChoiceState（已有）
        builder.choice("ChoiceState")
                .choiceItem("$.[amount] > 1000", "HighAmount")
                .defaultChoice("NormalAmount")
                .end();

        // 6. 成功/失败节点（已有）
        builder.succeed("Succeed");
        builder.fail("Fail", "ERROR", "failed");

        // 生成 JSON 和领域模型
        String json = builder.buildJson();
        StateMachineImpl stateMachine = builder.build();
        System.out.println(json);
        System.out.println(stateMachine);
    }


}
