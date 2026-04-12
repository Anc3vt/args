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

package com.ancevt.util.args;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.lang.String.format;

/**
 * Utility class for parsing and handling command line arguments.
 * Supports both space-separated and key=value style arguments.
 *
 * Example:
 * <pre>
 *     Args args = Args.parse("--port=8080 --debug true");
 *     int port = args.get(Integer.class, "--port");
 *     boolean debug = args.get(Boolean.class, "--debug");
 * </pre>
 */
public class Args implements Iterable<String> {

    private final String source;
    private final String[] elements;
    private int index;
    private Throwable problem;
    private String lastContainsCheckedKey;

    /**
     * Creates a new {@code Args} instance by parsing the given source string.
     * Splits the string by spaces.
     *
     * @param source the raw argument string
     */
    public Args(String source) {
        this.source = source;
        elements = ArgsSplitHelper.split(source, '\0');
    }

    /**
     * Creates a new {@code Args} instance by parsing the given source string
     * using a custom delimiter represented as a {@code String}.
     *
     * @param source        the raw argument string
     * @param delimiterChar the delimiter string used to split arguments
     */
    public Args(String source, String delimiterChar) {
        this.source = source;
        elements = ArgsSplitHelper.split(source, delimiterChar);
    }

    /**
     * Creates a new {@code Args} instance by parsing the given source string
     * using a custom delimiter represented as a {@code char}.
     *
     * @param source        the raw argument string
     * @param delimiterChar the delimiter character used to split arguments
     */
    public Args(String source, char delimiterChar) {
        this.source = source;
        elements = ArgsSplitHelper.split(source, delimiterChar);
    }

    /**
     * Creates a new {@code Args} instance from an array of argument strings.
     * The original array is preserved, and a formatted source string is built for reference.
     *
     * @param args the array of argument strings
     */
    public Args(String[] args) {
        this.source = collectSource(args);
        elements = args;
    }

