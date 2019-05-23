package ru.ifmo.rain.tynyanov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebCrawler implements Crawler {

    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private Downloader downloader;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        downloadersPool = Executors.newFixedThreadPool(downloaders);
        extractorsPool = Executors.newFixedThreadPool(extractors);
    }

    private void recursiveDownload(String curUrl, int depth, Phaser phaser, final Set<String> visited,
                                   final Map<String, IOException> urlsWithExceptions) {
        if (!visited.contains(curUrl)) {
            visited.add(curUrl);
            phaser.register();
            downloadersPool.submit(() -> {
                try {
                    Document page = downloader.download(curUrl);
                    if (depth > 1) {
                        phaser.register();
                        extractorsPool.submit(() -> {
                            try {
                                page.extractLinks().forEach(link -> recursiveDownload(link, depth - 1, phaser, visited, urlsWithExceptions));
                            } catch (IOException ignored) {
                                //Caught in other try block
                            } finally {
                                phaser.arrive();
                            }
                        });
                    }
                } catch (IOException e) {
                    urlsWithExceptions.put(curUrl, e);
                } finally {
                    phaser.arrive();
                }
            });
        }
    }

    @Override
    public Result download(String url, int depth) {
        Map<String, IOException> urlsWithExceptions = new ConcurrentHashMap<>();
        Set<String> visited = new ConcurrentSkipListSet<>();
        Phaser phaser = new Phaser(1);
        recursiveDownload(url, depth, phaser, visited, urlsWithExceptions);
        phaser.arriveAndAwaitAdvance();
        visited.removeAll(urlsWithExceptions.keySet());
        return new Result(new ArrayList<>(visited), urlsWithExceptions);
    }

    @Override
    public void close() {
        downloadersPool.shutdownNow();
        extractorsPool.shutdownNow();
    }

    public static void main(String[] args) {
        if (args == null || args.length < 2 || args.length > 5) {
            System.out.println("You are expected to type from 2 to 5 arguments");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.out.println("All arguments must be not null");
                return;
            }
        }
        String url = args[0];
        int depth;
        int downloaders;
        int extractors;
        int perHost;
        try {
            depth = Integer.parseInt(args[1]);
            downloaders = args.length > 2 ? Integer.parseInt(args[2]) : 8;
            extractors = args.length > 3 ? Integer.parseInt(args[3]) : 8;
            perHost = args.length > 4 ? Integer.parseInt(args[4]) : 8;
        } catch (NumberFormatException e) {
            System.out.println("One of arguments can't be converted to int: " + e.getMessage());
            return;
        }
        try (WebCrawler crawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
            crawler.download(url, depth);
        } catch (IOException e) {
            System.out.println("Error while creating Caching Downloader: " + e.getMessage());
        }
    }
}
