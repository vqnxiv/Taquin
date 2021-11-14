package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.solver.SearchRunner;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BottomBarController {


    @FXML private Label heapUsage, searchInfo;
    
    private final ScheduledExecutorService execHeap;
    private final MemoryMXBean memoryMXBean;
    private final long maxHeap;
    private final SearchRunner searchRunner;
    
    
    public BottomBarController() {
        searchRunner = SearchRunner.createRunner();
        
        execHeap = Executors.newScheduledThreadPool(1);
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        maxHeap = memoryMXBean.getHeapMemoryUsage().getMax() / 1048576;
    }

    @FXML
    public void initialize() {
        execHeap.scheduleAtFixedRate(updateHeapUsage, 0, 200, TimeUnit.MILLISECONDS);
    }
    
    public void shutdown() {
        execHeap.shutdown();
    }
    
    
    private final Runnable updateHeapUsage = new Runnable() {
        @Override
        public void run() {
            var cH = memoryMXBean.getHeapMemoryUsage().getUsed() / 1048576;
            Platform.runLater(() -> heapUsage.setText(formatHeapLabel(cH)));
            Platform.runLater(() -> searchInfo.setText(searchRunner.getLastSearchInfoAsString()));
        }
    };
    
    private String formatHeapLabel(long current) {
        return String.format("Heap: %4d / %4d MB", current, maxHeap);
    }

}
