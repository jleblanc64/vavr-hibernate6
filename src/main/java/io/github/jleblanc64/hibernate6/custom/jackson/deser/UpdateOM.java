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
package io.github.jleblanc64.hibernate6.custom.jackson.deser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.jleblanc64.hibernate6.custom.hibernate.Utils;
import io.github.jleblanc64.hibernate6.custom.meta.MetaList;
import io.github.jleblanc64.hibernate6.custom.meta.MetaOption;
import lombok.SneakyThrows;
import org.springframework.http.converter.HttpMessageConverter;

import java.util.List;

public class UpdateOM {
    @SneakyThrows
    public static void update(ObjectMapper om, List<HttpMessageConverter<?>> converters, MetaOption metaOption, MetaList metaList) {
        om.registerModule(new OptionModule(metaOption));

        var simpleModule = new SimpleModule()
                .addDeserializer(metaList.monadClass(), new ListDeser.Deserializer(metaList))
                .addSerializer(metaList.monadClass(), new ListDeser.Serializer(metaList));
        om.registerModule(simpleModule);

        var msgConverterClass = Class.forName("org.springframework.http.converter.json.MappingJackson2HttpMessageConverter");
        io.vavr.collection.List.ofAll(converters).filter(c -> msgConverterClass.isAssignableFrom(c.getClass()))
                .forEach(c -> setObjectMapper(c, om));
    }

    @SneakyThrows
    static void setObjectMapper(Object msgConverter, ObjectMapper om) {
        Utils.invoke(msgConverter, "setObjectMapper", om);
    }
}
