package com.sms.client.view;

import java.io.ByteArrayInputStream;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/** Khung ảnh hồ sơ chân dung; chưa có ảnh thì hiện chữ cái đầu của họ tên trên nền xám. */
public final class AvatarView extends StackPane {

    private final ImageView imageView = new ImageView();
    private final Label initials = new Label();

    public AvatarView(double width, double height) {
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
        setAlignment(Pos.CENTER);
        getStyleClass().add("avatar-placeholder");
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        initials.getStyleClass().add("avatar-initials");
        initials.setStyle("-fx-font-size: " + Math.max(14, (int) (width / 3.2)) + "px;");
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        setClip(clip);
        getChildren().addAll(initials, imageView);
        setPerson(null, null);
    }

    /** photo null hoặc không giải mã được thì hiện chữ cái đầu của tên. */
    public void setPerson(String hoTen, byte[] photo) {
        Image image = null;
        if (photo != null && photo.length > 0) {
            Image decoded = new Image(new ByteArrayInputStream(photo));
            image = decoded.isError() ? null : decoded;
        }
        imageView.setImage(image);
        imageView.setVisible(image != null);
        initials.setText(initialsOf(hoTen));
        initials.setVisible(image == null);
    }

    static String initialsOf(String hoTen) {
        if (hoTen == null || hoTen.isBlank()) {
            return "?";
        }
        String[] words = hoTen.trim().split("\\s+");
        String last = words[words.length - 1].substring(0, 1);
        String first = words.length > 1 ? words[0].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
