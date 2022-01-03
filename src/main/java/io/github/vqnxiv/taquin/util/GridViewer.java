package io.github.vqnxiv.taquin.util;


import io.github.vqnxiv.taquin.model.Grid;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;


/**
 * This class controls the small window popup which displays a grid
 * 
 * @see Grid
 * @see GridControl
 */
public class GridViewer {

    /**
     * Enum used for the right-click context menu.
     * <p>
     * Each value of the enum corresponds to a specific {@code MenuItem}.
     */
    private enum ContextMenuItem {
        SAVE_GRID(
            "save", true, false,
            GridViewer::pushControlToProperty
        ),
        CANCEL_GRID_CHANGES(
            "undo", true, false,
            GridViewer::pushPropertyToControl
        ),
        RESIZE_GRID(
            "resize", true, false,
            GridViewer::resizeDialog
        ),
        COPY(
            "copy",true, true,
            GridViewer::saveToBuffer
        ),
        PASTE_RESIZE(
            "paste & resize", true, false,
            g -> g.pasteFromBuffer(true)
        ),
        PASTE_IGNORE_SIZE(
            "paste", true, false,
            g -> g.pasteFromBuffer(false)
        ),
        SHOW_DETAILS(
            "details", false, true,
            GridViewer::pushControlToProperty
        )
        ;

        /**
         * The name which will be used for the {@code MenuItem}'s name.
         */
        private final String itemName;

        /**
         * Whether this item should be shown when the {@code GridViewer}'s {@code editableProperty}
         * has a value of {@code true}.
         */
        private final boolean editor;

        /**
         * Whether this item should be shown when the {@code GridViewer}'s {@code editableProperty}
         * has a value of {@code false}.
         */
        private final boolean readOnly;

        /**
         * Consumer which represents the action to perform when this value's {@code MenuItem}
         * {@code ActionEvent} {@code EventHandler} is called
         */
        private final Consumer<GridViewer> consumer;

        
        /**
         * Enum constructor
         * 
         * @param itemName the name for this {@code ContextMenuItem}
         * @param editor the value for {@code editor}
         * @param readOnly the value for {@code readOnly}
         * @param consumer the value for {@code consumer}
         */
        ContextMenuItem(String itemName, boolean editor, boolean readOnly, Consumer<GridViewer> consumer) {
            this.itemName = itemName;
            this.editor = editor;
            this.readOnly = readOnly;
            this.consumer = consumer;
        }

        /**
         * Getter for this value's consumer.
         * 
         * @return {@code EventHandler}
         */
        private Consumer<GridViewer> getConsumer() {
            return consumer;
        }

        /**
         * Returns all {@code ContextMenuItem} which match at least one of the booleans
         * passed in arguments
         * 
         * @param editor whether to return {@code ContextMenuItem}s which should be displayed 
         * when the {@code GridViewer}'s {@code editableProperty} is set to {@code true}
         * @param readOnly whether to return {@code ContextMenuItem}s which should be displayed 
         * when the {@code GridViewer}'s {@code editableProperty} is set to {@code false}
         * @return {@code List} of {@code ContextMenuItem}s which match either {@code editor} 
         * or {@code readOnly} 
         */
        private static List<ContextMenuItem> getAll(boolean editor, boolean readOnly) {
            return Arrays.stream(values())
                .filter(i ->
                    (editor && i.editor == editor) ||
                    (readOnly && i.readOnly == readOnly)
                )
                .toList();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return itemName.substring(0,1).toUpperCase() + itemName.substring(1).toLowerCase();
        }
    }
    

    private static final Logger LOGGER = LogManager.getLogger();

    
    /**
     * A 2D array which is used to send a value between {@code GridViewer}s
     * when using copy/paste
     */
    private static Integer[][] copyPasteBuffer;
    
    
    /**
     * The {@code Stage} for this object. It's a non blocking stage owned by the main stage, 
     * and is displayed on top.
     */
    private final Stage stage;

    /**
     * The {@code GridControl} which is one of the only two nodes in the scene graph for this stage.
     */
    private final GridControl gridControl;

    /**
     * This grid property is either bound to a {@code SpaceSearch.Builder} grid property
     * or to a {@code Search} current grid property.
     * <p>
     * This side of the binding only updates the property on stage close.
     */
    private final ObjectProperty<Grid> gridProperty;

