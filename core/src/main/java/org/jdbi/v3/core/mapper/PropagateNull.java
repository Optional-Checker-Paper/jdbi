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
package org.jdbi.v3.core.mapper;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Signals that the annotated property signals the presence of the mapped type:
 * reflective mappers should map a null bean if this property is null, rather than a
 * present bean with a null property value.  This is useful e.g. for a {@code LEFT OUTER JOIN}
 * or an optionally-present compound value type.
 */
@Retention(RUNTIME)
@Target({PARAMETER, FIELD, METHOD, TYPE})
public @interface PropagateNull {

    /**
     * When annotating a type, the {@code value} is the column name to check for null.
     * When annotating a property, the {@code value} is unused: instead, the property value is tested against null.
     *
     * @return the column name whose null-ness shall be propagated
     */
    String value() default "";
}
