package io.github.vqnxiv.taquin.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class LogController {
    
    @FXML
    private TextArea logOutput;
    
    
    public LogController() {}
    
    @FXML
    public void initialize() {}
    
    
    public TextArea getOutput() {
        return logOutput;
    }
}
