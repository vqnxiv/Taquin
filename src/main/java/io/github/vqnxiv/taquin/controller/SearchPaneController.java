package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.Taquin;
import io.github.vqnxiv.taquin.solver.SearchRunner;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import java.io.IOException;


public class SearchPaneController {


    @FXML private VBox builderVBox;
    @FXML private Button addBuilderButton;
    
    FXMLLoader loader;
    
    private final SearchRunner runner;
    
    
    public SearchPaneController() {
        runner = new SearchRunner();
    }
    
    public SearchRunner getRunner() {
        return runner;
    }
    
    
    @FXML private void onAddBuilderActivated() {
        loader = new FXMLLoader(Taquin.class.getResource("/fxml/primary/builder.fxml"));
        loader.setControllerFactory(SearchBuilderController -> new BuilderController(runner));
        
        try {
            builderVBox.getChildren().add(loader.load());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
