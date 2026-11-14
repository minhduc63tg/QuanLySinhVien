package com.sms.client.view;

import com.sms.client.util.Alerts;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Ui;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Khung bảng + thanh công cụ (tìm kiếm, tải lại, các nút hành động) dùng cho màn hình danh mục. */
public class CrudView<T> extends VBox {

    protected final TableView<T> table = new TableView<>();
    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final FilteredList<T> filtered = new FilteredList<>(items);
    private final HBox toolbar = new HBox(8);
    private final TextField search = new TextField();
    private Function<T, String> searchText = Object::toString;
    private Supplier<Callable<List<T>>> loader;

    protected CrudView(String title) {
        setSpacing(10);
        search.setPromptText("Tìm kiếm...");
        search.textProperty().addListener((obs, old, text) -> {
            String needle = text == null ? "" : text.trim().toLowerCase();
            filtered.setPredicate(item -> needle.isEmpty()
                    || searchText.apply(item).toLowerCase().contains(needle));
        });
        SortedList<T> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        toolbar.getChildren().addAll(search, Ui.button("Tải lại", this::reload));
        getChildren().addAll(Ui.title(title), toolbar, table);
    }

    protected void column(String title, Function<T, String> value, double width) {
        table.getColumns().add(Ui.col(title, value, width));
    }

    protected void searchBy(Function<T, String> text) {
        searchText = text;
    }

    protected void addNode(Node node) {
        toolbar.getChildren().add(node);
    }

    /** Nút thao tác; nếu needsSelection thì yêu cầu chọn một dòng trước. */
    protected Button action(String text, Consumer<T> handler, boolean needsSelection) {
        Button button = Ui.button(text, () -> {
            T selected = table.getSelectionModel().getSelectedItem();
            if (needsSelection && selected == null) {
                Alerts.info("Hãy chọn một dòng trong bảng trước.");
                return;
            }
            handler.accept(selected);
        });
        toolbar.getChildren().add(button);
        return button;
    }

    protected void deleteAction(Function<T, Callable<?>> deleter, Function<T, String> label) {
        action("Xóa", item -> {
            if (Alerts.confirm("Xóa " + label.apply(item) + "?")) {
                AsyncRunner.run(deleter.apply(item), done -> reload(), table);
            }
        }, true);
    }

    /** loader chạy trên FX thread để đọc giá trị ô lọc, rồi trả Callable chạy nền. */
    protected void load(Supplier<Callable<List<T>>> loader) {
        this.loader = loader;
        reload();
    }

    public void reload() {
        if (loader == null) {
            return;
        }
        AsyncRunner.run(loader.get(), list -> items.setAll(list), table);
    }
}
