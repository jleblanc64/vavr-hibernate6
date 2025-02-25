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
package io.github.jleblanc64.hibernate6.hibernate.duplicate;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.*;

public class MyPersistentBag<E> extends AbstractPersistentCollection<E> {

    protected List<E> bag;

    /**
     * The Collection provided to a PersistentBag constructor
     */
    private Collection<E> providedCollection;

    /**
     * Constructs a PersistentBag.  Needed for SOAP libraries, etc
     */
    @SuppressWarnings("UnusedDeclaration")
    public MyPersistentBag() {
    }

    /**
     * Constructs a PersistentBag
     *
     * @param session The session
     */
    public MyPersistentBag(SharedSessionContractImplementor session) {
        super(session);
    }

    /**
     * Constructs a PersistentBag
     *
     * @param session The session
     * @param coll    The base elements.
     */
    public MyPersistentBag(SharedSessionContractImplementor session, Collection<E> coll) {
        super(session);
        providedCollection = coll;
        if (coll instanceof List) {
            bag = (List<E>) coll;
        } else {
            bag = new ArrayList<>(coll);
        }
        setInitialized();
        setDirectlyAccessible(true);
    }

    @Override
    public boolean isWrapper(Object collection) {
        return bag == collection;
    }

    @Override
    public boolean isDirectlyProvidedCollection(Object collection) {
        return isDirectlyAccessible() && providedCollection == collection;
    }

    @Override
    public boolean empty() {
        return bag.isEmpty();
    }

    @Override
    public Iterator<E> entries(CollectionPersister persister) {
        return bag.iterator();
    }

    public void injectLoadedState(PluralAttributeMapping attributeMapping, List<?> loadingState) {
        assert bag == null;

        final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();
        final CollectionSemantics<?, ?> collectionSemantics = collectionDescriptor.getCollectionSemantics();

        final int elementCount = loadingState == null ? 0 : loadingState.size();

        this.bag = (List<E>) collectionSemantics.instantiateRaw(elementCount, collectionDescriptor);

        if (loadingState != null) {
            for (int i = 0; i < elementCount; i++) {
                bag.add((E) loadingState.get(i));
            }
        }
    }

    @Override
    public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
        final Type elementType = persister.getElementType();
        final List<?> sn = (List<?>) getSnapshot();
        if (sn.size() != bag.size()) {
            return false;
        }

        // HHH-11032 - Group objects by Type.getHashCode() to reduce the complexity of the search
        final Map<Integer, List<Object>> hashToInstancesBag = groupByEqualityHash(bag, elementType);
        final Map<Integer, List<Object>> hashToInstancesSn = groupByEqualityHash(sn, elementType);
        if (hashToInstancesBag.size() != hashToInstancesSn.size()) {
            return false;
        }

        // First iterate over the hashToInstancesBag entries to see if the number
        // of List values is different for any hash value.
        for (Map.Entry<Integer, List<Object>> hashToInstancesBagEntry : hashToInstancesBag.entrySet()) {
            final Integer hash = hashToInstancesBagEntry.getKey();
            final List<Object> instancesBag = hashToInstancesBagEntry.getValue();
            final List<Object> instancesSn = hashToInstancesSn.get(hash);
            if (instancesSn == null || (instancesBag.size() != instancesSn.size())) {
                return false;
            }
        }

        // We already know that both hashToInstancesBag and hashToInstancesSn have:
        // 1) the same hash values;
        // 2) the same number of values with the same hash value.

