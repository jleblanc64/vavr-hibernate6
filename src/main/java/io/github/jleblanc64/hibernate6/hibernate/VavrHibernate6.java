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
package io.github.jleblanc64.hibernate6.hibernate;

import io.github.jleblanc64.hibernate6.hibernate.duplicate.FieldCustomType;
import io.github.jleblanc64.hibernate6.hibernate.duplicate.TypeImpl;
import io.github.jleblanc64.hibernate6.impl.MetaListImpl;
import io.github.jleblanc64.hibernate6.impl.MetaOptionImpl;
import io.github.jleblanc64.hibernate6.jackson.VavrJackson;
import io.github.jleblanc64.hibernate6.meta.MetaList;
import io.github.jleblanc64.hibernate6.meta.MetaOption;
import io.github.jleblanc64.hibernate6.spring.OverrideContentType;
import io.github.jleblanc64.hibernate6.spring.VavrSpring;
import io.github.jleblanc64.libcustom.LibCustom;
import lombok.SneakyThrows;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.internal.PluralAttributeMetadata;
import org.hibernate.metamodel.model.domain.internal.PluralAttributeBuilder;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.type.BagType;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static io.github.jleblanc64.hibernate6.hibernate.Utils.checkPersistentBag;
import static io.github.jleblanc64.hibernate6.hibernate.Utils.isOfType;
import static org.mockito.Mockito.mock;

public class VavrHibernate6 {
    public static void override() {
        var metaList = new MetaListImpl();
        var metaOption = new MetaOptionImpl();

        overrideCustom(metaList, metaOption);
    }

    public static void overrideCustom(MetaList metaList, MetaOption metaOption) {
        overrideCustom(metaList);
        VavrSpring.overrideCustom(metaList);
        VavrJackson.overrideCustom(metaList);

        overrideCustom(metaOption);
        VavrSpring.overrideCustom(metaOption);
        VavrJackson.overrideCustom(metaOption);

        OverrideContentType.override();

        LibCustom.load();
    }

    @SneakyThrows
    private static void overrideCustom(MetaList metaList) {

        LibCustom.modifyReturn(JavaReflectionManager.class, "getXProperty", x -> {
            var returned = x.returned;
            var typeS = Utils.getRefl(returned, "type").toString();
            var env = (TypeEnvironment) Utils.getRefl(returned, "env");
            var factory = (JavaReflectionManager) Utils.getRefl(returned, "factory");

            if (typeS.startsWith(metaList.monadClass().getName() + "<"))
                return mock(returned.getClass(), invocation -> {
                    var args = invocation.getRawArguments();
                    var m = invocation.getMethod();
                    m.setAccessible(true);
                    var name = m.getName();

                    var result = m.invoke(returned, args);
                    if (name.equals("isCollection"))
                        return true;
                    if (name.equals("getCollectionClass"))
                        return List.class;
                    if (name.equals("getElementClass"))
                        return buildClass(typeS, env, factory);


                    return result;
                });

            return LibCustom.ORIGINAL;
        });

        LibCustom.override(Class.forName("org.hibernate.metamodel.internal.PluralAttributeMetadataImpl"), "determineCollectionType",
                args -> {
                    var clazz = (Class) args[0];
                    if (metaList.isSuperClassOf(clazz))
                        return CollectionClassification.LIST;

                    return LibCustom.ORIGINAL;
                });

        LibCustom.modifyArg(PluralAttributeBuilder.class, "build", 0, args -> {
            var attributeMetadata = (PluralAttributeMetadata) args[0];
            if (metaList.isSuperClassOf(attributeMetadata.getJavaType()))
                return mock(attributeMetadata.getClass(), invocation -> {
                    var m = invocation.getMethod();
                    m.setAccessible(true);
                    var name = m.getName();

                    var result = m.invoke(attributeMetadata, invocation.getRawArguments());
                    if (name.equals("getJavaType"))
                        return List.class;

                    return result;
                });

            return LibCustom.ORIGINAL;
        });

        LibCustom.modifyArg(Class.forName("org.hibernate.type.CollectionType"), "getElementsIterator", 0, args -> {
            var collection = args[0];
            if (metaList.isSuperClassOf(collection))
                return metaList.toJava(collection);

            return collection;
        });

        var bagProvList = metaList.bag();
        LibCustom.override(BagType.class, "instantiate", args -> {
            if (args.length == 1)
                return LibCustom.ORIGINAL;

            var pers = (AbstractCollectionPersister) args[1];
            if (isOfType(pers, metaList))
                return checkPersistentBag(bagProvList.of((SharedSessionContractImplementor) args[0]));

            return LibCustom.ORIGINAL;
        });

        LibCustom.override(BagType.class, "wrap", args -> {
            var arg1 = args[1];

            if (metaList.isSuperClassOf(arg1)) {
                var c = metaList.toJava(arg1);
                return checkPersistentBag(bagProvList.of((SharedSessionContractImplementor) args[0], c));
            }

            return LibCustom.ORIGINAL;
        });
    }

    @SneakyThrows
    private static void overrideCustom(MetaOption<?> metaOption) {
        var setterFieldImplClass = Class.forName("org.hibernate.property.access.spi.SetterFieldImpl");
        var getterFieldImplClass = Class.forName("org.hibernate.property.access.spi.GetterFieldImpl");

        LibCustom.modifyArgWithSelf(setterFieldImplClass, "set", 1, argsSelf -> {
            var args = argsSelf.args;
            var value = args[1];
            var self = argsSelf.self;
            var field = (Field) Utils.getRefl(self, setterFieldImplClass.getDeclaredField("field"));

            if (metaOption.isSuperClassOf(field.getType()) && !metaOption.isSuperClassOf(value))
                return metaOption.fromValue(value);

            return LibCustom.ORIGINAL;
        });

        LibCustom.modifyReturn(getterFieldImplClass, "get", x -> {
            var ret = x.returned;
            if (metaOption.isSuperClassOf(ret))
                return metaOption.getOrNull(ret);

            return ret;
        });

        LibCustom.modifyArg(Class.forName("org.hibernate.annotations.common.reflection.java.JavaXProperty"), "create", 0, args -> {
            var member = args[0];
            if (member instanceof Field) {
                var field = (Field) member;
                if (!(field.getGenericType() instanceof ParameterizedType))
                    return LibCustom.ORIGINAL;

                var type = (ParameterizedType) field.getGenericType();
                var typeRaw = type.getRawType();
                var typeParam = type.getActualTypeArguments()[0];
                var ownerType = ((ParameterizedType) field.getGenericType()).getOwnerType();

                if (metaOption.isSuperClassOf(typeRaw))
                    return FieldCustomType.create(field, new TypeImpl((Class<?>) typeParam, new Type[]{}, ownerType));
            }

            return LibCustom.ORIGINAL;
        });
    }

    @SneakyThrows
    private static XClass buildClass(String typeS, TypeEnvironment env, JavaReflectionManager factory) {
        var paramClass = Utils.paramClass(typeS);
        var clazzJavaXClass = Class.forName("org.hibernate.annotations.common.reflection.java.JavaXClass");
        var constructor = clazzJavaXClass.getDeclaredConstructor(Class.class, TypeEnvironment.class, JavaReflectionManager.class);
        constructor.setAccessible(true);

        return (XClass) constructor.newInstance(paramClass, env, factory);
    }
}
