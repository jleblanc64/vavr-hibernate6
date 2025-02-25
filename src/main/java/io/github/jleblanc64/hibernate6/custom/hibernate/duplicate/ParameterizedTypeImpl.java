/*
 * Copyright 2024 - Charles Dabadie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.jleblanc64.hibernate6.custom.hibernate.duplicate;

import lombok.SneakyThrows;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

// https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/reflect/TypeUtils.java#L137
public class ParameterizedTypeImpl {
    @SneakyThrows
    public static ParameterizedType of(Class<?> rawType, Type typeArg, Type ownerType) {

        var clazz = Class.forName("org.apache.commons.lang3.reflect.TypeUtils$ParameterizedTypeImpl");
        var constructor = clazz.getDeclaredConstructor(Class.class, Type.class, Array.newInstance(Type.class, 0).getClass());
        constructor.setAccessible(true);

        return (ParameterizedType) constructor.newInstance(rawType, ownerType, new Type[]{typeArg});
    }
}