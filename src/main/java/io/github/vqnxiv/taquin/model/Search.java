package io.github.vqnxiv.taquin.model;


import io.github.vqnxiv.taquin.util.IBuilder;
import io.github.vqnxiv.taquin.util.Utils;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.openjdk.jol.info.GraphLayout;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToLongFunction;


/**
 * The main solver class. It represents a search through the graph of states 
 * following an algorithm which is in a subclass.
 * <p>
 * This abstract class contains the utilities needed to apply a search algorithm
 * to a {@link SearchSpace} which is injected by calling {@link #setSearchSpace(SearchSpace)}.
 * <p>
 * It also has some {@link StringProperty} ({@link #properties}) to monitor
 * the progress of the search.
 */
public abstract class Search {

    /**
     * Builder class which implements {@link IBuilder}. Class extending {@link Search}
     * should have a {@link Builder} class which extends this one.
     * 
     * @param <B> Subclass extending this one.
     */
    public abstract static class Builder<B extends Builder<B>> implements IBuilder {

        protected final StringProperty name;
        protected final ObjectProperty<Grid.Distance> heuristic;
        
        protected final ObjectProperty<Grid.EqualPolicy> equalPolicy;
        protected final BooleanProperty filterExplored;
        protected final BooleanProperty filterQueued;
        protected final BooleanProperty linkExisting;
        
        protected final IntegerProperty maxTime;
        protected final IntegerProperty maxDepth;
        protected final IntegerProperty maxGenerated;
        protected final IntegerProperty maxExplored;
        protected final IntegerProperty maxMemory;
        
        
        protected final BooleanProperty monitorMemory;
        protected final BooleanProperty log;
        protected final IntegerProperty throttle;

        /**
         * Base no args constructor.
         */
        protected Builder() {
            equalPolicy = new SimpleObjectProperty<>(this, "equal policy", Grid.EqualPolicy.NONE);
            heuristic = new SimpleObjectProperty<>(this, "heuristic", Grid.Distance.NONE);
            filterExplored = new SimpleBooleanProperty(this, "filter explored", true);
            filterQueued = new SimpleBooleanProperty(this, "filter queued", true);
            linkExisting = new SimpleBooleanProperty(this, "link already existing", false);
            name = new SimpleStringProperty(this, "name", "");
            monitorMemory = new SimpleBooleanProperty(this, "monitor memory", false);
            log = new SimpleBooleanProperty(this, "log search", true);
            throttle = new SimpleIntegerProperty(this, "throttle", 0);
            maxExplored = new SimpleIntegerProperty(this, "Maximum explored", 0);
            maxTime = new SimpleIntegerProperty(this, "Maximum time", 0);
            maxDepth = new SimpleIntegerProperty(this, "Maximum depth", 0);
            maxGenerated = new SimpleIntegerProperty(this, "Maximum generated", 0);
            maxMemory = new SimpleIntegerProperty(this, "Maximum memory", 0);
        }

        /**
         * Copy constructor. Used when converting from a subclass to another.
         * 
         * @param toCopy The builder to copy.
         */
        protected Builder(Builder<?> toCopy) {
            heuristic = toCopy.heuristic;
            equalPolicy = toCopy.equalPolicy;
            filterExplored = toCopy.filterExplored;
            filterQueued = toCopy.filterQueued;
            linkExisting = toCopy.linkExisting;
            name = toCopy.name;
            monitorMemory = toCopy.monitorMemory;
            log = toCopy.log;
            throttle = toCopy.throttle;
            maxExplored = toCopy.maxExplored;
            maxTime = toCopy.maxTime;
            maxDepth = toCopy.maxDepth;
            maxGenerated = toCopy.maxGenerated;
            maxMemory = toCopy.maxMemory;
        }

        /**
         * Getter for the main {@link Property}s.
         * 
         * @return {@link Map} of {@link #name}, {@link #heuristic}.
         */
        @Override
        public Map<String, Property<?>> getNamedProperties() {
            return Map.of(
                name.getName(), name,
                heuristic.getName(), heuristic
            );
        }

