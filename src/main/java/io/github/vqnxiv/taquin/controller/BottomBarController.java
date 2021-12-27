package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.solver.SearchRunner;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BottomBarController {

    private static final Logger LOGGER = LogManager.getLogger();

    @FXML private Label heapUsage;
    @FXML private Label searchInfo;
    
    private final ScheduledExecutorService execHeap;
    private final MemoryMXBean memoryMXBean;
    private final long maxHeap;
    private final SearchRunner searchRunner;
    
    private boolean belowWarningValue = true;
    private double warningValue = 0.9d;
    
    
    public BottomBarController() {
        LOGGER.info("Creating bottom bar");

        searchRunner = SearchRunner.getRunner();
        
        // todo: thread naming
        execHeap = Executors.newScheduledThreadPool(1);
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        maxHeap = memoryMXBean.getHeapMemoryUsage().getMax() / 1048576;
    }

    @FXML
    public void initialize() {
        LOGGER.trace("Initializing bottom bar");
        // todo: faster heap update
        execHeap.scheduleAtFixedRate(this::monitorHeap, 0, 50, TimeUnit.MILLISECONDS);
        searchInfo.textProperty().bind(searchRunner.lastSearchInfo());
    }
    
    
    private void monitorHeap() {
        var cH = memoryMXBean.getHeapMemoryUsage().getUsed() / 1048576;
        if((double) cH / maxHeap >= warningValue) {
            if(belowWarningValue) {
                belowWarningValue = false;
                LOGGER.warn((int) (warningValue * 100) +  "% heap usage: the app may be slowed down");
            }
        }
        else {
            belowWarningValue = true;
        }

        Platform.runLater(() -> heapUsage.setText(String.format("Heap: %4d / %4d MB", cH, maxHeap)));
    }

    public void shutdown() {
        LOGGER.info("Shutting down bottom bar");

        execHeap.shutdown();
    }
}
