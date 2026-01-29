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
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StateMachineBuilder DSL-style API.
 *
 */
public class StateMachineBuilderTest {

    @Test
    public void testSimpleStateMachine() {
        StateMachine stateMachine = StateMachineBuilder.stateMachineBuilder()
                .withName("simpleTestStateMachine")
                .withVersion("0.0.1")
                .withComment("Simple State Machine for Testing")
                .withStartState("FirstState")
                .withStates()
                .newServiceTask("FirstState")
                .withServiceName("demoService")
                .withServiceMethod("foo")
                .withNext("ChoiceState")
                .and()
                .newChoice("ChoiceState")
                .withChoice("foo == 1", "FirstMatchState")
                .withChoice("foo == 2", "SecondMatchState")
                .withDefault("FailEnd")
                .and()
                .newServiceTask("FirstMatchState")
                .withServiceName("firstMatchService")
                .withServiceMethod("process")
                .withNext("SuccessEnd")
                .and()
                .newServiceTask("SecondMatchState")
                .withServiceName("secondMatchService")
                .withServiceMethod("process")
                .withNext("SuccessEnd")
                .and()
                .succeedEnd("SuccessEnd")
                .failEnd("FailEnd")
                .configure()
                .build();

        assertNotNull(stateMachine);
        assertEquals("simpleTestStateMachine", stateMachine.getName());
        assertEquals("0.0.1", stateMachine.getVersion());
        assertEquals("FirstState", stateMachine.getStartState());

        // Verify states are registered
        Map<String, State> states = stateMachine.getStates();
        assertEquals(6, states.size());
        assertTrue(states.containsKey("FirstState"));
        assertTrue(states.containsKey("ChoiceState"));
        assertTrue(states.containsKey("FirstMatchState"));
        assertTrue(states.containsKey("SecondMatchState"));
        assertTrue(states.containsKey("SuccessEnd"));
        assertTrue(states.containsKey("FailEnd"));

        // Verify state types
        assertEquals(StateType.SERVICE_TASK, states.get("FirstState").getType());
        assertEquals(StateType.CHOICE, states.get("ChoiceState").getType());
        assertEquals(StateType.SUCCEED, states.get("SuccessEnd").getType());
        assertEquals(StateType.FAIL, states.get("FailEnd").getType());

        // Verify transition
        assertEquals("ChoiceState", states.get("FirstState").getNext());
    }

    @Test
    public void testStateMachineWithCompensation() {
        StateMachine stateMachine = StateMachineBuilder.stateMachineBuilder()
                .withName("orderProcess")
                .withVersion("1.0")
                .withStartState("CreateOrder")
                .withStates()
                .newServiceTask("CreateOrder")
                .withServiceName("orderService")
                .withServiceMethod("createOrder")
                .withNext("UpdateInventory")
                .and()
                .newServiceTask("UpdateInventory")
                .withServiceName("inventoryService")
                .withServiceMethod("decrease")
                .withCompensateState("CompensateInventory")
                .withNext("Payment")
                .and()
                .newServiceTask("Payment")
                .withServiceName("paymentService")
                .withServiceMethod("pay")
                .withCompensateState("CompensatePayment")
                .withNext("SuccessEnd")
                .and()
                .newServiceTask("CompensateInventory")
                .withServiceName("inventoryService")
                .withServiceMethod("increase")
                .withForCompensation(true)
                .and()
                .newServiceTask("CompensatePayment")
                .withServiceName("paymentService")
                .withServiceMethod("refund")
                .withForCompensation(true)
                .and()
                .succeedEnd("SuccessEnd")
                .configure()
                .build();

        assertNotNull(stateMachine);

        // Verify compensation states are marked correctly
        ServiceTaskState compensateInventory = (ServiceTaskState) stateMachine.getState("CompensateInventory");
        assertTrue(compensateInventory.isForCompensation());

        ServiceTaskState compensatePayment = (ServiceTaskState) stateMachine.getState("CompensatePayment");
        assertTrue(compensatePayment.isForCompensation());

        // Verify compensation links
        TaskState updateInventory = (TaskState) stateMachine.getState("UpdateInventory");
        assertEquals("CompensateInventory", updateInventory.getCompensateState());

        TaskState payment = (TaskState) stateMachine.getState("Payment");
        assertEquals("CompensatePayment", payment.getCompensateState());
    }

