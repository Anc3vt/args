package com.ancevt.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgsAnnotationTest {

    @Test
    void testAnnotationParsing() {
        String[] argv = {
                "--host", "1.2.3.4",
                "-p", "9999",
                "--debug",
                "file1.txt",
                "file2.txt"
        };

        Opts opts = Args.parse(argv, new Opts());

        assertEquals("1.2.3.4", opts.host);
        assertEquals(9999, opts.port);
        assertTrue(opts.debug);

        assertEquals("file1.txt", opts.input);
        assertEquals("file2.txt", opts.output);
    }

    @Test
    void testDefaults() {
        String[] argv = {
                // no --host, no --port, no -d
                "input.txt"
        };

        Opts opts = Args.parse(argv, new Opts());

        assertEquals("localhost", opts.host); // default
        assertEquals(8080, opts.port);        // default
        assertFalse(opts.debug);              // default

        assertEquals("input.txt", opts.input);
        assertNull(opts.output);
    }

    @Test
    void testAliases() {
        String[] argv = {
                "-h", "myhost",    // alias for --host
                "--port", "7777",  // direct
                "a.txt"
        };

        Opts opts = Args.parse(argv, new Opts());
        assertEquals("myhost", opts.host);
        assertEquals(7777, opts.port);
        assertEquals("a.txt", opts.input);
    }
}
