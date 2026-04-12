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

import java.util.ArrayList;
import java.util.List;

class ArgsSplitHelper {

    private static final String SPACE_CHARS = "\n\t\r\b ";

    private ArgsSplitHelper() {}

    static String[] split(final String source, char delimiterChar) {
        final List<String> result = new ArrayList<>();
        final StringBuilder buffer = new StringBuilder();

        final int length = source.length();
        boolean insideQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < length; ) {
            char current = source.charAt(i++);

            // Handle escaped characters
            if (current == '\\' && i < length) {
                buffer.append(source.charAt(i++));
                continue;
            }

            // Handle quoted strings
            if (insideQuotes) {
                if (current == quoteChar) {
                    insideQuotes = false;
                } else {
                    buffer.append(current);
                }
                continue;
            }

            if (current == '"' || current == '\'') {
                insideQuotes = true;
                quoteChar = current;
                continue;
            }

            // Handle delimiters
            boolean isDelimiter;
            if (delimiterChar == '\0') {
                isDelimiter = SPACE_CHARS.indexOf(current) != -1;
            } else {
                isDelimiter = current == delimiterChar;
            }

            if (isDelimiter) {
                if (buffer.length() > 0) {
                    result.add(buffer.toString());
                    buffer.setLength(0);
                }
                continue;
            }

            buffer.append(current);
        }

        if (buffer.length() > 0) {
            result.add(buffer.toString());
        }

        return result.toArray(new String[0]);
    }

    public static String[] split(String source, String delimiterChar) {
        if (delimiterChar == null || delimiterChar.length() != 1) {
            throw new ArgsParseException("delimiter string must contain one character");
        }

        return split(source, delimiterChar.charAt(0));
    }
}

