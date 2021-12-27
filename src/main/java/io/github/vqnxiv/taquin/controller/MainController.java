package io.github.vqnxiv.taquin.controller;


import io.github.vqnxiv.taquin.logger.MainAppender;
import io.github.vqnxiv.taquin.solver.SearchRunner;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MainController {

    private static final Logger LOGGER = LogManager.getLogger();
    
    @FXML private ToolBar topBar;
    @FXML private TopBarController topBarController;
    @FXML private TextArea console;
    @FXML private LogController consoleController;
    @FXML private VBox searchPane;
    @FXML private SearchPaneController searchPaneController;
    @FXML private HBox bottomBar;
    @FXML private BottomBarController bottomBarController;
    
    private final SearchRunner searchRunner;
    
    public MainController() {
        searchRunner = SearchRunner.getRunner();
    }
    
    public void initialize() {
        MainAppender.setLogController(consoleController);
        LOGGER.info("Log Controller setup"); // keep this one here as it serves to tell the appender we good
    }
    
    public void shutdown() {
        LOGGER.info("Shutting down executors");
        searchRunner.shutdown(true);
        bottomBarController.shutdown();
    }
}
