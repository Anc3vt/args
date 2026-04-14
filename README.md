# Args

`Args` is a small Java library for parsing command-line argument strings and
binding parsed arguments to objects through annotations.

It supports:

- splitting argument strings while respecting quotes and escaped characters;
- reading values in both `--key value` and `--key=value` forms;
- converting values to common Java types;
- reading comma-separated values as `List` or `Set`;
- using aliases such as `-p` and `--port`;
- binding arguments to class fields with `@CommandArgument` and
  `@OptionArgument`;
- custom converters for application-specific value types.

## Installation

If the library is available in your Maven repository, add:

```xml
<dependency>
    <groupId>com.ancevt.util</groupId>
    <artifactId>args</artifactId>
    <version>1.0.0</version>
</dependency>
```

To build and install it locally:

```bash
mvn clean install
```

The project is configured for Java 8.

## Quick Start

```java
import com.ancevt.util.args.Args;

public class Example {
    public static void main(String[] rawArgs) {
        Args args = Args.parse("--port 8080 --host localhost --debug");

        int port = args.get(Integer.class, "--port", 80);
        String host = args.get("--host", "127.0.0.1");
        boolean debug = args.contains("--debug");

        System.out.println(host + ":" + port);
        System.out.println("debug = " + debug);
    }
}
```

## Parsing Strings

By default, an argument string is split on whitespace characters: spaces, tabs,
line breaks, carriage returns, and backspaces.

```java
Args args = Args.parse("run --name Alice --count 3");

System.out.println(args.size());           // 5
System.out.println(args.getElements()[0]); // run
System.out.println(args.getElements()[1]); // --name
```

Quoted text is kept as a single value:

```java
Args args = Args.parse("send --message \"hello world\" --to 'John Smith'");

System.out.println(args.get("--message")); // hello world
System.out.println(args.get("--to"));      // John Smith
```

The escape character `\` includes the next character in the current value:

```java
Args args = Args.parse("open C:\\\\temp\\\\file.txt escaped\\ value");

System.out.println(args.get(String.class, 1)); // C:\temp\file.txt
System.out.println(args.get(String.class, 2)); // escaped value
```

## Custom Delimiters

You can parse a string using a single custom delimiter instead of whitespace:

```java
Args args = Args.parse("one,two,\"three,with,commas\"", ',');

System.out.println(args.get(String.class, 0)); // one
System.out.println(args.get(String.class, 1)); // two
System.out.println(args.get(String.class, 2)); // three,with,commas
```

There is also a `String` delimiter overload. The delimiter string must contain
exactly one character:

```java
Args args = Args.parse("one|two|three", "|");
```

## Reading Values

`Args` supports both common option formats:

```java
Args args = Args.parse("--host localhost --port=8080");

String host = args.get("--host");
int port = args.get(Integer.class, "--port");
```

When a key is missing, pass a default value:

```java
Args args = Args.parse("--host localhost");

int port = args.get(Integer.class, "--port", 80);
String mode = args.get("--mode", "dev");
```

Use an array of keys for aliases:

```java
Args args = Args.parse("-p 8080");

int port = args.get(Integer.class, new String[]{"--port", "-p"}, 80);
```

## contains and the Last Matched Key

`contains` checks for one or more keys. When a key is found, it is remembered
and can be used through `get(Class<T>)` without repeating the key name:

```java
Args args = Args.parse("--count=42");

if (args.contains("-c", "--count")) {
    int count = args.get(Integer.class);
    System.out.println(count); // 42
}
```

Both forms are recognized:

```java
Args.parse("--name Alice").contains("--name");  // true
Args.parse("--name=Alice").contains("--name"); // true
```

## Supported Types

The built-in converter supports:

- `String`;
- `boolean` / `Boolean`;
- `int` / `Integer`;
- `long` / `Long`;
- `float` / `Float`;
- `double` / `Double`;
- `short` / `Short`;
- `byte` / `Byte`;
- enum types;
- `List`;
- `Set`.

Example:

```java
enum Mode {
    FAST,
    SLOW
}

