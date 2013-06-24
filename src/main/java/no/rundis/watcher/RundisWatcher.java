package no.rundis.watcher;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

public class RundisWatcher {
    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private final PathEventListener listener;
    private final GlobMatcher globMatcher;
    private volatile boolean stop;

    private RundisWatcher(PathEventListener listener, GlobMatcher globMatcher) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();
        this.listener = listener;
        this.globMatcher = globMatcher;
    }


    public static RundisWatcher create(Path rootPath, ArrayList<String> globPatterns, PathEventListener pathEventListener) {
        try {
            GlobMatcher globMatcher = new GlobMatcher(rootPath, globPatterns);
            RundisWatcher rundisWatcher = new RundisWatcher(pathEventListener, globMatcher);
            rundisWatcher.registerAll(rootPath);
            rundisWatcher.processEvents();

            return rundisWatcher;
        } catch(IOException e) {
            throw new RundisWatcherException("Error creating watcher", e);
        }
    }

    void stop() {
        stop = true;
    }


    /**
     * Process all events for keys queued to the watcher
     */
    public void processEvents() {
        while(!stop) {
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                Path child = resolveChild(dir, event);
                if(globMatcher.matches(child)) {
                    listener.pathChange(event.kind(), child);
                }

                if (kind == ENTRY_CREATE) {
                    registerIfDirectory(child);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }

    }

    private Path resolveChild(Path dir, WatchEvent<?> event) {
        WatchEvent<Path> ev = cast(event);
        Path name = ev.context();
        return dir.resolve(name);
    }

    private void registerIfDirectory(Path child) {
        try {
            if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                registerAll(child);
            }
        } catch (IOException x) {
            throw new RundisWatcherException("Error registering new directory", x);
        }
    }


    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);

        System.out.println("Registered dir:" + dir.toString());

    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
}
