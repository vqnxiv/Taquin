package io.github.vqnxiv.taquin.solver;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.SearchSpace;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SearchRunner {

    /**
     * Root logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(SearchRunner.class);

    private static final int MAXIMUM_CONCURRENT_SEARCHES = 1;
    
    private final ExecutorService executorService;
    private Search lastRunningSearch;
    
    private final StringProperty lastSearchInfo;    
    private final List<Search> searches;
    
    
    public SearchRunner() {
        LOGGER.debug("Creating search runner");

        executorService = Executors.newFixedThreadPool(
            MAXIMUM_CONCURRENT_SEARCHES, 
            r -> new Thread(r, "Search thread")
        );
        lastSearchInfo = new SimpleStringProperty("");
        
        searches = new ArrayList<>();
    }
    
    
    public ReadOnlyStringProperty lastSearchInfo() {
        return lastSearchInfo;
    }

    
    public void shutdown(boolean stopSearch) {
        if(lastRunningSearch != null && lastRunningSearch.getState() == Search.SearchState.RUNNING) {
            if(stopSearch) {
                LOGGER.info("Stopping running search: " + lastRunningSearch.getName());
                lastRunningSearch.stop();
            } else {
                LOGGER.info("Pausing running search: " + lastRunningSearch.getName());
                lastRunningSearch.pause();
            }
        }
        
        LOGGER.info("Shutting down search runner");
        executorService.shutdown();
    }
    
    
    public boolean deleteSearch(Search s) {
        LOGGER.info("Deleting search: " + s.getName());

        return searches.remove(s);
    }
    
    public Optional<Search> createSearch(Search.Builder<?> searchBuilder, SearchSpace.Builder spaceBuilder) {
        LOGGER.info("Creating search");
        
        LOGGER.debug("Checking grids");
        var m = spaceBuilder.getNamedProperties();
        if(m.get("start").getValue() == null) {
            LOGGER.error("Start grid is null");
            return Optional.empty();
        }
        else if(m.get("end").getValue() == null) {
            LOGGER.error("End grid is null");
            return Optional.empty();
        }

        LOGGER.debug("Checking grids content");
        if(!((Grid) m.get("start").getValue()).checkCompatibility((Grid) m.get("end").getValue())) {
            LOGGER.error("Grids do not share the same alphabet");
            return Optional.empty();
        }
        
        var s = searchBuilder.searchSpace(spaceBuilder.build()).build();
        LOGGER.info("Search successfully created: " + s.getName());
        searches.add(s);
        return Optional.of(s);
    }
    
    public void pauseSearch(Search s) {
        LOGGER.info("Pausing search: " + s.getName());
        
        if(!s.equals(lastRunningSearch)) {
            LOGGER.error("Search is not currently running: " + s.getName());
            return;
        }

        s.pause();
    }
    
    public void stopSearch(Search s) {
        LOGGER.info("Stopping search: " + s.getName());

        if(!s.equals(lastRunningSearch)) {
            LOGGER.error("Search is not currently running: " + s.getName());
            return;
        }
        
        s.stop();
    }
    
    
    // todo: accessible throttle + iterations from builder controller
    public void runSearch(Search s, int n) {
        LOGGER.info("Attempting to run search: " + s.getName());
        
        if(lastRunningSearch == null || lastRunningSearch.getState() != Search.SearchState.RUNNING) {
            lastRunningSearch = s;
        }

        if(!s.equals(lastRunningSearch)) {
            LOGGER.error("A search is already running: " + lastRunningSearch.getName());
            return;
        }
        
        lastSearchInfo.bind(Bindings.concat(s.getName(), ": ", s.getCurrentStateProperty()));
        LOGGER.debug("Submitting search run: " + s.getName() + " (" + n + ")");
        executorService.submit(s.newSearchTask(n, 0));
    }
    
}
