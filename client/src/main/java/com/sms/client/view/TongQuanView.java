package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Formatters;
import com.sms.client.util.Ui;
import com.sms.common.dto.SiSoLopHocPhanDTO;
import com.sms.common.dto.ThongKeKhoaDTO;
import com.sms.common.dto.ThongKeTrangThaiDTO;
import java.util.List;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Tổng quan cho quản lý: số sinh viên theo khoa, theo trạng thái và sĩ số các lớp học phần. */
public final class TongQuanView extends VBox {

    private record Stats(List<ThongKeKhoaDTO> khoa, List<ThongKeTrangThaiDTO> trangThai,
            List<SiSoLopHocPhanDTO> siSo) {
    }

    private final TextField hocKy = new TextField(Api.config().hocKyMacDinh());
    private final BarChart<String, Number> khoaChart = chart("Sinh viên theo khoa");
    private final BarChart<String, Number> trangThaiChart = chart("Sinh viên theo trạng thái");
    private final TableView<SiSoLopHocPhanDTO> siSoTable = new TableView<>();
    private final Label tongLabel = new Label();

    public TongQuanView() {
        setSpacing(10);
        hocKy.setPrefColumnCount(12);
        hocKy.setOnAction(event -> reload());
        siSoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        siSoTable.getColumns().add(Ui.col("Mã LHP", SiSoLopHocPhanDTO::maLhp, 120));
        siSoTable.getColumns().add(Ui.col("Môn", SiSoLopHocPhanDTO::tenMon, 240));
        siSoTable.getColumns().add(Ui.col("Học kỳ", SiSoLopHocPhanDTO::hocKy, 120));
        siSoTable.getColumns().add(Ui.col("Sĩ số", s -> s.soLuongDaDk() + "/" + s.soLuongToiDa(), 90));
        siSoTable.setPrefHeight(260);

        HBox charts = new HBox(12, khoaChart, trangThaiChart);
        HBox.setHgrow(khoaChart, Priority.ALWAYS);
        HBox.setHgrow(trangThaiChart, Priority.ALWAYS);
        VBox content = new VBox(10, tongLabel, charts,
                Ui.bar(new Label("Sĩ số lớp học phần - học kỳ:"), hocKy, Ui.button("Xem", this::reload)),
                siSoTable);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().addAll(Ui.title("Tổng quan"), scroll);
        reload();
    }

    private static BarChart<String, Number> chart(String title) {
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(260);
        return chart;
    }

    private void reload() {
        String semester = hocKy.getText() == null ? "" : hocKy.getText().trim();
        AsyncRunner.run(() -> new Stats(
                Api.thongKe(s -> s.soSinhVienTheoKhoa(Api.token())),
                Api.thongKe(s -> s.soSinhVienTheoTrangThai(Api.token())),
                Api.thongKe(s -> s.siSoLopHocPhan(Api.token(), semester))), stats -> {
            fill(khoaChart, stats.khoa().stream()
                    .map(k -> new XYChart.Data<String, Number>(k.tenKhoa(), k.soSinhVien())).toList());
            fill(trangThaiChart, stats.trangThai().stream()
                    .map(t -> new XYChart.Data<String, Number>(Formatters.trangThai(t.trangThai()), t.soSinhVien())).toList());
            long total = stats.trangThai().stream().mapToLong(ThongKeTrangThaiDTO::soSinhVien).sum();
            tongLabel.setText("Tổng số sinh viên: " + total);
            siSoTable.getItems().setAll(stats.siSo());
        }, hocKy);
    }

    private static void fill(BarChart<String, Number> chart, List<XYChart.Data<String, Number>> data) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().addAll(data);
        chart.getData().setAll(List.of(series));
    }
}
