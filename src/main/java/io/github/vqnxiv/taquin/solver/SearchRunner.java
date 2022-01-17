package io.github.vqnxiv.taquin.solver;


import io.github.vqnxiv.taquin.model.CollectionWrapper;
import io.github.vqnxiv.taquin.model.DataStructure;
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
     * List which contains all created searches.
     */
    private final List<Search> searches;


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
        
        searches = new ArrayList<>();
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
     * Deletes a search from {@link #searches}.
     * 
     * @param s The search to remove.
     * @return {@code true} if the search was successfully deleted; {@code false} otherwise.
     */
    public boolean deleteSearch(Search s) {
        LOGGER.info("Deleting search: {}", s.getName());

        return searches.remove(s);
    }

    /**
     * Creates a {@link Search} and a {@link SearchSpace} instances from the given
     * {@link io.github.vqnxiv.taquin.solver.Search.Builder} and 
     * {@link io.github.vqnxiv.taquin.model.SearchSpace.Builder}.
     * 
     * @param searchBuilder The builder to build a search from.
     * @param spaceBuilder The builder to build a searchspace from.
     * @param queuedBuilder {@link io.github.vqnxiv.taquin.model.CollectionWrapper.Builder} for {@code spaceBuilder}.
     * @param exploredBuilder {@link io.github.vqnxiv.taquin.model.CollectionWrapper.Builder} for {@code spaceBuilder}.
     * @return
     */
    public Optional<Search> createSearchAndSpace(
        Search.Builder<?> searchBuilder, SearchSpace.Builder spaceBuilder,
        // CollectionWrapper.Builder queuedBuilder, CollectionWrapper.Builder exploredBuilder
        DataStructure.Builder queuedBuilder, DataStructure.Builder exploredBuilder
    ) {
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
        
        searches.add(s);
        return Optional.of(s);
    }

    /**
     * Pauses the given {@link Search} if it is running.
     * 
     * @param s The search to pause.
     */
    public void pauseSearch(Search s) {
        LOGGER.info("Pausing search: {}", s.getName());
        
        if(!s.equals(lastRunningSearch)) {
            LOGGER.error("Search is not currently running: {}", s.getName());
            return;
        }

        s.pause();
    }

    /**
     * Stops the given {@link Search} regardless of its state.
     * 
     * @param s The search to stop.
     */
    public void stopSearch(Search s) {
        LOGGER.info("Stopping search: {}", s.getName());

        if(!s.equals(lastRunningSearch)) {
            LOGGER.error("Search is not currently running: {}", s.getName());
            return;
        }
        
        s.stop();
    }

    /**
     * Runs a {@link Search} by creating calling {@link Search#newSearchTask(int, int, boolean, boolean)}
     * and submitting its returned {@link io.github.vqnxiv.taquin.solver.Search.SearchTask} (if present)
     * to {@link #executorService}.
     * 
     * @param s The {@link Search} to create a {@link io.github.vqnxiv.taquin.solver.Search.SearchTask} for.
     * @param iter The number of iterations for the task.
     * @param throttle Throttle delay for the task.
     * @param log Whether to log the task.
     * @param mem Whether to update memory usage on every iteration.
     */
    public void runSearch(Search s, int iter, int throttle, boolean log, boolean mem) {
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
                stopSearch(lastRunningSearch);
            } else {
                pauseSearch(lastRunningSearch);
            }
        }

        LOGGER.info("Shutting down search runner");
        executorService.shutdown();
    }
}
