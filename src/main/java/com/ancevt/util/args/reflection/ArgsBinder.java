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
package com.ancevt.util.args.reflection;


import com.ancevt.util.args.Args;
import com.ancevt.util.args.ArgsParseException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Utility class that binds parsed {@link Args} to fields of a target object
 * using reflection and annotations {@link CommandArgument} and {@link OptionArgument}.
 * <p>
 * Example:
 * <pre>
 * class MyCommand {
 *     {@literal @}CommandArgument
 *     String name;
 *
 *     {@literal @}OptionArgument(names = {"-c", "--count"}, required = true)
 *     int count;
 * }
 *
 * Args args = Args.parse("hello -c 5");
 * MyCommand cmd = ArgsBinder.convert(args, MyCommand.class);
 * // cmd.name == "hello"
 * // cmd.count == 5
 * </pre>
 */
public class ArgsBinder {

    /**
     * Fills fields of an existing object with values from {@link Args}.
     *
     * @param args         parsed arguments
     * @param objectToFill existing instance to bind values into
     * @param <T>          type of target object
     * @return same instance with bound values
     * @throws IllegalAccessException if a field is not accessible
     * @throws ArgsParseException     if a required option argument is missing
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Args args, T objectToFill) throws IllegalAccessException {
        Class<T> type = (Class<T>) objectToFill.getClass();

        for (Field field : type.getDeclaredFields()) {

            CommandArgument commandArgumentAnnotation = field.getDeclaredAnnotation(CommandArgument.class);
            if (commandArgumentAnnotation != null) {
                int index = 0;
                try {
                    index = commandArgumentAnnotation.index();
                } catch (Exception ignored) {
                }

                Class<?> t = field.getType();
                field.setAccessible(true);

                Object value = args.get(t, index);
                if (value == null && commandArgumentAnnotation.required()) {
                    throw new ArgsParseException("Missing required positional argument at index " + index);
                }

                field.set(objectToFill, value);
                continue;
            }

            OptionArgument optionArgumentAnnotation = field.getDeclaredAnnotation(OptionArgument.class);
            if (optionArgumentAnnotation != null) {

                boolean found = false;
                String[] names = optionArgumentAnnotation.names();

                if (names != null) {
                    for (String name : names) {
                        if (args.contains(name)) {
                            field.setAccessible(true);
                            Class<?> fieldType = field.getType();

                            Class<?> converterClass = null;
                            try {
                                converterClass = optionArgumentAnnotation.converter();
                            } catch (Exception ignored) {
                                // NO-OP
                            }

                            ArgsConverter<?> converterInstance = null;
                            if (converterClass != null
                                    && converterClass != ArgsConverter.NoConverter.class) {
                                try {
                                    converterInstance = (ArgsConverter<?>) converterClass.getDeclaredConstructor().newInstance();
                                } catch (Exception e) {
                                    throw new ArgsParseException(
                                            "Failed to create converter for field '" + field.getName() + "'", e);
                                }
                            }

                            Object rawValue;
                            if (fieldType == boolean.class || fieldType == Boolean.class) {
                                rawValue = true;
                            } else {
                                rawValue = args.get(fieldType, name);
                            }

                            Object finalValue = converterInstance != null
                                    ? converterInstance.convert(String.valueOf(rawValue))
                                    : rawValue;

                            field.set(objectToFill, finalValue);
                            found = true;
                            break;
                        }
                    }
                }

                if (optionArgumentAnnotation.required() && !found) {
                    throw new ArgsParseException(
                            "Required parameter " + Arrays.toString(names) + " not found");
                }

                continue;
            }
        }

        return objectToFill;
    }


    /**
     * Creates a new instance of the given class (using no-arg constructor),
     * and fills its fields with values from {@link Args}.
     *
     * @param args parsed arguments
     * @param type class to instantiate and bind
     * @param <T>  type of target object
     * @return a new instance with bound values
     * @throws NoSuchMethodException     if no default constructor is found
     * @throws InvocationTargetException if constructor throws exception
     * @throws InstantiationException    if instance cannot be created
     * @throws IllegalAccessException    if field or constructor access fails
     * @throws ArgsParseException        if a required option argument is missing
     */
    public static <T> T convert(Args args, Class<T> type)
            throws NoSuchMethodException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException {

        Constructor<T> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return convert(args, constructor.newInstance());
    }

}
