package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.Taquin;
import io.github.vqnxiv.taquin.logger.AbstractFxAppender;
import io.github.vqnxiv.taquin.logger.MainAppender;
import io.github.vqnxiv.taquin.model.Search;
import io.github.vqnxiv.taquin.model.SearchRunner;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


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
            setRestartOnFailure(true);
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
                            LOGGER.warn("{}% heap usage: the app may be slowed down", (int) (warningValue * 100) );
                        }
                    } else {
                        belowWarningValue = true;
                    }
                    return currentHeap;
                }
            };
        }
    }

    
    /**
     * Root logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(MainController.class);

    /**
     * {@link InlineCssTextArea} in which app log events are displayed. {@link MainAppender}
     */
    @FXML 
    private InlineCssTextArea logOutput;

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
     * {@link SearchRunner} which is dependency-injected in the {@link BuilderController}.
     * Closing the app makes this {@link MainController} call {@link SearchRunner#shutdown(boolean)}.
     */
    private final SearchRunner searchRunner;

    /**
     * {@link Map} which is used as a convenient way to link a {@link BuilderController}
     * and its {@link VBox} main node
     */
    private final Map<BuilderController, VBox> builderMap;
    
    
    /**
     * Constructor which initializes {@link #searchRunner} and {@link #heapService}.
     */
    public MainController() {
        searchRunner = new SearchRunner();
        builderMap = new HashMap<>();
        
        heapService = new HeapMonitorService();
        heapService.setPeriod(Duration.millis(100d));
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
        AbstractFxAppender.injectMainController(this);
        searchInfo.textProperty().bind(searchRunner.lastSearchInfo());
        LOGGER.info("Successfully loaded GUI");
        onAddBuilderActivated();
        heapService.start();
    }
    

    /**
     * Getter for {@link #logOutput}.
     * 
     * @return a {@link InlineCssTextArea} in which logs are displayed.
     */
    public InlineCssTextArea getMainLogOutput() {
        return logOutput;
    }

    /**
     * Retrieves the corresponding {@link BuilderController} {@link InlineCssTextArea} 
     * for search logs ({@link io.github.vqnxiv.taquin.logger.SearchAppender})
     * from a {@link Search#getId()}.
     * 
     * @param id The id of the {@link Search}.
     * @return {@link Optional#of(Object)} the {@link InlineCssTextArea} if a {@link Search} 
     * with such an id exists; {@link Optional#empty()} otherwise.
     */
    public Optional<InlineCssTextArea> getBuilderLogOutputFromSearchID(int id) {
        for(var builder : builderMap.keySet()) {
            if(builder.getSearchId() == id) {
                return Optional.of(builder.getLogOutput());
            }
        }
        
        return Optional.empty();
    }
    
    
    /**
     * Creates a new {@link VBox} and adds it to {@link #builderVBox}. 
     * Also adds it and its {@link BuilderController} in {@link #builderMap}.
     */
    @FXML
    private void onAddBuilderActivated() {
        LOGGER.debug("Adding new build controller");
        
        FXMLLoader loader = new FXMLLoader(Taquin.class.getResource("/fxml/primary/builder.fxml"));

        loader.setControllerFactory(
            param -> new BuilderController(searchRunner)
        );

        try {
            VBox vb = loader.load();
            builderVBox.getChildren().add(vb);
            builderMap.put(loader.getController(), vb);
        } catch(IOException e) {
            LOGGER.error(e.getMessage());
        }
    }
    


    public void onAboutButtonActivated() {
        return;
    }

    public void onHelpButtonActivated() { 
        return;
    }

    public void onSettingsButtonActivated() { 
        return;
    }


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
