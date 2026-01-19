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

public interface SubStateBuilder {

    /**
     * 标准方法：完成当前子节点的配置，返回顶层的StateMachineBuilder
     * 等价于你原来的 endStateBuilder()，语义更贴合标准Builder，兼容原有方法
     */
    StateMachineBuilder end();

    /**
     * 兼容你原有代码的方法名，避免业务层修改代码，底层调用and()即可
     */
    default StateMachineBuilder endStateBuilder() {
        return end();
    }

    /**
     * 返回下一个节点的状态
     */
    SubStateBuilder next(String nextState);
}
