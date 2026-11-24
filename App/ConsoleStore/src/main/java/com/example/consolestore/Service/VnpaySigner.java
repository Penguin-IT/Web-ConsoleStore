package com.example.consolestore.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Ký và kiểm tra chữ ký theo tài liệu VNPAY (HMAC-SHA512).
 * Class thuần Java, không phụ thuộc Spring để dễ kiểm thử.
 */
public final class VnpaySigner {

    private VnpaySigner() {
    }

    public static String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            ));

            return HexFormat.of().formatHex(
                    mac.doFinal(data.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Chuỗi "key=value&key=value" đã sắp xếp theo tên tham số,
     * bỏ giá trị rỗng, giá trị được URL-encode.
     * Đây vừa là chuỗi để ký, vừa là query string gửi sang VNPAY.
     */
    public static String canonical(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(e -> e.getValue() != null
                        && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "="
                        + URLEncoder.encode(
                        e.getValue(),
                        StandardCharsets.US_ASCII
                ))
                .collect(Collectors.joining("&"));
    }

    /** Tạo URL chuyển khách sang cổng thanh toán. */
    public static String paymentUrl(
            String baseUrl,
            String secret,
            Map<String, String> params
    ) {
        String query = canonical(params);

        return baseUrl + "?" + query
                + "&vnp_SecureHash=" + hmacSha512(secret, query);
    }

    /**
     * Kiểm tra dữ liệu VNPAY trả về (return URL / IPN).
     * Chỉ lấy tham số vnp_*, bỏ vnp_SecureHash và vnp_SecureHashType.
     */
    public static boolean verify(
            Map<String, String> received,
            String secret
    ) {
        String hash = received.get("vnp_SecureHash");

        if (hash == null || hash.isBlank() || secret == null) {
            return false;
        }

        Map<String, String> signed = new TreeMap<>();

        received.forEach((key, value) -> {
            if (key.startsWith("vnp_")
                    && !key.equals("vnp_SecureHash")
                    && !key.equals("vnp_SecureHashType")) {
                signed.put(key, value);
            }
        });

        String expected = hmacSha512(secret, canonical(signed));

        return MessageDigest.isEqual(
                expected.toLowerCase(Locale.ROOT)
                        .getBytes(StandardCharsets.UTF_8),
                hash.strip().toLowerCase(Locale.ROOT)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}