Args args = Args.parse("--enabled true --retries 3 --timeout 1.5 --mode slow");

boolean enabled = args.get(Boolean.class, "--enabled");
int retries = args.get(Integer.class, "--retries");
double timeout = args.get(Double.class, "--timeout");
Mode mode = args.get(Mode.class, "--mode");

System.out.println(enabled); // true
System.out.println(retries); // 3
System.out.println(timeout); // 1.5
System.out.println(mode);    // SLOW
```

Enum conversion is case-insensitive for input values: `slow` becomes
`Mode.SLOW`.

## Lists and Sets

`List` and `Set` values are read from comma-separated strings:

```java
Args args = Args.parse("--names alice,bob,alice --roles admin,user,admin");

List<?> names = args.get(List.class, "--names");
Set<?> roles = args.get(Set.class, "--roles");

System.out.println(names); // [alice, bob, alice]
System.out.println(roles); // [admin, user], order is not guaranteed
```

`List` keeps duplicates. `Set` removes duplicates.

## Sequential Reading

`Args` can also be consumed as a stream-like sequence with `next()`:

```java
Args args = Args.parse("deploy production 3");

String command = args.next();
String environment = args.next();
int replicas = args.next(Integer.class);

System.out.println(command);     // deploy
System.out.println(environment); // production
System.out.println(replicas);    // 3
```

The current index can be reset or advanced:

```java
Args args = Args.parse("one two three");

args.next();       // one
args.resetIndex();
args.skip(2);

System.out.println(args.next()); // three
```

If there are no more arguments, `next()` throws `ArgsParseException`.

## Iteration

`Args` implements `Iterable<String>`:

```java
Args args = Args.parse("one two three");

for (String element : args) {
    System.out.println(element);
}
```

You can also use `forEach`:

```java
args.forEach(System.out::println);
```

## Object Binding

The `com.ancevt.util.args.reflection` package lets you describe a command as a
plain Java class.

```java
import com.ancevt.util.args.Args;
import com.ancevt.util.args.reflection.ArgsBinder;
import com.ancevt.util.args.reflection.CommandArgument;
import com.ancevt.util.args.reflection.OptionArgument;

public class BindExample {

    static class DeployCommand {
        @CommandArgument
        String service;

        @OptionArgument(names = {"-e", "--env"}, required = true)
        String environment;

        @OptionArgument(names = {"-r", "--replicas"})
        int replicas;

        @OptionArgument(names = {"--dry-run"})
        boolean dryRun;
    }

    public static void main(String[] rawArgs) throws Exception {
        Args args = Args.parse("api --env production --replicas 3 --dry-run");

        DeployCommand command = ArgsBinder.convert(args, DeployCommand.class);

        System.out.println(command.service);     // api
        System.out.println(command.environment); // production
        System.out.println(command.replicas);    // 3
        System.out.println(command.dryRun);      // true
    }
}
```

`ArgsBinder.convert(args, SomeClass.class)` creates a new object through a
no-argument constructor. The constructor may be private.

You can also fill an existing instance:

```java
DeployCommand command = new DeployCommand();
command.replicas = 1;

ArgsBinder.convert(Args.parse("api --env staging"), command);

System.out.println(command.replicas); // 1, because the option was not provided
```

## Positional Arguments

`@CommandArgument` reads values by index:

```java
static class CopyCommand {
    @CommandArgument(index = 0)
    String source;

    @CommandArgument(index = 1)
    String target;
}

CopyCommand command = ArgsBinder.convert(
        Args.parse("input.txt output.txt"),
        CopyCommand.class
);

System.out.println(command.source); // input.txt
System.out.println(command.target); // output.txt
```

By default, a positional argument is required. Mark it optional with
`required = false`:

```java
static class Command {
    @CommandArgument(index = 0, required = false)
    String optionalName;
}
```

If a required positional argument is missing, `ArgsParseException` is thrown.

## Option Arguments

`@OptionArgument` searches for one of the configured names:

```java
static class ServerCommand {
    @OptionArgument(names = {"-p", "--port"}, required = true)
    int port;

