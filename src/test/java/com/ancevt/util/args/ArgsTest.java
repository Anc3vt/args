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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ArgsTest {

    enum Mode {
        FAST,
        SLOW
    }

    @Test
    public void testParseSplitsWhitespaceAndKeepsQuotedValues() {
        Args args = Args.parse("alpha \"two words\" 'three four' escaped\\ value\nlast");

        assertArrayEquals(
                new String[]{"alpha", "two words", "three four", "escaped value", "last"},
                args.getElements()
        );
        assertEquals(5, args.size());
        assertFalse(args.isEmpty());
        assertEquals("alpha \"two words\" 'three four' escaped\\ value\nlast", args.getSource());
    }

    @Test
    public void testParseWithCustomDelimiterKeepsQuotedDelimiter() {
        Args args = Args.parse("one,\"two,too\",three", ',');

        assertArrayEquals(new String[]{"one", "two,too", "three"}, args.getElements());
    }

    @Test
    public void testParseWithStringDelimiterValidatesSingleCharacter() {
        Args args = Args.parse("one|two|three", "|");

        assertArrayEquals(new String[]{"one", "two", "three"}, args.getElements());
        assertThrows(ArgsParseException.class, () -> Args.parse("one::two", "::"));
        assertThrows(ArgsParseException.class, () -> Args.parse("one two", (String) null));
    }

    @Test
    public void testParseArrayPreservesElementsAndBuildsSource() {
        Args args = Args.parse(new String[]{"hello", "two words", "say \"hi\""});

        assertArrayEquals(new String[]{"hello", "two words", "say \"hi\""}, args.getElements());
        assertEquals("\"hello\" \"two words\" \"say \\\\\"hi\\\\\"\"", args.getSource());
    }

    @Test
    public void testContainsStoresLastCheckedKeyForValueLookup() {
        Args args = Args.parse("--count=42 --name bob");

        assertTrue(args.contains("-c", "--count"));
        assertEquals(42, args.get(Integer.class));
        assertTrue(args.contains("--name"));
        assertEquals("bob", args.get(String.class));
        assertFalse(args.contains("--missing"));
        assertEquals("bob", args.get(String.class));
    }

    @Test
    public void testGetSupportsSpaceAndEqualsSeparatedOptions() {
        Args args = Args.parse("--port 8080 --host=localhost --enabled true");

        assertEquals(8080, args.get(Integer.class, "--port").intValue());
        assertEquals("localhost", args.get("--host"));
        assertTrue(args.get(Boolean.class, "--enabled"));
        assertEquals("fallback", args.get("--missing", "fallback"));
    }

    @Test
    public void testGetSupportsAliasesAndDefaultValue() {
        Args args = Args.parse("-p 9090 --name server");

        assertEquals(9090, args.get(Integer.class, new String[]{"--port", "-p"}, 80).intValue());
        assertEquals("server", args.get(new String[]{"--missing", "--name"}, "fallback"));
        assertEquals("fallback", args.get(new String[]{"--missing", "--absent"}, "fallback"));
    }

    @Test
    public void testIndexBasedGetConvertsSimpleTypes() {
        Args args = Args.parse("true 12 13 1.5 2.5 7 8 slow");

        assertTrue(args.get(Boolean.class, 0));
        assertEquals(12, args.get(Integer.class, 1).intValue());
        assertEquals(13L, args.get(Long.class, 2).longValue());
        assertEquals(1.5f, args.get(Float.class, 3), 0.001f);
        assertEquals(2.5, args.get(Double.class, 4), 0.001);
        assertEquals((short) 7, args.get(Short.class, 5).shortValue());
        assertEquals((byte) 8, args.get(Byte.class, 6).byteValue());
        assertEquals(Mode.SLOW, args.get(Mode.class, 7));
    }

    @Test
    public void testGetConvertsCommaSeparatedListsAndSets() {
        Args args = Args.parse("--list a,b,a --set a,b,a");

        List<?> list = args.get(List.class, "--list");
        Set<?> set = args.get(Set.class, "--set");

        assertEquals(Arrays.asList("a", "b", "a"), list);
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), set);
    }

    @Test
    public void testInvalidIndexConversionReturnsDefaultAndStoresProblem() {
        Args args = Args.parse("not-a-number");

        assertEquals(10, args.get(Integer.class, 0, 10).intValue());
        assertTrue(args.hasProblem());
        assertTrue(args.getProblem() instanceof NumberFormatException);
        assertEquals(20, args.get(Integer.class, 5, 20).intValue());
    }

    @Test
    public void testUnsupportedTypeWithoutDefaultReturnsNullAndStoresProblem() {
        Args args = Args.parse("value");

        Object value = args.get(Object.class, 0);

        assertNull(value);
        assertTrue(args.hasProblem());
        assertTrue(args.getProblem() instanceof ArgsParseException);
    }

    @Test
    public void testInvalidEnumThrowsParseExceptionForKeyLookup() {
        Args args = Args.parse("--mode unknown");

        ArgsParseException exception = assertThrows(
                ArgsParseException.class,
                () -> args.get(Mode.class, "--mode")
        );
        assertTrue(exception.getMessage().contains("Invalid enum value"));
    }

    @Test
    public void testNextSkipAndResetIndex() {
        Args args = Args.parse("one 2 three");

        assertTrue(args.hasNext());
        assertEquals("one", args.next());
        assertEquals(1, args.getIndex());
        assertEquals(2, args.next(Integer.class).intValue());
        args.resetIndex();
        args.skip(2);
        assertEquals("three", args.next(String.class, "fallback"));
        assertFalse(args.hasNext());
        assertThrows(ArgsParseException.class, args::next);
    }

    @Test
    public void testSetIndexRejectsOutOfBounds() {
        Args args = Args.parse("one two");

        args.setIndex(1);
        assertEquals("two", args.next());
        assertThrows(ArgsParseException.class, () -> args.setIndex(2));
    }

    @Test
    public void testIteratorAndForEachWalkAllElements() {
        Args args = Args.parse("one two");
        Iterator<String> iterator = args.iterator();
        List<String> values = new ArrayList<>();

        assertTrue(iterator.hasNext());
        assertEquals("one", iterator.next());
        assertEquals("two", iterator.next());
        assertFalse(iterator.hasNext());
        assertThrows(java.util.NoSuchElementException.class, iterator::next);

        args.forEach(values::add);
        assertEquals(Arrays.asList("one", "two"), values);
    }

    @Test
    public void testEmptySourceProducesEmptyArgs() {
        Args args = Args.parse("");

        assertTrue(args.isEmpty());
        assertEquals(0, args.size());
        assertFalse(args.hasNext());
    }
}
