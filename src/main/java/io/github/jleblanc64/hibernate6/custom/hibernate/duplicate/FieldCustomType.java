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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

import static org.mockito.Mockito.mock;

public class FieldCustomType {

    public static Field create(Field field, Type type) {
        return mock(Field.class, invocation -> {
            var args = invocation.getRawArguments();
            var m = invocation.getMethod();
            m.setAccessible(true);
            var name = m.getName();

            var result = m.invoke(field, args);
            if (name.equals("getGenericType"))
                return type;
            if (name.equals("getType"))
                return List.class;


            return result;
        });
    }
}
