package io.github.vqnxiv.taquin.util;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.util.Arrays;


/**
 * This class is a specialized {@code GridPane} whose all children nodes are 
 * {@code TextField} which only accept int values thanks to a {@code TextFormatter}
 * and {@code StringConverter}.
 * 
 * @see GridPane
 * @see Grid
 * @see GridViewer
 */
public class GridControl extends GridPane {

    /**
     * String converter which converts between String and Integer.
     * 
     * Special values: blank String -> {@code null} Integer, {@code null} Integer -> ""
     */
    private static final StringConverter<Integer> intStringConverter = new StringConverter<>() {
        @Override
        public String toString(Integer n) {
            return (n != null) ? Integer.toString(n) : "";
        }

        @Override
        public Integer fromString(String string) {
            return (!string.isBlank()) ? Integer.parseInt(string) : null;
        }
    };
    

    /**
     * A 2D-array which contains references to the {@code TextField}s placed in the {@code GridPane}
     */
    private TextField[][] fields;

    /**
     * {@code BooleanProperty} to which all {@code TextField} {@code disableProperty} are bound
     */
    private BooleanProperty editableProperty;


    /**
     * Base constructor which creates a {@code 3}x{@code 3} array of {@code TextField} with {@code null} values
     * 
     * @param id The unique id for this GridControl. Must respect {@code id.isBlank() == false}
     * @param edit The initial value for {@code editable}
     */
    public GridControl(String id, boolean edit) {
        this(id, edit, new Integer[3][3]);
    }

    /**
     * Creates a {@code width} x {@code height} array of {@code TextField} with {@code null} values
     * 
     * @param id The unique id for this GridControl. Must respect {@code id.isBlank() == false}
     * @param edit The initial value for {@code editableProperty}
     * @param width The number of columns
     * @param height The number of rows
     */
    public GridControl(String id, boolean edit, int width, int height) {
        this(id, edit, new Integer[height][width]);
    }

    /**
     * Creates an array of TextFields with the dimensions of {@code values} and 
     * whose {@TextFormatter} values are set to that of {@code values}
     *
     * @param id The unique id for this GridControl. Must respect {@code id.isBlank() == false}
     * @param edit The initial value for {@code editableProperty}
     * @param values Initial values for the textfields
     */
    public GridControl(String id, boolean edit, int[][] values) {
        this(id, edit,
            Arrays.stream(values)
                .map(array -> Arrays.stream(array).boxed().toArray(Integer[]::new))
                .toArray(Integer[][]::new)
        );
    }

    /**
     * Creates an array of TextFields with the dimensions of {@code values} and 
     * whose {@TextFormatter} values are set to that of {@code values}
     *
     * @param id The unique id for this GridControl. Must respect {@code id.isBlank() == false}
     * @param edit The initial value for {@code editableProperty}
     * @param values Initial values for the textfields
     */
    public GridControl(String id, boolean edit, Integer[][] values) {
        /*
        values = Arrays.stream(vals)
            .map(t -> Arrays.stream(t)
                .mapToObj(i -> new SimpleIntegerProperty(i))
                .toArray(SimpleIntegerProperty[]::new)
            ).toArray(SimpleIntegerProperty[][]::new);
        */
            // .map(array -> Arrays.stream(array).boxed().toArray(Integer[]::new))
            // .toArray(Integer[][]::new);
        
        if(id.isBlank()) {
            throw new IllegalArgumentException("Identifier must not be blank");
        }
        
        idProperty().set(id);
        editableProperty = new SimpleBooleanProperty(edit);
        
        fields = new TextField[][]{};
        /*
        fields = new TextField[values.length][values[0].length];
        
        for(int row = 0; row < values.length; row++) {
            for(int col = 0; col < values[0].length; col++) {
                fields[row][col] = createTextField();
            }
        }
        */
        setValues(values);
        
        /*
        for(int row = 0; row < vals.length; row++) {
            for(int col = 0; col < vals[0].length; col++) {
                fields[row][col] = createTextField();
            }
        }
        
        resizeSuper();
        */
    }

    /**
     * Factory method which creates a {@TextField} which gets set a {@code TextFormatter} initialized 
     * with {@code intStringConverter} and {@code Utils.integerFilter}, and whose {@code disableProperty}
     * gets bound to {@code editableProperty}
     * 
     * @return a {@code TextField} with a {@code TextFormatter}
     */
    private TextField createTextField() {
        var tf = new TextField();
        tf.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        tf.setAlignment(Pos.CENTER);
        tf.setTextFormatter(new TextFormatter<>(intStringConverter, null, Utils.integerFilter));
        tf.disableProperty().bind(editableProperty.not());
        
        return tf;
    }


    /**
     * Getter for {@code editableProperty}
     * 
     * @return {@code editableProperty}
     */
    public BooleanProperty editableProperty() {
        return editableProperty;
    }

    /**
     * Getter for the value of {@code editableProperty}
     * 
     * @return the boolean value of {@code editableProperty}
     */
    public boolean isEditable() {
        return editableProperty.get();
    }

