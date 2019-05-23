package ru.ifmo.rain.tynyanov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements SortedSet<T> {
    private List<T> data;
    private Comparator<? super T> comparator;

    public ArraySet() {
        data = Collections.emptyList();
    }

    public ArraySet(Collection<? extends T> collection) {
        data = new ArrayList<>(new TreeSet<>(collection));
    }

    public ArraySet(Comparator<? super T> comparator) {
        data = Collections.emptyList();
        this.comparator = comparator;
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        SortedSet<T> temp = new TreeSet<>(comparator);
        temp.addAll(collection);
        this.comparator = comparator;
        data = new ArrayList<>(temp);
    }

    private boolean checkIndex(int ind) {
        return ind >= 0 && ind < data.size();
    }

    private int findElement(T t) {
        int ind = Collections.binarySearch(data, Objects.requireNonNull(t), comparator);
        if (ind < 0) {
            ind = -ind - 1;
        }
        return ind;
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    private SortedSet<T> subSet(T fromElement, T toElement, boolean lastInclusive) {
        int indFrom = findElement(fromElement);
        int indTo = findElement(toElement) + (lastInclusive ? 1 : 0);
        if (indFrom > indTo || !checkIndex(indTo - 1) || !checkIndex(indFrom)) {
            return new ArraySet<>(comparator);
        }
        return new ArraySet<>(data.subList(indFrom, indTo), comparator);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        if (data.isEmpty()) {
            return new ArraySet<>(comparator);
        }
        return subSet(fromElement, last(), true);
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        if (data.isEmpty()) {
            return new ArraySet<>(comparator);
        }
        return subSet(first(), toElement);
    }


    @Override
    public T first() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(0);
    }

    @Override
    public T last() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return data.get(data.size() - 1);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(data, (T) Objects.requireNonNull(o), comparator) >= 0;
    }
}
