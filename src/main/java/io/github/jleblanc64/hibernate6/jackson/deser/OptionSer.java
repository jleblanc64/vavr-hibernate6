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
package io.github.jleblanc64.hibernate6.jackson.deser;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.StdConverter;
import io.github.jleblanc64.hibernate6.meta.MetaOption;

public class OptionSer<T> extends StdDelegatingSerializer {
    public OptionSer(MetaOption<T> metaOption) {
        super(new StdConverter<T, Object>() {
            @Override
            public Object convert(T value) {
                return metaOption.getOrNull(value);
            }
        });
    }

    @Override
    protected StdDelegatingSerializer withDelegate(Converter<Object, ?> c, JavaType t, JsonSerializer<?> deser) {
        return new StdDelegatingSerializer(c, t, deser);
    }
}

