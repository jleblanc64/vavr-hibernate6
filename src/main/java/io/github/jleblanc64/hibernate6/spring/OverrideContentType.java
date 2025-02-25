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
package io.github.jleblanc64.hibernate6.spring;

import io.github.jleblanc64.libcustom.LibCustom;
import lombok.SneakyThrows;

public class OverrideContentType {
    @SneakyThrows
    public static void override() {
        // accept text/plain content-type as json
        var httpHeadersClass = Class.forName("org.springframework.http.HttpHeaders");
        var mediaTypeClass = Class.forName("org.springframework.http.MediaType");
        LibCustom.modifyReturn(httpHeadersClass, "getContentType", argsR -> {
            var mediaType = argsR.returned;
            if (mediaType != null && mediaType.toString().toLowerCase().startsWith("text/plain"))
                return mediaTypeClass.getMethod("parseMediaType", String.class).invoke(null, "application/json");

            return mediaType;
        });
    }
}
