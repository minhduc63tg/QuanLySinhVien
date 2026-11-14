package com.sms.client.util;

import com.sms.common.dto.LichHocDTO;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

/** Các hàm dựng giao diện nhỏ dùng chung cho mọi màn hình. */
public final class Ui {

    private Ui() {
    }

    public static <T> TableColumn<T, String> col(String title, Function<T, String> value, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    public static Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-title");
        return label;
    }

    public static HBox bar(Node... nodes) {
        HBox box = new HBox(8, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    public static <T> ComboBox<T> combo(List<T> items, Function<T, String> display) {
        ComboBox<T> box = new ComboBox<>();
        box.getItems().setAll(items);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : display.apply(value);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        });
        return box;
    }

    /** Đọc ô văn bản bắt buộc; ném IllegalArgumentException với thông điệp thân thiện nếu trống. */
    public static String required(TextField field, String label) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " không được để trống.");
        }
        return value;
    }

    public static String optional(TextField field) {
        String value = field.getText() == null ? "" : field.getText().trim();
        return value.isEmpty() ? null : value;
    }

    public static int parseInt(TextField field, String label, int min, int max) {
        try {
            int value = Integer.parseInt(required(field, label));
            if (value < min || value > max) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " phải là số nguyên từ " + min + " đến " + max + ".");
        }
    }

    public static String thu(int thu) {
        return thu == 8 ? "Chủ nhật" : "Thứ " + thu;
    }

    /** Ví dụ "Thứ 2, 11/01/2027" hoặc "Chủ nhật, 17/01/2027". */
    public static String thu(java.time.LocalDate date) {
        int day = date.getDayOfWeek().getValue(); // 1 = thứ hai ... 7 = chủ nhật
        return (day == 7 ? "Chủ nhật" : "Thứ " + (day + 1)) + ", " + Formatters.date(date);
    }

    public static String lich(List<LichHocDTO> lichHoc) {
        if (lichHoc == null || lichHoc.isEmpty()) {
            return "";
        }
        return lichHoc.stream()
                .map(l -> (l.thu() == 8 ? "CN" : "T" + l.thu()) + " tiết " + l.tietBatDau() + "-"
                        + (l.tietBatDau() + l.soTiet() - 1) + " (" + l.phong() + ")")
                .collect(Collectors.joining("; "));
    }
}
