package io.github.vqnxiv.taquin.util;


import io.github.vqnxiv.taquin.model.Grid;
import io.github.vqnxiv.taquin.model.SearchSpace;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import java.util.HashSet;
import java.util.function.UnaryOperator;


public class GridViewer {
    
    
    Stage stage;
    Scene scene;
    AnchorPane anchorPane;
    GridPane gridPane;
    ToolBar toolBar;
    
    int toolBarHeight = 30;
    int minCellSize = 50;
    
    int stageWidth = 250;
    int stageHeight = 250;
    
    Grid grid;
    
    // width = # columns; height = # rows
    int currentGridWidth;
    int currentGridHeight;
    int newGridWidth;
    int newGridHeight;
    
    boolean isReadOnly;
    
    String stageName;
    
    SearchSpace.Builder spaceBuilder;
    

    private final UnaryOperator<TextFormatter.Change> integerFilter = change -> {
        String input = change.getText();
        if (input.matches("[0-9]*")) {
            return change;
        }
        return null;
    };
    
    
    public GridViewer(SearchSpace.Builder b, String name, boolean readOnly) {

        spaceBuilder = b;
        
        if(spaceBuilder.getGrid(name) == null) {
            // rows, columns
            var t = new int[3][3];

            for(int row = 0; row < t.length; row++)
                for(int col = 0; col < t[0].length; col++)
                    t[row][col] = -1;

            // to avoid NPE on Grid instantiation -- make a boolean dontFindZero constructor?
            t[0][0] = 0;
            grid = new Grid(t, Grid.EqualPolicy.RANDOM);
            grid.getSelf()[0][0] = -1;
        }
        else {
            grid = spaceBuilder.getGrid(name);
        }
        
        initialise(name, readOnly);
    }
    
    
    public GridViewer(Grid g, String name, boolean readOnly) {
        this.grid = g;
        initialise(name, readOnly);
    }
    
    
    private void initialise(String name, boolean readOnly) {
        stageName = name;
        isReadOnly = readOnly;
        
        stage = new Stage();
        anchorPane = new AnchorPane();
        scene = new Scene(anchorPane);
        
        stage.widthProperty().addListener(
            (obs, oldVal, newVal) -> stage.setHeight(
                stage.getWidth() * currentGridHeight / currentGridWidth + ((isReadOnly) ? 0 : toolBarHeight)
            )
        );

        stage.heightProperty().addListener(
            (obs, oldVal, newVal) -> stage.setWidth(
                (stage.getHeight() - ((isReadOnly) ? 0 : toolBarHeight)) * currentGridWidth / currentGridHeight
            )
        );
        
        createGridPane();
        anchorPane.getChildren().add(gridPane);
        
        if(!isReadOnly) {
            createToolBar();
            anchorPane.getChildren().add(toolBar);
            AnchorPane.setTopAnchor(toolBar, 0d);
            AnchorPane.setLeftAnchor(toolBar, 0d);
            AnchorPane.setRightAnchor(toolBar, 0d);
        }
        
        AnchorPane.setBottomAnchor(gridPane, 0d);
        AnchorPane.setTopAnchor(gridPane, (isReadOnly) ? 0d : toolBarHeight);
        AnchorPane.setLeftAnchor(gridPane, 0d);
        AnchorPane.setRightAnchor(gridPane, 0d);

        JMetro j = new JMetro(Style.LIGHT);
        j.setScene(scene);
        stage.setScene(scene);
        // stage.initStyle(StageStyle.UTILITY);
        stage.show();
    }
    
