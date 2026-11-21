package com.example.consolestore.Service;

import com.example.consolestore.DTO.CheckoutRequest;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Kiểm tra và chuẩn hóa thông tin nhận hàng ở phía máy chủ.
 *
 * Quy tắc phải khớp với static/js/checkout.js (kiểm tra phía trình duyệt),
 * vì trình duyệt chỉ giúp người dùng nhập nhanh, không thay thế được
 * việc kiểm tra tại đây.
 */
public final class CheckoutValidator {

    public static final int NAME_MAX = 100;
    public static final int PROVINCE_MAX = 40;
    public static final int DISTRICT_MAX = 50;
    public static final int WARD_MAX = 50;
    public static final int STREET_MIN = 5;
    public static final int STREET_MAX = 100;
    public static final int NOTE_MAX = 255;

    // Tối đa 100 + 50 + 50 + 40 + 3 dấu ", " = 246 ký tự, vừa cột diaChi (255).
    public static final int ADDRESS_MAX = 255;

    private static final Set<String> METHODS = Set.of(
            "CHUYEN_KHOAN",
            "MOMO",
            "ZALOPAY",
            "TIEN_MAT"
    );
    // Họ tên: chữ cái (có dấu), khoảng trắng, dấu chấm, nháy đơn, gạch ngang.
    private static final Pattern NAME = Pattern.compile(
            "^\\p{L}[\\p{L}\\p{M}\\s.'-]*$"
    );

    // Đầu số di động Việt Nam: 03x, 05x, 07x, 08x, 09x.
    private static final Pattern PHONE = Pattern.compile(
            "^0(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$"
    );

    // Tên tỉnh/huyện/xã.
    private static final Pattern PLACE = Pattern.compile(
            "^[\\p{L}\\p{N}][\\p{L}\\p{N}\\p{M}\\s.,'()/-]*$"
    );

    // Số nhà, tên đường.
    private static final Pattern STREET = Pattern.compile(
            "^[\\p{L}\\p{N}][\\p{L}\\p{N}\\p{M}\\s.,'()/#-]*$"
    );

    private CheckoutValidator() {
    }

    /**
     * Chuẩn hóa dữ liệu trong form (cắt khoảng trắng, đổi số điện thoại về
     * dạng 0xxxxxxxxx) rồi trả về các lỗi theo từng trường.
     * Khi không có lỗi, form.address được ghép sẵn để lưu vào hóa đơn.
     */
    public static Map<String, String> validate(CheckoutRequest form) {
        Map<String, String> errors = new LinkedHashMap<>();

        form.setFullName(text(form.getFullName()));
        form.setPhone(phone(form.getPhone()));
        form.setProvince(text(form.getProvince()));
        form.setDistrict(text(form.getDistrict()));
        form.setWard(text(form.getWard()));
        form.setStreet(text(form.getStreet()));
        form.setNote(note(form.getNote()));

        String method = form.getPaymentMethod() == null
                ? ""
                : form.getPaymentMethod().strip();

        form.setPaymentMethod(method);

        String name = form.getFullName();

        if (name.isEmpty()) {
            errors.put("fullName", "Vui lòng nhập họ và tên.");
        } else if (name.length() < 2) {
            errors.put("fullName", "Họ và tên quá ngắn.");
        } else if (name.length() > NAME_MAX) {
            errors.put(
                    "fullName",
                    "Họ và tên tối đa " + NAME_MAX + " ký tự."
            );
        } else if (!NAME.matcher(name).matches()) {
            errors.put(
                    "fullName",
                    "Họ và tên chỉ gồm chữ cái và khoảng trắng."
            );
        }

        String phone = form.getPhone();

        if (phone.isEmpty()) {
            errors.put("phone", "Vui lòng nhập số điện thoại.");
        } else if (!PHONE.matcher(phone).matches()) {
            errors.put(
                    "phone",
                    "Số điện thoại không hợp lệ. Ví dụ: 0912345678."
            );
        }

        place(errors, "province", form.getProvince(),
                PROVINCE_MAX, "Tỉnh / Thành phố");

        place(errors, "district", form.getDistrict(),
                DISTRICT_MAX, "Quận / Huyện");

        place(errors, "ward", form.getWard(),
                WARD_MAX, "Phường / Xã");

        String street = form.getStreet();

        if (street.isEmpty()) {
            errors.put(
                    "street",
                    "Vui lòng nhập số nhà, tên đường."
            );
        } else if (street.length() < STREET_MIN) {
            errors.put(
                    "street",
                    "Địa chỉ quá ngắn. Ví dụ: 12 Nguyễn Huệ."
            );
        } else if (street.length() > STREET_MAX) {
            errors.put(
                    "street",
                    "Số nhà, tên đường tối đa " + STREET_MAX + " ký tự."
            );
        } else if (!STREET.matcher(street).matches()) {
            errors.put(
                    "street",
                    "Địa chỉ chứa ký tự không hợp lệ."
            );
        }

        if (form.getNote().length() > NOTE_MAX) {
            errors.put(
                    "note",
                    "Ghi chú tối đa " + NOTE_MAX + " ký tự."
            );
        }

        if (!METHODS.contains(method)) {
            errors.put(
                    "paymentMethod",
                    "Vui lòng chọn phương thức thanh toán."
            );
        }

        if (errors.isEmpty()) {
            form.setAddress(
                    String.join(
                            ", ",
                            form.getStreet(),
                            form.getWard(),
                            form.getDistrict(),
                            form.getProvince()
                    )
            );
        } else {
            form.setAddress("");
        }

        return errors;
    }

    private static void place(
            Map<String, String> errors,
            String key,
            String value,
            int max,
            String label
    ) {
        if (value.isEmpty()) {
            errors.put(key, "Vui lòng chọn " + label + ".");
        } else if (value.length() > max) {
            errors.put(key, label + " tối đa " + max + " ký tự.");
        } else if (!PLACE.matcher(value).matches()) {
            errors.put(key, label + " chứa ký tự không hợp lệ.");
        }
    }

    // Đưa về dạng NFC, gộp khoảng trắng liên tiếp, cắt hai đầu.
    private static String text(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFC)
                .replaceAll("\\s+", " ")
                .strip();
    }

    // Ghi chú được phép xuống dòng nhưng không chứa ký tự điều khiển khác.
    private static String note(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFC)
                .replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]", "")
                .replaceAll("[\\r\\n\\t]+", " ")
                .strip();
    }

    // Bỏ khoảng trắng, dấu chấm, gạch ngang; đổi +84 / 84 thành số 0 đầu.
    private static String phone(String value) {
        if (value == null) {
            return "";
        }

        String digits = value.replaceAll("[\\s.()-]", "");

        if (digits.startsWith("+84")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }

        return digits;
    }
}
