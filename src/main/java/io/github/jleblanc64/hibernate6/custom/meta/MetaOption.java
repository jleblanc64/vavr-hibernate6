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
package io.github.jleblanc64.hibernate6.custom.meta;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.ReferenceTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;

import java.lang.reflect.Type;

public interface MetaOption<T> extends WithClass<T> {
    T fromValue(Object v);

    Object getOrNull(Object o);

    default Deserializers.Base deserBase() {
        return new Deserializers.Base() {
            @Override
            public JsonDeserializer<?> findReferenceDeserializer(ReferenceType refType,
                                                                 DeserializationConfig config, BeanDescription beanDesc,
                                                                 TypeDeserializer contentTypeDeserializer, JsonDeserializer<?> contentDeserializer) {
                if (isSuperClassOf(refType.getRawClass()))
                    return deser(refType, null, contentTypeDeserializer, contentDeserializer);

                return null;
            }
        };
    }

    default ReferenceTypeDeserializer<T> deser(JavaType fullType, ValueInstantiator inst, TypeDeserializer typeDeser, JsonDeserializer<?> deser) {
        return new ReferenceTypeDeserializer<>(fullType, inst, typeDeser, deser) {
            @Override
            protected ReferenceTypeDeserializer withResolved(TypeDeserializer typeDeser, JsonDeserializer valueDeser) {
                return deser(_fullType, _valueInstantiator, typeDeser, valueDeser);
            }

            @Override
            public T getNullValue(DeserializationContext ctxt) throws JsonMappingException {
                return fromValue(_valueDeserializer.getNullValue(ctxt));
            }

            @Override
            public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
                return getNullValue(ctxt);
            }

            @Override
            public T referenceValue(Object contents) {
                return fromValue(contents);
            }

            @Override
            public Object getReferenced(T reference) {
                return getOrNull(reference);
            }

            @Override
            public T updateReference(T reference, Object contents) {
                return fromValue(contents);
            }
        };
    }

    default TypeModifier typeModifier() {
        return new TypeModifier() {
            @Override
            public JavaType modifyType(JavaType type, Type jdkType, TypeBindings bindings, TypeFactory typeFactory) {
                if (isSuperClassOf(type.getRawClass()))
                    return ReferenceType.upgradeFrom(type, type.containedTypeOrUnknown(0));

                return type;
            }
        };
    }
}