package no.rundis.watcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public interface PathEventListener {
    void pathChange(WatchEvent.Kind eventKind, Path path);
}
