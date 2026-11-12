package com.example.consolestore.Service;

import org.springframework.core.NestedExceptionUtils;

public class NghiepVuException extends RuntimeException {
    public NghiepVuException(String message) { super(message); }

    /** Đổi lỗi SIGNAL của trigger MySQL thành câu dễ hiểu. */
    public static String tuLoiDb(Throwable e) {
        String msg = String.valueOf(NestedExceptionUtils.getMostSpecificCause(e).getMessage());
        if (msg.contains("Khong du ton kho")) return "Có sản phẩm vừa hết hàng, vui lòng kiểm tra lại.";
        if (msg.contains("Serial")) return "Số serial không hợp lệ hoặc đã được bán.";
        return "Thao tác không thực hiện được, vui lòng thử lại.";
    }
}