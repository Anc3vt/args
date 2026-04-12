/*
 * Copyright (C) 2026 Ancevt.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
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
package com.ancevt.util.args.reflection;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * Marks a field as a command argument.
 * <p>
 * Command arguments are positional (not prefixed with - or --).
 * They are bound in the order they appear in the command line.
 * <p>
 * Example:
 * <pre>
 * class Example {
 *     {@literal @}CommandArgument
 *     String name;
 * }
 *
 * Args args = Args.parse("hello");
 * Example ex = ArgsBinder.convert(args, Example.class);
 * // ex.name == "hello"
 * </pre>
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({FIELD})
public @interface CommandArgument {
    boolean required() default true;

    int index() default 0;
}
