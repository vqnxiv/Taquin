package io.github.vqnxiv.taquin.solver;


import javafx.scene.control.Label;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SearchRunner {
    
    
    private int maximumAllowedConcurrentSearches = 1;
    
    private final ExecutorService executorService;
    private Search lastRunningSearch;
    
    
    public SearchRunner() {
        executorService = Executors.newFixedThreadPool(maximumAllowedConcurrentSearches);
    }

    public void shutdown() {
        if(lastRunningSearch != null && lastRunningSearch.getState() == Search.State.RUNNING)
            lastRunningSearch.stop();

        executorService.shutdown();
    }
    

    public void pauseSearch(Search s) {
        if(!s.equals(lastRunningSearch)) {
            return;
        }

        s.pause();
    }
    
    public void stopSearch(Search s) {
        if(!s.equals(lastRunningSearch)) {
            return;
        }
        
        s.stop();
    }
    
    public void stepsSearch(Search s, Label toBind, int n) {

        if(lastRunningSearch == null || lastRunningSearch.getState() != Search.State.RUNNING) {
            lastRunningSearch = s;
        }

        if(!s.equals(lastRunningSearch)) {
            return;
        }
        
        var steps = s.getSteps(n);
        toBind.textProperty().bind(steps.messageProperty());
        executorService.execute(steps);
    }
    
    public void runSearch(Search s, Label toBind) {

        if(lastRunningSearch == null || lastRunningSearch.getState() != Search.State.RUNNING) {
            lastRunningSearch = s;
        }
        
        if(!s.equals(lastRunningSearch)) {
            return;
        }

        var run = s.getRun();
        toBind.textProperty().bind(run.messageProperty());
        executorService.execute(run);
    }
    
}
