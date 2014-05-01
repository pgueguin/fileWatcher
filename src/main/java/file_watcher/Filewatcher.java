package file_watcher;

import org.apache.log4j.Logger;

import java.nio.file.Path;

/**
 * Created by Pierre GUEGUIN on 29/04/14.
 */

//TODO Add resilienceto config not loaded
//TODO crossplatform file separator
//TODO test on windows
//TODO create test
public class Filewatcher {
    private static Logger logger = Logger.getLogger(Filewatcher.class);

    public static void main (String[] args) throws Exception {
         boolean recursive = false;
        while(true) {
             XMLReader xmlReader = new XMLReader();
            logger.info("Start Reading Configuration file");
            Config config = xmlReader.read();
            logger.info("Stop Reading Configuration file");
            logger.info("Starting filewatcher");
            Path dir = config.getSource().getDirectory();
            new WatchDir(dir, recursive, config).processEvents();
            logger.info("Stopping filewatcher");

        }
    }
}
