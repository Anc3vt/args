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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArgsTest {

    @Test
    void testParseFromString() {
        Args args = Args.from("--host localhost --port 1234 --debug");

        assertEquals("localhost", args.get("--host", String.class));
        assertEquals(1234, args.get("--port", Integer.class));
        assertTrue(args.has("--debug"));
        assertNull(args.get("--foo", String.class));
    }

    @Test
    void testParseFromArray() {
        String[] arr = {"--host", "127.0.0.1", "--port", "4321"};
        Args args = Args.from(arr);

        assertEquals("127.0.0.1", args.get("--host", String.class));
        assertEquals(4321, args.get("--port", Integer.class));
    }

    @Test
    void testAliasesGet() {
        Args args = Args.from("--port 1111 -p 2222");

        // если оба присутствуют — берём первый из списка алиасов
        assertEquals(1111, args.get("--port|-p", Integer.class));
        // если основного нет, берем алиас
        args = Args.from("-p 3333");
        assertEquals(3333, args.get("--port|-p", Integer.class));
    }

    @Test
    void testAliasesHas() {
        Args args = Args.from("-d --debug");

        assertTrue(args.has("--debug|-d"));
        assertFalse(args.has("--prod|-p"));
    }

    @Test
    void testDefaultValue() {
        Args args = Args.from("");

        assertEquals(8080, args.get("--port", Integer.class, 8080));
        assertEquals("abc", args.get("--host", String.class, "abc"));

        // Алиасы + дефолт
        assertEquals(9090, args.get("--port|-p", Integer.class, 9090));
    }

    @Test
    void testBooleanFlags() {
        Args args = Args.from("--debug --prod");
        assertTrue(args.has("--debug"));
        assertTrue(args.has("--prod"));
        assertFalse(args.has("--test"));
    }

    @Test
    void testQuotedValues() {
        Args args = Args.from("--message \"Hello world!\" --name 'John Doe'");
        assertEquals("Hello world!", args.get("--message", String.class));
        assertEquals("John Doe", args.get("--name", String.class));
    }

    @Test
    void testConversionException() {
        Args args = Args.from("--port notanumber");
        assertThrows(ArgsException.class, () -> args.get("--port", Integer.class));
    }

    @Test
    void testPositionalArguments() {
        Args args = Args.from("file.txt --mode edit another.txt");
        assertEquals("edit", args.get("--mode", String.class));

        // Проверяем позиционные аргументы
        List<String> positionals = args.getPositionals();
        assertEquals(2, positionals.size());
        assertEquals("file.txt", positionals.get(0));
        assertEquals("another.txt", positionals.get(1));
    }

    @Test
    void testResetAndNext() {
        Args args = Args.from("--a 1 --b 2");
        assertTrue(args.hasNext());
        ArgsToken t1 = args.next();
        assertEquals("--a", t1.getKey());
        assertEquals("1", t1.getValue());
        ArgsToken t2 = args.next();
        assertEquals("--b", t2.getKey());
        assertEquals("2", t2.getValue());
        assertFalse(args.hasNext());
        args.reset();
        assertTrue(args.hasNext());
    }
}
