module io.github.vqnxiv.taquin {
    
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires org.reflections;
    requires org.jfxtras.styles.jmetro;
    requires java.management;
    requires jdk.management;
    
    
    opens io.github.vqnxiv.taquin.controller;
    opens io.github.vqnxiv.taquin.solver;
    opens io.github.vqnxiv.taquin.model;
    
    exports io.github.vqnxiv.taquin;
    exports io.github.vqnxiv.taquin.controller;
    exports io.github.vqnxiv.taquin.solver;
    exports io.github.vqnxiv.taquin.model;

}