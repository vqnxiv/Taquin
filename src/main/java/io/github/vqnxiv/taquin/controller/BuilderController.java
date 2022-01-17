package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.model.CollectionWrapper;
import io.github.vqnxiv.taquin.model.DataStructure;
import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.SearchSpace;
import io.github.vqnxiv.taquin.model.structure.Sortable;
import io.github.vqnxiv.taquin.model.structure.Sorted;
import io.github.vqnxiv.taquin.model.structure.Unsorted;
import io.github.vqnxiv.taquin.model.structure.jstructure.JLinkedHashSet;
import io.github.vqnxiv.taquin.model.structure.jstructure.JPriorityQueue;
import io.github.vqnxiv.taquin.solver.Search;
import io.github.vqnxiv.taquin.solver.SearchRunner;
import io.github.vqnxiv.taquin.solver.search.Astar;
import io.github.vqnxiv.taquin.util.FxUtils;
import io.github.vqnxiv.taquin.util.IBuilder;
import io.github.vqnxiv.taquin.util.Utils;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class BuilderController {

    private enum Lock {
        NOT_LOCKED, MODIFICATION_LOCKED, FULLY_LOCKED
    }


    /**
     * Root logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(BuilderController.class);


    @FXML
    private HBox hbox1;
    
    @FXML
    private HBox hbox2;
    
    @FXML
    private TabPane parameterTabPane;

    @FXML
    private GridPane progressPane;
    
    @FXML
    private TextArea logOutput;
    
    private ChoiceBox<Grid.Distance> distanceCB;
    private ChoiceBox<Class<?>> queuedClasses;

    private Tab extraTab;
    
    // ------
    
    private final ObjectProperty<Lock> lockLevel;
    
    
    private final SearchRunner searchRunner;
    private Search search;
    private SearchSpace space;
    
    private Search.Builder<?> searchBuilder;
    private final SearchSpace.Builder spaceBuilder;
    // private final CollectionWrapper.Builder exploredBuilder;
    // private final CollectionWrapper.Builder queuedBuilder;
    private final DataStructure.Builder exploredBuilder;
    private final DataStructure.Builder queuedBuilder;
    
    private final Map<String, Control> miscValues;
    
    // todo: map/local?
    private final GridViewer startViewer, endViewer; //, currentViewer;
    
    
    // ------
    
    public BuilderController(SearchRunner searchRunner) {
        LOGGER.debug("Creating builder controller");
        
        lockLevel = new SimpleObjectProperty<>(Lock.NOT_LOCKED);
        lockLevel.set(Lock.NOT_LOCKED);
        
        searchBuilder   = new Astar.Builder();
        spaceBuilder    = new SearchSpace.Builder();
        // exploredBuilder = new CollectionWrapper.Builder("explored", LinkedHashSet.class);
        // queuedBuilder   = new CollectionWrapper.Builder("queued", PriorityQueue.class);
        exploredBuilder = new DataStructure.Builder("explored", JLinkedHashSet.class);
        queuedBuilder = new DataStructure.Builder("queued", JPriorityQueue.class);
        
        this.searchRunner = searchRunner;
        
        startViewer = new GridViewer("Start", true);
        endViewer = new GridViewer("End", true);
        //currentViewer = new GridViewer("Current", true);
        
        miscValues = new HashMap<>();
    }

    @FXML public void initialize() {
        LOGGER.debug("Initializing builder controller");
        
        setupBase();
        setupProgressPane();
        setupTabPane();
    }
    
    
    public TextArea getLogOutput() {
        return logOutput;
    }
    
    public boolean hasSearchWithID(int id) {
        return search != null && search.getID() == id;
    }
    
    
    // BASE
    
    // todo: hbox1 & hbox2 local here
    private void setupBase() {
        LOGGER.debug("Creating base panel");
        
        var m = getNamedMap();
        
        final String[] firstRow = { "name", "explored class", "queued class", "search", "heuristic" };
        final String[] secondRow = { "start", "end", "run", "pause", "stop", "steps", "steps number", "current" };

        LOGGER.debug("Creating first row");
        setHbox(hbox1, firstRow, m);
        LOGGER.debug("Creating second row");
        setHbox(hbox2, secondRow, m);
    }
    
    private Map<String, Property<?>> getNamedMap() {
        LOGGER.debug("Fetching named properties from builders");

        var map = new HashMap<String, Property<?>>();

        for(var b : new IBuilder[]{ exploredBuilder, queuedBuilder, spaceBuilder, searchBuilder }) {
            map.putAll(b.getNamedProperties());
        }

        return map;
    }

    private void setHbox(HBox hbox, String[] ctrls, Map<String, Property<?>> props) {
        for(String s : ctrls) {
            var c = (props.get(s) != null) ?
                controlFromProperty(props.get(s)) : handleSpecial(s);
            hbox.getChildren().add(c);
            
            if(s.equals("steps number")) {
                miscValues.put("steps", c);    
            }
            else if(s.equals("heuristic")) {
                distanceCB = (ChoiceBox<Grid.Distance>) c;
            }
        }
    }
    
    private Control handleSpecial(String s) {
        return switch(s.toLowerCase()) {
            case "search" -> {
                var cb = createClassChoiceBox(new SimpleObjectProperty<>(searchBuilder.getClass().getDeclaringClass()));
                cb.setOnAction(event -> onSearchAlgActivated(cb));
                yield cb;
            }
            case "run" ->
                createRunnerButton(s, false, 
                    event ->  {
                        if(canAttemptRun()) submitRun(false);
                    }
                );
            case "pause" ->
                createRunnerButton(s, true, 
                    event -> searchRunner.pauseSearch(search)
                );
            case "stop" ->
                createRunnerButton(s, true, 
                    event -> {
                        lockLevel.set(Lock.FULLY_LOCKED);
                        searchRunner.stopSearch(search);
                    }
                );
            case "steps" ->
                createRunnerButton(s, false,
                    event ->  {
                        if(canAttemptRun()) submitRun(true);
                    }
                );
            case "steps number" -> {
                var tf = new TextField();
                tf.setMaxWidth(85.0);
                tf.setTextFormatter(new TextFormatter<>(Utils.intStringConverter, 0, Utils.integerFilter));
                yield tf;
            }
            default -> new Label(s);
        };
    }
    
    private Button createRunnerButton(String s, boolean notLockedDisabled, EventHandler<ActionEvent> v) {
        LOGGER.trace("Creating button for " + s);
        Button btn = new Button(s);
        btn.setOnAction(v);
        if(notLockedDisabled) {
            btn.disableProperty().bind(lockLevel.isNotEqualTo(Lock.MODIFICATION_LOCKED));
        }
        else {
            btn.disableProperty().bind(lockLevel.isEqualTo(Lock.FULLY_LOCKED));
        }
        
        return btn;
    }
    
    private boolean canAttemptRun() {
        LOGGER.debug("Checking if search can run");
        if(lockLevel.get() == Lock.MODIFICATION_LOCKED) {
            return true;
        }

        LOGGER.warn("Search is not initialized");
        var opt = searchRunner.createSearchAndSpace(
            searchBuilder, spaceBuilder, queuedBuilder, exploredBuilder
        );
        
        
        if(opt.isPresent()) {
            search = opt.get();
            space = search.getSearchSpace();
            LOGGER.debug("Locking controller for modifications");
            lockLevel.set(Lock.MODIFICATION_LOCKED);
            bindProgressPane();
            return true;
        }
        
        LOGGER.error("Cannot run search: search could not be created");
        return false;
    }
    
    private void submitRun(boolean isSteps) {
        int iter = 0;
        int throttle = 0;
        boolean log = ((CheckBox) miscValues.get("log search")).isSelected();
        boolean memory = ((CheckBox) miscValues.get("monitor memory")).isSelected();
        
        if(isSteps) {
            var i = (Integer) ((TextField) miscValues.get("steps")).getTextFormatter().getValue();
            if(i != null) {
                iter = i;
            }
        }

        var i = (Integer) ((TextField) miscValues.get("throttle")).getTextFormatter().getValue();
        if(i != null) {
            throttle = i;
        }
        
        searchRunner.runSearch(search, iter, throttle, log, memory);
    }

    private void onSearchAlgActivated(ChoiceBox<Class<?>> cb) {

        // converts the builder eg BreadthFirst.Builder -> DepthFirst.Builder
        var c = cb.getValue().getDeclaredClasses()[0];

        try {
            searchBuilder = (Search.Builder<?>) c.getDeclaredConstructor(Search.Builder.class).newInstance(searchBuilder);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            LOGGER.error("Could not create builder " + e.getMessage());
        }

        
        setSearchParams();
        setQueuedClasses();

        if(searchBuilder.isHeuristicRequired()) {
            distanceCB.getItems().remove(Grid.Distance.NONE);
        }
        else if(!distanceCB.getItems().contains(Grid.Distance.NONE)) {
            distanceCB.getItems().add(Grid.Distance.NONE);
        }
        
    }
    
    private void setSearchParams() {
        var l = getPropertyMap().get(IBuilder.Category.SEARCH_EXTRA);

        var gp = createGridPane(l.size());

        for(var prop : l) {
            int i = l.indexOf(prop);
            gp.add(new Label(prop.getName()), i, 0);
            var c = controlFromProperty(prop);
            gp.add(c, i, 1);
        }
        
        extraTab.setContent(gp);
    }
    
    private void setQueuedClasses() {
        Reflections reflections = new Reflections("io.github.vqnxiv.taquin");

        if(searchBuilder.isHeuristicRequired()) {
            var sorted = reflections
                .get(Scanners.SubTypes.of(Sorted.class).asClass())
                .stream()
                .toList();
            
            var sortable = reflections
                .get(Scanners.SubTypes.of(Sortable.class).asClass())
                .stream()
                .toList();
            
            queuedClasses.getItems().clear();
            queuedClasses.getItems().addAll(sorted);
            queuedClasses.getItems().addAll(sortable);
        }
        else {
            queuedClasses.getItems().clear();
            queuedClasses.getItems().addAll(
                reflections
                    .get(Scanners.SubTypes.of(Unsorted.class).asClass())
                    .stream()
                    .toList()
            );
        }
    }
    
    // PROGRESS
    
    // ???
    private void setupProgressPane() {
        LOGGER.debug("Creating progress panel");
        var v = Search.SearchProperty.values();
    }
    
    private void bindProgressPane() {
        LOGGER.debug("Binding progress panel to search");
        var props = search.getProperties();
        var values = Search.SearchProperty.values();
        
        for(int i = 0; i < values.length; i++) {
            var l = new Label("");
            l.textProperty().bind(props.get(values[i]));
            progressPane.add(l, i , 1);
        }
    }
    
    // PARAMETERS

    private void setupTabPane() {
        LOGGER.debug("Creating parameter panem");

        var m = getPropertyMap();

        for(var p : IBuilder.Category.values()) {
            var tab = createTab(p, m.get(p));
            parameterTabPane.getTabs().add(tab);
        }
    }
    
    private EnumMap<IBuilder.Category, List<Property<?>>> getPropertyMap() {
        LOGGER.debug("Fetching batch properties");

        var map = new EnumMap<IBuilder.Category, List<Property<?>>>(IBuilder.Category.class);
        
        for(var p : IBuilder.Category.values()) {
            map.put(p, new ArrayList<>());
        }
        
        for(var b : new IBuilder[]{ exploredBuilder, queuedBuilder, spaceBuilder, searchBuilder }) {
            for(var e : b.getBatchProperties().entrySet()) {
                map.get(e.getKey()).addAll(e.getValue());
            }
        }
        
        return map;
    }

    private Tab createTab(IBuilder.Category p, List<Property<?>> items) {
        LOGGER.debug("Creating parameter tab: " + p.toString());

        Tab tab = new Tab();
        tab.setText(p.toString());

        var gridpane = createGridPane(items.size());

        for(var prop : items) {
            int i = items.indexOf(prop);
            gridpane.add(new Label(prop.getName()), i, 0);
            var c = controlFromProperty(prop);
            gridpane.add(c, i, 1);

            if(p == IBuilder.Category.MISCELLANEOUS) {
                miscValues.put(prop.getName(), c);
            }
        }

        tab.setContent(gridpane);

        if(p.toString().toLowerCase().contains("extra")) {
            extraTab = tab;
        }
        
        return tab;
    }
    
    private GridPane createGridPane(int colNumber) {
        LOGGER.trace("Creating gridPane");
        var gridp = new GridPane();

        gridp.setVgap(2.0d);
        gridp.setPadding(new Insets(5.0, 5.0, 5.0, 5.0));

        gridp.getRowConstraints().addAll(
            FxUtils.getMultipleRowConstraints(2, VPos.CENTER, Priority.SOMETIMES)
        );
        
        gridp.getColumnConstraints().addAll(
            FxUtils.getMultipleColConstraints(colNumber, HPos.CENTER, Priority.SOMETIMES)
        );
        
        return gridp;
    }
    
    
    // CONTROLS
    
    @SuppressWarnings("unchecked")
    private Control controlFromProperty(Property<?> prop) {
        return switch(prop) {
            case BooleanProperty b -> createCheckBox(b);
            case IntegerProperty i -> createIntTF(i);
            case StringProperty s -> createStringTF(s);
            default -> switch(prop.getValue()) {
                // cannot be safely cast if case Property<Enum> in main switch
                case Enum ignored -> createEnumChoiceBox((Property<Enum>) prop);
                case Class c -> createClassChoiceBox((Property<Class<?>>) prop);
                case Grid g -> createGridButton((Property<Grid>) prop);
                default -> throw new IllegalStateException("Unexpected property type: " + prop.getValue().getClass());
            };
        };
    }
    
    private CheckBox createCheckBox(BooleanProperty b) {
        LOGGER.trace("Creating chekbox for " + b.getName());
        
        CheckBox cb = new CheckBox();
        cb.selectedProperty().bindBidirectional(b);
        cb.disableProperty().bind(lockLevel.isNotEqualTo(Lock.NOT_LOCKED));
        cb.disableProperty().addListener(
            event -> cb.selectedProperty().unbind()
        );
        return cb;
    }

    @SuppressWarnings("unchecked")
    private TextField createIntTF(IntegerProperty i) {
        LOGGER.trace("Creating textfield for " + i.getName());
        
        TextField tf = new TextField();
        tf.setMaxWidth(85.0d);
    
        tf.setTextFormatter(new TextFormatter<>(Utils.intStringConverter, 0, Utils.integerFilter));
        ((Property<Number>) tf.getTextFormatter().valueProperty()).bindBidirectional(i);

        tf.disableProperty().bind(lockLevel.isNotEqualTo(Lock.NOT_LOCKED));
        tf.disableProperty().addListener(
            event -> tf.getTextFormatter().valueProperty().unbind()
        );
        return tf;
    }

    private TextField createStringTF(StringProperty s) {
        LOGGER.trace("Creating textfield for " + s.getName());

        TextField tf = new TextField();
        tf.setMaxWidth(85.0d);

        tf.textProperty().bindBidirectional(s);

        tf.disableProperty().bind(lockLevel.isNotEqualTo(Lock.NOT_LOCKED));
        tf.disableProperty().addListener(
            event -> tf.textProperty().unbind()
        );
        
        return tf;
    }

    private <T extends Enum<T>> ChoiceBox<T> createEnumChoiceBox(Property<T> p) {
        LOGGER.trace("Creating choicebox for " + p.getName());

        var cb = new ChoiceBox<T>();
        cb.setMaxWidth(110.0d);
        cb.setPrefWidth(110.0d);

        var o = Utils.staticMethodCallAndCast(p.getValue().getClass(), "values", new ArrayList<T[]>());
        var l = o.map(Arrays::asList).orElseGet(() -> List.of(p.getValue()));
        cb.setItems(FXCollections.observableArrayList(l));

        cb.valueProperty().bindBidirectional(p);

        cb.disableProperty().bind(lockLevel.isNotEqualTo(Lock.NOT_LOCKED));
        cb.disableProperty().addListener(
            event -> cb.valueProperty().unbind()
        );
        
        return cb;
    }
    
    // remove?
    //@SuppressWarnings("unchecked")
    private ChoiceBox<Class<?>> createClassChoiceBox(Property<Class<?>> p) {
        LOGGER.trace("Creating choicebox for " + p.getName());

        var cb = new ChoiceBox<Class<?>>();
        cb.setMaxWidth(110.0d);
        cb.setPrefWidth(110.0d);

        // CollectionWrapper special case until reworked
        // TODO: setQueuedClasses
        // todo: setExploredClasses
        if(Collection.class.isAssignableFrom(p.getValue())) {
            // cb.setItems(FXCollections.observableArrayList(CollectionWrapper.getAcceptedSubClasses()));
            // cb.setConverter(Utils.clsStringConverter);
            /*
            Reflections reflections = new Reflections("io.github.vqnxiv.taquin");

            cb.setItems(FXCollections.observableList(
                reflections
                    .get(Scanners.SubTypes.of(DataStructure.class).asClass())
                    .stream()
                    .toList()
            ));
            */
            if(p.getName().contains("queued")) {
                queuedClasses = cb;
                setQueuedClasses();
            }
            else {
                Reflections reflections = new Reflections("io.github.vqnxiv.taquin");

                cb.setItems(FXCollections.observableList(
                    reflections
                        .get(Scanners.SubTypes.of(Unsorted.class).asClass())
                        .stream()
                        .toList()
                ));
            }
            
            cb.setConverter(Utils.clsStringConverter);
        }
        else {
            Reflections reflections = new Reflections("io.github.vqnxiv.taquin");
            
            cb.setItems(FXCollections.observableList(
                reflections
                    .get(Scanners.SubTypes.of(Search.class).asClass())
                    .stream()
                    .toList()
            ));
            //cb.setConverter(Utils.clsStringConverter);
            cb.setConverter(Utils.srchClsConv);
            
        }
        
        cb.valueProperty().bindBidirectional(p);
        
        cb.disableProperty().bind(lockLevel.isNotEqualTo(Lock.NOT_LOCKED));
        cb.disableProperty().addListener(
            event -> cb.valueProperty().unbind()
        );
        return cb;
    }
    
    private Button createGridButton(Property<Grid> p) {
        LOGGER.trace("Creating button for " + p.getName());

        var b = new Button(p.getName());
        
        if(p.getName().equalsIgnoreCase("start")) {
            b.setOnAction(e -> startViewer.setShowing(true));
            p.bind(startViewer.gridProperty());
            startViewer.editableProperty().bind(lockLevel.isEqualTo(Lock.NOT_LOCKED));
        }
        else if (p.getName().equalsIgnoreCase("end")){
            b.setOnAction(e -> endViewer.setShowing(true));
            p.bind(endViewer.gridProperty());
            endViewer.editableProperty().bind(lockLevel.isEqualTo(Lock.NOT_LOCKED));
        }
        return b;
    }
}
