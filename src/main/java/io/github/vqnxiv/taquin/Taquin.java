package io.github.vqnxiv.taquin;


import io.github.vqnxiv.taquin.controller.MainController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;


public class Taquin extends Application {


    // https://sematext.com/blog/log4j2-tutorial/
    // https://github.com/MSPaintIDE/MSPaintIDE/tree/master/src/main/java/com/uddernetworks/mspaint/logging


    public static final int PRIMARY_MIN_WIDTH = 533;
    public static final int PRIMARY_MIN_HEIGHT = 400;
    public static final int AUXILIARY_MIN_WIDTH = 500;
    public static final int AUXILIARY_MIN_HEIGHT = 375;

    public static final String APP_NAME = "Taquin";
    public static final float APP_VERSION = 0.1f;


    private MainController mainController;

    // todo: logger
    private static final Logger LOGGER = LogManager.getLogger();


    public static void main(String[] args) {
        LOGGER.info("Starting application");
        
        launch();
    }


    @Override
    public void start(Stage primaryStage) throws IOException {
        Thread.currentThread().setName("JFX Thread");
        LOGGER.info("Creating main stage");
        
        primaryStage.setTitle(APP_NAME + " " + APP_VERSION);

        primaryStage.setMinWidth(PRIMARY_MIN_WIDTH);
        primaryStage.setMinHeight(PRIMARY_MIN_HEIGHT);

        FXMLLoader fxmlLoader = new FXMLLoader(Taquin.class.getResource("/fxml/primary/main.fxml"));

        Parent root = fxmlLoader.load();

        mainController = fxmlLoader.getController();

        Scene scene = new Scene(root);

        JMetro j = new JMetro(Style.LIGHT);
        j.setScene(scene);

        primaryStage.setScene(scene);

        primaryStage.show();
    }

    @Override
    public void stop() {
        LOGGER.info("Shutting down application");
        mainController.shutdown();
        LOGGER.info("Closing application");
    }

}