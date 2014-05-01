package file_watcher;

import java.nio.file.Path;

/**
 * Created by pierre on 29/04/14.
 */
public class Destination {
    private String pattern;
    private Path directory;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Path getDirectory() {
        return directory;
    }

    public void setDirectory(Path directory) {
        this.directory = directory;
    }
}
