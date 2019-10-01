package org.gillius.jfxutils.examples;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.gillius.jfxutils.JFXUtil;

import java.util.Random;

public class ScatterChartSelection extends Application {

    @FXML
    private ScatterChart<Number,Number> chart;

    private XYChart.Series<Number, Number> unselected;
    private XYChart.Series<Number, Number> selected;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ScatterChartSelection.fxml"));
        Region contentRootRegion = (Region) loader.load();

        StackPane root = JFXUtil.createScalePane(contentRootRegion, 960, 540, false);
        Scene scene = new Scene(root, root.getPrefWidth(), root.getPrefHeight());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Scatter Chart Selection Demo");
        primaryStage.show();
    }

    @FXML
    void initialize() {
        unselected = generateSeries(0, 1.0, 0.0, 1.0, 50, 1234L);
        selected = new XYChart.Series<>();

        chart.getData().add(unselected);
        chart.getData().add(selected);
    }

    @FXML
    void resetView() {
    }

    private static XYChart.Series<Number, Number> generateSeries(
            double xMin, double xMax, double yMin, double yMax,
            int numPoints, long randomSeed) {

        final XYChart.Series<Number, Number> result = new XYChart.Series<>();

        final Random random = new Random(randomSeed);
        final double xSpread = xMax - xMin;
        final double ySpread = yMax - yMin;

        for (int i=0; i<numPoints; i++) {
            double x = xMin + random.nextDouble()*xSpread;
            double y = yMin + random.nextDouble()*ySpread;
            result.getData().add(new XYChart.Data<>(x, y));
        }

        return result;
    }
}
