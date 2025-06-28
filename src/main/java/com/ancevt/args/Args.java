/**
 * Copyright (C) 2025 Ancevt.
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

package com.ancevt.args;

import java.lang.reflect.Field;
import java.util.*;

public class Args {

    private final List<ArgsToken> tokens;
    private int index = 0;

    private Args(List<ArgsToken> tokens) {
        this.tokens = tokens;
    }

    public static Args from(String source) {
        List<String> raw = ArgsTokenizer.tokenize(source);
        return new Args(parseTokens(raw, new HashSet<>()));
    }

    public static Args from(String[] args) {
        return new Args(parseTokens(Arrays.asList(args), new HashSet<>()));
    }

    private static Set<String> collectBooleanFlags(Object target) {
        Set<String> flags = new HashSet<>();
        for (Field field : target.getClass().getFields()) {
            if (field.isAnnotationPresent(ArgOption.class)) {
                Class<?> type = field.getType();
                if (type == boolean.class || type == Boolean.class) {
                    ArgOption opt = field.getAnnotation(ArgOption.class);
                    flags.addAll(Arrays.asList(opt.names()));
                }
            }
        }
        return flags;
    }

    public List<String> getPositionals() {
        List<String> list = new ArrayList<>();
        for (ArgsToken t : tokens) {
            if (t.getKey() == null && t.getValue() != null) {
                list.add(t.getValue());
            }
        }
        return list;
    }

    public <T> T getPositional(int index, Class<T> type) {
        List<String> positionals = getPositionals();
        if (index < 0 || index >= positionals.size()) return null;
        return convert(positionals.get(index), type);
    }

    public <T> T getPositional(int index, Class<T> type, T defaultValue) {
        T value = getPositional(index, type);
        return value != null ? value : defaultValue;
    }

    private static List<ArgsToken> parseTokens(List<String> rawTokens, Set<String> booleanFlags) {
        List<ArgsToken> parsed = new ArrayList<>();
        for (int i = 0; i < rawTokens.size(); i++) {
            String current = rawTokens.get(i);
            boolean isKey = (current.startsWith("--") && current.length() > 2)
                    || (current.startsWith("-") && current.length() > 1 && !current.startsWith("--"));
            if (isKey) {
                String key = current;
                String value = null;
                if (!booleanFlags.contains(key) && i + 1 < rawTokens.size() && !rawTokens.get(i + 1).startsWith("-")) {
                    value = rawTokens.get(++i);
                }
                parsed.add(new ArgsToken(key, value));
            } else {
                parsed.add(new ArgsToken(null, current)); // positional
            }
        }
        return parsed;
    }


    public boolean has(String keys) {
        if (keys.contains("|")) {
            return has(keys.split("\\|"));
        } else {
            return hasSingle(keys);
        }
    }

    public boolean has(String[] keys) {
        for (String key : keys) {
            if (hasSingle(key)) return true;
        }
        return false;
    }

    private boolean hasSingle(String key) {
        for (ArgsToken token : tokens) {
            if (key.equals(token.getKey())) return true;
        }
        return false;
    }

    public <T> T get(String keys, Class<T> type) {
        if (keys.contains("|")) {
            return get(keys.split("\\|"), type);
        } else {
            return getSingle(keys, type); // <-- вот тут настоящий get по 1 ключу!
        }
    }

    public <T> T get(String[] keys, Class<T> type) {
        for (String key : keys) {
            T value = getSingle(key, type);
            if (value != null) return value;
        }
        return null;
    }

    private <T> T getSingle(String key, Class<T> type) {
        for (ArgsToken token : tokens) {
            if (key.equals(token.getKey()) && token.getValue() != null) {
                return convert(token.getValue(), type);
            }
        }
        return null;
    }

    public <T> T get(String keys, Class<T> type, T defaultValue) {
        if (keys.contains("|")) {
            return get(keys.split("\\|"), type, defaultValue);
        } else {
            T value = getSingle(keys, type);
            return value != null ? value : defaultValue;
        }
    }

    public <T> T get(String[] keys, Class<T> type, T defaultValue) {
        for (String key : keys) {
            T value = getSingle(key, type);
            if (value != null) return value;
        }
        return defaultValue;
    }

    public boolean nextIsFlag() {
        return peek() != null && peek().isFlag();
    }

    public ArgsToken next() {
        if (!hasNext()) {
            throw new ArgsException("No more tokens");
        }
        return tokens.get(index++);
    }

    public ArgsToken peek() {
        return hasNext() ? tokens.get(index) : null;
    }

    public boolean hasNext() {
        return index < tokens.size();
    }

    public void reset() {
        index = 0;
    }

    private static <T> T convert(String raw, Class<T> type) {
        try {
            if (type == String.class) return type.cast(raw);
            if (type == Integer.class || type == int.class) return (T) Integer.valueOf(raw);
            if (type == Boolean.class || type == boolean.class) return (T) Boolean.valueOf(raw);
            if (type == Long.class || type == long.class) return (T) Long.valueOf(raw);
            if (type == Double.class || type == double.class) return (T) Double.valueOf(raw);
            throw new ArgsException("Unsupported type: " + type);
        } catch (Exception e) {
            throw new ArgsException("Conversion failed for value: " + raw + ", type: " + type.getSimpleName(), e);
        }
    }


    public static <T> T parse(String[] argv, T target) {
        Set<String> booleanFlags = collectBooleanFlags(target); // <--- вот оно!
        Args args = new Args(parseTokens(Arrays.asList(argv), booleanFlags));

        for (Field field : target.getClass().getFields()) {
            if (field.isAnnotationPresent(ArgOption.class)) {
                ArgOption opt = field.getAnnotation(ArgOption.class);
                Object value = null;
                Class<?> type = field.getType();
                for (String name : opt.names()) {
                    if (type == boolean.class || type == Boolean.class) {
                        if (args.has(name)) value = true;
                    } else {
                        Object v = args.get(name, type);
                        if (v != null) {
                            value = v;
                            break;
                        }
                    }
                }
                if (value != null) {
                    try {
                        field.set(target, value);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        List<String> positionals = args.getPositionals();
        for (Field field : target.getClass().getFields()) {
            if (field.isAnnotationPresent(ArgPositional.class)) {
                ArgPositional pos = field.getAnnotation(ArgPositional.class);
                int index = pos.index();
                if (index < positionals.size()) {
                    Object val = convert(positionals.get(index), field.getType());
                    try {
                        field.set(target, val);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return target;
    }

    public static String generateHelp(Class<?> optionsClass, String appName) {
        StringBuilder sb = new StringBuilder();

        sb.append("Usage: ").append(appName).append(" [options]");

        List<Field> positionals = new ArrayList<>();
        for (Field field : optionsClass.getFields()) {
            if (field.isAnnotationPresent(ArgPositional.class)) {
                positionals.add(field);
            }
        }
        positionals.sort((a, b) -> {
            int ia = a.getAnnotation(ArgPositional.class).index();
            int ib = b.getAnnotation(ArgPositional.class).index();
            return Integer.compare(ia, ib);
        });
        for (Field field : positionals) {
            sb.append(" <").append(field.getName()).append(">");
        }
        sb.append("\n\nOptions:\n");

        for (Field field : optionsClass.getFields()) {
            if (field.isAnnotationPresent(ArgOption.class)) {
                ArgOption opt = field.getAnnotation(ArgOption.class);
                sb.append("  ");
                sb.append(String.join(", ", opt.names()));
                sb.append("    ");

                String usage = opt.usage();
                if (!usage.isEmpty()) {
                    sb.append(usage);
                }

                // default value
                Object defValue = null;
                try {
                    defValue = field.get(null);
                } catch (Exception ignored) {
                }

                if (defValue != null && !(field.getType() == boolean.class || field.getType() == Boolean.class)) {
                    sb.append(" (default: ").append(defValue).append(")");
                }
                sb.append("\n");
            }
        }

        if (!positionals.isEmpty()) {
            sb.append("\nArguments:\n");
            for (Field field : positionals) {
                ArgPositional pos = field.getAnnotation(ArgPositional.class);
                sb.append("  ").append(field.getName()).append("    ");
                if (!pos.usage().isEmpty()) {
                    sb.append(pos.usage());
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

}

