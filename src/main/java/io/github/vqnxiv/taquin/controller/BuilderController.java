package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.model.DataStructure;
import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.SearchSpace;
import io.github.vqnxiv.taquin.model.structure.Sortable;
import io.github.vqnxiv.taquin.model.structure.Sorted;
import io.github.vqnxiv.taquin.model.structure.Unsorted;
import io.github.vqnxiv.taquin.model.structure.jstructure.JLinkedHashSet;
import io.github.vqnxiv.taquin.model.structure.jstructure.JPriorityQueue;
import io.github.vqnxiv.taquin.model.Search;
import io.github.vqnxiv.taquin.model.SearchRunner;
import io.github.vqnxiv.taquin.model.search.Astar;
import io.github.vqnxiv.taquin.util.FxUtils;
import io.github.vqnxiv.taquin.model.IBuilder;
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
import org.fxmisc.richtext.InlineCssTextArea;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.InvocationTargetException;
import java.util.*;


/**
 * Class which represents the UI element where the user inputs the details for a {@link Search}.
 */
public class BuilderController {

    /**
     * The different levels of lock for a {@link BuilderController}.
     */
    private enum Lock {
        /**
         * Not locked at all, the {@link IBuilder} can still be modified.
         */
        NOT_LOCKED,
        /**
         * The {@link IBuilder} can no longer be modified, i.e the {@link Search} has been created.
         */
        MODIFICATION_LOCKED,
        /**
         * Completely locked, i.e the {@link Search} has completed its full run.
         */
        FULLY_LOCKED
    }


    /**
     * Root logger.
     */
    private static final Logger LOGGER = LogManager.getLogger(BuilderController.class);
    
    /**
     * 1st part of the main bar.
     */
    @FXML
    private HBox hbox1;

    /**
     * 2nd part of the main bar.
     */
    @FXML
    private HBox hbox2;

    /**
     * TabPane which will contain all that is related to the {@link IBuilder} that doesn't go in
     * {@link #hbox1} and {@link #hbox2}.
     */
    @FXML
    private TabPane parameterTabPane;

    /**
     * GridPane in which {@link Search.SearchProperty} will be displayed.
     */
    @FXML
    private GridPane progressPane;

    /**
     * {@link InlineCssTextArea} in which log messages will be displayed.
     */
    @FXML
    private InlineCssTextArea logOutput;

    /**
     * ChoiceBox with the classes for the heuristic property of {@link #searchBuilder}.
     */
    private ChoiceBox<Grid.Distance> distanceCB;

    /**
     * ChoiceBox with the classes for {@link #queuedBuilder}.
     */
    private ChoiceBox<Class<?>> queuedClasses;

    /**
     * Tab which will contain extra search paramaters.
     */
    private Tab extraTab;


    /**
     * Describes the {@link Lock} state of this {@link BuilderController}.
     */
    private final ObjectProperty<Lock> lockLevel;

    /**
     * {@link SearchRunner} injected from {@link MainController}.
     */
    private final SearchRunner searchRunner;

    /**
     * The id of the search created from {@link #searchBuilder}.
     */
    private int searchId;

    /**
     * {@link Search.Builder} which will be sent to {@link #searchRunner}.
     */
    private Search.Builder<?> searchBuilder;

    /**
     * {@link SearchSpace.Builder} for the {@link Search} from {@link #searchBuilder}.
     */
    private final SearchSpace.Builder spaceBuilder;

    /**
     * {@link DataStructure.Builder} for {@link #spaceBuilder}'s explored property.
     */
    private final DataStructure.Builder exploredBuilder;

    /**
     * {@link DataStructure.Builder} for {@link #spaceBuilder}'s queued property.
     */
    private final DataStructure.Builder queuedBuilder;

    /**
     * Contains misc values used when calling {@link #submitRun(boolean)}.
     */
    private final Map<String, Control> miscValues;

    /**
     * {@link GridViewer} for the start grid of {@link #spaceBuilder}.
     */
    private final GridViewer startViewer;
    
    /**
     * {@link GridViewer} for the end grid of {@link #spaceBuilder}.
     */
    private final GridViewer endViewer;