        // Now check if the number of occurrences of each element is the same.
        for (Map.Entry<Integer, List<Object>> hashToInstancesBagEntry : hashToInstancesBag.entrySet()) {
            final Integer hash = hashToInstancesBagEntry.getKey();
            final List<Object> instancesBag = hashToInstancesBagEntry.getValue();
            final List<Object> instancesSn = hashToInstancesSn.get(hash);
            for (Object instance : instancesBag) {
                if (!expectOccurrences(
                        instance,
                        instancesBag,
                        elementType,
                        countOccurrences(instance, instancesSn, elementType)
                )) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Groups items in searchedBag according to persistence "equality" as defined in Type.isSame and Type.getHashCode
     *
     * @return Map of "equality" hashCode to List of objects
     */
    private Map<Integer, List<Object>> groupByEqualityHash(List<?> searchedBag, Type elementType) {
        if (searchedBag.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, List<Object>> map = new HashMap<>();
        for (Object o : searchedBag) {
            map.computeIfAbsent(nullableHashCode(o, elementType), k -> new ArrayList<>()).add(o);
        }
        return map;
    }

    /**
     * @return the default elementType hashcode of the object o, or null if the object is null
     */
    private Integer nullableHashCode(Object o, Type elementType) {
        if (o == null) {
            return null;
        } else {
            return elementType.getHashCode(o);
        }
    }

    @Override
    public boolean isSnapshotEmpty(Serializable snapshot) {
        return ((Collection<?>) snapshot).isEmpty();
    }

    private int countOccurrences(Object element, List<Object> list, Type elementType) {
        int result = 0;
        for (Object listElement : list) {
            if (elementType.isSame(element, listElement)) {
                result++;
            }
        }
        return result;
    }

    private boolean expectOccurrences(Object element, List<Object> list, Type elementType, int expected) {
        int result = 0;
        for (Object listElement : list) {
            if (elementType.isSame(element, listElement)) {
                if (result++ > expected) {
                    return false;
                }
            }
        }
        return result == expected;
    }

    @Override
    public Serializable getSnapshot(CollectionPersister persister)
            throws HibernateException {
        final ArrayList<E> clonedList = new ArrayList<>(bag.size());
        for (E item : bag) {
            clonedList.add((E) persister.getElementType().deepCopy(item, persister.getFactory()));
        }
        return clonedList;
    }

    @Override
    public Collection<E> getOrphans(Serializable snapshot, String entityName) throws HibernateException {
        final List<E> sn = (List<E>) snapshot;
        return getOrphans(sn, bag, entityName, getSession());
    }

    @Override
    public void initializeEmptyCollection(CollectionPersister persister) {
        assert bag == null;
        //noinspection unchecked
        bag = (List<E>) persister.getCollectionSemantics().instantiateRaw(0, persister);
        endRead();
    }

    @Override
    public Object disassemble(CollectionPersister persister) {
        final int length = bag.size();
        final Serializable[] result = new Serializable[length];
        for (int i = 0; i < length; i++) {
            result[i] = persister.getElementType().disassemble(bag.get(i), getSession(), null);
        }
        return result;
    }

    @Override
    public void initializeFromCache(CollectionPersister collectionDescriptor, Object disassembled, Object owner)
            throws HibernateException {
        assert bag == null;

        final Serializable[] array = (Serializable[]) disassembled;
        final int size = array.length;

        this.bag = (List<E>) collectionDescriptor.getCollectionSemantics().instantiateRaw(size, collectionDescriptor);

        for (Serializable item : array) {
            final Object element = collectionDescriptor.getElementType().assemble(item, getSession(), owner);
            if (element != null) {
                bag.add((E) element);
            }
        }
    }

    @Override
    public boolean needsRecreate(CollectionPersister persister) {
        return !persister.isOneToMany();
    }

    // For a one-to-many, a <bag> is not really a bag;
    // it is *really* a set, since it can't contain the
    // same element twice. It could be considered a bug
    // in the mapping dtd that <bag> allows <one-to-many>.

    // Anyway, here we implement <set> semantics for a
    // <one-to-many> <bag>!

    @Override
    public Iterator<?> getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
        final Type elementType = persister.getElementType();
        final ArrayList<Object> deletes = new ArrayList<>();
        final List<?> sn = (List<?>) getSnapshot();
        final Iterator<?> olditer = sn.iterator();
        int i = 0;
        while (olditer.hasNext()) {
            final Object old = olditer.next();
            final Iterator<E> newiter = bag.iterator();
            boolean found = false;
            if (bag.size() > i && elementType.isSame(old, bag.get(i++))) {
                //a shortcut if its location didn't change!
                found = true;
            } else {
                //search for it
                //note that this code is incorrect for other than one-to-many
                while (newiter.hasNext()) {
                    if (elementType.isSame(old, newiter.next())) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                deletes.add(old);
            }
        }
        return deletes.iterator();
    }

    @Override
    public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
        final List<?> sn = (List<?>) getSnapshot();
        if (sn.size() > i && elemType.isSame(sn.get(i), entry)) {
            //a shortcut if its location didn't change!
            return false;
        } else {
            //search for it
            //note that this code is incorrect for other than one-to-many
            for (Object old : sn) {
                if (elemType.isSame(old, entry)) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public boolean isRowUpdatePossible() {
        return false;
    }

    @Override
    public boolean needsUpdating(Object entry, int i, Type elemType) {
        return false;
    }

    public int size() {
        return readSize() ? getCachedSize() : bag.size();
    }

    public boolean isEmpty() {
        return readSize() ? getCachedSize() == 0 : bag.isEmpty();
    }

    public boolean contains(Object object) {
        final Boolean exists = readElementExistence(object);
        return exists == null ? bag.contains(object) : exists;
    }

    public Iterator<E> iteratorPriv() {
        read();
        return new IteratorProxy<>(bag.iterator());
    }

    public Object[] toArrayPriv() {
        read();
        return bag.toArray();
    }

    public <A> A[] toArray(A[] a) {
        read();
        return bag.toArray(a);
    }

    public boolean add(E object) {
        if (!isOperationQueueEnabled()) {
            write();
            return bag.add(object);
        } else {
            queueOperation(new SimpleAdd(object));
            return true;
        }
    }

    public boolean removePriv(Object o) {
        initialize(true);
        if (bag.remove(o)) {
            elementRemoved = true;
            dirty();
            return true;
        } else {
            return false;
        }
    }

    public boolean containsAll(Collection<?> c) {
        read();
        return bag.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> values) {
        if (values.size() == 0) {
            return false;
        }
        if (!isOperationQueueEnabled()) {
            write();
            return bag.addAll(values);
        } else {
            for (E value : values) {
                queueOperation(new SimpleAdd(value));
            }
            return values.size() > 0;
        }
    }

    public boolean removeAll(Collection<?> c) {
        if (c.size() > 0) {
            initialize(true);
            if (bag.removeAll(c)) {
                elementRemoved = true;
                dirty();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean retainAll(Collection<?> c) {
        initialize(true);
        if (bag.retainAll(c)) {
            dirty();
            return true;
        } else {
            return false;
        }
    }

    public void clear() {
        if (isClearQueueEnabled()) {
            queueOperation(new Clear());
        } else {
            initialize(true);
            if (!bag.isEmpty()) {
                bag.clear();
                dirty();
            }
        }
    }

    @Override
    public Object getIndex(Object entry, int i, CollectionPersister persister) {
        throw new UnsupportedOperationException("Bags don't have indexes : " + persister.getRole());
    }

    @Override
    public Object getElement(Object entry) {
        return entry;
    }

    @Override
    public Object getSnapshotElement(Object entry, int i) {
        final List<?> sn = (List<?>) getSnapshot();
        return sn.get(i);
    }

    /**
     * Count how many times the given object occurs in the elements
     *
     * @param o The object to check
     * @return The number of occurrences.
     */
    @SuppressWarnings("UnusedDeclaration")
    public int occurrences(Object o) {
        read();
        final Iterator<E> itr = bag.iterator();
        int result = 0;
        while (itr.hasNext()) {
            if (o.equals(itr.next())) {
                result++;
            }
        }
        return result;
    }

    // List OPERATIONS:

    public void add(int i, E o) {
        write();
        bag.add(i, o);
    }

    public boolean addAll(int i, Collection<? extends E> c) {
        if (c.size() > 0) {
            write();
            return bag.addAll(i, c);
        } else {
            return false;
        }
    }

    public E get(int i) {
        read();
        return bag.get(i);
    }

    public int indexOf(Object o) {
        read();
        return bag.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        read();
        return bag.lastIndexOf(o);
    }

    public ListIterator<E> listIterator() {
        read();
        return new ListIteratorProxy(bag.listIterator());
    }

    public ListIterator<E> listIterator(int i) {
        read();
        return new ListIteratorProxy(bag.listIterator(i));
    }

    public E remove(int i) {
        write();
        return bag.remove(i);
    }

    public E set(int i, E o) {
        write();
        return bag.set(i, o);
    }

    public List<E> subList(int start, int end) {
        read();
        return new ListProxy(bag.subList(start, end));
    }

    @Override
    public boolean entryExists(Object entry, int i) {
        return entry != null;
    }

    @Override
    public String toString() {
        read();
        return bag.toString();
    }

    /**
     * For efficiency, bag does not respect the semantics of
     * {@link List#equals(Object)} as specified by the supertype
     * {@link List}. Instead, instance equality is used, to avoid
     * the need to fetch the elements of the bag.
     *
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    final class Clear implements DelayedOperation<E> {
        @Override
        public void operate() {
            bag.clear();
        }

        @Override
        public E getAddedInstance() {
            return null;
        }

        @Override
        public E getOrphan() {
            throw new UnsupportedOperationException("queued clear cannot be used with orphan delete");
        }
    }

    final class SimpleAdd extends AbstractValueDelayedOperation {

        public SimpleAdd(E addedValue) {
            super(addedValue, null);
        }

        @Override
        public void operate() {
            // Delayed operations only work on inverse collections i.e. collections with mappedBy,
            // and these collections don't have duplicates by definition.
            // Since cascading also operates on delayed operation's elements,
            // it can happen that an element is already associated with the collection after cascading,
            // but the queued operations are still executed after the lazy initialization of the collection.
            // To avoid duplicates, we have to check if the bag already contains this element
            if (!bag.contains(getAddedInstance())) {
                bag.add(getAddedInstance());
            }
        }
    }

    final class SimpleRemove extends AbstractValueDelayedOperation {

        public SimpleRemove(E orphan) {
            super(null, orphan);
        }

        @Override
        public void operate() {
            bag.remove(getOrphan());
        }
    }
}
