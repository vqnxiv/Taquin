package io.github.vqnxiv.taquin.controller;


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
    
    
    public MainController() {
        
    }
    
    public void initialize() {
        
    }
    
    public void shutdown() {
        searchPaneController.getRunner().shutdown();
        bottomBarController.shutdown();
    }
}