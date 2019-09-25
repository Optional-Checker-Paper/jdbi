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
package org.jdbi.v3.core.argument;

import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.meta.Beta;

/**
 * Inspect a value with optional static qualified type information and produce an {@link Argument}
 * that binds the value to a prepared statement.
 *
 * <p>Make sure to override {@link Object#toString} in your {@link Argument} instances if you want
 * to be able to log their values with an {@link org.jdbi.v3.core.statement.SqlLogger}.
 *
 * <p>Note that {@code null} is handled specially in a few cases, and a few {@code Jdbi} features
 * assume you'll return an instance of {@link NullArgument} when you intend to bind null.
 */
@FunctionalInterface
@Beta
public interface QualifiedArgumentFactory {
    /**
     * Returns an {@link Argument} for the given value if the factory supports it; empty otherwise.
     *
     * @param type the known qualified type of value. Depending on the situation this may be a full
     *     generic signature e.g. {@link java.lang.reflect.ParameterizedType}, a {@link Class}, or
     *     Object.class if no type information is known.
     * @param value the value to convert into an {@link Argument}
     * @param config the config registry, for composition
     * @return an argument for the given value if this factory supports it, or <code>Optional.empty()
     *     </code> otherwise.
     * @see org.jdbi.v3.core.statement.StatementContext#findArgumentFor(QualifiedType, Object)
     * @see Arguments#findFor(QualifiedType, Object)
     * @see QualifiedType
     */
    Optional<Argument> build(QualifiedType<?> type, Object value, ConfigRegistry config);

    /**
     * Adapts an {@link ArgumentFactory} into a QualifiedArgumentFactory. The returned factory only
     * matches qualified types with zero qualifiers.
     *
     * @param factory the factory to adapt
     */
    static QualifiedArgumentFactory adapt(ArgumentFactory factory) {
        if (factory instanceof ArgumentFactory.Preparable) {
            return adapt((ArgumentFactory.Preparable) factory);
        }
        return (type, value, config) -> type.getQualifiers().equals(
                config.get(Qualifiers.class).findFor(factory.getClass()))
            ? factory.build(type.getType(), value, config)
            : Optional.empty();
    }

    /**
     * Adapts an {@link ArgumentFactory.Preparable} into a QualifiedArgumentFactory.Preparable.
     * The returned factory only matches qualified types with zero qualifiers.
     *
     * @param factory the factory to adapt
     */
    static QualifiedArgumentFactory.Preparable adapt(ArgumentFactory.Preparable factory) {
        return QualifiedArgumentFactory.Preparable.adapt(factory);
    }

    /**
     * QualifiedArgumentFactory extension interface that allows preparing arguments for efficient batch binding.
     */
    @Beta
    interface Preparable extends QualifiedArgumentFactory {
        Optional<Function<Object, Argument>> prepare(QualifiedType<?> type, ConfigRegistry config);

        /**
         * Adapts an {@link ArgumentFactory.Preparable} into a QualifiedArgumentFactory.Preparable
         * The returned factory only matches qualified types with zero qualifiers.
         *
         * @param factory the factory to adapt
         */
        static QualifiedArgumentFactory.Preparable adapt(ArgumentFactory.Preparable factory) {
            return new Preparable() {
                @Override
                public Optional<Argument> build(QualifiedType<?> type, Object value, ConfigRegistry config) {
                    return type.getQualifiers().equals(
                                config.get(Qualifiers.class).findFor(factory.getClass()))
                            ? factory.build(type.getType(), value, config)
                            : Optional.empty();
                }

                @Override
                public Optional<Function<Object, Argument>> prepare(QualifiedType<?> type, ConfigRegistry config) {
                    return type.getQualifiers().equals(
                                config.get(Qualifiers.class).findFor(factory.getClass()))
                            ? factory.prepare(type.getType(), config)
                            : Optional.empty();
                }
            };
        }
    }
}
