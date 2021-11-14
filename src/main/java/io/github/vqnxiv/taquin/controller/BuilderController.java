package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.model.CollectionWrapper;
import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.SearchSpace;
import io.github.vqnxiv.taquin.solver.Search;
import io.github.vqnxiv.taquin.solver.SearchRunner;
import io.github.vqnxiv.taquin.solver.search.Astar;
import io.github.vqnxiv.taquin.util.GridViewer;
import io.github.vqnxiv.taquin.util.Utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.UnaryOperator;


public class BuilderController {

    
    @FXML private Label stateProgressLabel, timeProgressLabel, keyProgressLabel, depthProgressLabel, exploredProgressLabel, queuedProgressLabel;

    @FXML private TextField searchNameTF;
    @FXML private Button startGridButton, endGridButton;
    @FXML private ChoiceBox<Class<?>> queuedClassCB;
    @FXML private ChoiceBox<Class<?>> searchAlgCB;
    @FXML private ChoiceBox<Grid.Distance> heuristicCB;
    
    @FXML private Button runSearchButton, pauseSearchButton, stopSearchButton, stepsSearchButton, currentGridButton;
    @FXML private TextField stepsNumberTF;
    
    @FXML private ChoiceBox<Class<?>> exploredClassCB;
    @FXML private CheckBox increasedSizeCheck;
    @FXML private TextField exploredCapacityTF, queuedCapacityTF;
    
    @FXML private CheckBox filterExploredCheck;
    @FXML private CheckBox filterQueuedCheck;
    @FXML private ChoiceBox<Grid.EqualPolicy> equalPolicyCB;
    @FXML private TextField throttleTF;
    @FXML private CheckBox linkExistingCheck;
    
    @FXML private GridPane searchParameters;
    
    @FXML private TextField maxTimeTF, maxMemoryTF, maxDepthTF, maxExploredTF, maxGeneratedTF;
    @FXML private CheckBox goalQueuedCheck;
    
    
    // ------

    private final StringConverter<Integer> intConv = new StringConverter<>() {
        @Override
        public String toString(Integer object) {
            return (object != null) ? Integer.toString(object) : "0";
        }

        @Override
        public Integer fromString(String string) {
            return (!string.equals("")) ? Integer.parseInt(string) : 0;
        }
    };
    
    private final StringConverter<Class<?>> clsConv = new StringConverter<>() {
        @Override
        public String toString(Class object) {
            return (object != null) ? object.getSimpleName() : "";
        }

        @Override
        public Class<?> fromString(String string) {
            return null;
        }
    };
    
    private final StringConverter<Class<?>> srchClsConv = new StringConverter<>() {
        @Override
        public String toString(Class<?> object) {
            return (object != null) ? Utils.staticMethodReflectionCall(object, "getShortName", String.class) : "";
        }

        @Override
        public Class<?> fromString(String string) {
            return null;
        }
    };
    
    private final UnaryOperator<TextFormatter.Change> integerFilter = change -> {
        String input = change.getText();
        if (input.matches("[0-9]*")) {
            return change;
        }
        return null;
    };
    
    
    // ------

    private final BooleanProperty modificationLocked;
    private final BooleanProperty fullyLocked;

    private final SearchRunner searchRunner;
    private Search search;
    private SearchSpace space;
    
    private Search.Builder<?> searchBuilder;
    private final SearchSpace.Builder spaceBuilder;
    private final CollectionWrapper.Builder exploredBuilder;
    private final CollectionWrapper.Builder queuedBuilder;
    
    private final GridViewer startViewer, endViewer, currentViewer;
    
    
    // ------
    
    static final ObservableList<Class<?>> SEARCH_CLASSES;

    static {
        Reflections reflections = new Reflections("io.github.vqnxiv.taquin");
        
        SEARCH_CLASSES = FXCollections.observableList(
            reflections
                .get(Scanners.SubTypes.of(Search.class).asClass())
                .stream()
                .toList()
        );
    }    
    
    
    // ------
    
    public BuilderController() {
        modificationLocked = new SimpleBooleanProperty(false);
        fullyLocked = new SimpleBooleanProperty(false);
        
        searchBuilder   = new Astar.Builder(null);
        spaceBuilder    = new SearchSpace.Builder();
        exploredBuilder = new CollectionWrapper.Builder(LinkedHashSet.class);
        queuedBuilder   = new CollectionWrapper.Builder(PriorityQueue.class);
        
        searchRunner = SearchRunner.createRunner();
        
        startViewer = new GridViewer("Start", false);
        startViewer.readOnlyProperty().bind(modificationLocked);
        endViewer = new GridViewer("End", false);
        endViewer.readOnlyProperty().bind(modificationLocked);
        currentViewer = new GridViewer("Current", true);
    }