    private void setDimensions() {
        if(currentGridWidth == grid.getSelf()[0].length && currentGridHeight == grid.getSelf().length)
            return;
        
        boolean updateOnWidth = (currentGridWidth == grid.getSelf()[0].length);
        
        currentGridWidth = grid.getSelf()[0].length;
        currentGridHeight = grid.getSelf().length;

        if(!Double.isNaN(stage.getWidth())) stageWidth = (int) stage.getWidth();
        if(!Double.isNaN(stage.getHeight())) stageHeight = (int) stage.getHeight();
        
        if(updateOnWidth) {
            stageWidth = Math.max(stageWidth, currentGridWidth * minCellSize);
            stage.setWidth(stageWidth);
            stage.setHeight((double) stageWidth * currentGridHeight / currentGridWidth + ((isReadOnly) ? 0 : toolBarHeight));
        }
        else {
            stageHeight = Math.max(stageHeight, currentGridHeight * minCellSize + ((isReadOnly) ? 0 : toolBarHeight));
            stage.setHeight(stageHeight);
            stage.setWidth((double) (stageHeight - ((isReadOnly) ? 0 : toolBarHeight)) * currentGridWidth / currentGridHeight);
        }
    }
    
    private void validateGrid() {
        
        boolean hasAZero = false;
        boolean allUniques = true;
        boolean isFilled = true;
        
        var values = new HashSet<Integer>();
        
        for(int row = 0; row < currentGridHeight; row++) {
            for (int col = 0; col < currentGridWidth; col++) {
                if(grid.getSelf()[row][col] == 0) hasAZero = true;
                if(grid.getSelf()[row][col] == -1) {
                    isFilled = false;
                    System.out.println("[" + stageName + "] Empty cell: (" + row + ", " + col + ")");
                } 
                else {
                    if(values.contains(grid.getSelf()[row][col])) {
                        allUniques = false;
                        System.out.println("[" + stageName + "] Duplicate value: (" + row + ", " + col + ")");
                    } 
                    else values.add(grid.getSelf()[row][col]);
                }
            }
        }
        
        if(!hasAZero) System.out.println("[" + stageName + "] No cell with zero value");
        
        if(hasAZero && allUniques && isFilled) {
            if(stageName.equalsIgnoreCase("start")) {
                spaceBuilder.start(grid);
                System.out.println("Successfully set start grid");
            }
            else if(stageName.equalsIgnoreCase("end")) {
                spaceBuilder.end(grid);
                System.out.println("Successfully set end grid");
            }
        }
    }
    
    private void resizeGrid() {
        if(currentGridWidth == newGridWidth && currentGridHeight == newGridHeight)
            return;

        var t = new int[newGridHeight][newGridWidth];
        boolean noZero = true;
        
        for(int row = 0; row < newGridHeight; row++)
            for(int col = 0; col < newGridWidth; col++) 
                if(row < currentGridHeight && col < currentGridWidth) {
                    t[row][col] = grid.getSelf()[row][col];
                    if(t[row][col] == 0) noZero = false;
                }
                else t[row][col] = -1;
                
        // make a setSelf() in grid so we can change the 2d array without creating a new grid?        
        if(noZero){
            int nonZeroValue = t[newGridHeight - 1][newGridWidth - 1];
            t[newGridHeight - 1][newGridWidth - 1] = 0;
            var g = new Grid(t, Grid.EqualPolicy.RANDOM);
            g.getSelf()[newGridHeight - 1][newGridWidth - 1] = nonZeroValue;
            setGrid(g, stageName);
        }
        else setGrid(new Grid(t, Grid.EqualPolicy.RANDOM), stageName);
    }
    
