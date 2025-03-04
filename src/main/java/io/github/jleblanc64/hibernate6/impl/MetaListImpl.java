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
package io.github.jleblanc64.hibernate6.impl;

import io.github.jleblanc64.hibernate6.meta.BagProvider;
import io.github.jleblanc64.hibernate6.meta.MetaList;
import io.vavr.collection.List;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.Collection;

public class MetaListImpl implements MetaList<List> {
    @Override
    public Class<List> monadClass() {
        return List.class;
    }

    @Override
    public List fromJava(java.util.List l) {
        return List.ofAll(l);
    }

    @Override
    public java.util.List toJava(List l) {
        return l.asJava();
    }

    @Override
    public BagProvider<? extends List> bag() {
        return new BagProvider<PersistentBagImpl>() {

            @Override
            public PersistentBagImpl of(SharedSessionContractImplementor session) {
                return new PersistentBagImpl(session);
            }

            @Override
            public PersistentBagImpl of(SharedSessionContractImplementor session, Collection<?> collection) {
                return new PersistentBagImpl(session, collection);
            }
        };
    }
}
