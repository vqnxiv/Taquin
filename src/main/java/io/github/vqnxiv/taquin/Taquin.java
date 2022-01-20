package io.github.vqnxiv.taquin;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Main class.
 */
public class Taquin {

    /*
     * Async loggers.
     */
    static {
        System.setProperty(
            "Log4jContextSelector",
            "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
        );
    }
    
    /**
     * Root logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(Taquin.class);

    /**
     * Main.
     * 
     * @param args args.
     */
    public static void main(String[] args) {
        LOGGER.info("Starting application");
        JfxApp.main(args);
    }
}