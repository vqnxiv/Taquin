package io.github.vqnxiv.taquin.util;


import io.github.vqnxiv.taquin.model.Grid;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * This class controls the small window popup which displays a grid
 * 
 * @see Grid
 * @see GridControl
 */
public class GridViewer {

    private static final Logger LOGGER = LogManager.getLogger();

    
    /**
     * The {@code Stage} for this object. It's a non blocking stage owned by the main stage, 
     * and is displayed on top.
     */
    private final Stage stage;

    /**
     * The {@code GridControl} which is the only node in the scene graph for this stage.
     */
    private final GridControl gridControl;

    /**
     * This grid property is either bound to a {@code SpaceSearch.Builder} grid property
     * or to a {@code Search} current grid property.
     * 
     * This side of the binding only updates the property on stage close.
     * TODO: add to right click options
     */
    private final ObjectProperty<Grid> gridProperty;

    /**
     * A simple boolean property which is directly bound to the {@code editableProperty} 
     * from {@code gridControl} and is used to determine whether validation should be processed 
     * when updating {@code gridProperty} & for the right click options.
     */
    private final BooleanProperty editableProperty;


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
        gridProperty = new SimpleObjectProperty<>(Grid.empty(3, 3));
        gridControl = new GridControl(name, editable, 3, 3);
        editableProperty = new SimpleBooleanProperty(editable);
        
        initializeStage(name);
        
        gridProperty.addListener(
            (obs, oldGrid, newGrid) -> gridControl.setValues(gridProperty.get().getCopyOfSelf())
        );
        
    }

    /**
     * Initialize the stage: 
     * binds its dimensions so it keeps its aspect ratio, 
     * and add onClose action, which is updating {@code gridProperty} if {@code editableProperty}
     * checks {@code true}.
     * 
     * @param name
     */
    private void initializeStage(String name) {
        stage.setTitle(name);
        stage.setAlwaysOnTop(true);
        
        stage.widthProperty().addListener(
            (obs, oldVal, newVal) -> stage.setHeight(
                stage.getWidth() * (gridControl.getRowCount() / gridControl.getColumnCount())
            )
        );
        
        stage.heightProperty().addListener(
            (obs, oldVal, newVal) -> stage.setWidth(
                stage.getHeight() * (gridControl.getRowCount() / gridControl.getColumnCount())
            )
        );
        
        var anchorPane = new AnchorPane();
        initializeGridControl(anchorPane);
        stage.setScene(new Scene(anchorPane));
        // stage.setWidth(gridControl.getColumnCount() * 50d);
        
        stage.setOnCloseRequest(
            event -> {
                if(gridControl.isEditable()) {
                    var opt = Grid.from(gridControl.collectValues(-1), Grid.EqualPolicy.RANDOM);
                    if(opt.isPresent()) {
                        gridProperty.setValue(opt.get());
                        LOGGER.info("Successfully set grid for " + stage.getTitle());
                    }
                }
            }
        );
    }

    /**
     * Initialize {@code gridControl} & sets onMouseClick event handler
     * 
     * @param anchorPane the anchorPane in which the {@code gridControl} is placed
     */
    private void initializeGridControl(AnchorPane anchorPane) {
        gridControl.editableProperty().bind(editableProperty);
        
        gridControl.setOnMouseClicked(
            event -> {
                if(event.getButton() == MouseButton.SECONDARY) {
                    handleRightClick();
                }
            }
        );
        
        anchorPane.getChildren().add(gridControl);
        AnchorPane.setTopAnchor(gridControl, 0d);
        AnchorPane.setBottomAnchor(gridControl, 0d);
        AnchorPane.setLeftAnchor(gridControl, 0d);
        AnchorPane.setRightAnchor(gridControl, 0d);
    }

    
    /**
     * Handles right-click action.
     * 
     * If {@code editableProperty} checks {@false}, nothing happens.
     */
    private void handleRightClick() {
        if(!editableProperty.get()) {
            return;
        }
    }


    /**
     * Getter for {@code gridProperty}
     * 
     * @return this object's {@gridProperty}
     */
    public ObjectProperty<Grid> gridProperty() {
        return gridProperty;
    }

    /**
     * Getter for {@code editableProperty}
     *
     * @return this object's {@editableProperty}
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
