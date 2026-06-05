package com.treble.feasimulation;

import com.treble.feasimulation.model.FEAData;import com.treble.feasimulation.view.MainView;import com.treble.feasimulation.presenter.MainPresenter;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("2D FEA Simulator");

        // Create the view and model, then wire up the presenter so the full FEA UI is available
        MainView view = new MainView(800, 600);
        FEAData model = new FEAData();
        MainPresenter presenter = new MainPresenter(model, view);

        Scene scene = new Scene(view.getRoot(), 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();

        presenter.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
