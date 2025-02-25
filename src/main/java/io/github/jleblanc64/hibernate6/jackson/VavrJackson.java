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
package io.github.jleblanc64.hibernate6.jackson;

import io.github.jleblanc64.hibernate6.meta.MetaList;
import io.github.jleblanc64.hibernate6.meta.MetaOption;
import io.github.jleblanc64.libcustom.LibCustom;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

import java.util.ArrayList;

import static io.github.jleblanc64.libcustom.FieldMocked.*;

public class VavrJackson {
    public static void override(MetaList metaList) {
        LibCustom.modifyReturn(AbstractJackson2HttpMessageConverter.class, "readJavaType", argsR -> {
            var returned = argsR.returned;
            if (returned == null)
                return returned;

            fields(returned).forEach(f -> {
                var type = f.getType();
                Object empty;
                if (metaList.isSuperClassOf(type))
                    empty = metaList.fromJava(new ArrayList());
                else
                    return;

                var o = getRefl(returned, f);
                if (o == null)
                    setRefl(returned, f, empty);
            });

            return returned;
        });
    }

    public static void override(MetaOption metaOption) {
        LibCustom.modifyReturn(AbstractJackson2HttpMessageConverter.class, "readJavaType", argsR -> {
            var returned = argsR.returned;
            if (returned == null)
                return returned;

            fields(returned).forEach(f -> {
                var type = f.getType();
                Object empty;
                if (metaOption.isSuperClassOf(type))
                    empty = metaOption.fromValue(null);
                else
                    return;

                var o = getRefl(returned, f);
                if (o == null)
                    setRefl(returned, f, empty);
            });

            return returned;
        });
    }
}
