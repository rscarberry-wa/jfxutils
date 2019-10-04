package org.gillius.jfxutils.examples;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.gillius.jfxutils.JFXUtil;
import org.gillius.jfxutils.chart.ChartPanManager;
import org.gillius.jfxutils.chart.JFXChartUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ScatterChartSelection extends Application {

    @FXML
    private ScatterChart<Number,Number> chart;

    private XYChart.Series<Number, Number> unselected;
    private XYChart.Series<Number, Number> selected;

    private List<XYChart.Data<Number, Number>> data;
    private DataKDTree kdTree;

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

        this.data = unselected.getData().stream().collect(Collectors.toList());
        this.kdTree = DataKDTree.forData(data);

        chart.getData().add(unselected);
        chart.getData().add(selected);

        // Pan either with right mouse button or ctrl-left mouse button.
        ChartPanManager panner = new ChartPanManager(chart);
        panner.setMouseFilter(e -> {
            if (e.getButton() == MouseButton.SECONDARY ||
                    (e.getButton() == MouseButton.SECONDARY && e.isShortcutDown())) {
                return;
            }
            e.consume();
        });
        panner.start();

        // Only zoom with the mouse wheel, consume all mouse events.
        JFXChartUtil.setupSelection(chart, e -> {
            if (e.getButton() != MouseButton.PRIMARY || e.isShortcutDown()) {
                e.consume();
            }
        }, rect -> {

            int[] selectedIndices = kdTree.inside(rect);

            System.out.printf("%d points were in the selection area\n", selectedIndices.length);

        });

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

    public static class DataKDTree {

        private final List<XYChart.Data<Number, Number>> data;

        private int[] nodes;
        private int[] lefts;
        private int[] rights;
        private boolean[] deleted;

        private int count;

        public DataKDTree(List<XYChart.Data<Number, Number>> data) {
            this.data = data;
        }

        public static DataKDTree forData(List<XYChart.Data<Number, Number>> data) {
            DataKDTree kdTree = new DataKDTree(data);
            final int sz = data.size();
            for (int i=0; i<sz; i++) {
                kdTree.insert(i);
            }
            return kdTree;
        }

        public void insert(int ndx) {

            checkNdx(ndx);

            if (count == 0) {
                ensureCapacity(1);
                nodes[0] = ndx;
                count++;
                return;
            }

            int n = 0;
            int level = 0;

            while(true) {
                int curNode = nodes[n];
                if (curNode == ndx) {
                    if (deleted[n]) {
                        deleted[n] = false;
                        return;
                    }
                } else {
                    double value = getDataValue(ndx, level);
                    double nodeValue = getDataValue(curNode, level);
                    if (value > nodeValue) {
                        if (rights[n] == -1) {
                            newNodeOnRight(n, ndx);
                            return;
                        } else {
                            n = rights[n];
                        }
                    } else {
                        if (lefts[n] == -1) {
                            newNodeOnLeft(n, ndx);
                            return;
                        } else {
                            n = lefts[n];
                        }
                    }
                }
                level++;
                if (level == 2) {
                    level = 0;
                }
            }
        }

        public void delete(int ndx) {

            checkNdx(ndx);

            if (count == 0) {
                return;
            }

            int n = 0;
            int level = 0;

            while(true) {
                int curNode = nodes[n];
                if (curNode == ndx) {
                    deleted[n] = true;
                    return;
                } else {
                    double value = getDataValue(ndx, level);
                    double nodeValue = getDataValue(curNode, level);
                    if (value > nodeValue) {
                        if (rights[n] == -1) {
                            return;
                        }
                        n = rights[n];
                    } else {
                        if (lefts[n] == -1) {
                            return;
                        }
                        n = lefts[n];
                    }
                }
                level++;
                if (level == 2) {
                    level = 0;
                }
            }

        }

        public int[] inside(Rectangle2D rect) {

            double minX = rect.getMinX();
            double minY = rect.getMinY();
            double maxX = rect.getMaxX();
            double maxY = rect.getMaxY();

            double midX = (minX + maxX)/2;
            double midY = (minY + maxY)/2;
            double maxXDiff = rect.getWidth()/2;
            double maxYDiff = rect.getHeight()/2;

            List<Integer> indexList = new ArrayList<>();
            rcloseTo(0, midX, midY, MutableRectangle.infiniteRectangle(), maxXDiff, maxYDiff,
                    0, indexList, -1);

            return indexList.stream().mapToInt(Integer::intValue).toArray();
        }

        private void rcloseTo(int curNodeNdx,
                              double targetX,
                              double targetY,
                              MutableRectangle constraintRect,
                              double maxXDiff, double maxYDiff,
                              int level,
                              List<Integer> indexList,
                              int ndxToExclude) {

            int curNode = nodes[curNodeNdx];
            if (curNode == -1) {
                return;
            }

            double currentXValue = getDataValue(curNode, 0);
            double currentYValue = getDataValue(curNode, 1);

            double currentNodeValue = level == 0 ? currentXValue : currentYValue;
            double targetValue = level == 0 ? targetX : targetY;

            boolean targetInLeft = targetValue <= currentNodeValue;

            int nearerNodeNdx = -1;
            int furtherNodeNdx = -1;

            if (targetInLeft) {
                nearerNodeNdx = lefts[curNodeNdx];
                furtherNodeNdx = rights[curNodeNdx];
            } else {
                nearerNodeNdx = rights[curNodeNdx];
                furtherNodeNdx = lefts[curNodeNdx];
            }

            if (nearerNodeNdx >= 0) {
                double oldCoordValue = 0;
                if (targetInLeft) {
                    oldCoordValue = constraintRect.maxCornerValue(level);
                    constraintRect.setMaxCornerValue(level, currentNodeValue);
                } else {
                    oldCoordValue = constraintRect.minCornerValue(level);
                    constraintRect.setMinCornerValue(level, currentNodeValue);
                }

                int newLevel = level + 1;
                if (newLevel == 2) {
                    newLevel = 0;
                }

                rcloseTo(nearerNodeNdx, targetX, targetY, constraintRect,
                        maxXDiff, maxYDiff, newLevel, indexList, ndxToExclude);

                if (targetInLeft) {
                    constraintRect.setMaxCornerValue(level, oldCoordValue);
                } else {
                    constraintRect.setMinCornerValue(level, oldCoordValue);
                }
            }

            if (furtherNodeNdx >= 0) {

                double oldCoordValue = 0;
                if (targetInLeft) {
                    oldCoordValue = constraintRect.minCornerValue(level);
                    constraintRect.setMinCornerValue(level, currentNodeValue);
                } else {
                    oldCoordValue = constraintRect.maxCornerValue(level);
                    constraintRect.setMaxCornerValue(level, currentNodeValue);
                }

                double[] closestPoint = constraintRect.closestPoint(targetX, targetY);

                if (diffsWithinBoundaries(closestPoint[0], closestPoint[1], targetX, targetY, maxXDiff, maxYDiff)) {
                    int newLevel = level + 1;
                    if (newLevel == 2) {
                        newLevel = 0;
                    }
                    rcloseTo(furtherNodeNdx, targetX, targetY, constraintRect, maxXDiff, maxYDiff,
                            newLevel, indexList, ndxToExclude);
                }

                if (targetInLeft) {
                    constraintRect.setMinCornerValue(level, oldCoordValue);
                } else {
                    constraintRect.setMaxCornerValue(level, oldCoordValue);
                }
            }

            if (!deleted[curNodeNdx] && curNodeNdx != ndxToExclude) {
                if (diffsWithinBoundaries(currentXValue, currentYValue, targetX, targetY, maxXDiff, maxYDiff)) {
                    indexList.add(curNode);
                }
            }
        }

        private static boolean diffsWithinBoundaries(
                double x1, double y1, double x2, double y2, double maxXDiff, double maxYDiff) {
            return Math.abs(x1 - x2) <= maxXDiff && Math.abs(y1 - y2) <= maxYDiff;
        }

        private static IllegalArgumentException badDimension(int dim) {
            return new IllegalArgumentException("dim must be 0 or 1: " + dim);
        }

        private void newNodeOnLeft(int parentNdx, int ndx) {
            final int n = count;
            count++;
            ensureCapacity(count);
            nodes[n] = ndx;
            lefts[parentNdx] = n;
        }

        private void newNodeOnRight(int parentNdx, int ndx) {
            final int n = count;
            count++;
            ensureCapacity(count);
            nodes[n] = ndx;
            rights[parentNdx] = n;
        }

        private double getDataValue(int dataIndex, int dimension) {
            XYChart.Data<Number, Number> dataObj = this.data.get(dataIndex);
            if (dimension == 0) {
                return dataObj.getXValue().doubleValue();
            } else if (dimension == 1) {
                return dataObj.getYValue().doubleValue();
            }
            throw new IllegalArgumentException("dimension must be 0 or 1: " + dimension);
        }

        private void ensureCapacity(int minCap) {
            final int curCap = currentCapacity();
            if (curCap < minCap) {
                final int newCap = Math.max(2*curCap, minCap);
                int[] newNodes = new int[newCap];
                int[] newLefts = new int[newCap];
                int[] newRights = new int[newCap];
                boolean[] newDeleted = new boolean[newCap];
                if (curCap > 0) {
                    System.arraycopy(nodes, 0, newNodes, 0, curCap);
                    System.arraycopy(lefts, 0, newLefts, 0, curCap);
                    System.arraycopy(rights, 0, newRights, 0, curCap);
                    System.arraycopy(deleted, 0, newDeleted, 0, curCap);
                }
                Arrays.fill(newNodes, curCap, newCap, -1);
                Arrays.fill(newLefts, curCap, newCap, -1);
                Arrays.fill(newRights, curCap, newCap, -1);
                Arrays.fill(newDeleted, curCap, newCap, false);
                nodes = newNodes;
                lefts = newLefts;
                rights = newRights;
                deleted = newDeleted;
            }
        }

        private int currentCapacity() {
            return nodes != null ? nodes.length : 0;
        }

        private void checkNdx(int ndx) {
            if (ndx < 0 || ndx >= data.size()) {
                throw new IndexOutOfBoundsException(String.format("index not in range [%d - %d]: %d",
                        0, data.size(), ndx));
            }
        }

    }

    public static class MutableRectangle {

        private double minX;
        private double minY;
        private double maxX;
        private double maxY;

        public MutableRectangle(double minX, double minY, double maxX, double maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        public static MutableRectangle infiniteRectangle() {
            return new MutableRectangle(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        public double minCornerValue(int dim) {
            return dim == 0 ? minX : minY;
        }

        public double maxCornerValue(int dim) {
            return dim == 0 ? maxX : maxY;
        }

        public void setMinCornerValue(int dim, double value) {
            if (dim == 0) {
                minX = value;
            } else {
                minY = value;
            }
        }

        public void setMaxCornerValue(int dim, double value) {
            if (dim == 0) {
                maxX = value;
            } else {
                maxY = value;
            }
        }

        public double[] closestPoint(final double x, final double y) {
            double[] closest = new double[2];
            if (x < minX) {
                closest[0] = minX;
            } else if (x > maxX) {
                closest[0] = maxX;
            } else {
                closest[0] = x;
            }
            if (y < minY) {
                closest[1] = minY;
            } else if (y > maxY) {
                closest[1] = maxY;
            } else {
                closest[1] = y;
            }
            return closest;
        }
    }
}
