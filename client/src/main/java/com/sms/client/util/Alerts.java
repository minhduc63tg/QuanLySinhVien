package com.sms.client.util;

import com.sms.client.nav.SceneNavigator;
import com.sms.client.session.SessionHolder;
import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.extern.slf4j.Slf4j;

/** Hộp thoại thông báo; chỉ gọi trên FX thread. */
@Slf4j
public final class Alerts {

    private Alerts() {
    }

    public static void error(String message) {
        show(Alert.AlertType.ERROR, "Lỗi", message);
    }

    public static void info(String message) {
        show(Alert.AlertType.INFORMATION, "Thông báo", message);
    }

    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    /** Xử lý lỗi chung: thông điệp thân thiện, hết phiên thì quay về màn hình đăng nhập. */
    public static void handle(Throwable error) {
        if (error instanceof BusinessException business) {
            if (business.getCode() == ErrorCode.CHUA_DANG_NHAP && SessionHolder.isLoggedIn()) {
                SessionHolder.clear();
                info("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.");
                SceneNavigator.showLogin();
                return;
            }
            error(business.getMessage());
            return;
        }
        log.error("Lỗi không mong muốn ở client", error);
        error("Đã xảy ra lỗi không mong muốn, vui lòng thử lại.");
    }

    /** Thông điệp để hiển thị inline (không mở hộp thoại). */
    public static String messageOf(Throwable error) {
        if (error instanceof BusinessException business) {
            return business.getMessage();
        }
        log.error("Lỗi không mong muốn ở client", error);
        return "Đã xảy ra lỗi không mong muốn, vui lòng thử lại.";
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
