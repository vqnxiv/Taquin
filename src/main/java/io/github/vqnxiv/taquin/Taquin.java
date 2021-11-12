package io.github.vqnxiv.taquin;


import io.github.vqnxiv.taquin.controller.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import java.io.IOException;


public class Taquin extends Application {
    
    // https://sematext.com/blog/log4j2-tutorial/
    
    
    public static final int PRIMARY_MIN_WIDTH = 533;
    public static final int PRIMARY_MIN_HEIGHT = 400;
    public static final int AUXILIARY_MIN_WIDTH = 500;
    public static final int AUXILIARY_MIN_HEIGHT = 375;

    public static final String APP_NAME = "Taquin";
    public static final float APP_VERSION = 0.1f;
    
    
    private MainController mainController;
    

    public static void main(String[] args) {
        launch();
    }
    

    @Override
    public void start(Stage primaryStage) throws IOException {
        
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
    public void stop() throws Exception {
        mainController.shutdown();
    }

}