    /**
     * {@link GridViewer} for the current grid of the {@link SearchSpace} from {@link #spaceBuilder}.
     */
    private final GridViewer currentViewer;


    /**
     * Constructor.
     * 
     * @param searchRunner {@link SearchRunner} injected from {@link MainController}.
     */
    public BuilderController(SearchRunner searchRunner) {
        LOGGER.debug("Creating builder controller");
        
        lockLevel = new SimpleObjectProperty<>(Lock.NOT_LOCKED);
        lockLevel.set(Lock.NOT_LOCKED);
        
        searchBuilder   = new Astar.Builder();
        spaceBuilder    = new SearchSpace.Builder();
        exploredBuilder = new DataStructure.Builder("explored", JLinkedHashSet.class);
        queuedBuilder = new DataStructure.Builder("queued", JPriorityQueue.class);
        
        this.searchRunner = searchRunner;
        
        startViewer = new GridViewer("Start", true);
        endViewer = new GridViewer("End", true);
        currentViewer = new GridViewer("Current", false);
        
        miscValues = new HashMap<>();
    }

    /**
     * JFX method.
     */
    @FXML 
    public void initialize() {
        LOGGER.debug("Initializing builder controller");
        
        setupBase();
        setupTabPane();
        
        var cMenu = new ContextMenu();
        var mItem = new MenuItem("Clear");
        mItem.setOnAction(e -> logOutput.clear());
        cMenu.getItems().add(mItem);
        
        logOutput.setContextMenu(cMenu);
    }


    /**
     * Getter for {@link #logOutput}.
     * 
     * @return {@link #logOutput}.
     */
    InlineCssTextArea getLogOutput() {
        return logOutput;
    }

    /**
     * Getter for {@link #searchId}.
     * 
     * @return {@link #searchId}.
     */
    int getSearchId() {
        return searchId;
    }


    /**
     * Sets up {@link #hbox1} and {@link #hbox2}.
     */
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

    /**
     * Aggregates all the {@link IBuilder#getNamedProperties()} maps from all the {@link IBuilder}.
     * 
     * @return The created {@link Map}.
     */
    private Map<String, Property<?>> getNamedMap() {
        LOGGER.debug("Fetching named properties from builders");

        var map = new HashMap<String, Property<?>>();

        for(var b : new IBuilder[]{ exploredBuilder, queuedBuilder, spaceBuilder, searchBuilder }) {
            map.putAll(b.getNamedProperties());
        }

        return map;
    }

    /**
     * Fills the content of {@link #hbox1} and {@link #hbox2}.
     * 
     * @param hbox Which {@link HBox} to fill.
     * @param ctrls The {@link Control} to create.
     * @param props The {@link Property} to which the controls will be bound.
     */
    @SuppressWarnings("unchecked")
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

