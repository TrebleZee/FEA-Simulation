package com.treble.feasimulation.presenter;

import com.treble.feasimulation.model.FEAData;
import com.treble.feasimulation.view.BeamCanvasView;
import com.treble.feasimulation.view.MainView;
import javafx.application.Platform;

public class MainPresenter implements Presenter {
    private final FEAData model;
    private final MainView view;
    private final BeamCanvasView canvasView;

    public MainPresenter(FEAData model, MainView view) {
        this.model = model;
        this.view = view;
        this.canvasView = new BeamCanvasView(view.getCanvas(), model);

        // Wire simple actions
        view.getExitItem().setOnAction(e -> Platform.exit());
        view.getClearItem().setOnAction(e -> canvasView.clear());
    }

    @Override
    public void start() {
        // placeholder: load model, initialize state
    }
}
