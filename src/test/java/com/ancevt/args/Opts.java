package com.ancevt.args;

public class Opts {
    @ArgOption(names = {"--host", "-h"})
    public String host = "localhost";

    @ArgOption(names = {"--port", "-p"})
    public int port = 8080;

    @ArgOption(names = {"--debug", "-d"}, usage = "Usage text")
    public boolean debug;

    @ArgPositional(index = 0)
    public String input;

    @ArgPositional(index = 1)
    public String output;

    public static void main(String[] args) {
        System.out.println(Args.generateHelp(Opts.class, "myapp"));
    }
}