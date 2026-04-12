/*
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

package com.ancevt.util.args.reflection;

import com.ancevt.util.args.Args;
import com.ancevt.util.args.ArgsParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArgsBinderTest {

    static class SimpleCommand {
        @CommandArgument
        String name;

        @OptionArgument(names = {"-c", "--count"}, required = true)
        int count;

        @OptionArgument(names = {"-f", "--flag"})
        boolean flag;

        @OptionArgument(names = {"-d", "--desc"})
        String description;
    }

    static class NumericCommand {
        @CommandArgument
        String id;

        @OptionArgument(names = "--b")
        byte b;

        @OptionArgument(names = {"--s"})
        short s;

        @OptionArgument(names = {"--i"})
        int i;

        @OptionArgument(names = {"--l"})
        long l;

        @OptionArgument(names = {"--f"})
        float f;

        @OptionArgument(names = {"--d"})
        double d;

        @OptionArgument(names = {"--bool"})
        boolean bool;
    }

    @Test
    public void testBindCommandArgument() throws Exception {
        Args args = Args.parse("hello -c 42 --flag --desc testdesc");
        SimpleCommand cmd = ArgsBinder.convert(args, SimpleCommand.class);

        assertEquals("hello", cmd.name);
        assertEquals(42, cmd.count);
        assertTrue(cmd.flag);
        assertEquals("testdesc", cmd.description);
    }

    @Test
    public void testBindOptionalArgumentNotPresent() throws Exception {
        Args args = Args.parse("hello -c 10");
        SimpleCommand cmd = ArgsBinder.convert(args, SimpleCommand.class);

        assertEquals("hello", cmd.name);
        assertEquals(10, cmd.count);
        assertFalse(cmd.flag);
        assertNull(cmd.description);
    }

    @Test
    public void testMissingRequiredOptionThrowsException() {
        Args args = Args.parse("hello");

        assertThrows(ArgsParseException.class,
                () -> ArgsBinder.convert(args, SimpleCommand.class));
    }

    @Test
    public void testBindToExistingInstance() throws Exception {
        SimpleCommand cmd = new SimpleCommand();
        cmd.description = "preset";

        Args args = Args.parse("world -c 7 --flag");
        ArgsBinder.convert(args, cmd);

        assertEquals("world", cmd.name);
        assertEquals(7, cmd.count);
        assertTrue(cmd.flag);
        // не был передан -> остаётся прежним
        assertEquals("preset", cmd.description);
    }

    @Test
    public void testBindNumericTypes() throws Exception {
        Args args = Args.parse("id123 --b 1 --s 2 --i 3 --l 4 --f 5.5 --d 6.6 --bool true");
        NumericCommand num = ArgsBinder.convert(args, NumericCommand.class);

        assertEquals("id123", num.id);
        assertEquals((byte) 1, num.b);
        assertEquals((short) 2, num.s);
        assertEquals(3, num.i);
        assertEquals(4L, num.l);
        assertEquals(5.5f, num.f, 0.001);
        assertEquals(6.6, num.d, 0.001);
        assertTrue(num.bool);
    }

    @Test
    public void testBindNumericTypesDefaults() throws Exception {
        Args args = Args.parse("id999");
        NumericCommand num = ArgsBinder.convert(args, NumericCommand.class);

        assertEquals("id999", num.id);
        assertEquals((byte) 0, num.b);
        assertEquals((short) 0, num.s);
        assertEquals(0, num.i);
        assertEquals(0L, num.l);
        assertEquals(0.0f, num.f, 0.001);
        assertEquals(0.0, num.d, 0.001);
        assertFalse(num.bool);
    }
}
