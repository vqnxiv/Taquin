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
    
    private final SearchRunner searchRunner;
    
    
    public SearchPaneController() {
        searchRunner = SearchRunner.createRunner();
    }
    
    public void initialize() {
        onAddBuilderActivated();
    }
    
    
    @FXML private void onAddBuilderActivated() {
        loader = new FXMLLoader(Taquin.class.getResource("/fxml/primary/builder.fxml"));
        
        try {
            builderVBox.getChildren().add(loader.load());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
