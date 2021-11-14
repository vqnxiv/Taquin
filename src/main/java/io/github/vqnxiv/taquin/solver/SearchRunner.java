package io.github.vqnxiv.taquin.solver;


import javafx.scene.control.Label;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SearchRunner {
    
    private static boolean IS_SEARCH_RUNNER_INITIALISED;
    private static SearchRunner runner;
    
    private int maximumAllowedConcurrentSearches = 1;
    
    private final ExecutorService executorService;
    private Search lastRunningSearch;
    
    
    public String getLastSearchInfoAsString() {
        if(lastRunningSearch == null) {
            return "";
        }
        
        return lastRunningSearch.getName() + ": " + lastRunningSearch.getState();
    }
    
    
    public static SearchRunner createRunner() {
        if(IS_SEARCH_RUNNER_INITIALISED) {
            return runner;
        }
        
        return new SearchRunner();
    }
    
    private SearchRunner() {
        IS_SEARCH_RUNNER_INITIALISED = true;
        runner = this;
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
    
    
    public void runSearch(Search s, int n, Label... toBind) {
        if(lastRunningSearch == null || lastRunningSearch.getState() != Search.State.RUNNING) {
            lastRunningSearch = s;
        }

        if(!s.equals(lastRunningSearch)) {
            return;
        }
        
        var task = s.newSearchTask(n);
        toBind[0].textProperty().bind(task.searchStateProperty());
        toBind[1].textProperty().bind(task.timeProperty());
        toBind[2].textProperty().bind(task.currentKeyProperty());
        toBind[3].textProperty().bind(task.currentDepthProperty());
        toBind[4].textProperty().bind(task.exploredProperty());
        toBind[5].textProperty().bind(task.queuedProperty());
        
        executorService.submit(task);
    }
    
}
