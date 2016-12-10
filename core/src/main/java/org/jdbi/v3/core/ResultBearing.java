/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core;

public interface ResultBearing
{
    /**
     * Execute the statement.  The given {@code StatementExecutor}
     * decides the execution strategy and return type.
     *
     * Most users will not use this method directly.
     *
     * @param executor the StatementExecutor to use
     * @return the produced results
     */
    <R> R execute(ResultProducer<R> executor);

    /**
     * @return the current statement context
     */
    StatementContext getContext();
}
