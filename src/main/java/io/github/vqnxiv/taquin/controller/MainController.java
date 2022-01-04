package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.Taquin;
import io.github.vqnxiv.taquin.logger.MainAppender;
import io.github.vqnxiv.taquin.solver.SearchRunner;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;


/**
 * Main controller class which handles {@code main.fxml}.
 */
public class MainController {
    
    /**
     * Private class which extends {@link ScheduledService} and is responsible for
     * checking the heap's current used memory.
     */
    private static class HeapMonitorService extends ScheduledService<Long> {

        /**
         * Memory bean from which we retrieve the heap memory values.
         */
        private final MemoryMXBean memoryMXBean;

        /**
         * Maximum heap memory allowed. {@link MemoryUsage#getMax()}
         */
        private final long maxHeap;

        /**
         * Whether to log a warning in case of high heap usage (greather than {@link #warningValue}).
         */
        private boolean belowWarningValue = true;

        /**
         * Value in percent which is considered high heap usage and logs a warning.
         */
        private final double warningValue = 0.9d;

        /**
         * Default constructor. Initializes {@link #memoryMXBean} and {@link #maxHeap}.
         */
        private HeapMonitorService() {
            memoryMXBean = ManagementFactory.getMemoryMXBean();
            maxHeap = memoryMXBean.getHeapMemoryUsage().getMax() / 1048576;
        }

        /**
         * Creates the {@link Task} which is executed in the background. 
         * <p>
         * Logs a warning if {@link #belowWarningValue} is {@code true} and {@code currentHeap / maxHeap} 
         * >= {@link #warningValue}.
         * 
         * @return {@link Long} value of the current's heap usage: {@link MemoryUsage#getUsed()}
         */
        @Override
        protected Task<Long> createTask() {
            return new Task<>() {
                @Override
                protected Long call() throws Exception {
                    var currentHeap = memoryMXBean.getHeapMemoryUsage().getUsed() / 1048576;
                    if((double) currentHeap / maxHeap >= warningValue) {
                        if(belowWarningValue) {
                            belowWarningValue = false;
                            LOGGER.warn((int) (warningValue * 100) + "% heap usage: the app may be slowed down");
                        }
                    } else {
                        belowWarningValue = true;
                    }
                    return currentHeap;
                }
            };
        }
    }
    
    
    private static final Logger LOGGER = LogManager.getLogger();


    /**
     * {@link TextArea} in which app log events are displayed. {@link MainAppender}
     */
    @FXML 
    private TextArea logOutput;

    /**
     * {@link VBox} which contains all {@link BuilderController} created.
     */
    @FXML 
    private VBox builderVBox;

    /**
     * {@link Label} whose {@link Label#textProperty} is bound to {@link SearchRunner#lastSearchInfo()}.
     */
    @FXML 
    private Label searchInfo;

    /**
     * {@link Label} which displays the current heap usage. {@link HeapMonitorService}
     */
    @FXML 
    private Label heapUsage;

    /**
     * {@link ScheduledService} which regularly checks the heap memory usage.
     */
    private final HeapMonitorService heapService;

    /**
     * {@link FXMLLoader} which is used when creating {@link BuilderController} in {@link #onAddBuilderActivated()}.
     */
    private FXMLLoader loader;

    /**
     * {@link SearchRunner} which is dependency-injected in the {@link BuilderController}.
     * Closing the app makes this {@link MainController} call {@link SearchRunner#shutdown(boolean)}.
     */
    private final SearchRunner searchRunner;

    /**
     * Constructor which initializes {@link #searchRunner} and {@link #heapService}.
     */
    public MainController() {
        searchRunner = new SearchRunner();

        heapService = new HeapMonitorService();
        heapService.setRestartOnFailure(true);
        heapService.setOnSucceeded(
            e -> heapUsage.setText(
                String.format("Heap: %4d / %4d MB", heapService.getValue(), heapService.maxHeap)
            )
        );
    }

    /**
     * JavaFX method.
     */
    public void initialize() {
        MainAppender.setLogOutput(logOutput);
        onAddBuilderActivated();
        searchInfo.textProperty().bind(searchRunner.lastSearchInfo());
        LOGGER.info("Successfully loaded GUI"); // keep this one here as it serves to tell the appender we good
        heapService.start();
    }

    /**
     * Creates a new {@link BuilderController} and adds it to {@link #builderVBox}.
     */
    @FXML
    private void onAddBuilderActivated() {
        LOGGER.info("Adding new build controller");

        loader = new FXMLLoader(Taquin.class.getResource("/fxml/primary/builder.fxml"));
        loader.setControllerFactory(
            param -> new BuilderController(searchRunner)
        );

        try {
            builderVBox.getChildren().add(loader.load());
        } catch(IOException e) {
            LOGGER.error("Could not find FXML file '/fxml/primary/builder.fxml'");
            e.printStackTrace();
        }
    }
    


    public void onAboutButtonActivated() { }

    public void onHelpButtonActivated() { }

    public void onSettingsButtonActivated() { }


    /**
     * Should be called when the app is shut down.
     * 
     * Cancels {@link #heapService} by calling {@link ScheduledService#cancel()} and
     * {@link #searchRunner} by calling {@link SearchRunner#shutdown(boolean)}.
     */
    public void shutdown() {
        LOGGER.info("Shutting down executors");
        searchRunner.shutdown(true);
        heapService.cancel();
    }
}
