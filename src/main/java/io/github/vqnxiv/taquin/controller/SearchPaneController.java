package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.Taquin;
import io.github.vqnxiv.taquin.solver.SearchRunner;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;


public class SearchPaneController {


    private static final Logger LOGGER = LogManager.getLogger();
    
    @FXML private VBox builderVBox;
    @FXML private Button addBuilderButton;
    
    FXMLLoader loader;
    
    private final SearchRunner searchRunner;
    
    
    public SearchPaneController() {
        LOGGER.info("Creating search pane controller");
        searchRunner = SearchRunner.getRunner();
    }
    
    public void initialize() {
        LOGGER.info("Initializing search pane controller");
        onAddBuilderActivated();
    }
    
    
    @FXML private void onAddBuilderActivated() {
        LOGGER.info("Adding new build controller");

        loader = new FXMLLoader(Taquin.class.getResource("/fxml/primary/builder.fxml"));
        
        try {
            builderVBox.getChildren().add(loader.load());
        } catch(IOException e) {
            LOGGER.error("Could not find FXML file '/fxml/primary/builder.fxml'");
            e.printStackTrace();
        }
    }
}
