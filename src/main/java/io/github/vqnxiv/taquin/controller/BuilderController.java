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
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class BuilderController {
    

    @FXML private TextField searchNameTF;
    @FXML private Button startGridButton, endGridButton;
    @FXML private ChoiceBox<Class<?>> queuedClassCB;
    @FXML private ChoiceBox<Class<?>> searchAlgCB;
    @FXML private ChoiceBox<Grid.Distance> heuristicCB;
    
    @FXML private Button runSearchButton, pauseSearchButton, stopSearchButton, stepsSearchButton, currentGridButton;
    @FXML private TextField stepsNumberTF;
    
    @FXML private ChoiceBox<Class<?>> exploredClassCB;
    @FXML private CheckBox increasedSizeCheck;
    
    @FXML private GridPane searchParameters;
    private final ChoiceBox<Grid.EqualPolicy> equalPolicyCB;
    
    @FXML private TextField maxTimeTF, maxMemoryTF, maxDepthTF, maxExploredTF, maxGeneratedTF;
    @FXML private CheckBox goalQueuedCheck;

    @FXML private Label progressLabel;
    
    
    // ------

    private final StringConverter<Class<?>> clsConv = new StringConverter<>() {
        @Override
        public String toString(Class object) {
            // remove null check when defaults are done
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
            return (object != null) ? Utils.getStringMethodReturn(object, "getShortName") : "";
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

    // cant run while its not locked (= built)
    private final BooleanProperty modificationLocked;
    private final BooleanProperty fullyLocked;

    private final SearchRunner runner;
    private Search search;
    private SearchSpace space;
    
    private Search.Builder<?> searchBuilder;
    private final SearchSpace.Builder spaceBuilder;
    private final CollectionWrapper.Builder exploredBuilder;
    private final CollectionWrapper.Builder queuedBuilder;

    // todo: limited gridviewers (3)
    
    
    // ------
    
    // false = insert order, true = natural + insert order
    static final HashMap<Boolean, ObservableList<Class<?>>> CWRAPPER_CLASSES;
    static final ObservableList<Class<?>> SEARCH_CLASSES;

    static {
        Map<Boolean, List<Class<?>>> splitClasses =
            Arrays
                .stream(CollectionWrapper.getAcceptedSubClasses())
                .collect(Collectors.partitioningBy(CollectionWrapper::doesClassUseNaturalOrder));

        CWRAPPER_CLASSES = new HashMap<>();
        CWRAPPER_CLASSES.put(true,
            FXCollections.observableList(
                Stream.concat(
                    splitClasses.get(false).stream(),
                    splitClasses.get(true).stream()
                ).collect(Collectors.toList()))
        );
        CWRAPPER_CLASSES.put(false, FXCollections.observableList(splitClasses.get(false)));
        
        Reflections reflections = new Reflections("io.github.vqnxiv.taquin");
        
        SEARCH_CLASSES = FXCollections.observableList(
            reflections
                .get(Scanners.SubTypes.of(Search.class).asClass())
                .stream()
                .toList()
        );
    }    
    
    
    // ------
    
    // todo: defaults

    public BuilderController(SearchRunner runner) {
        modificationLocked = new SimpleBooleanProperty(false);
        fullyLocked = new SimpleBooleanProperty(false);

        
        searchBuilder = new Astar.Builder(null);
        spaceBuilder = new SearchSpace.Builder();
        exploredBuilder = new CollectionWrapper.Builder();
        queuedBuilder = new CollectionWrapper.Builder();
        this.runner = runner;
        
        equalPolicyCB = new ChoiceBox<>();
        equalPolicyCB.setItems(FXCollections.observableArrayList(Grid.EqualPolicy.values()));
        equalPolicyCB.setValue(Grid.EqualPolicy.NEWER_FIRST);
        equalPolicyCB.setOnAction(
            event -> spaceBuilder.equalPolicy(equalPolicyCB.getValue())
        );
        equalPolicyCB.disableProperty().bind(modificationLocked);
    }

    @FXML public void initialize() {
        
        // todo: defaults
        searchAlgCB.setConverter(srchClsConv);
        queuedClassCB.setConverter(clsConv);
        exploredClassCB.setConverter(clsConv);
        
        searchAlgCB.setItems(SEARCH_CLASSES);
        searchAlgCB.setValue(Astar.class);
        setQueuedClasses();
        exploredClassCB.setItems(CWRAPPER_CLASSES.get(false));
        heuristicCB.setItems(FXCollections.observableArrayList(Grid.Distance.values()));
        
        
        heuristicCB.getItems().remove(Grid.Distance.NONE);
        
        exploredClassCB.setValue(LinkedHashSet.class);

        
        initializeLimitsTF();
        
        
        for(var n : new Control[]{
                searchNameTF, queuedClassCB, searchAlgCB, heuristicCB, goalQueuedCheck, exploredClassCB, increasedSizeCheck
        }) {
            n.disableProperty().bind(modificationLocked);
        }
        
        for(var n : new Control[]{runSearchButton, pauseSearchButton, stopSearchButton, stepsSearchButton, stepsNumberTF}) {
            n.disableProperty().bind(fullyLocked);
        }
        
        currentGridButton.disableProperty().bind(modificationLocked.not());
    }
    
    
    // ------
    
    private void setSearchParams() {
        searchParameters.getChildren().clear();

        searchParameters.add(new Label("Equal policy"), 0, 0);
        searchParameters.add(equalPolicyCB, 0, 1);
        
        var c = searchAlgCB.getValue().getDeclaredClasses()[0];

        var newParameters = new ArrayList<>(List.of(c.getDeclaredMethods()));
        newParameters.removeIf(m -> m.getName().equals("self") || m.getName().equals("build"));
        
        // resize up the gridpane
        if(newParameters.size() >= searchParameters.getColumnCount()) {
            for(var cc : searchParameters.getColumnConstraints())
                cc.setMaxWidth(600.0 / (newParameters.size() + 1));
            
            for(int i = searchParameters.getColumnCount(); i < newParameters.size() + 1; i++) {
                var colConst = new ColumnConstraints();
                colConst.setHalignment(HPos.CENTER);
                colConst.setHgrow(Priority.SOMETIMES);
                searchParameters.getColumnConstraints().add(colConst);
            }
        }
        // resize down
        else {
            searchParameters.getColumnConstraints().remove(newParameters.size() + 1, searchParameters.getColumnCount());
            for(var cc : searchParameters.getColumnConstraints())
                cc.setMaxWidth(600.0 / (newParameters.size() + 1));
        }
        
        for(var m : newParameters) {
            addControlFromMethod(m, newParameters.indexOf(m) + 1);
        }
    }
    
    private void addControlFromMethod(Method m, int index) {

        searchParameters.add(
            new Label(Utils.toReadable(m.getName())), 
            index, 0
        );

        if(m.getParameterTypes()[0] == boolean.class) {
            var tmpCheck = new CheckBox();
            tmpCheck.setOnAction(
                event -> {
                    try {
                        m.invoke(searchBuilder, tmpCheck.isSelected());
                    } catch(IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            );
            tmpCheck.disableProperty().bind(modificationLocked);
            
            searchParameters.add(tmpCheck, index, 1);
        }
    }
    
    private void setQueuedClasses() {
        queuedClassCB.setItems(CWRAPPER_CLASSES.get(Utils.getBooleanMethodReturn(searchAlgCB.getValue(), "isHeuristicNeeded")));
        queuedClassCB.setValue(LinkedHashSet.class);
    }

    @FXML private void onSearchNameActivated() {
        searchBuilder.name(searchNameTF.getText());
    }

    @FXML private void onStartGridActivated() {
        var gw = new GridViewer(spaceBuilder, "Start", modificationLocked.get());
    }

    @FXML private void onEndGridActivated() {
        var gw = new GridViewer(spaceBuilder, "End", modificationLocked.get());
    }

    @FXML private void onQueuedClassActivated() {
        if(queuedClassCB.getValue() != null)
            queuedBuilder.subClass(queuedClassCB.getValue());
    }
    
    @FXML private void onSearchAlgActivated() {
        // new class input vs current builder class
        boolean update = Utils.getBooleanMethodReturn(searchAlgCB.getValue(), "isHeuristicNeeded") !=
                Utils.getBooleanMethodReturn(searchBuilder.getClass().getDeclaringClass(), "isHeuristicNeeded");
        
        // converts the builder eg BreadthFirst.Builder -> DepthFirst.Builder
        var c = searchAlgCB.getValue().getDeclaredClasses()[0];
        try {
            searchBuilder = (Search.Builder<?>) c.getDeclaredConstructors()[0].newInstance(searchBuilder);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        
       setSearchParams();
        
        if(update) {
            setQueuedClasses();
            
            if(Utils.getBooleanMethodReturn(searchAlgCB.getValue(), "isHeuristicNeeded")) {
                heuristicCB.getItems().remove(Grid.Distance.NONE);
                if(heuristicCB.getValue() == Grid.Distance.NONE) heuristicCB.setValue(Grid.Distance.MANHATTAN);
            }
            else {
                heuristicCB.getItems().add(Grid.Distance.NONE);
            }
        }
        
    }
    
    @FXML private void onHeuristicActivated() {
        if(heuristicCB.getValue() != null)
            searchBuilder.heuristic(heuristicCB.getValue());
    }

    
    // ------

    private void build() {
        space = spaceBuilder.explored(exploredBuilder.build()).queued(queuedBuilder.build()).build();
        search = searchBuilder.searchSpace(space).build();
        modificationLocked.set(true);
    }
    
    @FXML private void onRunSearchActivated() {
        if(!modificationLocked.get()) {
            build();
        }
        
        if(modificationLocked.get()) {
            runner.runSearch(search, progressLabel);
        }
    }

    @FXML private void onPauseSearchActivated() {
        if(modificationLocked.get()) {
            runner.pauseSearch(search);
        }
    }

    @FXML private void onStopSearchActivated() {
        if(modificationLocked.get()) {
            runner.stopSearch(search);
            fullyLocked.set(true);
        }
    }

    @FXML private void onStepsSearchActivated() {
        if(!modificationLocked.get()) {
            build();
        }
        
        if(modificationLocked.get()) {
            runner.stepsSearch(
                search, progressLabel, 
                (stepsNumberTF.getText().equals("")) ? 1 : Integer.parseInt(stepsNumberTF.getText())
            );
        }
    }
    
    @FXML private void onCurrentGridActivated() {
        var t = new GridViewer(space.getCurrent(), "Current: " + space.getCurrent().getKey(), true);
    }
    
    
    // ------
    
    @FXML private void onIncreasedSizeActivated() {
        exploredBuilder.initialCapacity(increasedSizeCheck.isSelected());
        queuedBuilder.initialCapacity(increasedSizeCheck.isSelected());
    }

    @FXML private void onExploredClassActivated() {
        if(exploredClassCB.getValue() != null)
            exploredBuilder.subClass(exploredClassCB.getValue());
    }

    
    // ------
    
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

    @FXML private void onGoalQueuedActivated() {
        searchBuilder.checkForQueuedEnd(goalQueuedCheck.isSelected());
    }
}

