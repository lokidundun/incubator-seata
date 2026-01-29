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

import org.apache.seata.saga.statelang.domain.TaskState;
import org.apache.seata.saga.statelang.domain.impl.AbstractTaskState;
import org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for task state builders.
 * Provides common properties for ServiceTask, ScriptTask, etc.
 *
 * @param <T> the type of TaskState this builder creates
 * @param <B> the concrete builder type for fluent chaining
 */
public abstract class AbstractTaskStateBuilder<T extends TaskState, B extends AbstractTaskStateBuilder<T, B>>
        implements StateBuilder<T> {

    protected String name;
    protected String comment;
    protected String next;
    protected String compensateState;
    protected boolean isForCompensation = false;
    protected boolean isAsync = false;
    protected List<AbstractTaskState.Retry> retry;
    protected List<AbstractTaskState.ExceptionMatch> catches;
    protected List<Object> inputExpressions;
    protected Map<String, Object> outputExpressions;
    protected boolean isPersist = true;

    protected final StatesConfigurer statesConfigurer;

    public AbstractTaskStateBuilder(String name, StatesConfigurer statesConfigurer) {
        this.name = name;
        this.statesConfigurer = statesConfigurer;
    }

    /**
     * Sets the name of this state.
     *
     * @param name the state name
     * @return this builder
     */
    public B withName(String name) {
        this.name = name;
        return (B) this;
    }

    /**
     * Sets a comment for this state.
     *
     * @param comment the comment
     * @return this builder
     */
    public B withComment(String comment) {
        this.comment = comment;
        return (B) this;
    }

    /**
     * Sets the next state to transition to after this state completes.
     *
     * @param nextState the name of the next state
     * @return this builder
     */
    public B withNext(String nextState) {
        this.next = nextState;
        return (B) this;
    }

    /**
     * Sets the compensation state for this task.
     *
     * @param compensateState the name of the compensation state
     * @return this builder
     */
    public B withCompensateState(String compensateState) {
        this.compensateState = compensateState;
        return (B) this;
    }

    /**
     * Sets whether this is a compensation task.
     *
     * @param forCompensation true if this is a compensation task
     * @return this builder
     */
    public B withForCompensation(boolean forCompensation) {
        this.isForCompensation = forCompensation;
        return (B) this;
    }

    /**
     * Sets whether to execute asynchronously.
     *
     * @param async true for async execution
     * @return this builder
     */
    public B withAsync(boolean async) {
        this.isAsync = async;
        return (B) this;
    }

    /**
     * Adds a retry configuration.
     *
     * @param maxAttempts the maximum number of retry attempts
     * @param intervalSeconds the interval between retries in seconds
     * @return this builder
     */
    public B withRetry(int maxAttempts, double intervalSeconds) {
        return withRetry(maxAttempts, intervalSeconds, 1.0);
    }

    /**
     * Adds a retry configuration with backoff rate.
     *
     * @param maxAttempts the maximum number of retry attempts
     * @param intervalSeconds the interval between retries in seconds
     * @param backoffRate the backoff rate multiplier
     * @return this builder
     */
    public B withRetry(int maxAttempts, double intervalSeconds, double backoffRate) {
        AbstractTaskState.RetryImpl retryImpl = new AbstractTaskState.RetryImpl();
        retryImpl.setMaxAttempts(maxAttempts);
        retryImpl.setIntervalSeconds(intervalSeconds);
        retryImpl.setBackoffRate(backoffRate);
        if (this.retry == null) {
            this.retry = new ArrayList<>();
        }
        this.retry.add(retryImpl);
        return (B) this;
    }

    /**
     * Adds an exception catch configuration.
     *
     * @param exceptionClass the exception class to catch
     * @param nextState the state to transition to when this exception is caught
     * @return this builder
     */
    public B withCatch(Class<? extends Throwable> exceptionClass, String nextState) {
        AbstractTaskState.ExceptionMatchImpl exceptionMatch = new AbstractTaskState.ExceptionMatchImpl();
        exceptionMatch.setExceptions(Collections.singletonList(exceptionClass.getName()));
        exceptionMatch.setNext(nextState);
        if (this.catches == null) {
            this.catches = new ArrayList<>();
        }
        this.catches.add(exceptionMatch);
        return (B) this;
    }

    /**
     * Sets the input expressions for this task.
     *
     * @param inputExpressions the input expressions (SpEL)
     * @return this builder
     */
    public B withInput(Object... inputExpressions) {
        this.inputExpressions = Arrays.asList(inputExpressions);
        return (B) this;
    }

    /**
     * Sets the output expressions for this task.
     *
     * @param outputExpressions a map of variable name to expression (SpEL)
     * @return this builder
     */
    public B withOutput(Map<String, Object> outputExpressions) {
        this.outputExpressions = outputExpressions;
        return (B) this;
    }

    /**
     * Sets whether to persist execution for this task.
     *
     * @param persist true to persist execution log
     * @return this builder
     */
    public B withPersist(boolean persist) {
        this.isPersist = persist;
        return (B) this;
    }

    /**
     * Gets the name of this state.
     *
     * @return the state name
     */
    public String getName() {
        return name;
    }

    @Override
    public StatesConfigurer and() {
        return statesConfigurer;
    }

    /**
     * Applies common properties to the task state.
     *
     * @param state the state to configure
     */
    protected void applyCommonProperties(T state) {
        AbstractTaskState taskState = (AbstractTaskState) state;
        taskState.setName(name);
        taskState.setComment(comment);
        taskState.setNext(next);
        taskState.setCompensateState(compensateState);
        taskState.setForCompensation(isForCompensation);
        if (state instanceof ServiceTaskStateImpl) {
            ((ServiceTaskStateImpl) state).setAsync(isAsync);
        }
        if (retry != null) {
            taskState.setRetry(retry);
        }
        if (catches != null) {
            taskState.setCatches(catches);
        }
        if (inputExpressions != null) {
            taskState.setInputExpressions(inputExpressions);
        }
        if (outputExpressions != null) {
            taskState.setOutputExpressions(outputExpressions);
        }
        taskState.setPersist(isPersist);
    }
}
