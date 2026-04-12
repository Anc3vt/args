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

/**
 * Represents a functional interface for converting raw string arguments
 * into typed Java objects during command-line argument binding.
 * <p>
 * Converters are primarily used with the {@link OptionArgument#converter()}
 * and (optionally) with {@link CommandArgument}-based fields to override
 * the default type conversion performed by
 * {@link com.ancevt.util.args.Args}.
 *
 * <h2>Purpose</h2>
 * By default, the REPLines argument system automatically converts simple types
 * (e.g., {@code int}, {@code boolean}, {@code double}, {@code Enum}, etc.).
 * However, in many cases, developers need to handle complex or domain-specific
 * types — for example, ports, file paths, hostnames, color codes, etc.
 * <p>
 * Implementing a custom {@code ArgumentConverter} allows defining this logic
 * declaratively and linking it via annotation configuration.
 *
 * <h2>Usage Example</h2>
 *
 * <h3>1. Custom converter class</h3>
 * <pre>{@code
 * class PortConverter implements ArgumentConverter<Integer> {
 *     @Override
 *     public Integer convert(String input) {
 *         int port = Integer.parseInt(input);
 *         if (port < 1024) {
 *             throw new IllegalArgumentException("Port must be >= 1024");
 *         }
 *         return port;
 *     }
 * }
 * }</pre>
 *
 * <h3>2. Applying converter to an option argument</h3>
 * <pre>{@code
 * class Example {
 *     @OptionArgument(names = {"--port"}, converter = PortConverter.class)
 *     int port;
 * }
 *
 * Args args = Args.parse("--port 8080");
 * Example ex = ArgsBinder.convert(args, Example.class);
 *
 * // ex.port == 8080
 * }</pre>
 *
 * <h3>3. Converters for non-primitive types</h3>
 * <pre>{@code
 * class FileConverter implements ArgumentConverter<File> {
 *     @Override
 *     public File convert(String input) {
 *         return new File(input).getAbsoluteFile();
 *     }
 * }
 *
 * class Example {
 *     @OptionArgument(names = {"--file"}, converter = FileConverter.class)
 *     File file;
 * }
 *
 * Args args = Args.parse("--file ./config.yaml");
 * Example ex = ArgsBuilder.convert(args, Example.class);
 *
 * // ex.file -> /absolute/path/to/config.yaml
 * }</pre>
 *
 * <h2>Implementation requirements</h2>
 * <ul>
 *   <li>Converter must implement a single method {@link #convert(String)}.</li>
 *   <li>Converter class must have a <b>public no-argument constructor</b>.</li>
 *   <li>Conversion should be fast and deterministic; avoid heavy I/O or reflection.</li>
 *   <li>In case of invalid input, throw a meaningful {@link IllegalArgumentException} or a subclass of {@link RuntimeException}.</li>
 * </ul>
 *
 * <h2>Default behavior</h2>
 * The nested {@link NoConverter} implementation serves as a default placeholder.
 * It performs an identity transformation, i.e., returns the same string input unchanged.
 * <p>
 * It is automatically used when no custom converter is specified in
 * {@link OptionArgument#converter()} or {@link CommandArgument}.
 *
 * <h2>See also</h2>
 * <ul>
 *   <li>{@link OptionArgument}</li>
 *   <li>{@link CommandArgument}</li>
 *   <li>{@link ArgsBinder}</li>
 *   <li>{@link com.ancevt.util.args.Args}</li>
 * </ul>
 *
 * @param <T> the type of the converted result
 */
public interface ArgsConverter<T> {

    /**
     * Converts a raw string argument into a value of type {@code T}.
     * <p>
     * The method must be pure — its result should depend only on the input value.
     * In case of an invalid format or conversion failure, implementations
     * should throw a {@link RuntimeException} (e.g., {@link IllegalArgumentException}).
     *
     * @param input the raw string value provided by the user
     * @return a converted and type-safe object representation of the input
     * @throws RuntimeException if conversion fails
     */
    T convert(String input);

    /**
     * Default identity converter used when no specific converter is defined.
     * <p>
     * This implementation simply returns the input string as-is,
     * preserving the default {@link String} representation of arguments.
     * <p>
     * It is automatically assigned by
     * {@link OptionArgument#converter()} when no converter is explicitly set.
     */
    class NoConverter implements ArgsConverter<Object> {
        /**
         * Returns the same string value without modification.
         *
         * @param input input string value
         * @return the same input string
         */
        @Override
        public Object convert(String input) {
            return input;
        }
    }
}