    private String collectSource(String[] args) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (String a : args) {
            a = a.replace("\"", "\\\\\"");
            stringBuilder.append('"').append(a).append('"').append(' ');
        }
        if (stringBuilder.length() > 0) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }

    /**
     * Returns all parsed elements.
     *
     * @return array of argument elements
     */
    public String[] getElements() {
        return elements;
    }

    /**
     * Returns all parsed elements.
     *
     * @return array of argument elements
     */
    public boolean contains(String... keys) {
        for (final String e : elements) {
            for (final String k : keys) {
                if (e.equals(k) || e.startsWith(k + "=")) {
                    lastContainsCheckedKey = k;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if there are more arguments available for iteration.
     *
     * @return true if there are remaining arguments
     */
    public boolean hasNext() {
        return index < elements.length;
    }

    /**
     * Skips the next argument.
     */
    public void skip() {
        next();
    }

    /**
     * Skips the given number of arguments.
     *
     * @param count number of arguments to skip
     */
    public void skip(int count) {
        for (int i = 0; i < count; i++) next();
    }

    /**
     * Returns the next argument as a String.
     *
     * @return the next argument
     */
    public String next() {
        return next(String.class);
    }

    /**
     * Returns the next argument converted to the given type.
     *
     * @param type target type
     * @param <T>  type parameter
     * @return argument converted to type
     * @throws ArgsParseException if no more elements or conversion fails
     */
    public <T> T next(Class<T> type) {
        if (index >= elements.length) {
            throw new ArgsParseException(format("next: Index out of bounds, index: %d, elements: %d", index, elements.length));
        }

        T result = get(type, index);
        if (result == null) {
            throw new ArgsParseException(String.format("Args exception no such element at index %d, type: %s", index, type));
        }

        index++;
        return result;
    }

    /**
     * Returns the next argument converted to the given type, or a default if unavailable.
     *
     * @param type         target type
     * @param defaultValue fallback value
     * @param <T>          type parameter
     * @return argument converted to type or defaultValue
     */
    public <T> T next(Class<T> type, T defaultValue) {
        if (index >= elements.length) {
            throw new ArgsParseException(format("next: Index out of bounds, index: %d, elements: %d", index, elements.length));
        }

        T result = get(type, index, defaultValue);
        index++;
        return result;
    }

    /**
     * Returns the current index in the arguments array.
     *
     * @return index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the current index.
     *
     * @param index new index
     * @throws ArgsParseException if out of bounds
     */
    public void setIndex(int index) {
        if (index >= elements.length) {
            throw new ArgsParseException(format("Index out of bounds, index: %d, elements: %d", index, elements.length));
        }

        this.index = index;
    }

    /**
     * Resets the index back to 0.
     */
    public void resetIndex() {
        index = 0;
    }

    /**
     * Returns the number of parsed elements.
     *
     * @return element count
     */
    public int size() {
        return elements.length;
    }

    /**
     * Gets the value for the last checked key, converted to the given type.
     *
     * @param type target type
     * @param <T>  type parameter
     * @return value converted to type or null
     */
    public <T> T get(Class<T> type) {
        return get(type, lastContainsCheckedKey);
    }

    /**
     * Gets the argument at the given index as the specified type,
     * or returns a default value if invalid or not found.
     *
     * @param type         target type
     * @param index        index in array
     * @param defaultValue fallback value
     * @param <T>          type parameter
     * @return converted argument or defaultValue
     */
    public <T> T get(Class<T> type, int index, T defaultValue) {
        if (index < 0 || index >= elements.length) return defaultValue;
        try {
            return convertToType(elements[index], type);
        } catch (Exception e) {
            problem = e;
            return defaultValue;
        }
    }

    /**
     * Gets the argument at the given index as the specified type.
     *
     * @param type target type
     * @param index index in array
     * @param <T> type parameter
     * @return converted argument or null
     */
    public <T> T get(Class<T> type, int index) {
        return get(type, index, null);
    }

    /**
     * Gets the value associated with the given key.
     * Supports both "--key value" and "--key=value" styles.
     *
     * @param type target type
     * @param key argument key
     * @param defaultValue fallback value
     * @param <T> type parameter
     * @return converted argument or defaultValue
     */
    public <T> T get(Class<T> type, String key, T defaultValue) {
        for (int i = 0; i < elements.length; i++) {
            final String currentArg = elements[i];

            if (currentArg.equals(key)) {
                if (i + 1 < elements.length) {
                    return convertToType(elements[i + 1], type);
                }
            }

            if (currentArg.startsWith(key + "=")) {
                return convertToType(currentArg.substring((key + "=").length()), type);
            }
        }

        return defaultValue;
    }

    /**
     * Gets the value for any of the given keys.
     *
     * @param type target type
     * @param keys possible keys
     * @param defaultValue fallback value
     * @param <T> type parameter
     * @return converted argument or defaultValue
     */
    public <T> T get(Class<T> type, String[] keys, T defaultValue) {
        for (final String key : keys) {
            for (int i = 0; i < elements.length; i++) {
                final String currentArg = elements[i];

                if (currentArg.equals(key)) {
                    if (i + 1 < elements.length) {
                        return convertToType(elements[i + 1], type);
                    }
                }

                if (currentArg.startsWith(key + "=")) {
                    return convertToType(currentArg.substring((key + "=").length()), type);
                }
            }
        }

        return defaultValue;
    }

    /**
     * Gets the value for the given key.
     *
     * @param type target type
     * @param key argument key
     * @param <T> type parameter
     * @return converted argument or null
     */
    public <T> T get(Class<T> type, String key) {
        return get(type, key, null);
    }

    /**
     * Gets the value for any of the given keys.
     *
     * @param type target type
     * @param keys possible keys
     * @param <T> type parameter
     * @return converted argument or null
     */
    public <T> T get(Class<T> type, String[] keys) {
        return get(type, keys, null);
    }

    /**
     * Gets a String value for the given key or a default.
     *
     * @param key argument key
     * @param defaultValue fallback value
     * @return argument value or defaultValue
     */
    public String get(String key, String defaultValue) {
        return get(String.class, key, defaultValue);
    }

    /**
     * Gets a String value for any of the given keys or a default.
     *
     * @param keys possible keys
     * @param defaultValue fallback value
     * @return argument value or defaultValue
     */
    public String get(String[] keys, String defaultValue) {
        return get(String.class, keys, defaultValue);
    }

    /**
     * Gets a String value for the given key.
     *
     * @param key argument key
     * @return argument value or null
     */
    public String get(String key) {
        return get(String.class, key);
    }

    /**
     * Gets a String value for any of the given keys.
     *
     * @param keys possible keys
     * @return argument value or null
     */
    public String get(String[] keys) {
        return get(String.class, keys);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertToType(String element, Class<T> type) {
        if (List.class.isAssignableFrom(type)) {
            String[] parts = element.split(",");
            return (T) java.util.Arrays.asList(parts);
        }

        if (Set.class.isAssignableFrom(type)) {
            String[] parts = element.split(",");
            return (T) new java.util.HashSet<>(java.util.Arrays.asList(parts));
        }

        if (type == String.class) {
            return (T) element;
        } else if (type == boolean.class || type == Boolean.class) {
            return (T) Boolean.valueOf(element.equalsIgnoreCase("true"));
        } else if (type == int.class || type == Integer.class) {
            return (T) Integer.valueOf(element);
        } else if (type == long.class || type == Long.class) {
            return (T) Long.valueOf(element);
        } else if (type == float.class || type == Float.class) {
            return (T) Float.valueOf(element);
        } else if (type == short.class || type == Short.class) {
            return (T) Short.valueOf(element);
        } else if (type == double.class || type == Double.class) {
            return (T) Double.valueOf(element);
        } else if (type == byte.class || type == Byte.class) {
            return (T) Byte.valueOf(element);
        } else if (type.isEnum()) {
            try {
                @SuppressWarnings("unchecked")
                T value = (T) Enum.valueOf((Class<Enum>) type.asSubclass(Enum.class), element.toUpperCase());
                return value;
            } catch (IllegalArgumentException e) {
                throw new ArgsParseException("Invalid enum value '" + element + "' for " + type.getSimpleName());
            }
        } else {
            throw new ArgsParseException("Type " + type + " not supported");
        }
    }

    /**
     * Returns the original source string of the arguments.
     *
     * @return original command line
     */
    public String getSource() {
        return source;
    }

    /**
     * Checks if there are no arguments.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return elements == null || elements.length == 0;
    }

    /**
     * Checks if there was a problem during type conversion.
     *
     * @return true if a problem occurred
     */
    public boolean hasProblem() {
        return problem != null;
    }

    /**
     * Returns the last exception that occurred during conversion.
     *
     * @return Throwable or null
     */
    public Throwable getProblem() {
        return problem;
    }

    /**
     * Creates an Args instance from a string.
     *
     * @param source command line string
     * @return Args instance
     */
    public static Args parse(String source) {
        return new Args(source);
    }

    /**
     * Creates an Args instance from an array.
     *
     * @param args array of arguments
     * @return Args instance
     */
    public static Args parse(String[] args) {
        return new Args(args);
    }

    /**
     * Creates an Args instance with a custom delimiter.
     *
     * @param source command line string
     * @param delimiterChar delimiter character as string
     * @return Args instance
     */
    public static Args parse(String source, String delimiterChar) {
        return new Args(source, delimiterChar);
    }

    /**
     * Creates an Args instance with a custom delimiter.
     *
     * @param source command line string
     * @param delimiterChar delimiter character
     * @return Args instance
     */
    public static Args parse(String source, char delimiterChar) {
        return new Args(source, delimiterChar);
    }

    /**
     * Returns an iterator over arguments.
     *
     * @return iterator
     */
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < elements.length;
            }

            @Override
            public String next() {
                if (!hasNext()) throw new java.util.NoSuchElementException();
                return elements[i++];
            }
        };
    }

    /**
     * Applies an action to each argument.
     *
     * @param action consumer to process each argument
     */
    @Override
    public void forEach(Consumer<? super String> action) {
        for (String element : elements) {
            action.accept(element);
        }
    }

}