    @Test
    public void testStateMachineWithRetry() {
        StateMachine stateMachine = StateMachineBuilder.stateMachineBuilder()
                .withName("processWithRetry")
                .withStartState("ProcessTask")
                .withStates()
                .newServiceTask("ProcessTask")
                .withServiceName("processService")
                .withServiceMethod("execute")
                .withNext("SuccessEnd")
                .withRetry(3, 1.0)
                .and()
                .succeedEnd("SuccessEnd")
                .failEnd("FailEnd")
                .configure()
                .build();

        assertNotNull(stateMachine);

        ServiceTaskState processTask = (ServiceTaskState) stateMachine.getState("ProcessTask");
        assertNotNull(processTask.getRetry());
        assertEquals(1, processTask.getRetry().size());
        assertEquals(3, processTask.getRetry().get(0).getMaxAttempts());
    }

    @Test
    public void testStateMachineWithScriptTask() {
        StateMachine stateMachine = StateMachineBuilder.stateMachineBuilder()
                .withName("scriptProcess")
                .withStartState("ValidateData")
                .withStates()
                .newScriptTask("ValidateData")
                .withScriptType("groovy")
                .withScriptContent("def validate(data) { return data != null }")
                .withNext("ProcessData")
                .and()
                .newServiceTask("ProcessData")
                .withServiceName("dataProcessor")
                .withServiceMethod("process")
                .withNext("SuccessEnd")
                .and()
                .succeedEnd("SuccessEnd")
                .configure()
                .build();

        assertNotNull(stateMachine);

        ScriptTaskState validateData = (ScriptTaskState) stateMachine.getState("ValidateData");
        assertEquals(StateType.SCRIPT_TASK, validateData.getType());
        assertEquals("groovy", validateData.getScriptType());
        assertNotNull(validateData.getScriptContent());
    }

    @Test
    public void testStateMachineWithCompensationTrigger() {
        StateMachine stateMachine = StateMachineBuilder.stateMachineBuilder()
                .withName("manualCompensation")
                .withStartState("ExecuteStep1")
                .withStates()
                .newServiceTask("ExecuteStep1")
                .withServiceName("service1")
                .withServiceMethod("execute")
                .withNext("ExecuteStep2")
                .and()
                .newServiceTask("ExecuteStep2")
                .withServiceName("service2")
                .withServiceMethod("execute")
                .withNext("ManualCompensationTrigger")
                .and()
                .compensationTrigger("ManualCompensationTrigger")
                .succeedEnd("SuccessEnd")
                .failEnd("FailEnd")
                .configure()
                .build();

        assertNotNull(stateMachine);

        State trigger = stateMachine.getState("ManualCompensationTrigger");
        assertEquals(StateType.COMPENSATION_TRIGGER, trigger.getType());
    }

    @Test
    public void testStateMachineWithOutputExpressions() {
        StateMachine stateMachine = StateMachineBuilder.stateMachineBuilder()
                .withName("outputTest")
                .withStartState("Process")
                .withStates()
                .newServiceTask("Process")
                .withServiceName("processor")
                .withServiceMethod("process")
                .withInput("param1", "param2")
                .withOutput(new HashMap<String, Object>() {
                    {
                        put("key1", "value1");
                        put("key2", "value2");
                    }
                })
                .withNext("SuccessEnd")
                .and()
                .succeedEnd("SuccessEnd")
                .configure()
                .build();

        assertNotNull(stateMachine);

        ServiceTaskStateImpl process = (ServiceTaskStateImpl) stateMachine.getState("Process");
        assertNotNull(process.getInputExpressions());
        assertEquals(2, process.getInputExpressions().size());
        assertNotNull(process.getOutputExpressions());
    }