    /**
     * A simple boolean property which is directly bound to the {@code editableProperty} 
     * from {@code gridControl} and is used to determine whether validation should be done 
     * when updating {@code gridProperty} and for the right click options.
     */
    private final BooleanProperty editableProperty;

    /**
     * This object's {@code ContextMenu} which is shown on a right click event.
     */
    private final ContextMenu contextMenu;
    
    /**
     * The default cell size used when initializing this object's stage. 
     * <p>
     * The stage's width is set at {@code defaultCellSize * gridControl}'s number of columns. 
     */
    private double defaultCellSize = 50;

    /**
     * The minimum cell size when changing the dimensions of this object's {@code GridControl}.
     * <p> 
     * I.e changing its number of columns or rows, not dragging the borders of the stage to resize it.
     */
    private double minCellSize = 50;
    
    
    /**
     * Constructor which will create an empty {@code GridControl} and an empty {@code Grid}
     * with a size of 3x3. The name passed as a parameter serves both for the {@code stage} name
     * and the {@code GridControl}'s {@code idProperty}
     * 
     * @param name the name for this {@code GridViewer}
     * @param editable initial value for {@code editableProperty}
     */
    public GridViewer(String name, boolean editable) {
        stage = new Stage();
        gridProperty = new SimpleObjectProperty<>(Grid.invalidOfSize(3, 3));
        gridControl = new GridControl(name, editable, 4, 3);
        editableProperty = new SimpleBooleanProperty(editable);
        contextMenu = new ContextMenu();
        
        initializeStage(name);
        makeContextMenu();
        
        editableProperty.addListener(
            (obs, oldValue, newValue) -> makeContextMenu()
        );
        gridProperty.addListener(
            (obs, oldGrid, newGrid) -> gridControl.setValues(gridProperty.get().getCopyOfSelf())
        );
        
    }

    /**
     * Initialize the stage:
     * <p>
     * binds its dimensions so it keeps its aspect ratio, 
     * and add onClose action, which is updating {@code gridProperty} if {@code editableProperty}
     * checks {@code true}.
     * 
     * @param name the name for this stage
     */
    private void initializeStage(String name) {
        stage.setTitle(name);
        stage.setAlwaysOnTop(true);
        stage.initStyle(StageStyle.UTILITY);
        
        stage.widthProperty().addListener(
            (obs, oldVal, newVal) -> stage.setHeight(
                stage.getWidth() * ((double) gridControl.getRowCount() / gridControl.getColumnCount())
            )
        );
        
        stage.heightProperty().addListener(
            (obs, oldVal, newVal) -> stage.setWidth(
                stage.getHeight() * ((double) gridControl.getColumnCount() / gridControl.getRowCount())
            ) 
        );
        
        var anchorPane = new AnchorPane();
        initializeGridControl(anchorPane);
        stage.setScene(new Scene(anchorPane));
        stage.setWidth(gridControl.getColumnCount() * defaultCellSize);
        
        stage.setOnCloseRequest(event -> pushControlToProperty());
    }

    /**
     * Initialize {@code gridControl} and sets onMouseClick event handler
     * 
     * @param anchorPane the anchorPane in which the {@code gridControl} is placed
     */
    private void initializeGridControl(AnchorPane anchorPane) {
        gridControl.editableProperty().bind(editableProperty);
        
        gridControl.setOnContextMenuRequested(
            event -> contextMenu.show(gridControl, event.getScreenX(), event.getScreenY())
        );
        
        anchorPane.getChildren().add(gridControl);
        AnchorPane.setTopAnchor(gridControl, 0d);
        AnchorPane.setBottomAnchor(gridControl, 0d);
        AnchorPane.setLeftAnchor(gridControl, 0d);
        AnchorPane.setRightAnchor(gridControl, 0d);
    }
    
    
    /**
     * Updates the content of this object's {@code ContextMenu}.
     */
    private void makeContextMenu() {
        contextMenu.getItems().clear();
        
        for(var i : ContextMenuItem.getAll(editableProperty.get(), !editableProperty.get())) {
            var mi = new MenuItem(i.toString());
            mi.setOnAction(event -> i.getConsumer().accept(this));
            contextMenu.getItems().add(mi);
        }
        
        gridControl.clearAndSetContextMenu(contextMenu.getItems());
    }