        /**
         * Getter for the batch {@link Property}s.
         * 
         * @return {@link Map} of 
         * <ul>
         * <li>{@link #filterExplored}, {@link #filterQueued}, {@link #linkExisting},
         * {@link #equalPolicy}
         * </li>
         * <li> {@link #monitorMemory} </li>
         * </ul>
         */
        @Override
        public EnumMap<Category, List<Property<?>>> getBatchProperties() {
            return new EnumMap<>(Map.of(
                IBuilder.Category.SEARCH_MAIN, List.of(filterExplored, filterQueued, linkExisting, equalPolicy),
                IBuilder.Category.LIMITS, List.of(maxTime, maxDepth, maxExplored, maxGenerated, maxMemory),
                IBuilder.Category.MISCELLANEOUS, List.of(monitorMemory, log, throttle)
            ));
        }

        /**
         * Method which should return {@code true} if the search algorithm
         * of the subclass is an informed search (and so requires an heuristic).
         * {@code false} otherwise.
         * 
         * @return {@code true} if the {@link Search} requires an heuristic.
         */
        public abstract boolean isHeuristicRequired();

        /**
         * Abstract method used to chain setters calls. Concrete extending classes
         * should override this by doing {@code return this;}.
         *
         * @return This {@link Builder} instance.
         */
        protected abstract B self();

        /**
         * Build method which creates the {@link Search} object. Concrete extending classes
         * should return an instance of the {@link Search} subclass they're building for.
         * 
         * @return An instance of a subclass of {@link Search}.
         */
        protected abstract Search build();
    }


    /**
     * This class extends {@link Task} which extends Callable and is
     * responsible for advancing through a search.
     * <p>
     * It should be created through the factory method {@link #newSearchTask(int, int, boolean, boolean)}
     * and then passed on to an {@code ExecutorService} or similar so it can be executed async.
     * <p>
     * It also updates {@link #properties} with the {@link Search}'s progress.
     * 
     * @param <S> {@link SearchState}
     */
    
    class SearchTask<S> extends Task<S> {

        /**
         * {@link Map} of {@link AtomicReference} which are used to throttle updates
         * to {@link #properties}. The map contains a {@link SearchProperty} key
         * for each {@link StringProperty} that should be updated.
         */
        private final EnumMap<SearchProperty, AtomicReference<String>> atomicReferences;

        /**
         * Method which updates {@link #properties} based on {@link #atomicReferences}
         * by making {@link Platform#runLater(Runnable)} calls if an {@link AtomicReference}
         * is {@code null}, which means either that the previous call has completed or that
         * no call has been done yet.
         */
        private void updateAll() {
            for(var e : atomicReferences.entrySet()) {
                var k = e.getKey(); 
                var v = e.getValue();
                if(v.getAndSet(k.calcToString(Search.this)) == null) {
                    Platform.runLater(() -> {
                        final String s = v.getAndSet(null);
                        properties.get(k).set(s);
                    });
                }
            }
        }

        /**
         * Throttle the search by calling {@link Thread#sleep(long)}. Pauses the timer
         * represented by {@link #startTime} and {@link #elapsedTime} before the call
         * and starts it again once it is completed. The method does nothing if
         * {@link #throttle} is equal or inferior to {@code 0}.
         * <p>
         * If an {@link InterruptedException}, the search is stopped by calling
         * {@link Search#stop()}.
         */
        private void throttle() {
            if(throttle <= 0) {
                return;
            }
            
            elapsedTime += System.nanoTime() - startTime;
            try {
                Thread.sleep(throttle);
            } catch(InterruptedException e) {
                Search.this.stop();
            }
            startTime = System.nanoTime();
        }

