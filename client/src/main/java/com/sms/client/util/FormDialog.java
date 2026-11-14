package com.sms.client.util;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.geometry.VPos;
import javafx.stage.Screen;
import javafx.stage.Window;

/**
 * Hộp thoại biểu mẫu. Khi bấm nút lưu: {@code prepare} chạy trên FX thread để đọc/kiểm tra ô nhập
 * (ném IllegalArgumentException để báo lỗi ngay trong hộp thoại), rồi Callable trả về chạy nền.
 * Lỗi từ server hiện trong hộp thoại, giữ nguyên dữ liệu đã nhập.
 */
public final class FormDialog {

    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final GridPane grid = new GridPane();
    private final Label error = new Label();
    private int row;
    private ScrollPane scroll;

    /** Ước lượng phần cố định của hộp thoại ngoài nội dung: thanh tiêu đề + hàng nút Lưu/Hủy + lề. */
    private static final double CHROME_HEIGHT = 120;
    private static final double MIN_WIDTH = 520;

    public FormDialog(String title) {
        dialog.setTitle(title);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        error.getStyleClass().add("error-label");
        error.setVisible(false);
        error.managedProperty().bind(error.visibleProperty());
        error.setMaxWidth(420);
    }

    public TextField text(String label, String initial) {
        TextField field = new TextField(initial == null ? "" : initial);
        field.setPrefColumnCount(22);
        node(label, field);
        return field;
    }

    public <T> ComboBox<T> combo(String label, List<T> items, T selected, Function<T, String> display) {
        ComboBox<T> box = Ui.combo(items, display);
        box.setPrefWidth(300);
        box.getSelectionModel().select(selected);
        node(label, box);
        return box;
    }

    public DatePicker date(String label, LocalDate initial) {
        DatePicker picker = new DatePicker(initial);
        picker.setPrefWidth(300);
        node(label, picker);
        return picker;
    }

    public void node(String label, Node control) {
        Label caption = new Label(label);
        caption.setMinWidth(Region.USE_PREF_SIZE); // không bị rút gọn thành "..."
        GridPane.setValignment(caption, VPos.TOP);
        grid.add(caption, 0, row);
        grid.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
        row++;
    }

    /** Thêm nút chiếm cả hai cột (ví dụ bảng lịch học). */
    public void wide(Node node) {
        grid.add(node, 0, row++, 2, 1);
    }

    public void show(String okText, Supplier<Callable<?>> prepare, Runnable onSaved) {
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        grid.add(error, 0, row, 2, 1);
        // Nội dung nằm trong khung cuộn chỉ để phòng màn hình rất thấp; kích thước thật được đo
        // sau khi hộp thoại hiện ra (lúc đó CSS/skin đã áp dụng nên prefHeight mới chính xác).
        scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        dialog.getDialogPane().setContent(scroll);
        dialog.setOnShown(event -> Platform.runLater(this::fit));
        Node okButton = dialog.getDialogPane().lookupButton(ok);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            Callable<?> work;
            try {
                work = prepare.get();
            } catch (IllegalArgumentException invalid) {
                show(invalid.getMessage());
                return;
            }
            error.setVisible(false);
            AsyncRunner.run(work,
                    done -> {
                        dialog.setResult(ok);
                        dialog.close();
                        onSaved.run();
                    },
                    failure -> show(Alerts.messageOf(failure)),
                    okButton, grid);
        });
        dialog.showAndWait();
    }

    private void show(String message) {
        error.setText(message);
        error.setVisible(true);
        Platform.runLater(this::fit); // thông báo lỗi làm form cao thêm
    }

    /**
     * Cho hộp thoại vừa đúng nội dung: cao đủ để không phải cuộn, tối đa 92% chiều cao vùng làm việc
     * (trừ thanh tiêu đề và hàng nút); chỉ khi vượt quá mới xuất hiện thanh cuộn.
     */
    private void fit() {
        if (scroll == null || scroll.getScene() == null) {
            return;
        }
        double available = Screen.getPrimary().getVisualBounds().getHeight() * 0.92 - CHROME_HEIGHT;
        double width = Math.max(MIN_WIDTH, grid.prefWidth(-1));
        double need = grid.prefHeight(width) + 4;
        scroll.setPrefViewportWidth(width);
        scroll.setPrefViewportHeight(Math.min(need, available));
        scroll.setVbarPolicy(need > available ? ScrollPane.ScrollBarPolicy.AS_NEEDED
                : ScrollPane.ScrollBarPolicy.NEVER);
        Window window = scroll.getScene().getWindow();
        window.sizeToScene();
        window.centerOnScreen();
    }
}