    private void createToolBar() {
        toolBar = new ToolBar();
        toolBar.setMaxHeight(toolBarHeight);

        var widthTF = new TextField();
        widthTF.setPrefSize((stageWidth - 10)/5, toolBarHeight-12);
        widthTF.setTextFormatter(new TextFormatter<String>(integerFilter));
        widthTF.textProperty().addListener(event ->
                newGridWidth = (widthTF.getText().equals("")) ? newGridWidth : Integer.parseInt(widthTF.getText())
        );
        widthTF.setText("" + currentGridWidth);

        var heightTF = new TextField();
        heightTF.setPrefSize((stageWidth - 10)/5, toolBarHeight-12);
        heightTF.setTextFormatter(new TextFormatter<String>(integerFilter));
        heightTF.textProperty().addListener(event ->
                newGridHeight = (heightTF.getText().equals("")) ? newGridHeight : Integer.parseInt(heightTF.getText())
        );
        heightTF.setText("" + currentGridHeight);
        
        var updateBtn = new Button();
        updateBtn.setPrefSize((stageWidth - 10)/5, toolBarHeight-12);
        updateBtn.setText("update size");
        updateBtn.setOnAction(action -> resizeGrid());

        var validateBtn = new Button();
        validateBtn.setPrefSize((stageWidth - 10)/5, toolBarHeight-12);
        validateBtn.setText("validate grid");
        validateBtn.setOnAction(action -> validateGrid());
        
        // rows * columns
        toolBar.getItems().addAll(heightTF, widthTF, updateBtn, validateBtn);
    }
    
    private void createGridPane() {
        
        setDimensions();
        
        stage.setTitle(stageName);
        
        gridPane = new GridPane();
        gridPane.setGridLinesVisible(true);
        
        for(int col = 0; col < currentGridWidth; col++) {
            var colConst = new ColumnConstraints();
            colConst.setPercentWidth(100.0 / currentGridWidth);
            gridPane.getColumnConstraints().add(colConst);
        }
        
        for(int row = 0; row < currentGridHeight; row++) {
            var rowConst = new RowConstraints();
            rowConst.setPercentHeight(100.0 / currentGridHeight);
            gridPane.getRowConstraints().add(rowConst);
        }
        
        refreshGridPane();
    }

    // make it so we reuse existing controls instead of destroying + recreating the pool every time
    private void refreshGridPane() {
        gridPane.getChildren().retainAll(gridPane.getChildren().get(0));
        gridPane.setGridLinesVisible(true);

        for(int row = 0; row < currentGridHeight; row++)
            for(int col = 0; col < currentGridWidth; col++)
                // gridPane indexes are inversed: (col, row)
                if(isReadOnly) gridPane.add(createNewGridLabel(row, col), col, row);
                else gridPane.add(createNewGridTextField(row, col), col, row);
    }
    
    private Label createNewGridLabel(int row, int col) {
        var l = new Label(
            (grid.getSelf()[row][col] != 0) ? 
            String.valueOf(grid.getSelf()[row][col]) : ""
        );
        l.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER);
        return l;
    }
    
    private TextField createNewGridTextField(int row, int col) {
        var tf = new TextField((grid.getSelf()[row][col] < 0) ? "" : String.valueOf(grid.getSelf()[row][col]));
        tf.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        tf.setTextFormatter(new TextFormatter<String>(integerFilter));
        tf.setAlignment(Pos.CENTER);
        tf.textProperty().addListener(event ->
                grid.getSelf()[GridPane.getRowIndex(tf)][GridPane.getColumnIndex(tf)] =
                        (tf.getText().equals("")) ? -1 : Integer.parseInt(tf.getText())
        );
        
        return tf;
    }



    public void setGrid(Grid g, String name) {
        grid = g;
        
        if(!stageName.equals(name)) {
            stageName = name;
            stage.setTitle(name);
        }
        
        if(currentGridHeight == grid.getSelf().length && currentGridWidth == g.getSelf()[0].length)
            refreshGridPane();
        else{
            anchorPane.getChildren().clear();
            createGridPane();
            anchorPane.getChildren().add(gridPane);

            if(!isReadOnly) {
                anchorPane.getChildren().add(toolBar);
                AnchorPane.setTopAnchor(toolBar, 0d);
                AnchorPane.setLeftAnchor(toolBar, 0d);
                AnchorPane.setRightAnchor(toolBar, 0d);
            }

            AnchorPane.setBottomAnchor(gridPane, 0d);
            AnchorPane.setTopAnchor(gridPane, (isReadOnly) ? 0d : toolBarHeight);
            AnchorPane.setLeftAnchor(gridPane, 0d);
            AnchorPane.setRightAnchor(gridPane, 0d);
        }
    }
}
