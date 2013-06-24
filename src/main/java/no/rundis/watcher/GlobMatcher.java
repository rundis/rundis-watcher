package no.rundis.watcher;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

public class GlobMatcher {
    private final Path rootPath;
    private final List<String> globPatterns;


    public GlobMatcher(Path rootPath, List<String> globPatterns) {
        this.rootPath = rootPath;
        this.globPatterns = globPatterns;
    }

    public boolean matches(Path path) {
        for(String pattern : globPatterns) {
            if(pathMatcher(pattern).matches(candidatePath(path))) {
                return true;
            }
        }
        return false;
    }

    private PathMatcher pathMatcher(String pattern) {
        return rootPath.getFileSystem().getPathMatcher("glob:" + pattern);
    }

    private Path candidatePath(Path path) {
        return path.startsWith(rootPath) ? rootPath.relativize(path) : path;
    }


}
