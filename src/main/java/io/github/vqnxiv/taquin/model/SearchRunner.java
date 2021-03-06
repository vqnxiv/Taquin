package io.github.vqnxiv.taquin.model;


import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Class which manages searches.
 * 
 * @see Search
 * @see io.github.vqnxiv.taquin.controller.BuilderController
 */
public class SearchRunner {

    /**
     * Root logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(SearchRunner.class);

    /**
     * The number of concurrent searches allowed.
     */
    private static final int MAXIMUM_CONCURRENT_SEARCHES = 1;
    
    /**
     * {@link ExecutorService} which executes the {@link Search} async.
     */
    private final ExecutorService executorService;

    /**
     * Last search that was running.
     */
    private Search lastRunningSearch;

    /**
     * String property which can be bound to and contains the name and state of {@link #lastRunningSearch}.
     */
    private final StringProperty lastSearchInfo;

    /**
     * Maps which contains all created searches, where the keys are the searches id.
     */
    private final Map<Integer, Search> searches;


    /**
     * Constructor.
     */
    public SearchRunner() {
        LOGGER.debug("Creating search runner");

        executorService = Executors.newFixedThreadPool(
            MAXIMUM_CONCURRENT_SEARCHES, 
            r -> new Thread(r, "Search thread")
        );
        lastSearchInfo = new SimpleStringProperty("");
        
        searches = new HashMap<>();
    }


    /**
     * Read only getter for {@link #lastSearchInfo}.
     * 
     * @return {@link ReadOnlyStringProperty} of {@link #lastSearchInfo}.
     */
    public ReadOnlyStringProperty lastSearchInfo() {
        return lastSearchInfo;
    }

    /**
     * Getter for the properties of a {@link Search}'s {@link SearchSpace}.
     *
     * @param searchId The id for the {@link Search}.
     * @return {@link Optional#of(Object)} the {@link SearchSpace} if it exists; {@link Optional#empty()} otherwise.
     */
    public Optional<SearchSpace> getSearchSpace(int searchId) {
        var s = searches.get(searchId);

        if(s == null) {
            LOGGER.info("No search with id {}", searchId);
            return Optional.empty();
        }

        return Optional.of(s.getSearchSpace());
    }
    
    /**
     * Getter for the properties of a {@link Search}.
     * 
     * @param searchId The id for the {@link Search}.
     * @return {@link Optional#of(Object)} the map of properties if it exists; {@link Optional#empty()} otherwise.
     */
    public Optional<Map<Search.SearchProperty, StringProperty>> getSearchProgressProperties(int searchId) {
        var s = searches.get(searchId);

        if(s == null) {
            LOGGER.info("No search with id {}", searchId);
            return Optional.empty();
        }
        
        return Optional.of(s.getProperties());
    }
    
    /**
     * Deletes a search from {@link #searches}.
     * 
     * @param searchId The id of the search to remove.
     */
    public void deleteSearch(int searchId) {
        Search s;
        
        if((s = searches.remove(searchId)) != null) {
            LOGGER.info("Deleted search {}", s.getName());
        }
        else {
            LOGGER.info("No search with id {}", searchId);
        }
    }

