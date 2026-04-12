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
 * Marks a field as an <b>option argument</b> in a command-line context.
 * <p>
 * Option arguments are named parameters (unlike positional ones),
 * typically prefixed with a dash ({@code -}) or double dash ({@code --}),
 * and can have values or act as boolean flags.
 * <p>
 * The annotation is processed by {@link ArgsBinder},
 * which automatically binds parsed {@link com.ancevt.util.args.Args}
 * into the annotated fields of a command object.
 *
 * <h2>Usage examples</h2>
 *
 * <h3>1. Basic example</h3>
 * <pre>{@code
 * class Example {
 *     @OptionArgument(names = {"-p", "--port"}, required = true)
 *     int port;
 *
 *     @OptionArgument(names = {"-v", "--verbose"})
 *     boolean verbose;
 * }
 *
 * Args args = Args.parse("--port 8080 --verbose");
 * Example ex = ArgsBinder.convert(args, Example.class);
 *
 * // ex.port == 8080
 * // ex.verbose == true
 * }</pre>
 *
 * <h3>2. Using the {@code converter} parameter</h3>
 * Custom converters allow transforming string argument values
 * into complex or domain-specific types.
 *
 * <pre>{@code
 * class PortConverter implements ArgumentConverter<Integer> {
 *     public Integer convert(String input) {
 *         return Integer.parseInt(input) + 1000;
 *     }
 * }
 *
 * class Example {
 *     @OptionArgument(names = {"--port"}, converter = PortConverter.class)
 *     int port;
 * }
 *
 * Args args = Args.parse("--port 80");
 * Example ex = ArgsBinder.convert(args, Example.class);
 *
 * // ex.port == 1080
 * }</pre>
 *
 * <h3>3. Boolean flags</h3>
 * When the field type is {@code boolean} or {@code Boolean},
 * the argument is considered a <b>flag</b> — its presence means {@code true}.
 * No explicit value is required.
 *
 * <pre>{@code
 * class Example {
 *     @OptionArgument(names = {"--debug"})
 *     boolean debug;
 * }
 *
 * Args args = Args.parse("--debug");
 * Example ex = ArgsBinder.convert(args, Example.class);
 *
 * // ex.debug == true
 * }</pre>
 *
 * <h3>4. Required options</h3>
 * If {@link #required()} is set to {@code true}, and the argument is missing,
 * an {@link com.ancevt.util.args.ArgsParseException} is thrown.
 *
 * <pre>{@code
 *     @OptionArgument(names = {"--user"}, required = true)
 *     String user;
 * }
 *
 * Args args = Args
 * class Example {.parse(""); // missing
 * Example ex = ArgsBinder.convert(args, Example.class);
 * // -> throws ArgumentParseException: "required parameter [--user] not found"
 * }</pre>
 *
 * <h2>Supported field types</h2>
 * By default, the following types are supported by the internal converter:
 * <ul>
 *   <li>{@link String}</li>
 *   <li>Primitive types: {@code boolean}, {@code int}, {@code long}, {@code float}, {@code double}, {@code short}, {@code byte}</li>
 *   <li>{@link java.util.List} and {@link java.util.Set} (comma-separated values)</li>
 *   <li>{@link Enum} types (case-insensitive match)</li>
 * </ul>
 * For unsupported types, a custom {@link ArgsConverter} must be specified via {@link #converter()}.
 *
 * <h2>Integration with {@link ArgsBinder}</h2>
 * During parsing:
 * <ul>
 *   <li>Binder looks for matching names in {@link #names()}.</li>
 *   <li>If the option exists — extracts its value or sets {@code true} for boolean flags.</li>
 *   <li>If {@link #converter()} is not {@link ArgsConverter.NoConverter}, the converter is instantiated and applied.</li>
 *   <li>If {@link #required()} is {@code true} but the option is missing — parsing fails.</li>
 * </ul>
 *
 * <h2>See also</h2>
 * <ul>
 *   <li>{@link CommandArgument} — for positional arguments</li>
 *   <li>{@link ArgsBinder} — main binding logic</li>
 *   <li>{@link ArgsConverter} — interface for custom converters</li>
 * </ul>
 *
 * @see ArgsBinder
 * @see CommandArgument
 * @see ArgsConverter
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({FIELD})
public @interface OptionArgument {

    /**
     * List of option names (aliases) that can be used to specify this argument.
     * <p>
     * Example: {@code {"-p", "--port"}} allows using either short or long form.
     *
     * @return an array of option names
     */
    String[] names() default {};

    /**
     * Whether this option is mandatory.
     * <p>
     * If {@code true} and the argument is missing in the input,
     * {@link com.ancevt.util.args.ArgsParseException} will be thrown.
     *
     * @return {@code true} if required; {@code false} otherwise
     */
    boolean required() default false;

    /**
     * Specifies a custom converter class implementing {@link ArgsConverter},
     * used to transform the raw string value into the target type.
     * <p>
     * If not specified, the default type conversion mechanism of
     * {@link com.ancevt.util.args.Args} is used.
     * <p>
     * The converter class must have a public no-argument constructor.
     *
     * @return converter class type, or {@link ArgsConverter.NoConverter} by default
     */
    Class<? extends ArgsConverter<?>> converter() default ArgsConverter.NoConverter.class;
}
