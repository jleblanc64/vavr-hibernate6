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

import io.github.jleblanc64.hibernate6.hibernate.duplicate.MyPersistentBag;
import io.vavr.PartialFunction;
import io.vavr.collection.List;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.Collection;

public class PersistentBagImpl extends MyPersistentBag implements io.vavr.collection.List {
    public PersistentBagImpl(SharedSessionContractImplementor session) {
        super(session);
    }

    public PersistentBagImpl(SharedSessionContractImplementor session, Collection coll) {
        super(session, coll);
    }

    @Override
    public Object head() {
        return get(0);
    }

    @Override
    public int length() {
        return size();
    }

    @Override
    public List tail() {
        return List.ofAll(bag).tail();
    }

    @Override
    public Object apply(Object o) {
        return ((PartialFunction) List.ofAll(bag)).apply(o);
    }

    @Override
    public boolean isDefinedAt(Object value) {
        return ((PartialFunction) List.ofAll(bag)).isDefinedAt(value);
    }
}
