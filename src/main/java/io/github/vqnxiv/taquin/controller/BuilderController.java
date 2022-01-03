package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.model.CollectionWrapper;
import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.SearchSpace;
import io.github.vqnxiv.taquin.solver.Search;
import io.github.vqnxiv.taquin.solver.SearchRunner;
import io.github.vqnxiv.taquin.solver.search.Astar;
import io.github.vqnxiv.taquin.util.FxUtils;
import io.github.vqnxiv.taquin.util.GridViewer;
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

    public enum TabPaneItem {
        COLLECTION, SEARCH_MAIN, SEARCH_EXTRA, LIMITS, MISCELLANEOUS;
        
        @Override
        public String toString() {
            return Utils.screamingSnakeToReadable(this.name());
        }
    }

    private enum Lock {
        NOT_LOCKED, MODIFICATION_LOCKED, FULLY_LOCKED
    }

    
    private static final Logger LOGGER = LogManager.getLogger();


    @FXML
    private HBox hbox1;
    
    @FXML
    private HBox hbox2;
    
    @FXML
    private TabPane parameterTabPane;

    @FXML
    private GridPane progressPane;

    
    // ------
    
    private final ObjectProperty<Lock> lockLevel;
    
    
    private final SearchRunner searchRunner;
    private Search search;
    private SearchSpace space;
    
    private Search.Builder<?> searchBuilder;
    private final SearchSpace.Builder spaceBuilder;
    private final CollectionWrapper.Builder exploredBuilder;
    private final CollectionWrapper.Builder queuedBuilder;
    
    // todo: map/local?
    private final GridViewer startViewer, endViewer; //, currentViewer;
    
    
    // ------
    
    public BuilderController() {
        LOGGER.info("Creating builder controller");
        
        lockLevel = new SimpleObjectProperty<>(Lock.NOT_LOCKED);
        lockLevel.set(Lock.NOT_LOCKED);
        
        searchBuilder   = new Astar.Builder(null);
        spaceBuilder    = new SearchSpace.Builder();
        exploredBuilder = new CollectionWrapper.Builder("explored", LinkedHashSet.class);
        queuedBuilder   = new CollectionWrapper.Builder("queued", PriorityQueue.class);
        
        searchRunner = SearchRunner.getRunner();
        
        startViewer = new GridViewer("Start", true);
        endViewer = new GridViewer("End", true);
        //currentViewer = new GridViewer("Current", true);
    }

    @FXML public void initialize() {
        LOGGER.debug("Initializing builder controller");
        
        setupBase();
        setupProgressPane();
        setupTabPane();
    }
    
    
    // BASE
    
    // todo: hbox1 & hbox2 local here
    private void setupBase() {
        LOGGER.debug("Creating base panel");
        
        var m = getNamedMap();
        
        // todo: add search cb
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
        }
    }
    
    private Control handleSpecial(String s) {
        // todo: find a way to get the steps numbers
        // todo: add ui validation/update as well
        // String[] str = new String[]{ "run", "pause", "stop", "steps", "steps number" };
        return switch(s.toLowerCase()) {
            case "search" -> {
                var cb = createClassChoiceBox(new SimpleObjectProperty<>(searchBuilder.getClass().getDeclaringClass()));
                cb.setOnAction(event -> onSearchAlgActivated(cb));
                yield cb;
            }
            case "run" ->
                createRunnerButton(s, false, 
                    event ->  {
                        if(canAttemptRun()) searchRunner.runSearch(search, 0);
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
                        if(canAttemptRun()) searchRunner.runSearch(search, 1);
                    }
                );
            case "steps number" -> {
                var tf = new TextField();
                tf.setMaxWidth(85.0);
                yield tf;
            }
            default -> new Label(s);
        };
    }
    
    private Button createRunnerButton(String s, boolean notLockedDisabled, EventHandler<ActionEvent> v) {
        LOGGER.trace("Creating button for " + s);
        Button btn = new Button(s);
        btn.setOnAction(v);
        // todo
        if(notLockedDisabled) {
            btn.disableProperty().bind(lockLevel.isNotEqualTo(Lock.MODIFICATION_LOCKED));
        }
        else {
            btn.disableProperty().bind(lockLevel.isEqualTo(Lock.FULLY_LOCKED));
        }
        
        return btn;
    }
    
    private boolean canAttemptRun() {
        LOGGER.info("Checking if search can run");
        if(lockLevel.get() == Lock.MODIFICATION_LOCKED) {
            return true;
        }

        LOGGER.warn("Search is not initialized");
        var opt = searchRunner.createSearch(searchBuilder, 
            spaceBuilder.explored(exploredBuilder.build()).queued(queuedBuilder.build())
        );
        
        if(opt.isPresent()) {
            search = opt.get();
            LOGGER.info("Locking controller for modifications");
            lockLevel.set(Lock.MODIFICATION_LOCKED);
            bindProgressPane();
            return true;
        }
        
        LOGGER.error("Cannot run search: search could not be created");
        return false;
    }

    private void onSearchAlgActivated(ChoiceBox<Class<?>> cb) {

        // converts the builder eg BreadthFirst.Builder -> DepthFirst.Builder
        var c = cb.getValue().getDeclaredClasses()[0];

        try {
            searchBuilder = (Search.Builder<?>) c.getDeclaredConstructors()[0].newInstance(searchBuilder);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            LOGGER.error("Could not create builder " + e.getMessage());
        }

        /*
        setSearchParams();
        setQueuedClasses();

        if(searchBuilder.isHeuristicRequired()) {
            heuristicCB.getItems().remove(Grid.Distance.NONE);
        }
        else if(!heuristicCB.getItems().contains(Grid.Distance.NONE)) {
            heuristicCB.getItems().add(Grid.Distance.NONE);
        }
        */
    }
    
    
    // PROGRESS
    
    private void setupProgressPane() {
        LOGGER.debug("Creating progress panel");
        var v = Search.SearchProperty.values();
    }
    
    private void bindProgressPane() {
        LOGGER.debug("Binding progress panel to search");
        var props = search.getProperties();
        var values = Search.SearchProperty.values();
        
        for(int i = 0; i < values.length; i++) {
            if(props.get(values[i]) == null) {
                continue;
            }
            
            var l = new Label("");
            l.textProperty().bind(props.get(values[i]));
            progressPane.add(l, i , 1);
        }
    }
    
    // PARAMETERS

    private void setupTabPane() {
        LOGGER.debug("Creating parameter panem");

        var m = getPropertyMap();

        for(var p : TabPaneItem.values()) {
            var tab = createTab(p, m.get(p));
            parameterTabPane.getTabs().add(tab);
        }
    }
    
    private EnumMap<TabPaneItem, List<Property<?>>> getPropertyMap() {
        LOGGER.debug("Fetching batch properties");

        var map = new EnumMap<TabPaneItem, List<Property<?>>>(TabPaneItem.class);
        
        for(var p : TabPaneItem.values()) {
            map.put(p, new ArrayList<>());
        }
        
        for(var b : new IBuilder[]{ exploredBuilder, queuedBuilder, spaceBuilder, searchBuilder }) {
            for(var e : b.getBatchProperties().entrySet()) {
                map.get(e.getKey()).addAll(e.getValue());
            }
        }
        
        return map;
    }

    private Tab createTab(TabPaneItem p, List<Property<?>> items) {
        LOGGER.debug("Creating parameter tab: " + p.toString());

        Tab tab = new Tab();
        tab.setText(p.toString());

        var gridpane = createGridPane(items.size());

        for(var prop : items) {
            int i = items.indexOf(prop);
            gridpane.add(new Label(prop.getName()), i, 0);
            gridpane.add(controlFromProperty(prop), i, 1);
        }

        tab.setContent(gridpane);

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
        if(Collection.class.isAssignableFrom(p.getValue())) {
            cb.setItems(FXCollections.observableArrayList(CollectionWrapper.getAcceptedSubClasses()));
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
        
        /*
        var g = new GridViewer(p.getName(), false);
        p.bindBidirectional(g.gridProperty());
        
        b.setOnAction(event -> g.show());
        g.readOnlyProperty().bind(lockLevel.isNotEqualTo(Lock.NOT_LOCKED));
        */
        return b;
    }
}
