module com.treble.feasimulation {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    opens com.treble.feasimulation to javafx.fxml;
    opens com.treble.feasimulation.view to javafx.fxml;

    exports com.treble.feasimulation;
    exports com.treble.feasimulation.model;
    exports com.treble.feasimulation.view;
    exports com.treble.feasimulation.presenter;
    exports com.treble.feasimulation.solver;
    exports com.treble.feasimulation.service;
}