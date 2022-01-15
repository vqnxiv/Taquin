module io.github.vqnxiv.taquin {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.management;
    requires jdk.management;
    requires jdk.unsupported;
    requires jdk.attach;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.openjdk.jol;
    requires org.reflections;
    requires org.jfxtras.styles.jmetro;
    

    opens io.github.vqnxiv.taquin.controller;
    opens io.github.vqnxiv.taquin.solver;
    opens io.github.vqnxiv.taquin.model;
    opens io.github.vqnxiv.taquin.logger;

    exports io.github.vqnxiv.taquin;
    exports io.github.vqnxiv.taquin.controller;
    exports io.github.vqnxiv.taquin.solver;
    exports io.github.vqnxiv.taquin.model;
    exports io.github.vqnxiv.taquin.logger;
}