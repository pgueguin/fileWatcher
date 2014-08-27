package file_watcher;
/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*********
 *
 * Later updates performed by Pierre Guéguin
 * under GPL license
 *
 */
import org.apache.log4j.Logger;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

public class WatchDir {
    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private final boolean recursive;
    private boolean trace = false;
    private Config config;
    private List<Destination> destinations;
    private static Logger logger = Logger.getLogger(WatchDir.class);
    private boolean config_changed = false;
    private boolean overflow_occured = false;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                logger.info("register: " + dir);
            } else {
                if (!dir.equals(prev)) {
                    logger.info("update: " + prev + " " + dir);
                }
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException
            {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    WatchDir(Path dir, boolean recursive, Config config) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey,Path>();
        this.recursive = recursive;
        this.config = config;
        this.destinations = config.getDestinations();

        if (recursive) {
            logger.info("Scanning :"+dir);
            registerAll(dir);
            logger.info("Done.");
        } else {
            register(dir);
            register(Paths.get("config/"));
        }

        // enable trace after initial registration
        this.trace = true;

        //Process inbound directory
        for(Path filename: getFileList(dir)){
            processFile(filename);
        }

    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() {
        while(config_changed == false && overflow_occured == false) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                logger.error("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    logger.error("Overflow");
                    this.overflow_occured = true;
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // print out event
                logger.info("New event :"+event.kind().name()+": "+child);
                if(child.getParent().toString().equals("config")){
                    if(child.toString().equals("config"+File.separator+"config.xml")){
                        logger.info("Reloading configuration");
                        this.config_changed = true;
                    }
                } else if(event.kind().name().equals("ENTRY_MODIFY") || event.kind().name().equals("ENTRY_CREATE")) {
                    processFile(child);
                }

            }

            // reset key and remove from set if directory no longer accessible
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

    static void usage() {
        logger.error("usage: java file_watcher.WatchDir [-r] dir");
        System.exit(-1);
    }

    public  void processFile(Path filename) {
        logger.info("Processing File: "+filename);

        if(accept(filename.getFileName(),this.config.getSource().getPattern())) {
           logger.info("File accepted for input pattern");
            for (Destination destination: this.destinations) {
                if(accept(filename.getFileName(),destination.getPattern())) {
                    logger.info("File matched with Destination "+destination.getDirectory());
                    Path newDir = destination.getDirectory();
                    try {
                        Files.move(filename, newDir.resolve(filename.getFileName()),ATOMIC_MOVE);
                    } catch (IOException e) {
                        logger.error("Move error: "+e.getMessage());
                    }
                }
            }
        } else {
            logger.info("File not matching input pattern");
        }



        logger.info("Done processing"+filename);
    }

    public static List<Path> getFileList(Path directory) {
        List<Path> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                fileNames.add(path);
            }
        } catch (IOException ex) {}
        return fileNames;
    }


    public static boolean accept(Path filename, String pattern) {
        return filename.toString().matches(pattern);
    }

    public static void main(String[] args) throws IOException {
        // parse arguments
        if (args.length == 0 || args.length > 2)
            usage();
        boolean recursive = false;
        int dirArg = 0;
        if (args[0].equals("-r")) {
            if (args.length < 2)
                usage();
            recursive = true;
            dirArg++;
        }

        // register directory and process its events
        Path dir = Paths.get(args[dirArg]);
        //new WatchDir(dir, recursive).processEvents();
    }
}