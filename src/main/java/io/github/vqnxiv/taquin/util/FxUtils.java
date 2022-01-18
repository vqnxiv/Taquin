package io.github.vqnxiv.taquin.util;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import java.util.List;
import java.util.Optional;


/**
 * Non instantiable JavaFX related utility class which only contains static final methods.
 */
public final class FxUtils {

    /**
     * Can't be instantiated.
     */
    private FxUtils() {}

    /**
     * Creates a single {@link RowConstraints} from the given {@link VPos} and {@link Priority}
     * 
     * @param alignment the {@link VPos} for this {@link RowConstraints}
     * @param vgrow the {@link Priority} for this {@link RowConstraints}
     * @return a new {@link RowConstraints}
     */
    public static RowConstraints getRowConstraints(VPos alignment, Priority vgrow) {
        RowConstraints row = new RowConstraints();
        row.setValignment(alignment);
        row.setVgrow(vgrow);
        return row;
    }

    /**
     * Creates a single {@link ColumnConstraints} from the given {@link HPos} and {@link Priority}
     *
     * @param alignment the {@link HPos} for this {@link ColumnConstraints}
     * @param hgrow the {@link Priority} for this {@link ColumnConstraints}
     * @return a new {@link ColumnConstraints}
     */
    public static ColumnConstraints getColConstraints(HPos alignment, Priority hgrow) {
        ColumnConstraints col = new ColumnConstraints();
        col.setHalignment(alignment);
        col.setHgrow(hgrow);
        return col;
    }

    /**
     * Creates multiple {@link RowConstraints} from the given {@link VPos} and {@link Priority}
     * 
     * @param amount the number of {@link RowConstraints} to create.
     * @param alignment the {@link VPos} for these {@link RowConstraints}
     * @param vgrow the {@link Priority} for these {@link RowConstraints}
     * @return a {@link List} of the created {@link RowConstraints}
     */
    public static List<RowConstraints> getMultipleRowConstraints(int amount, VPos alignment, Priority vgrow) {
        RowConstraints[] tab = new RowConstraints[amount];
        
        for(int i = 0; i < amount; i++) {
            tab[i] = getRowConstraints(alignment, vgrow);
        }
        
        return List.of(tab);
    }

    /**
     * Creates multiple {@link ColumnConstraints} from the given {@link HPos} and {@link Priority}
     *
     * @param amount the number of {@link ColumnConstraints} to create.
     * @param alignment the {@link HPos} for these {@link ColumnConstraints}
     * @param hgrow the {@link Priority} for these {@link ColumnConstraints}
     * @return a {@link List} of the created {@link ColumnConstraints}
     */
    public static List<ColumnConstraints> getMultipleColConstraints(int amount, HPos alignment, Priority hgrow) {
        ColumnConstraints[] tab = new ColumnConstraints[amount];

        for(int i = 0; i < amount; i++) {
            tab[i] = getColConstraints(alignment, hgrow);
        }

        return List.of(tab);
    }

    /**
     * Gets a {@link Node} from a {@link GridPane} by its coordinates
     * 
     * @param gridPane the link {@link GridPane} which contains the {@link Node}
     * @param col The column index of the node.
     * @param row The row index of the node.
     * @return {@link Optional#of(Object)} the {@link Node} if the {@link GridPane} contains
     * a node at these coordinates; {@link Optional#empty()} otherwise
     */
    public static Optional<Node> getNodeFromGridPane(GridPane gridPane, int col, int row) {
        for (Node node : gridPane.getChildren()) {
            if (GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /**
     * Creates a {@link TextField} with the given width and a {@link TextFormatter<Integer>}.
     * 
     * @param maxWidth Maximum width for this {@link TextField}.
     * @param defaultValue The default value for the {@link TextFormatter}.
     * @return the created {@link TextField}
     */
    public static TextField createIntFilteredTextField(double maxWidth, int defaultValue) {
        TextField tf = new TextField();
        tf.setMaxWidth(maxWidth);
        tf.setPrefWidth(maxWidth);
        tf.setTextFormatter(new TextFormatter<>(Utils.intStringConverter, defaultValue, Utils.integerFilter));
        return tf;
    }
}