    @Test
    public void testFailEndState() {
        StateMachine stateMachine = StateMachineBuilder.stateMachineBuilder()
                .withName("failTest")
                .withStartState("Validate")
                .withStates()
                .newServiceTask("Validate")
                .withServiceName("validator")
                .withServiceMethod("validate")
                .withNext("SuccessEnd")
                .and()
                .failEnd("FailEnd")
                .succeedEnd("SuccessEnd")
                .configure()
                .build();

        assertNotNull(stateMachine);

        FailEndState failEnd = (FailEndState) stateMachine.getState("FailEnd");
        assertNotNull(failEnd);
        assertEquals(StateType.FAIL, failEnd.getType());
    }

    @Test
    public void testMissingStartStateThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            StateMachineBuilder.stateMachineBuilder().withName("test").build();
        });
    }

    @Test
    public void testInvalidStartStateThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            StateMachineBuilder.stateMachineBuilder()
                    .withName("test")
                    .withStartState("NonExistentState")
                    .withStates()
                    .succeedEnd("SuccessEnd")
                    .configure()
                    .build();
        });
    }

    @Test
    public void testStateMachineWithLoopStart() {
        StateMachine stateMachine = StateMachineBuilder.stateMachineBuilder()
                .withName("loopTest")
                .withStartState("LoopStart")
                .withStates()
                .newLoopStart("LoopStart")
                .withCollection("$.items")
                .withElementVariableName("item")
                .withElementIndexName("loopCounter")
                .withParallel(2)
                .withCompletionCondition("${loopCounter} >= 10")
                .withNext("ProcessTask")
                .and()
                .newServiceTask("ProcessTask")
                .withServiceName("itemProcessor")
                .withServiceMethod("process")
                .withNext("SuccessEnd")
                .and()
                .succeedEnd("SuccessEnd")
                .configure()
                .build();

        assertNotNull(stateMachine);

        LoopStartStateImpl loopStart = (LoopStartStateImpl) stateMachine.getState("LoopStart");
        assertNotNull(loopStart);
        assertEquals("$.items", loopStart.getCollection());
        assertEquals("item", loopStart.getElementVariableName());
        assertEquals("loopCounter", loopStart.getElementIndexName());
        assertEquals(2, loopStart.getParallel());
        assertEquals("${loopCounter} >= 10", loopStart.getCompletionCondition());
    }

    @Test
    public void testStateMachineWithSubStateMachine() {
        StateMachine stateMachine = StateMachineBuilder.stateMachineBuilder()
                .withName("subMachineTest")
                .withStartState("CallSubMachine")
                .withStates()
                .newSubStateMachine("CallSubMachine")
                .withStateMachineName("subProcess")
                .withNext("SuccessEnd")
                .and()
                .succeedEnd("SuccessEnd")
                .configure()
                .build();

        assertNotNull(stateMachine);

        SubStateMachine subMachine = (SubStateMachine) stateMachine.getState("CallSubMachine");
        assertNotNull(subMachine);
        assertEquals("subProcess", subMachine.getStateMachineName());
    }

    @Test
    public void testStateMachineWithCompensateSubMachine() {
        StateMachine stateMachine = StateMachineBuilder.stateMachineBuilder()
                .withName("compensateSubTest")
                .withStartState("CompensateSub")
                .withStates()
                .newCompensateSubMachine("CompensateSub")
                .withServiceName("compensateService")
                .withServiceMethod("compensate")
                .and()
                .succeedEnd("SuccessEnd")
                .configure()
                .build();

        assertNotNull(stateMachine);

        CompensateSubStateMachineState compensateSub =
                (CompensateSubStateMachineState) stateMachine.getState("CompensateSub");
        assertNotNull(compensateSub);
        assertEquals("compensateService", compensateSub.getServiceName());
        assertEquals("compensate", compensateSub.getServiceMethod());
    }
}