    /**
     * Getter for the values of the {@code TextField} in {@code fields}
     * 
     * @return an int array containing the values from the @{code TextFormatter} 
     * from the {@code TextField}
     */
    public int[][] collectValues(int valueForNull) {
        // return Arrays.stream(values).map(Integer[]::clone).toArray(t -> values.clone());
        return Arrays.stream(fields)
            .map(t -> Arrays.stream(t)
                .mapToInt(tf -> 
                    (int) (
                        (tf.getTextFormatter().valueProperty().get() != null) ?
                        tf.getTextFormatter().valueProperty().get() :
                        valueForNull
                    )
                )
                .toArray()
            ).toArray(int[][]::new);
    }


    /**
     * Edits the values of the {@code TextField} in {@code fields} and {@code editableProperty}.
     * Resize {@code fields} and the parent {@code GridPane} if needed
     * 
     * @param newValues the new values for the {@code TextField}
     * @param b the new value for {@code editableProperty}
     */
    public void setValuesAndEditable(int[][] newValues, boolean b) {
        setEditable(b);
        setValues(newValues);
    }

    /**
     * Edits the values of the {@code TextField} in {@code fields} and {@code editableProperty}.
     * Resize {@code fields} and the parent {@code GridPane} if needed
     *
     * @param newValues the new values for the {@code TextField}
     * @param b the new value for {@code editableProperty}
     */
    public void setValuesAndEditable(Integer[][] newValues, boolean b) {
        setEditable(b);
        setValues(newValues);
    }

    /**
     * Changes the value of {@code editableProperty}
     * 
     * @param b the new value for {@code editableProperty}
     */
    public void setEditable(boolean b) {
        editableProperty.set(b);
    }

    /**
     * Edits the values of the {@code TextField} in {@code fields}.
     * Resize {@code fields} and the parent {@code GridPane} if needed
     *
     * @param newValues the new values for the {@code TextField}
     */
    public void setValues(int[][] newValues) {
        setValues(Arrays.stream(newValues)
            .map(array -> Arrays.stream(array).boxed().toArray(Integer[]::new))
            .toArray(Integer[][]::new)
        );
    }

    /**
     * Edits the values of the {@code TextField} in {@code fields}.
     * Resize {@code fields} and the parent {@code GridPane} if needed
     *
     * @param newValues the new values for the {@code TextField}
     */
    public void setValues(Integer[][] newValues) {
        if(newValues.length != fields.length || newValues[0].length != fields[0].length) {
            resizeArray(newValues[0].length, newValues.length);
        }

        for(int row = 0; row < newValues.length; row++) {
            for(int col = 0; col < newValues[0].length; col++) {
                ((Property<Number>) fields[row][col].getTextFormatter().valueProperty()).setValue(newValues[row][col]);
            }
        }
    }

    /**
     * Resizes {@code fields} and the parent {@code GridPane} vertically by setting a new number of rows
     * 
     * @param rows The new number of rows for {@code fields}
     */
    public void setRowCount(int rows) {
        resizeArray(fields.length, rows);
    }

    /**
     * Resizes {@code fields} and the parent {@code GridPane} horizontally by setting a new number of columns
     *
     * @param cols The new number of columns for {@code fields}
     */
    public void setColumnCount(int cols) {
        resizeArray(cols, fields[0].length);
    }

    /**
     * Resizes {@code fields} and the parent {@code GridPane} both horizontally and vertically 
     * by setting a new number of rows and columns
     * 
     * @param rows The new number of rows for {@code fields}
     * @param cols The new number of columns for {@code fields}
     */
    public void setDimensions(int rows, int cols) {
        resizeArray(cols, rows);
    }

    /**
     * Resizes {@code fields} with the given amount of columns ({@code newWidth}) and rows
     * ({@code newHeight}). 
     * 
     * If {@code fields} is resized down, it will be trimmed down to the new dimensions;
     * if its dimensions are increased, new {@code TextField}s will be created by calling
     * {@code createTextField} to fill in the empty cells in {@code fields}.
     * 
     * Once {@code fields} has been resized, this calls {@code resizeSuper}.
     * 
     * @param newWidth The new number of columns for {@code fields}
     * @param newHeight The new number of rows for {@code fields}
     */
    private void resizeArray(int newWidth, int newHeight) {
        
        TextField[][] newFields = new TextField[newHeight][newWidth];
        
        for(int row = 0; row < newHeight; row++) {
            for(int col = 0; col < newWidth; col++) {
                if(row >= fields.length || col >= fields[0].length) {
                    newFields[row][col] = createTextField();
                }
                else {
                    newFields[row][col] = fields[row][col];
                }
            }
        }
        
        fields = newFields;
        
        resizeSuper();
    }

    /**
     * Resizes and updates the content of the parent {@code GridPane} according to {@code fields}
     * by clearing the children, row constraints and column constraints, then repopulating them
     */
    private void resizeSuper() {
        getChildren().clear();

        getRowConstraints().clear();
        getRowConstraints().addAll(
            FxUtils.getMultipleRowConstraints(fields.length, VPos.CENTER, Priority.ALWAYS)
        );
        
        getColumnConstraints().clear();
        getColumnConstraints().addAll(
            FxUtils.getMultipleColConstraints(fields[0].length, HPos.CENTER, Priority.ALWAYS)
        );
        
        for(int row = 0; row < fields.length; row++) {
            for(int col = 0; col < fields[0].length; col++) {
                add(fields[row][col], col, row);
            }
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if(o instanceof GridControl g) {
            return idProperty().get().equals(g.idProperty().get());
        }
        
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return idProperty().hashCode();
    }
}