        /**
         * Checks whether {@link #searchSpace}'s goal is queued if {@link #currentSearchState}
         * is not equal to {@link SearchState#PAUSED} or {@link SearchState#ENDED_SUCCESS}.
         */
        private void checkIfEndWasQueued() {
            if(currentSearchState != SearchState.PAUSED 
                && currentSearchState != SearchState.ENDED_SUCCESS
                && currentSearchState != SearchState.ENDED_FAILURE_EMPTY_SPACE) {
                int n;
                if((n = searchSpace.getQueued().dsIndexOf(searchSpace.getGoal())) > -1) {
                    log("End queued at index " + n);
                }
                else {
                    log("End not queued");
                }
            }
        }
        

        /**
         * The number of iterations (i.e {@link #step()} calls) for this task.
         * 0 means until {@link #checkConditions()} returns {@code false}.
         */
        private final int iterations;

        /**
         * The duration for which the thread should sleep in {@link #throttle()}.
         */
        private final int throttle;


        /**
         * Constructor which should only be called from {@link #newSearchTask(int, int, boolean, boolean)}
         * to ensure validation is done.
         * 
         * @param iter The value for {@link #iterations}.
         * @param thr The value for {@link #throttle}.
         * @param log Whether to log the search.
         * @param mem Whether to include memory updates ({@link SearchProperty#EXPLORED_MEMORY}, 
         * {@link SearchProperty#QUEUED_MEMORY}) in {@link #updateAll()} calls in every iteration.
         */
        private SearchTask(int iter, int thr, boolean log, boolean mem) {
            iterations = iter;
            throttle = thr;
            Search.this.log = log;

            atomicReferences = new EnumMap<>(SearchProperty.class);

            for(var k : properties.keySet()) {
                atomicReferences.put(k, new AtomicReference<>());
            }
            
            if(!mem) {
                atomicReferences.remove(SearchProperty.EXPLORED_MEMORY);
                atomicReferences.remove(SearchProperty.QUEUED_MEMORY);
            }
        }

        /**
         * Method which does a search run-through.
         * <p>
         * For {@link #iterations} (or indefinitely if it is {@code 0}),
         * this method calls {@link #step()}, {@link #updateAll()} and {@link #throttle()}
         * as long as {@link #checkConditions()} returns {@code true}. 
         * Calls {@link #checkIfEndWasQueued()} before returning {@link #currentSearchState}.
         * 
         * @return The {@link Search}'s {@link SearchState} in {@link #currentSearchState}.
         */
        @SuppressWarnings("unchecked")
        @Override
        protected S call() {
            
            if(currentSearchState == SearchState.READY || currentSearchState == SearchState.PAUSED) {

                currentSearchState = SearchState.RUNNING;
                startTime = System.nanoTime();
                log("Starting search");
                
                for(int i = 0; i < ((iterations > 0) ? iterations : iterations + 1); i += (iterations > 0) ? 1 : 0) {
                    if(checkConditions()) {
                        step();
                        updateAll();
                        throttle();
                    }
                    else break;
                }

                elapsedTime += System.nanoTime() - startTime;
                if(checkConditions()) {
                    pause();
                    log("Search paused");
                }
                else {
                    log(Search.this.getState().toString());
                }
                
                // on hold until we can async this or figure out why it's so slow.
                // for now it just blocks ui thread at the end it seems?
                // atomicReferences.putIfAbsent(SearchProperty.EXPLORED_MEMORY, new AtomicReference<>());
                // atomicReferences.putIfAbsent(SearchProperty.QUEUED_MEMORY, new AtomicReference<>());
                
                updateAll();
                
                checkIfEndWasQueued();
            }

            return (S) currentSearchState;
        }
    }


    /**
     * Enum which represents the different limits/conditions which can be imposed 
     * on a {@link Search}. The {@link Search} will immediatly end when one of its 
     * limits is reached.
     * <p>
     * Limits are checked in {@link #checkConditions()}.
     */
    public enum SearchLimit {
        
        /*
        (s -> SProperty.ELAPSED_TIME.calc(s) >= s.limitsMap.get(Limit.MAXIMUM_TIME)),
        (s -> SProperty.ELAPSED_TIME.calc(s) >= s.limitsMap.get(this)),
        => illegal self reference
        so we have to use an extra method for the check
        */