    /**
     * Creates a {@link Search} and a {@link SearchSpace} instances from the given
     * {@link Search.Builder} and 
     * {@link io.github.vqnxiv.taquin.model.SearchSpace.Builder}.
     * 
     * @param searchBuilder The builder to build a search from.
     * @param spaceBuilder The builder to build a searchspace from.
     * @param queuedBuilder {@link DataStructure.Builder} for {@code spaceBuilder}.
     * @param exploredBuilder {{@link DataStructure.Builder} for {@code spaceBuilder}.
     * @return {@link OptionalInt#of} the id of the created search if it was successfully created. 
     */
    public OptionalInt createSearchAndSpace(
        Search.Builder<?> searchBuilder, SearchSpace.Builder spaceBuilder,
        DataStructure.Builder queuedBuilder, DataStructure.Builder exploredBuilder
    ) {
        LOGGER.info("Creating search");
        
        LOGGER.debug("Checking grids");
        var m = spaceBuilder.getNamedProperties();
        if(m.get("start").getValue() == null) {
            LOGGER.error("Start grid is null");
            return OptionalInt.empty();
        }
        else if(m.get("end").getValue() == null) {
            LOGGER.error("End grid is null");
            return OptionalInt.empty();
        }

        LOGGER.debug("Checking grids content");

        if(!((Grid) m.get("start").getValue()).checkCompatibility((Grid) m.get("end").getValue())) {
            return OptionalInt.empty();
        }
        
        var s = searchBuilder.build();
        LOGGER.info("Search successfully created: {}", s.getName());
        
        exploredBuilder.comparator(s.getHeuristicComparator());
        queuedBuilder.comparator(s.getHeuristicComparator());
        
        s.setSearchSpace(
            spaceBuilder
                .queued(queuedBuilder)
                .explored(exploredBuilder)
                .build()
        );
        
        searches.put(s.getId(), s);
        return OptionalInt.of(s.getId());
    }

    /**
     * Pauses the given {@link Search} if it is running.
     * 
     * @param searchId The id of the search to pause.
     */
    public void pauseSearch(int searchId) {
        var s = searches.get(searchId);
        
        if(s == null) {
            LOGGER.info("No search with id {}", searchId);
            return;
        }
        
        if(s.getState() != Search.SearchState.RUNNING) {
            LOGGER.error("Search is not currently running: {}", s.getName());
            return;
        }
        
        LOGGER.info("Pausing search: {}", s.getName());
        s.pause();
    }

    /**
     * Stops the given {@link Search} regardless of its state.
     * 
     * @param searchId The id of the search to stop.
     */
    public void stopSearch(int searchId) {
        var s = searches.get(searchId);

        if(s == null) {
            LOGGER.info("No search with id {}", searchId);
            return;
        }

        LOGGER.info("Stopping search: {}", s.getName());
        s.stop();
    }

    /**
     * Runs a {@link Search} by creating calling {@link Search#newSearchTask(int, int, boolean, boolean)}
     * and submitting its returned {@link Search.SearchTask} (if present)
     * to {@link #executorService}.
     * 
     * @param searchId The if of the {@link Search} to create a 
     * {@link Search.SearchTask} for.
     * @param iter The number of iterations for the task.
     * @param throttle Throttle delay for the task.
     * @param log Whether to log the task.
     * @param mem Whether to update memory usage on every iteration.
     */
    public void runSearch(int searchId, int iter, int throttle, boolean log, boolean mem) {
        var s = searches.get(searchId);

        if(s == null) {
            LOGGER.info("No search with id {}", searchId);
            return;
        }
        
        LOGGER.info("Attempting to run search: {}", s.getName());
        
        if(lastRunningSearch == null || lastRunningSearch.getState() != Search.SearchState.RUNNING) {
            lastRunningSearch = s;
        }

        if(!s.equals(lastRunningSearch)) {
            LOGGER.error("A search is already running: {}", lastRunningSearch.getName());
            return;
        }
        
        lastSearchInfo.bind(Bindings.concat(s.getName(), ": ", s.getCurrentStateProperty()));
        LOGGER.debug("Submitting search run: {}", s.getName());
        
        s.newSearchTask(iter, throttle, log, mem).ifPresent(
            t -> {
                t.setOnSucceeded(
                    e -> LOGGER.info("Search run completed: {}", s.getName())
                );
                executorService.submit(t);
            }
        );
    }
    

    /**
     * Shutdowns this executor.
     *
     * @param stopSearch whether to call {@link Search#pause()} or {@link Search#stop()} 
     * on any running {@link Search}.
     */
    public void shutdown(boolean stopSearch) {
        if(lastRunningSearch != null && lastRunningSearch.getState() == Search.SearchState.RUNNING) {
            if(stopSearch) {
                stopSearch(lastRunningSearch.getId());
            } else {
                pauseSearch(lastRunningSearch.getId());
            }
        }

        LOGGER.info("Shutting down search runner");
        executorService.shutdown();
    }
}
