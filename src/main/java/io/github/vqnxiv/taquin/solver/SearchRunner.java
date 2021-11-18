package io.github.vqnxiv.taquin.solver;


import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SearchRunner {

    private static final int MAXIMUM_CONCURRENT_SEARCHES = 1;
    
    private static boolean IS_SEARCH_RUNNER_INITIALIZED;
    private static SearchRunner runner;
    
    private final ExecutorService executorService;
    private Search lastRunningSearch;
    
    private final StringProperty lastSearchInfo;    

    
    public static SearchRunner getRunner() {
        if(IS_SEARCH_RUNNER_INITIALIZED) {
            return runner;
        }
        
        return new SearchRunner();
    }
    
    private SearchRunner() {
        IS_SEARCH_RUNNER_INITIALIZED = true;
        runner = this;
        executorService = Executors.newFixedThreadPool(MAXIMUM_CONCURRENT_SEARCHES);
        lastSearchInfo = new SimpleStringProperty("");
    }
    
    public ReadOnlyStringProperty lastSearchInfo() {
        return lastSearchInfo;
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
    
    
    public void runSearch(Search s, int n) {
        if(lastRunningSearch == null || lastRunningSearch.getState() != Search.State.RUNNING) {
            lastRunningSearch = s;
        }

        if(!s.equals(lastRunningSearch)) {
            return;
        }
        
        lastSearchInfo.bind(Bindings.concat(s.getName(), ": ", s.currentStateProperty()));
        executorService.submit(s.newSearchTask(n));
    }
    
}
