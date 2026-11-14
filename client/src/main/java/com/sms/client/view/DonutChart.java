package com.sms.client.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;

/**
 * Biểu đồ tròn dạng vòng, hai vòng đồng tâm. Vòng ngoài: phần đã hoàn thành rồi phần đang học nối tiếp;
 * vòng trong: một tỉ lệ riêng. Màu lấy từ CSS (.donut-*) nên tự đổi theo theme.
 */
public final class DonutChart extends StackPane {

    private final double size;
    private final double stroke;
    private final Arc outerDone = arc("donut-outer");
    private final Arc outerPending = arc("donut-pending");
    private final Arc inner = arc("donut-inner");
    private final Label mainLabel = new Label();
    private final Label subLabel = new Label();

    public DonutChart(double size, double stroke) {
        this.size = size;
        this.stroke = stroke;
        double c = size / 2;
        double outerRadius = c - stroke / 2;
        double innerRadius = outerRadius - stroke - 6;
        Pane canvas = new Pane();
        canvas.setMinSize(size, size);
        canvas.setPrefSize(size, size);
        canvas.setMaxSize(size, size);
        for (double radius : new double[] {outerRadius, innerRadius}) {
            Circle track = new Circle(c, c, radius);
            track.getStyleClass().add("donut-track");
            track.setStrokeWidth(stroke);
            canvas.getChildren().add(track);
        }
        for (Arc arc : new Arc[] {outerPending, outerDone}) {
            place(arc, c, outerRadius);
            canvas.getChildren().add(arc);
        }
        place(inner, c, innerRadius);
        canvas.getChildren().add(inner);

        mainLabel.getStyleClass().add("donut-main");
        subLabel.getStyleClass().add("donut-sub");
        VBox center = new VBox(2, mainLabel, subLabel);
        center.setAlignment(Pos.CENTER);
        center.setMaxSize(innerRadius * 1.5, innerRadius * 1.5);
        getChildren().addAll(canvas, center);
        setAlignment(Pos.CENTER);
        setMinSize(size, size);
        setMaxSize(size, size);
    }

    private static Arc arc(String styleClass) {
        Arc arc = new Arc();
        arc.getStyleClass().add(styleClass);
        arc.setType(ArcType.OPEN);
        arc.setStrokeLineCap(StrokeLineCap.BUTT);
        return arc;
    }

    private void place(Arc arc, double c, double radius) {
        arc.setCenterX(c);
        arc.setCenterY(c);
        arc.setRadiusX(radius);
        arc.setRadiusY(radius);
        arc.setStrokeWidth(stroke);
        arc.setStartAngle(90);
        arc.setLength(0);
        arc.setVisible(false);
    }

    /**
     * @param done    tỉ lệ hoàn thành của vòng ngoài (0..1)
     * @param pending tỉ lệ đang học nối tiếp phần hoàn thành (0..1, tự cắt cho vừa vòng)
     * @param inner   tỉ lệ của vòng trong (0..1)
     */
    public void setValues(double done, double pending, double inner) {
        done = clamp(done);
        pending = Math.min(clamp(pending), 1 - done);
        set(outerDone, 90, done);
        set(outerPending, 90 - 360 * done, pending);
        set(this.inner, 90, clamp(inner));
    }

    public void setCenterText(String main, String sub) {
        mainLabel.setText(main);
        subLabel.setText(sub);
    }

    public double size() {
        return size;
    }

    private static double clamp(double ratio) {
        return Double.isNaN(ratio) ? 0 : Math.max(0, Math.min(1, ratio));
    }

    private static void set(Arc arc, double startAngle, double ratio) {
        arc.setStartAngle(startAngle);
        arc.setLength(-360 * ratio);
        arc.setVisible(ratio > 0);
    }
}
