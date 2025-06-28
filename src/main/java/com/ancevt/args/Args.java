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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Args {

    private final List<ArgsToken> tokens;
    private int index = 0;

    private Args(List<ArgsToken> tokens) {
        this.tokens = tokens;
    }

    public static Args from(String source) {
        List<String> raw = ArgsTokenizer.tokenize(source);
        return new Args(parseTokens(raw));
    }

    public static Args from(String[] args) {
        return new Args(parseTokens(Arrays.asList(args)));
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

    private static List<ArgsToken> parseTokens(List<String> rawTokens) {
        List<ArgsToken> parsed = new ArrayList<>();
        for (int i = 0; i < rawTokens.size(); i++) {
            String current = rawTokens.get(i);
            if ((current.startsWith("--") && current.length() > 2) ||
                    (current.startsWith("-") && current.length() > 1 && !current.startsWith("--"))) {
                String key = current;
                String value = null;
                if (i + 1 < rawTokens.size() && !rawTokens.get(i + 1).startsWith("-")) {
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

    private <T> T convert(String raw, Class<T> type) {
        try {
            if (type == String.class) return type.cast(raw);
            if (type == Integer.class || type == int.class) return type.cast(Integer.parseInt(raw));
            if (type == Boolean.class || type == boolean.class) return type.cast(Boolean.parseBoolean(raw));
            if (type == Long.class || type == long.class) return type.cast(Long.parseLong(raw));
            if (type == Double.class || type == double.class) return type.cast(Double.parseDouble(raw));
            throw new ArgsException("Unsupported type: " + type);
        } catch (Exception e) {
            throw new ArgsException("Conversion failed for value: " + raw + ", type: " + type.getSimpleName(), e);
        }
    }
}

