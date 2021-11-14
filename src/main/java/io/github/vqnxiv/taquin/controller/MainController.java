package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.solver.SearchRunner;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class MainController {
    
    
    @FXML private ToolBar topBar;
    @FXML private TopBarController topBarController;
    @FXML private ScrollPane console;
    @FXML private ConsoleController consoleController;
    @FXML private VBox searchPane;
    @FXML private SearchPaneController searchPaneController;
    @FXML private HBox bottomBar;
    @FXML private BottomBarController bottomBarController;
    
    private final SearchRunner searchRunner;
    
    public MainController() {
        searchRunner = SearchRunner.createRunner();
    }
    
    public void initialize() {
        
    }
    
    public void shutdown() {
        searchRunner.shutdown();
        bottomBarController.shutdown();
    }
}
