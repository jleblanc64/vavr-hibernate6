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
package io.github.jleblanc64.hibernate6.custom.hibernate;

import io.github.jleblanc64.hibernate6.custom.meta.WithClass;
import io.github.jleblanc64.libcustom.functional.ListF;
import jakarta.persistence.Entity;
import lombok.SneakyThrows;
import org.hibernate.collection.spi.AbstractPersistentCollection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import static io.github.jleblanc64.libcustom.Reflection.getAllFields;
import static io.github.jleblanc64.libcustom.functional.ListF.f;

public class Utils {
    private static Class PERSISTENT_COLLECTION_CLASS = AbstractPersistentCollection.class;

    @SneakyThrows
    private static Field roleToField(String role) {
        var i = role.lastIndexOf(".");
        var className = role.substring(0, i);
        var fieldName = role.substring(i + 1);

        return Class.forName(className).getDeclaredField(fieldName);
    }

    public static boolean isOfType(Object pers, WithClass w) {
        var role = getRefl(pers, "navigableRole");
        var path = (String) getRefl(role, "fullPath");

        var field = roleToField(path);
        return w.isSuperClassOf(field.getType());
    }

    @SneakyThrows
    static Object checkPersistentBag(Object o) {
        if (o == null)
            return o;

        if (!PERSISTENT_COLLECTION_CLASS.isAssignableFrom(o.getClass()))
            throw new RuntimeException("Output of IBagProvider implem must extend " + PERSISTENT_COLLECTION_CLASS.getName());

        return o;
    }

    public static boolean isEntity(Annotation[] annotations) {
        return f(annotations).stream().anyMatch(a -> a instanceof Entity);
    }

    public static Object getRefl(Object o, String field) {
        var f = findField(o.getClass(), field);
        return getRefl(o, f);
    }

    private static Field findField(Class<?> clazz, String field) {
        var currentClass = clazz;
        while (currentClass != null) {
            var found = f(currentClass.getDeclaredFields()).findSafe(f -> f.getName().equals(field));
            if (found != null)
                return found;

            currentClass = currentClass.getSuperclass();
        }

        return null;
    }

    @SneakyThrows
    public static Object getRefl(Object o, Field f) {
        f.setAccessible(true);
        return f.get(o);
    }

    @SneakyThrows
    public static void setRefl(Object o, Field f, Object value) {
        f.setAccessible(true);
        f.set(o, value);
    }

    public static ListF<Field> fields(Object o) {
        return f(getAllFields(o.getClass()));
    }

    @SneakyThrows
    public static Class<?> paramClass(String clazz) {
        return Class.forName(regex0(clazz, "(?<=\\<).*(?=\\>)"));
    }

    public static String regex0(String s, String pattern) {
        var matcher = Pattern.compile(pattern).matcher(s);
        matcher.find();
        return matcher.group(0);
    }

    @SneakyThrows
    public static Object invoke(Object o, String methodName, Object... args) {
        var m = findMethod(o, methodName, args);
        return m.invoke(o, args);
    }

    private static Method findMethod(Object o, String methodName, Object... args) {
        var currentClass = o.getClass();
        while (currentClass != null) {
            var match = f(currentClass.getDeclaredMethods()).findSafe(m -> matches(m, methodName, args));
            if (match != null)
                return match;

            currentClass = currentClass.getSuperclass();
        }

        return null;
    }

    private static boolean matches(Method m, String methodName, Object... args) {
        if (!m.getName().equals(methodName))
            return false;

        if (args.length != m.getParameterTypes().length)
            return false;

        for (var i = 0; i < m.getParameterTypes().length; i++)
            if (!matches(m.getParameterTypes()[i], args[i]))
                return false;

        return true;
    }

    private static boolean matches(Class<?> c, Object arg) {
        return arg == null || c.isAssignableFrom(arg.getClass());
    }
}