    @FXML public void initialize() {

        searchAlgCB.setItems(SEARCH_CLASSES);
        searchAlgCB.setConverter(srchClsConv);
        searchAlgCB.setValue(searchBuilder.getClass().getDeclaringClass());
        
        heuristicCB.setItems(FXCollections.observableArrayList(Grid.Distance.values()));

        queuedClassCB.setConverter(clsConv);
        queuedClassCB.getItems().addAll(CollectionWrapper.getAcceptedSubClasses());
        
        exploredClassCB.setConverter(clsConv);
        exploredClassCB.getItems().addAll(
            Arrays.stream(CollectionWrapper.getAcceptedSubClasses())
                .filter(c -> !CollectionWrapper.doesClassUseNaturalOrder(c))
                .toList()
        );
        
        initializeLimitsTF();
        
        bindSearchBuilder();
        bindSpaceBuilder();
        bindCWrapperBuilders();
        bindDisableProperties();
    }
    
    @SuppressWarnings("unchecked")
    private void bindSearchBuilder() {
        heuristicCB.valueProperty().bindBidirectional(searchBuilder.heuristic);
        filterExploredCheck.selectedProperty().bindBidirectional(searchBuilder.filterExplored);
        filterQueuedCheck.selectedProperty().bindBidirectional(searchBuilder.filterQueued);
        linkExistingCheck.selectedProperty().bindBidirectional(searchBuilder.linkExplored);
        goalQueuedCheck.selectedProperty().bindBidirectional(searchBuilder.checkForQueuedEnd);
        searchNameTF.textProperty().bindBidirectional(searchBuilder.name);
        
        throttleTF.setTextFormatter(new TextFormatter<>(intConv, 0, integerFilter));
        searchBuilder.throttle.bindBidirectional((Property<Number>) throttleTF.getTextFormatter().valueProperty());
    }
    
    private void bindSpaceBuilder() {
        spaceBuilder.start.bindBidirectional(startViewer.gridProperty());
        spaceBuilder.end.bindBidirectional(endViewer.gridProperty());
        
        equalPolicyCB.setItems(FXCollections.observableArrayList(Grid.EqualPolicy.values()));
        equalPolicyCB.valueProperty().bindBidirectional(spaceBuilder.equalPolicy);
    }

    @SuppressWarnings("unchecked")
    private void bindCWrapperBuilders() {
        exploredClassCB.valueProperty().bindBidirectional(exploredBuilder.subClass);
        queuedClassCB.valueProperty().bindBidirectional(queuedBuilder.subClass);
        increasedSizeCheck.selectedProperty().bindBidirectional(exploredBuilder.initialCapacity);
        increasedSizeCheck.selectedProperty().bindBidirectional(queuedBuilder.initialCapacity);
        
        exploredCapacityTF.setTextFormatter(new TextFormatter<>(intConv, 0, integerFilter));
        queuedCapacityTF.setTextFormatter(new TextFormatter<>(intConv, 0, integerFilter));
        
        exploredBuilder.userInitialCapacity.bindBidirectional(
            (Property<Number>) exploredCapacityTF.getTextFormatter().valueProperty()
        );
        queuedBuilder.userInitialCapacity.bindBidirectional(
                (Property<Number>) queuedCapacityTF.getTextFormatter().valueProperty()
        );
    }
    
    private void bindDisableProperties() {

        for(var n : new Control[]{
            searchNameTF, queuedClassCB, searchAlgCB, heuristicCB,
            exploredClassCB, increasedSizeCheck, exploredCapacityTF, queuedCapacityTF, 
            filterExploredCheck, filterQueuedCheck, equalPolicyCB, throttleTF, linkExistingCheck,
            goalQueuedCheck }) {
            n.disableProperty().bind(modificationLocked);
        }

        for(var n : new Control[]{ runSearchButton, pauseSearchButton, stepsSearchButton, stepsNumberTF }) {
            n.disableProperty().bind(fullyLocked);
        }
        
        for(var n : new Control[]{ pauseSearchButton, stopSearchButton }) {
            n.disableProperty().bind(modificationLocked.not().or(fullyLocked));
        }
        
        currentGridButton.disableProperty().bind(modificationLocked.not());
    }
    
    private void initializeLimitsTF() {
        setupLimTF(maxTimeTF, Search.Limit.MAX_TIME);
        setupLimTF(maxMemoryTF, Search.Limit.MAX_MEMORY);
        setupLimTF(maxDepthTF, Search.Limit.MAX_DEPTH);
        setupLimTF(maxExploredTF, Search.Limit.MAX_EXPLORED);
        setupLimTF(maxGeneratedTF, Search.Limit.MAX_GENERATED);
    }

    private void setupLimTF(TextField tf, Search.Limit l) {
        tf.setTextFormatter(new TextFormatter<String>(integerFilter));
        tf.disableProperty().bind(modificationLocked);
        tf.textProperty().addListener(
                event -> searchBuilder.limit(l, (tf.getText().equals("")) ? 0 : Long.parseLong(tf.getText()))
        );
    }
    
    private void setQueuedClasses() {
        if(searchBuilder.isHeuristicRequired() && !queuedClassCB.getItems().isEmpty()) {
            queuedClassCB.getItems().addAll(
                Arrays
                    .stream(CollectionWrapper.getAcceptedSubClasses())
                    .filter(CollectionWrapper::doesClassUseNaturalOrder)
                    .filter(c -> !queuedClassCB.getItems().contains(c))
                    .toList()
            );
        }
        else {
            queuedClassCB.getItems().removeIf(CollectionWrapper::doesClassUseNaturalOrder);
            if(queuedClassCB.getValue() != null && CollectionWrapper.doesClassUseNaturalOrder(queuedClassCB.getValue())) {
                queuedClassCB.setValue(ArrayDeque.class);
            }
        }
    }
    
    
    // ------
    
