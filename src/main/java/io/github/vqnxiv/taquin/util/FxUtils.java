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


public final class FxUtils {

    
    private FxUtils() {}
    
    
    public static RowConstraints getRowConstraints(VPos alignment, Priority vgrow) {
        RowConstraints row = new RowConstraints();
        row.setValignment(alignment);
        row.setVgrow(vgrow);
        return row;
    }

    public static ColumnConstraints getColConstraints(HPos alignment, Priority hgrow) {
        ColumnConstraints col = new ColumnConstraints();
        col.setHalignment(alignment);
        col.setHgrow(hgrow);
        return col;
    }
    
    public static List<RowConstraints> getMultipleRowConstraints(int amount, VPos alignement, Priority vgrow) {
        RowConstraints[] tab = new RowConstraints[amount];
        
        for(int i = 0; i < amount; i++) {
            tab[i] = getRowConstraints(alignement, vgrow);
        }
        
        return List.of(tab);
    }

    public static List<ColumnConstraints> getMultipleColConstraints(int amount, HPos alignement, Priority hgrow) {
        ColumnConstraints[] tab = new ColumnConstraints[amount];

        for(int i = 0; i < amount; i++) {
            tab[i] = getColConstraints(alignement, hgrow);
        }

        return List.of(tab);
    }

    public static Node getNodeFromGridPane(GridPane gridPane, int col, int row) {
        for (Node node : gridPane.getChildren()) {
            if (GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row) {
                return node;
            }
        }
        return null;
    }
    
    public static TextField createIntFilteredTextField(double maxWidth, int defaultValue) {
        TextField tf = new TextField();
        tf.setMaxWidth(maxWidth);
        tf.setPrefWidth(maxWidth);
        tf.setTextFormatter(new TextFormatter<>(Utils.intStringConverter, defaultValue, Utils.integerFilter));
        return tf;
    }
}