    @OptionArgument(names = {"-h", "--host"})
    String host;
}
```

Both examples work:

```java
ArgsBinder.convert(Args.parse("--port 8080 --host localhost"), ServerCommand.class);
ArgsBinder.convert(Args.parse("-p=8080 -h=localhost"), ServerCommand.class);
```

For boolean fields, option presence means `true`:

```java
static class Command {
    @OptionArgument(names = "--verbose")
    boolean verbose;
}

Command command = ArgsBinder.convert(Args.parse("--verbose"), Command.class);
System.out.println(command.verbose); // true
```

## Required Options

If `required = true`, a missing option causes `ArgsParseException`:

```java
static class LoginCommand {
    @OptionArgument(names = "--user", required = true)
    String user;
}

ArgsBinder.convert(Args.parse(""), LoginCommand.class); // ArgsParseException
```

## Custom Converters

For custom conversion logic, implement `ArgsConverter<T>` and reference it in
`@OptionArgument`.

```java
import com.ancevt.util.args.reflection.ArgsConverter;

static class UpperCaseConverter implements ArgsConverter<String> {
    @Override
    public String convert(String input) {
        return input.toUpperCase();
    }
}

static class Command {
    @OptionArgument(names = "--name", converter = UpperCaseConverter.class)
    String name;
}

Command command = ArgsBinder.convert(Args.parse("--name alice"), Command.class);

System.out.println(command.name); // ALICE
```

The converter must have an accessible no-argument constructor. If the converter
cannot be created, `ArgsBinder` throws `ArgsParseException`.

## Error Handling

The main library exception is `ArgsParseException`.

It is thrown, for example, when:

- `next()` is called after all arguments have been consumed;
- a required positional argument is missing;
- a required option argument is missing;
- an unsupported target type is requested;
- an enum value does not match any enum constant;
- a string delimiter contains anything other than one character.

Example:

```java
try {
    Args args = Args.parse("--count not-a-number");
    int count = args.get(Integer.class, "--count");
} catch (RuntimeException e) {
    System.err.println("Invalid arguments: " + e.getMessage());
}
```

For index-based reads with a default value, conversion errors are not thrown
outward. They are stored inside the `Args` instance:

```java
Args args = Args.parse("not-a-number");

int count = args.get(Integer.class, 0, 10);

System.out.println(count);             // 10
System.out.println(args.hasProblem()); // true
System.out.println(args.getProblem()); // NumberFormatException
```

## Complete Example

```java
import com.ancevt.util.args.Args;
import com.ancevt.util.args.reflection.ArgsBinder;
import com.ancevt.util.args.reflection.CommandArgument;
import com.ancevt.util.args.reflection.OptionArgument;

public class Main {

    enum Format {
        JSON,
        TEXT
    }

    static class ExportCommand {
        @CommandArgument(index = 0)
        String command;

        @CommandArgument(index = 1)
        String file;

        @OptionArgument(names = {"-f", "--format"})
        Format format;

        @OptionArgument(names = {"--tags"})
        java.util.List tags;

        @OptionArgument(names = {"--force"})
        boolean force;
    }

    public static void main(String[] rawArgs) throws Exception {
        Args args = Args.parse(
                "export report.txt --format json --tags daily,finance --force"
        );

        ExportCommand command = ArgsBinder.convert(args, ExportCommand.class);

        System.out.println(command.command); // export
        System.out.println(command.file);    // report.txt
        System.out.println(command.format);  // JSON
        System.out.println(command.tags);    // [daily, finance]
        System.out.println(command.force);   // true
    }
}
```

## Build and Test

Build the project:

```bash
mvn clean package
```

Run tests:

```bash
mvn test
```

## License

Apache License, Version 2.0.