    /**
     * Creates controls for everything that isn't from the {@link IBuilder} properties. 
     * 
     * @param s Which control to build.
     * @return The created {@link Control}.
     */
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
                    event -> searchRunner.pauseSearch(searchId)
                );
            case "stop" ->
                createRunnerButton(s, true, 
                    event -> {
                        lockLevel.set(Lock.FULLY_LOCKED);
                        searchRunner.stopSearch(searchId);
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
                tf.setTextFormatter(new TextFormatter<>(FxUtils.intStringConverter, 1, FxUtils.integerFilter));
                yield tf;
            }
            case "current" -> createGridButton(currentViewer.gridProperty());
            default -> new Label(s);
        };
    }

    /**
     * Creates a {@link Button} which will call {@link #searchRunner}.
     * 
     * @param s The text for the button.
     * @param notLockedDisabled Whether it should be disabled when this {@link BuilderController}
     * is locked for modification ({@link #lockLevel}).
     * @param v The event handler for when the button gets activated.
     * @return The created {@link Button}.
     */
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

    /**
     * Checks whether the search run can be submitted to {@link #searchRunner},
     * i.e whether {@link #searchId} is a valid id; and if not, whether a valid
     * {@link Search} can be created for {@link #searchBuilder}.
     * 
     * @return {@code true} if the run can be submitted; {@code false} otherwise.
     */
    private boolean canAttemptRun() {
        LOGGER.debug("Checking if search can run");
        if(lockLevel.get() == Lock.MODIFICATION_LOCKED) {
            return true;
        }

        LOGGER.warn("Search is not initialized");
        var opt = searchRunner.createSearchAndSpace(
            searchBuilder, spaceBuilder, queuedBuilder, exploredBuilder
        );
        
        if(opt.isEmpty()) {
            LOGGER.error("Cannot run search: search could not be created");
            return false;
        }
            
        searchId = opt.getAsInt();;
        LOGGER.debug("Locking controller for modifications");
        lockLevel.set(Lock.MODIFICATION_LOCKED);
        currentViewer.gridProperty().bind(
            searchRunner.getSearchSpace(searchId).get().currentGridProperty()
        );
        bindProgressPane();
        return true;
    }

    /**
     * Submits a search run to {@link #searchRunner}.
     * 
     * @param isSteps Whether to submit as a 'steps' or a full run of the search.
     */
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
        
        searchRunner.runSearch(searchId, iter, throttle, log, memory);
    }

    /**
     * Changes {@link #searchBuilder} to a builder from the corresponding class
     * and calls {@link #setSearchParams()} and {@link #setQueuedClasses()}.
     * 
     * @param cb The {@link ChoiceBox} which contains the classes for {@link #searchBuilder}.
     */
    private void onSearchAlgActivated(ChoiceBox<Class<?>> cb) {

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

    /**
     * Sets the additional parameters from {@link Search.Builder} subclasses.
     */
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

    /**
     * Sets the possible {@link DataStructure} implementations for {@link #queuedBuilder}.
     */
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

    /**
     * Binds the content of {@link #progressPane} to the properties of the {@link Search}
     * which was built from {@link #searchBuilder}.
     */
    private void bindProgressPane() {
        LOGGER.debug("Binding progress panel to search");
        
        var opt = searchRunner.getSearchProgressProperties(searchId);
        if(opt.isEmpty()) {
            LOGGER.error("Could not bind progress panel");
            return;
        }
        
        var props = opt.get();
        var values = Search.SearchProperty.values();
        
        for(int i = 0; i < values.length; i++) {
            var l = new Label("");
            l.textProperty().bind(props.get(values[i]));
            progressPane.add(l, i , 1);
        }
    }

    /**
     * Fills the content of {@link #parameterTabPane}. 
     */
    private void setupTabPane() {
        LOGGER.debug("Creating parameter panem");

        var m = getPropertyMap();

        for(var p : IBuilder.Category.values()) {
            var tab = createTab(p, m.get(p));
            parameterTabPane.getTabs().add(tab);
        }
    }

    /**
     * Aggregates and returns the batch maps returned from all the {@link IBuilder}.
     * 
     * @return The created {@link Map}.
     */
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

    /**
     * Creates a {@link Tab} for {@link #parameterTabPane}.
     * 
     * @param p The {@link IBuilder.Category} for this {@link Tab} (i.e its name).
     * @param items The {@link Property} from which {@link Control} will be created
     * and placed inside this {@link Tab}.
     * @return The created {@link Tab}.
     */
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

    /**
     * Creates a {@link GridPane} for the parameter tabpane {@link #parameterTabPane}.
     * 
     * @param colNumber The number of columns ({@link javafx.scene.layout.ColumnConstraints})
     * for this {@link GridPane}.
     * @return The created {@link GridPane}.
     */
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

    /**
     * Creates a {@link Control} based on the given property.
     * 
     * @param prop {@link Property} to which the control's value will be bound.
     * @return The created {@link Control}.
     */
    @SuppressWarnings("unchecked")
    private Control controlFromProperty(Property<?> prop) {
        return switch(prop) {
            case BooleanProperty b -> createCheckBox(b);
            case IntegerProperty i -> createIntTF(i);
            case StringProperty s -> createStringTF(s);
            default -> switch(prop.getValue()) {
                // cannot be safely cast if case Property<Enum> in main switch
                case Enum ignored -> createEnumChoiceBox((Property<Enum>) prop);
                case Class unused -> createClassChoiceBox((Property<Class<?>>) prop);
                case Grid useless -> createGridButton((Property<Grid>) prop);
                default -> throw new IllegalStateException("Unexpected property type: " + prop.getValue().getClass());
            };
        };
    }

    /**
     * Creates a {@link CheckBox}.
     * 
     * @param b The {@link BooleanProperty} to which this {@link CheckBox} is bound.
     * @return The created {@link CheckBox}.
     */
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

    /**
     * Creates a {@link TextField} which only accepts numerical input.
     * 
     * @param i {@link IntegerProperty} to which this textfield's {@link TextFormatter} is bound.
     * @return The created {@link TextField}.
     */
    @SuppressWarnings("unchecked")
    private TextField createIntTF(IntegerProperty i) {
        LOGGER.trace("Creating textfield for " + i.getName());
        
        TextField tf = new TextField();
        tf.setMaxWidth(85.0d);
    
        tf.setTextFormatter(new TextFormatter<>(FxUtils.intStringConverter, 0, FxUtils.integerFilter));
        ((Property<Number>) tf.getTextFormatter().valueProperty()).bindBidirectional(i);

        tf.disableProperty().bind(lockLevel.isNotEqualTo(Lock.NOT_LOCKED));
        tf.disableProperty().addListener(
            event -> tf.getTextFormatter().valueProperty().unbind()
        );
        return tf;
    }

    /**
     * Creates a generic {@link TextField} which is bound to a {@link StringProperty}.
     * 
     * @param s The property to which is bound the value of this textfield.
     * @return The created {@link TextField}.
     */
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

    /**
     * Creates a {@link ChoiceBox} which displays all the values from an {@link Enum}.
     * 
     * @param p The property to which is bound the value of this choicebox.
     * @param <T> The enum type.
     * @return The created {@link ChoiceBox}.
     */
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

    /**
     * Creates a {@link ChoiceBox} which displays Class objects.
     * 
     * @param p The property to which is bound the value of this choicebox.
     * @return The created {@link ChoiceBox}.
     */
    private ChoiceBox<Class<?>> createClassChoiceBox(Property<Class<?>> p) {
        LOGGER.trace("Creating choicebox for " + p.getName());

        var cb = new ChoiceBox<Class<?>>();
        cb.setMaxWidth(110.0d);
        cb.setPrefWidth(110.0d);

        if(Collection.class.isAssignableFrom(p.getValue())) {
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
            
            cb.setConverter(FxUtils.clsStringConverter);
        }
        else if(Search.class.isAssignableFrom(p.getValue())) {
            Reflections reflections = new Reflections("io.github.vqnxiv.taquin");
            
            cb.setItems(FXCollections.observableList(
                reflections
                    .get(Scanners.SubTypes.of(Search.class).asClass())
                    .stream()
                    .toList()
            ));
            cb.setConverter(FxUtils.srchClsConv);
        }
        
        cb.valueProperty().bindBidirectional(p);
        
        cb.disableProperty().bind(lockLevel.isNotEqualTo(Lock.NOT_LOCKED));
        cb.disableProperty().addListener(
            event -> cb.valueProperty().unbind()
        );
        return cb;
    }

    /**
     * Creates a button which opens a {@link GridViewer}.
     * 
     * @param p The Grid Property to bind to that of the {@link GridViewer}.
     * @return {@link Button} which opens the {@link GridViewer}.
     */
    private Button createGridButton(Property<Grid> p) {
        LOGGER.trace("Creating button for " + p.getName());

        var b = new Button(p.getName());
        
        if(p.getName().equalsIgnoreCase("start")) {
            b.setOnAction(e -> startViewer.setShowing(true));
            p.bind(startViewer.gridProperty());
            startViewer.editableProperty().bind(lockLevel.isEqualTo(Lock.NOT_LOCKED));
        }
        else if(p.getName().equalsIgnoreCase("end")) {
            b.setOnAction(e -> endViewer.setShowing(true));
            p.bind(endViewer.gridProperty());
            endViewer.editableProperty().bind(lockLevel.isEqualTo(Lock.NOT_LOCKED));
        }
        else if(p.getName().equalsIgnoreCase("current")) {
            b.setOnAction(e -> currentViewer.setShowing(true));
            b.disableProperty().bind(lockLevel.isEqualTo(Lock.NOT_LOCKED));
        }
        
        return b;
    }
}