    private void setSearchParams() {
        searchParameters.getChildren().clear();
        
        var newParameters = searchBuilder.properties();
        
        if(newParameters.length >= searchParameters.getColumnCount()) {
            for(var cc : searchParameters.getColumnConstraints()) {
                cc.setMaxWidth(600.0 / newParameters.length);
            }

            for(int i = searchParameters.getColumnCount(); i < newParameters.length; i++) {
                var colConst = new ColumnConstraints();
                colConst.setHalignment(HPos.CENTER);
                colConst.setHgrow(Priority.SOMETIMES);
                searchParameters.getColumnConstraints().add(colConst);
            }
        }
        else {
            searchParameters.getColumnConstraints().remove(newParameters.length, searchParameters.getColumnCount());
            for(var cc : searchParameters.getColumnConstraints()) {
                cc.setMaxWidth(600.0 / newParameters.length);
            }
        }
        
        for(int i = 0; i < newParameters.length; i++) {
            createControlsFromPoperty(newParameters[i], i);
        }
    }

    @SuppressWarnings("unchecked")
    private void createControlsFromPoperty(Property<?> p, int index) {
        searchParameters.add(new Label(p.getName()), index, 0);
        
        switch(p) {
            case BooleanProperty b -> {
                var cb = new CheckBox();
                cb.selectedProperty().bindBidirectional(b);
                cb.disableProperty().bind(modificationLocked);
                searchParameters.add(cb, index, 1);
            }
            case IntegerProperty i -> {
                var tf = new TextField(Integer.toString(i.get()));
                tf.setTextFormatter(new TextFormatter<>(intConv, 0, integerFilter));
                ((Property<Number>) tf.getTextFormatter().valueProperty()).bindBidirectional(i);
                tf.setMaxWidth(85);
                tf.disableProperty().bind(modificationLocked);
                searchParameters.add(tf, index, 1);
            }
            default -> {}
        }
    }
    
    @FXML private void onStartGridActivated() {
        startViewer.show();
    }

    @FXML private void onEndGridActivated() {
        endViewer.show();
    }
    
    @FXML private void onSearchAlgActivated() {
        
        // converts the builder eg BreadthFirst.Builder -> DepthFirst.Builder
        var c = searchAlgCB.getValue().getDeclaredClasses()[0];
        
        try {
            searchBuilder = (Search.Builder<?>) c.getDeclaredConstructors()[0].newInstance(searchBuilder);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        
        setSearchParams();
        setQueuedClasses();
        
        if(searchBuilder.isHeuristicRequired()) {
            heuristicCB.getItems().remove(Grid.Distance.NONE);
        }
        else if(!heuristicCB.getItems().contains(Grid.Distance.NONE)) {
            heuristicCB.getItems().add(Grid.Distance.NONE);
        }
    }

    
    // ------

    private boolean verifyGrids() {
        Grid sGrid, eGrid;
        if((sGrid = spaceBuilder.start.getValue()) == null) {
            return false;
        }
        else if((eGrid = spaceBuilder.end.getValue()) == null) {
            return false;
        }
        
        return sGrid.hasSameAlphabet(eGrid);
    }
    
    private void build() {
        space = spaceBuilder.explored(exploredBuilder.build()).queued(queuedBuilder.build()).build();
        search = searchBuilder.searchSpace(space).build();
        
        searchNameTF.textProperty().unbindBidirectional(searchBuilder.name);
        searchNameTF.setText(search.getName());
        modificationLocked.set(true);
        
        currentViewer.gridProperty().bind(space.currentGridProperty);
    }
    
    @FXML private void onRunSearchActivated() {
        if(!modificationLocked.get()) {
            if(!verifyGrids()) {
                return;
            }
            build();
        }
        
        searchRunner.runSearch(search, 0, stateProgressLabel, timeProgressLabel, keyProgressLabel, depthProgressLabel, exploredProgressLabel, queuedProgressLabel);
    }

    @FXML private void onPauseSearchActivated() {
        searchRunner.pauseSearch(search);
    }

    @FXML private void onStopSearchActivated() {
        searchRunner.stopSearch(search);
        fullyLocked.set(true);
    }

    @FXML private void onStepsSearchActivated() {
        if(!modificationLocked.get()) {
            if(!verifyGrids()) {
                return;
            }
            build();
        }
        
        var steps = (stepsNumberTF.getText().equals("")) ? 1 : Integer.parseInt(stepsNumberTF.getText());
        searchRunner.runSearch(search, steps, stateProgressLabel, timeProgressLabel, keyProgressLabel, depthProgressLabel, exploredProgressLabel, queuedProgressLabel);
    }
    
    @FXML private void onCurrentGridActivated() {
        currentViewer.show();
    }
}