    /**
     * Helper function which handles the saving of the {@code GridControl} values
     * to this object's {@code gridProperty}
     */
    private void pushControlToProperty() {
        if(gridControl.isEditable()) {
            var opt = Grid.of(gridControl.collectValues(-1), Grid.EqualPolicy.RANDOM);
            if(opt.isPresent()) {
                gridProperty.setValue(opt.get());
                LOGGER.info("Successfully set grid for " + stage.getTitle());
            }
        }
    }

    /**
     * Helper function which handles updating this object's {@code GridControl}'s values
     * with the content of of {@code gridProperty}
     */
    private void pushPropertyToControl() {
        gridControl.setValues(gridProperty.getValue().getCopyOfSelf());
    }

    /**
     * Saves the values of {@code gridControl} to {@code copyPasteBuffer}
     */
    private void saveToBuffer() {
        copyPasteBuffer = gridControl.collectValues();
    }

    /**
     * Sets the values of {@code copyPasteBuffer} to this object's {@code gridControl}
     */
    private void pasteFromBuffer(boolean resize) {
        if(copyPasteBuffer == null) {
            return;
        }
        
        int rows = gridControl.getRowCount();
        int cols = gridControl.getColumnCount();
        
        gridControl.setValues(copyPasteBuffer);
        
        if(!resize) {
            gridControl.setDimensions(rows, cols);
        }
        else {
            if(cols == gridControl.getColumnCount()) {
                stage.setHeight(gridControl.getRowCount() * defaultCellSize);
            }
            else {
                stage.setWidth(gridControl.getColumnCount() * defaultCellSize);
            }
        }
    }

    /**
     * Brings up a {@code PairInputDialog} which is a modal that extends {@code Dialog}.
     * <p>
     * Blocks this {@code GridViewer} until it is closed, and then calls {@code GridControl} 
     * {@code setDimensions} with the value from {@code resultProperty} 
     */
    private void resizeDialog() {
        record Pair(int width, int height){}
        
        var dialog = new Dialog<Pair>();
        
        var width = FxUtils.createIntFilteredTextField(
            50d, GridViewer.this.gridControl.getColumnCount()
        );
        var height = FxUtils.createIntFilteredTextField(
            50d, GridViewer.this.gridControl.getRowCount()
        );
        
        HBox hbox = new HBox(width, height);
        hbox.setSpacing(5d);
        
        dialog.getDialogPane().setContent(hbox);
        dialog.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Apply", ButtonBar.ButtonData.APPLY)
        );
        
        dialog.setTitle("Resize");
        dialog.setResultConverter(
            btn -> {
                if(btn == null || btn.getButtonData() != ButtonBar.ButtonData.APPLY) {
                    return null;
                }
                else {
                    return new Pair(
                        (int) (width.getTextFormatter().valueProperty().getValue() != null ?
                        width.getTextFormatter().valueProperty().get() : 1),
                        (int) (height.getTextFormatter().valueProperty().getValue() != null ?
                        height.getTextFormatter().valueProperty().get() : 1)
                    );
                }
            }
        );
        
        stage.setAlwaysOnTop(false);
        
        dialog.showAndWait().ifPresent(
            p -> {
                if(p.width() == gridControl.getColumnCount()) {
                    stage.setHeight(gridControl.getRowCount() * defaultCellSize);
                }
                else {
                    stage.setWidth(gridControl.getColumnCount() * defaultCellSize);
                }
                
                gridControl.setDimensions(p.height(), p.width());
            }
        );
    }
    
    /**
     * Getter for {@code gridProperty}
     * 
     * @return this object's {@code gridProperty}
     */
    public ObjectProperty<Grid> gridProperty() {
        return gridProperty;
    }

    /**
     * Getter for {@code editableProperty}
     *
     * @return this object's {@code editableProperty}
     */
    public BooleanProperty editableProperty() {
        return editableProperty;
    }

    /**
     * Sets a new value to this object's {@code editableProperty}
     * 
     * @param b new boolean value for {@code editableProperty}
     */
    public void setEditable(boolean b) {
        editableProperty.set(b);
    }

    /**
     * Sets whether this object's stage is showing.
     * 
     * @param b whether the stage should show
     */
    public void setShowing(boolean b) {
        if(b) {
            stage.show();
        }
        else {
            stage.hide();
        }
    }
}