        /**
         * The maximum time allowed for this search.
         */
        MAXIMUM_TIME
            (SearchProperty.ELAPSED_TIME::calc),
        /**
         * The maximum memory allowed for this search (i.e the maximum memory used
         * when storing explored and queued states).
         */
        MAXIMUM_MEMORY
            (s -> SearchProperty.EXPLORED_MEMORY.calc(s) + SearchProperty.QUEUED_MEMORY.calc(s)),
        /**
         * The maximum depth allowed for this search.
         */
        MAXIMUM_DEPTH
            (SearchProperty.CURRENT_DEPTH::calc),
        /**
         * The maximum number of states this search is allowed to explore.
         */
        MAXIMUM_EXPLORED_STATES
            (SearchProperty.EXPLORED_SIZE::calc),
        /**
         * The maximum number of states this search is allowed to generate
         * (i.e the number of explored states + the number of queued states).
         */
        MAXIMUM_GENERATED_STATES
            (s -> SearchProperty.EXPLORED_SIZE.calc(s) + SearchProperty.QUEUED_SIZE.calc(s))
        ;


        /**
         * Function which calculates the current value of a limit and is called 
         * in {@link #check(Search)}.
         */
        private final ToLongFunction<Search> function;


        /**
         * Constructor.
         * 
         * @param func The value for {@link #function}.
         */
        SearchLimit(ToLongFunction<Search> func) {
            function = func;
        }

        
        /**
         * Method which checks if a limit has been reached by calling {@link #function}
         * and comparing its value against the corresponding {@link Search#limitsMap}
         * value (not null safe).
         * 
         * @param s The {@link Search} to check.
         * @return {@code true} if this limit has been reached, {@code false} otherwise.
         */
        private boolean check(Search s) {
            return function.applyAsLong(s) >= s.limitsMap.get(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }

    /**
     * This enum represents the state of a search.
     */
    public enum SearchState {
        /**
         * The search is not ready to be run. Some conditions might not have been filled,
         * e.g the {@link SearchSpace} has not been injected yet.
         */
        NOT_READY (0L),
        /**
         * The search can run and has yet to run.
         */
        READY (1L),
        /**
         * The search is currently running, i.e an instance of {@link SearchTask} is being executed.
         */
        RUNNING (2L),
        /**
         * The search was running but received the instruction to pause. A new {@link SearchTask}
         * can be created and executed to resume the search where it was left off.
         */
        PAUSED (3L),
        /**
         * The search ended successfully (meaning the goal state was reached) and can no longer be run. 
         * No additional {@link SearchState} can be created for this instance of {@link Search}.
         */
        ENDED_SUCCESS (4L) { 
            @Override 
            public String toString() { 
                return "successfully ended"; 
            } 
        },
        /**
         * The search ended for a reason that is neither reaching the goal state or a limit nor
         * exploring the entire {@link SearchSpace}. As this state is only accessible from
         * outside from this class from calling {@link #stop()}, it is very likely user input
         * or closing down the entire app. No additional {@link SearchState} can be created 
         * for this instance of {@link Search}.
         */
        ENDED_FAILURE_USER_FORCED (5L) { 
            @Override 
            public String toString() { 
                return "forcefully ended"; 
            } 
        },
        /**
         * The search ended because it explored the entire search space; meaning {@link SearchSpace#getQueued()}
         * returns an empty collection and calling {@link SearchSpace#getNewNeighbors(boolean, boolean, boolean)}
         * while filtering both queued and explored states returns an empty collection as well.
         * No additional {@link SearchState} can be created for this instance of {@link Search}.
         * <p>
         * Do note that this state is automatically set when the above conditions are met; it does not guard
         * against a search algorithm which does not filter out explored or queued states. It is on the 
         * algorithm's implementation to take that in consideration.
         */
        ENDED_FAILURE_EMPTY_SPACE (6L) { 
            @Override 
            public String toString() { 
                return "empty space search";
            } 
        },
        /**
         * The search ended because it reached a limit. No additional {@link SearchState} can 
         * be created for this instance of {@link Search}.
         */
        ENDED_FAILURE_LIMIT (7L) { 
            @Override 
            public String toString() { 
                return "reached limit"; 
            } 
        };

        
        /**
         * Dummy value used for {@link SearchProperty#CURRENT_STATE} 
         * and {@link #valueOf(long)}.
         */
        private final long index;

        /**
         * Constructor.
         * 
         * @param l The index for this constant value.
         */
        SearchState(long l) {
            index = l;
        }


        /**
         * Getter for {@link #index}.
         * 
         * @return This constant's {@link #index}.
         */
        private long getIndex() {
            return index;
        }
        
        /**
         * Gets a constant from this enum by its {@link #index}.
         * 
         * @param l The index to find a constant for.
         * @return The constant with the specified index.
         * @throws IllegalArgumentException if no constant is found for the given index.
         */
        static SearchState valueOf(long l) {
            for(var v : values()) {
                if(v.index == l) {
                    return v;
                }
            }
            
            throw new IllegalArgumentException("No constant for " + l);
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }

    /**
     * Method which is used to represent a property (as in a characteristic,
     * not a {@link Property} object) of a search.
     */
    public enum SearchProperty {
        /**
         * The current {@link SearchState} of the state, i.e {@link #currentSearchState}.
         */
        CURRENT_STATE
            (x -> x.currentSearchState.getIndex()),
        /**
         * Total elapsed time while the search is running. See {@link #getElapsedTime()}.
         */
        ELAPSED_TIME
            (x -> x.getElapsedTime() / 1_000_000),
        /**
         * The key of the current grid. See {@link SearchSpace#getCurrent()} and
         * {@link Grid#getKey()}.
         */
        CURRENT_KEY
            (x -> (long) x.searchSpace.getCurrent().getKey()),
        /**
         * The depth of the current grid. See {@link SearchSpace#getCurrent()} and
         * {@link Grid#getDepth()}.
         */
        CURRENT_DEPTH
            (x -> (long) x.searchSpace.getCurrent().getDepth()),
        /**
         * The number of explored states, i.e the size of {@link SearchSpace#getExplored()}.
         */
        EXPLORED_SIZE
            (x -> (long) x.searchSpace.getExplored().size()),
        /**
         * The memory size of {@link SearchSpace#getExplored()}.
         */
        EXPLORED_MEMORY
            (x -> GraphLayout.parseInstance(x.searchSpace.getExplored()).totalSize() / 1048576L),
        /**
         * The number of queued states, i.e the size of {@link SearchSpace#getQueued()}.
         */
        QUEUED_SIZE
            (x -> (long) x.searchSpace.getQueued().size()),
        /**
         * The memory size of {@link SearchSpace#getQueued()}.
         */
        QUEUED_MEMORY
            (x -> GraphLayout.parseInstance(x.searchSpace.getQueued()).totalSize() / 1048576L)
        ;

        
        /**
         * The function which is used to get the value of a property. 
         */
        private final ToLongFunction<Search> function;

        /**
         * Constructor.
         * 
         * @param func Value for {@link #function}.
         */
        SearchProperty(ToLongFunction<Search> func) {
            function = func;
        }

        /**
         * Method which calls {@link #function} on a {@link Search}.
         * 
         * @param s The {@link Search} to check.
         * @return long value of {@link #function}.
         */
        long calc(Search s) {
            return function.applyAsLong(s);
        }

        /**
         * Returns a {@link String} representation of the result of {@link #function}.
         * 
         * @param s The {@link Search} to check.
         * @return {@link String} of the result of {@link #function}.
         */
        String calcToString(Search s) {
            if(this == CURRENT_STATE) {
                return SearchState.valueOf(function.applyAsLong(s)).toString();
            }
            
            return Long.toString(function.applyAsLong(s));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }
    

    /**
     * Search logger.
     */
    private static final Logger LOGGER = LogManager.getLogger("searchLogger");

    /**
     * Unique id for this search. It takes the value return from calling 
     * {@link System#currentTimeMillis()} divided by {@code 1_000} when creating the search.
     */
    private final int id;

    /**
     * Name for this search. If not specified in the {@link Builder}, it is {@link Long#valueOf(String)} of {@link #id}.
     */
    private final String name;

    /**
     * The current {@link SearchState} of this search.
     */
    private SearchState currentSearchState = SearchState.NOT_READY;

    /**
     * The {@link SearchSpace} this search is applied to.
     */
    protected SearchSpace searchSpace;

    /**
     * The {@link Grid.Distance} which is used as heuristic.
     * <p>
     * A fully qualified heuristic is comprised of a primary part {@link Grid.Distance} and a
     * secondary part {@link Grid.EqualPolicy}. As the {@link Grid.EqualPolicy} is optional 
     * (see {@link Grid.EqualPolicy#NONE}), this can be called 'heuristic'.
     */
    protected final Grid.Distance heuristic;

    /**
     * The {@link Grid.EqualPolicy} value used to complement {@link #heuristic}.
     */
    protected final Grid.EqualPolicy equalPolicy;

    /**
     * The {@link Comparator} which compares two grid according to this search heuristic,
     * i.e {@link #heuristic} and {@link #equalPolicy}.
     */
    protected final Comparator<Grid> heuristicComparator = (g1, g2) -> {
        if(g1.getHeuristicValue() == g2.getHeuristicValue()) {
            if(g1.equals(g2)) {
                return  0;
            }

            return Search.this.equalPolicy.calc(g1, g2);
        }

        return Float.compare(g1.getHeuristicValue(), g2.getHeuristicValue());
    };

    /**
     * The opposite of {@link #heuristicComparator}.
     * <p>
     * E.g if {@link #heuristicComparator} returns {@code -1} for {@code compare(g1, g2)},
     * this will return {@code 1} for the exact same {@code compare(g1, g2)} call.
     */
    protected final Comparator<Grid> reverseHeuristicComparator = 
        (g1, g2) -> - heuristicComparator.compare(g1, g2);

    /**
     * Map which holds the value for each of the {@link SearchLimit} for this search.
     */
    private final EnumMap<SearchLimit, Long> limitsMap;

    /**
     * Whether to filter out explored states when calling 
     * {@link SearchSpace#getNewNeighbors(boolean, boolean, boolean)}.
     */
    protected final boolean filterExplored;

    /**
     * Whether to filter out queued states when calling 
     * {@link SearchSpace#getNewNeighbors(boolean, boolean, boolean)}.
     */
    protected final boolean filterQueued;

    /**
     * Whether to link to existing neighbors states when calling 
     * {@link SearchSpace#getNewNeighbors(boolean, boolean, boolean)}.
     */
    protected final boolean linkExistingNeighbors;

    /**
     * The start time at the beggining of {@link SearchTask#call()}. See {@link #getElapsedTime()}.
     */
    private long startTime;

    /**
     * The previously elapsed time. See {@link #getElapsedTime()}.
     */
    private long elapsedTime = 0;

    /**
     * Whether to log this search.
     */
    private boolean log;

    /**
     * Map which holds {@link StringProperty} that represent this search's progress.
     * <p>
     * These properties can be accessed (and thus bound to) by calling {@link #getProperties()}.
     */
    private final EnumMap<SearchProperty, StringProperty> properties;


    /**
     * Constructor.
     * 
     * @param builder {@link Builder} instance from which to retrieve the initial values for this search.
     */
    protected Search(Builder<?> builder) {
        heuristic = builder.heuristic.get();
        equalPolicy = builder.equalPolicy.get();
        
        limitsMap = new EnumMap<>(SearchLimit.class);
        if(builder.maxMemory.get() != 0) {
            limitsMap.put(SearchLimit.MAXIMUM_MEMORY, (long) builder.maxMemory.get());
        }
        if(builder.maxTime.get() != 0) {
            limitsMap.put(SearchLimit.MAXIMUM_TIME, (long) builder.maxTime.get());
        }
        if(builder.maxDepth.get() != 0) {
            limitsMap.put(SearchLimit.MAXIMUM_DEPTH, (long) builder.maxDepth.get());
        }
        if(builder.maxExplored.get() != 0) {
            limitsMap.put(SearchLimit.MAXIMUM_EXPLORED_STATES, (long) builder.maxExplored.get());
        }
        if(builder.maxGenerated.get() != 0) {
            limitsMap.put(SearchLimit.MAXIMUM_GENERATED_STATES, (long) builder.maxGenerated.get());
        }
        
        filterExplored = builder.filterExplored.get();
        filterQueued = builder.filterQueued.get();
        linkExistingNeighbors = builder.linkExisting.get();
        
        id = (int) (System.currentTimeMillis() / 1000);
        
        name = (builder.name.get().equals("")) ? Long.toString(id) : builder.name.get();
        
        properties = createProperties();
    }

    /**
     * Creates an {@link EnumMap} of {@link SearchProperty}, {@link StringProperty}. Only called
     * once in {@link #Search(Builder)} to initialize {@link #properties}.
     * 
     * @return {@link EnumMap} of {@link SearchProperty}, {@link StringProperty}.
     */
    private EnumMap<SearchProperty, StringProperty> createProperties() {

        var s = EnumSet.allOf(SearchProperty.class);
        
        var propsMap = new EnumMap<SearchProperty, StringProperty>(SearchProperty.class);

        for(var s2 : s) {
            propsMap.put(
                s2,
                new SimpleStringProperty(this, s2.toString(), "0")
            );
        }

        return propsMap;
    }


    /**
     * Setter for {@link #searchSpace}. This will also call {@link #setReady()} and the 
     * callback method {@link #setSpaceDependentParameters()}.
     * 
     * @param space The value for {@link #searchSpace}.
     */
    void setSearchSpace(SearchSpace space) {
        searchSpace = space;
        setSpaceDependentParameters();
        setReady();
    }

    /**
     * Getter for this {@link #searchSpace} (not a defensive copy).
     * 
     * @return {@link #searchSpace}.
     */
    SearchSpace getSearchSpace() {
        return searchSpace;
    }

    /**
     * Getter for {@link #name}.
     * 
     * @return {@link #name}.
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for {@link #id}.
     * 
     * @return {@link #id}.
     */
    public int getId() {
        return id;
    }

    /**
     * Getter for {@link #heuristicComparator}.
     * 
     * @return {@link #heuristicComparator}.
     */
    Comparator<Grid> getHeuristicComparator() {
        return heuristicComparator;
    }

    /**
     * Getter for {@link #properties}.
     * 
     * return {@link #properties}.
     */
    Map<SearchProperty, StringProperty> getProperties() {
        return properties;
    }

    /**
     * Getter for a {@link Property} wrapping {@link #currentSearchState}.
     * 
     * @return The property for {@link SearchProperty#CURRENT_STATE} in {@link #properties}.
     */
    final ReadOnlyStringProperty getCurrentStateProperty() {
        return properties.get(SearchProperty.CURRENT_STATE);
    }

    /**
     * Getter for {@link #currentSearchState}.
     * 
     * @return {@link #currentSearchState}.
     */
    protected SearchState getState() { 
        return currentSearchState; 
    }


    /**
     * Sets {@link #currentSearchState} to {@link SearchState#READY}.
     */
    private void setReady() {
        currentSearchState = SearchState.READY;
    }

    /**
     * Calculates the total elapsed time.
     * 
     * @return The total elapsed time for this search.
     */
    private long getElapsedTime() {
        return (currentSearchState == SearchState.RUNNING)
            ? (System.nanoTime() - startTime) + elapsedTime
            : elapsedTime;
    }

    /**
     * Checks all the conditions that determine whether this search is allowed to continue.
     * <p>
     * This checks:
     * <ul>
     *     <li> that it was not paused or forcefully stopped </li>
     *     <li> that it has not reached its goal </li>
     *     <li> that there is still states to explore </li>
     *     <li> that it has not reached a limit. </li>
     * </ul>
     * 
     * @return {@code true} if the search may continue; {@code false} otherwise.
     */
    private boolean checkConditions() {
        log("Checking conditions");

        if(currentSearchState == SearchState.PAUSED || currentSearchState == SearchState.ENDED_FAILURE_USER_FORCED) {
            return false;
        }

        if(searchSpace.isCurrentGoal()) {
            currentSearchState = SearchState.ENDED_SUCCESS;
            return false;
        }

        if(searchSpace.getQueued().isEmpty()) {
            currentSearchState = SearchState.ENDED_FAILURE_EMPTY_SPACE;
            return false;
        }
        
        for(var l : limitsMap.keySet()) {
            if(l.check(this)){
                currentSearchState = SearchState.ENDED_FAILURE_LIMIT;
                return false;
            }
        }
        
        return true;
    }

    /**
     * Shorthand log method.
     * 
     * @param message The message to pass on to {@link #LOGGER}.
     */
    protected void log(String message) {
        if(!log) {
            return;
        }

        LOGGER.info(
            new MarkerManager.Log4jMarker(Integer.toString(id)),
            "{}\t   {}", getElapsedTime(), message
        );
    }


    /**
     * Pauses the search so that it can be resumed later. See {@link SearchState#PAUSED}.
     * <p>
     * Note that it will only signal to the search that it has to pause;
     * it won't pause immediately, but during the next call to {@link #checkConditions()}.
     */
    protected void pause() {
        if(currentSearchState != SearchState.RUNNING) {
            return;
        }

        this.currentSearchState = SearchState.PAUSED;
    }

    /**
     * Completely stops the search. See {@link SearchState#ENDED_FAILURE_USER_FORCED}.
     * <p>
     * Note that it will only signal to the search that it has to stop;
     * it won't pause immediately, but during the next call to {@link #checkConditions()}.
     */
    protected void stop() {
        if(currentSearchState != SearchState.RUNNING && currentSearchState != SearchState.PAUSED) {
            return;
        }
        
        this.currentSearchState = SearchState.ENDED_FAILURE_USER_FORCED;
    }

    /**
     * Factory method for {@link SearchTask}.
     * 
     * @param iterations The value for {@link SearchTask#iterations}.
     * @param throttle The value for {@link SearchTask#throttle}.
     * @param log Whether to log the task.
     * @param memory Whether to update the memory usage through the task, and not just when it ends.
     * @return {@link Optional#of(Object)} the created {@link SearchTask} if {@link #currentSearchState}
     * is either {@link SearchState#READY} or {@link SearchState#PAUSED}, and if {@link #searchSpace}
     * has been injected. Otherwise, {@link Optional#empty()}.
     */
    Optional<SearchTask<SearchState>> newSearchTask(
        int iterations, int throttle, boolean log, boolean memory
    ) {
        if((currentSearchState != SearchState.READY && currentSearchState != SearchState.PAUSED)
            || searchSpace == null ) {
            return Optional.empty();
        }
        return Optional.of(new SearchTask<>(iterations, throttle, log, memory));
    }
    
    
    /**
     * Abstract callback method which is used to set up some additional parameters and fields 
     * which are dependent on {@link #searchSpace}. Called by {@link #setSearchSpace(SearchSpace)}.
     */
    protected abstract void setSpaceDependentParameters();

    /**
     * Abstract method which is used to compute the heuristic of a {@link Grid},
     * with or without the use of {@link #heuristicComparator} or {@link #reverseHeuristicComparator}.
     * It should also call {@link Grid#setHeuristicValue(float)} on the grid passed in arguments with 
     * the result of the calculation.
     * 
     * @param g The {@link Grid} to compute the heuristic value for.
     */
    protected abstract void computeHeuristic(Grid g);

    /**
     * Abstract method which represents <u>one and only one</u> step of an algorithm.
     * <p>
     * This can also be seen as exploring a single state.
     */
    protected abstract void step();


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if(o instanceof Search s) {
            return id == s.id;
        }
        
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return id ^ (id >>> 32);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[" + id + "] " + name + ": " + currentSearchState;
    }
}
