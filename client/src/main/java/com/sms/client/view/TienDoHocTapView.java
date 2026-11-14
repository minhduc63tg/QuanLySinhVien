package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Ui;
import com.sms.common.dto.TienDoHocTapDTO;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/** Tiến độ học tập của sinh viên so với chương trình khung của khoa, dạng biểu đồ vòng. */
public final class TienDoHocTapView extends VBox {

    private final DonutChart chart = new DonutChart(260, 20);
    private final VBox legend = new VBox(12);
    private final Label khoaLabel = new Label();
    private final Label emptyLabel = new Label("Khoa của bạn chưa có chương trình khung nên chưa tính được tiến độ.");
    private final HBox body = new HBox(40);

    public TienDoHocTapView() {
        setSpacing(12);
        khoaLabel.getStyleClass().add("user-name");
        emptyLabel.setWrapText(true);
        emptyLabel.setVisible(false);
        emptyLabel.managedProperty().bind(emptyLabel.visibleProperty());

        VBox chartBox = new VBox(10, chart);
        chartBox.setAlignment(Pos.CENTER);
        chartBox.setPadding(new Insets(12));
        legend.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(legend, Priority.ALWAYS);
        body.setAlignment(Pos.CENTER_LEFT);
        body.getChildren().addAll(chartBox, legend);
        body.setVisible(false);
        body.managedProperty().bind(body.visibleProperty());

        getChildren().addAll(Ui.title("Tiến độ học tập"), khoaLabel, emptyLabel, body);
        chart.setCenterText("...", "tín chỉ");
        AsyncRunner.run(() -> Api.chuongTrinhKhung(s -> s.layTienDoCuaToi(Api.token())), this::show);
    }

    private void show(TienDoHocTapDTO progress) {
        khoaLabel.setText("Khoa: " + progress.chuongTrinh().tenKhoa());
        boolean hasProgram = progress.tinChiYeuCau() > 0;
        emptyLabel.setVisible(!hasProgram);
        body.setVisible(hasProgram);
        if (!hasProgram) {
            return;
        }
        double required = progress.tinChiYeuCau();
        double mandatory = Math.max(1, progress.tinChiBatBuocYeuCau());
        chart.setValues(progress.tinChiTichLuy() / required, progress.tinChiDangHoc() / required,
                progress.tinChiBatBuocYeuCau() == 0 ? 0 : progress.tinChiBatBuocTichLuy() / mandatory);
        chart.setCenterText(progress.tinChiTichLuy() + "/" + progress.tinChiYeuCau(), "tín chỉ tích lũy");

        int remaining = Math.max(0, progress.tinChiYeuCau() - progress.tinChiTichLuy() - progress.tinChiDangHoc());
        legend.getChildren().setAll(
                row("donut-dot-outer", "Tín chỉ đã tích lũy",
                        progress.tinChiTichLuy() + " / " + progress.tinChiYeuCau() + " ("
                                + percent(progress.tinChiTichLuy(), progress.tinChiYeuCau()) + ")"),
                row("donut-dot-pending", "Đang học (chưa có điểm)", progress.tinChiDangHoc() + " tín chỉ"),
                row("donut-dot-track", "Còn lại để tốt nghiệp", remaining + " tín chỉ"),
                row("donut-dot-inner", "Học phần bắt buộc đã đạt (vòng trong)",
                        progress.tinChiBatBuocTichLuy() + " / " + progress.tinChiBatBuocYeuCau() + " ("
                                + percent(progress.tinChiBatBuocTichLuy(), progress.tinChiBatBuocYeuCau()) + ")"));
    }

    private static String percent(int part, int whole) {
        return whole == 0 ? "0%" : String.format(Locale.ROOT, "%.1f%%", 100.0 * part / whole);
    }

    private static Node row(String dotStyle, String title, String value) {
        Circle dot = new Circle(7);
        dot.getStyleClass().add(dotStyle);
        Label name = new Label(title);
        Label amount = new Label(value);
        amount.getStyleClass().add("user-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(10, dot, name, spacer, amount);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(520);
        return box;
    }
}
