package ru.ifmo.rain.tynyanov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {
    private final ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private <T, R> R parallelAction(int threadsNum, List<? extends T> list,
                                    final Function<? super Stream<? extends T>, ? extends R> threadFunc,
                                    final Function<? super Stream<? extends R>, ? extends R> collectorFunc)
            throws InterruptedException {
        if (threadsNum < 1) {
            throw new IllegalArgumentException("Number of threads must be positive");
        }
        if (list == null || threadFunc == null || collectorFunc == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
        if (list.isEmpty()) {
            throw new NoSuchElementException("Can't work with empty list");
        }
        threadsNum = Math.min(Math.max(list.size(), 1), threadsNum);
        int threadBlockSize = list.size() / threadsNum;
        int elementsLeft = list.size() % threadsNum;
        List<Stream<? extends T>> threadsBlocks = new ArrayList<>();
        for (int l, r = 0, i = 0; i < threadsNum; ++i) {
            l = r;
            r = l + threadBlockSize;
            if (elementsLeft > 0) {
                ++r;
                --elementsLeft;
            }
            threadsBlocks.add(list.subList(l, r).stream());
        }
        List<R> threadsResults;
        if (mapper != null) {
            threadsResults = mapper.map(threadFunc, threadsBlocks);
        } else {
            threadsResults = new ArrayList<>(Collections.nCopies(threadsNum, null));
            Thread[] threads = new Thread[threadsNum];
            for (int i = 0; i < threadsNum; i++) {
                final int finalI = i;
                threads[i] = new Thread(() -> threadsResults.set(finalI, threadFunc.apply(threadsBlocks.get(finalI))));
                threads[i].start();
            }
            InterruptedException threadExceptions = null;
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    if (threadExceptions == null) {
                        threadExceptions = new InterruptedException("Exception in thread: " + e);
                    }
                    threadExceptions.addSuppressed(e);
                }
            }
            if (threadExceptions != null) {
                throw threadExceptions;
            }
        }

        return collectorFunc.apply(threadsResults.stream());
    }

    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (comparator == null) {
            throw new IllegalArgumentException("Comparator can't bw null");
        }
        Function<Stream<? extends T>, ? extends T> countMax = stream -> stream.max(comparator).orElse(null);
        return parallelAction(threads, values, countMax, countMax);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        if (predicate == null) {
            throw new IllegalArgumentException("Predicate can't be null");
        }
        return parallelAction(threads, values,
                stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(elem -> elem));
    }

    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelAction(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        if (predicate == null) {
            throw new IllegalArgumentException("Predicate can't bw null");
        }
        return parallelAction(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        if (f == null) {
            throw new IllegalArgumentException("Map function can't bw null");
        }
        return parallelAction(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }
}
