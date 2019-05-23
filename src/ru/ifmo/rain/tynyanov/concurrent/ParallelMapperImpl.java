package ru.ifmo.rain.tynyanov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private Thread[] threads;
    private final int MAX_SIZE = 1_000_000;
    private final Queue<Runnable> taskQueue = new ArrayDeque<>();

    public ParallelMapperImpl(int threadsNumber) {
        if (threadsNumber < 1) {
            throw new IllegalArgumentException("Number of threads must be positive");
        }
        threads = new Thread[threadsNumber];
        for (int i = 0; i < threadsNumber; i++) {
            threads[i] = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        threadSolve();
                    }
                } catch (InterruptedException e) {
                    //Do nothing
                } finally {
                    Thread.currentThread().interrupt();
                }

            });
            threads[i].start();
        }
    }

    private void addTask(Runnable task) throws InterruptedException {
        synchronized (taskQueue) {
            while (taskQueue.size() == MAX_SIZE) {
                taskQueue.wait();
            }
            taskQueue.add(task);
            taskQueue.notify();
        }
    }

    private void threadSolve() throws InterruptedException {
        Runnable curTask;
        synchronized (taskQueue) {
            while (taskQueue.isEmpty()) {
                taskQueue.wait();
            }
            curTask = taskQueue.poll();
            taskQueue.notify();
        }
        curTask.run();
    }

    class ResultList<R> {
        private final List<R> list;
        int numberOfElements;

        private ResultList(int size) {
            list = new ArrayList<>(Collections.nCopies(size, null));
            numberOfElements = 0;
        }

        public void set(int index, R element) {
            synchronized (this) {
                list.set(index, element);
                if (++numberOfElements == list.size()) {
                    notify();
                }
            }
        }

        synchronized public List<R> getList() throws InterruptedException {
            while (numberOfElements != list.size()) {
                wait();
            }
            return list;
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        if (f == null || args == null) {
            throw new IllegalArgumentException("Arguments can't be null");
        }
        ResultList<R> threadsResults = new ResultList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int finalI = i;
            addTask(() -> threadsResults.set(finalI, f.apply(args.get(finalI))));
        }
        return threadsResults.getList();
    }

    @Override
    public void close() {
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                //Do nothing
            }
        }
    }
}
