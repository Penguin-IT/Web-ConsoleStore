package com.example.consolestore.Service;

import com.example.consolestore.DTO.AiIntent;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hiểu câu tiếng Việt đơn giản bằng quy tắc, KHÔNG cần AI.
 * Dùng khi chưa có API key hoặc khi gọi AI bị lỗi, nên chat luôn chạy được.
 * Hiểu cả khi khách gõ không dấu: "tay cam ps5 duoi 1 trieu".
 *
 * Tên loại / nền tảng bên dưới phải trùng với bảng LoaiSanPham và NenTang.
 */
public final class AiIntentParser {

    private AiIntentParser() {}

    private static final int FLAGS = Pattern.UNICODE_CHARACTER_CLASS;

    // 2 triệu | 2tr5 | 2,5tr | 500k | 500 nghìn | 1.500.000 | 1500000
    private static final Pattern MONEY = Pattern.compile(
            "(?<![a-z0-9.,])(\\d+(?:[.,]\\d+)*)\\s*(trieu|tr|nghin|ngan|ty|k|m|vnd|dong|d)?(\\d{1,3})?(?![a-z0-9])", FLAGS);

    // 1-3 triệu | 1 đến 3 triệu  (hai số dùng chung đơn vị)
    private static final Pattern RANGE = Pattern.compile(
            "(?<![a-z0-9.,])(\\d+(?:[.,]\\d+)?)\\s*(?:-|den|toi)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|nghin|ngan|k|m)(?![a-z0-9])", FLAGS);

    private static final Pattern MAX_CTX = Pattern.compile(
            "(?<![a-z])(?:duoi|toi da|khong qua|ko qua|re hon|thap hon|nho hon|max|<=|<)\\s*(?:muc|gia|khoang|tam)?\\s*$", FLAGS);
    private static final Pattern MIN_CTX = Pattern.compile(
            "(?<![a-z])(?:tren|toi thieu|it nhat|lon hon|cao hon|min|>=|>|tu|hon)\\s*(?:muc|gia)?\\s*$", FLAGS);

    /** Bỏ dấu tiếng Việt + chữ thường. "Tay cầm" -> "tay cam". */
    static String fold(String s) {
        String t = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return t.toLowerCase(Locale.ROOT);
    }

    public static AiIntent parse(String raw) {
        String text = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT);
        String f = fold(text);

        Long min = null, max = null;

        Matcher range = RANGE.matcher(f);
        if (range.find()) {
            long unit = unitOf(range.group(3));
            long a = toVnd(range.group(1), unit), b = toVnd(range.group(2), unit);
            min = Math.min(a, b);
            max = Math.max(a, b);
        } else {
            long[] bounds = singlePrice(f);
            if (bounds != null) {
                min = bounds[0] < 0 ? null : bounds[0];
                max = bounds[1] < 0 ? null : bounds[1];
            }
        }

        return new AiIntent(min, max, category(f), platform(f), condition(text, f), null, sort(f));
    }

    /* ---------- giá ---------- */

    /** Trả về {min, max}; -1 nghĩa là không giới hạn. null nếu không thấy giá. */
    private static long[] singlePrice(String f) {
        List<Long> values = new ArrayList<>();
        int firstStart = -1;

        Matcher m = MONEY.matcher(f);
        while (m.find()) {
            long v = moneyValue(m);
            if (v < 0) continue;
            if (firstStart < 0) firstStart = m.start();
            values.add(v);
        }
        if (values.isEmpty()) return null;

        if (values.size() >= 2) {                         // "từ 1 triệu đến 3 triệu"
            long a = values.get(0), b = values.get(1);
            return new long[]{Math.min(a, b), Math.max(a, b)};
        }

        long v = values.get(0);
        String before = f.substring(Math.max(0, firstStart - 20), firstStart);

        if (MAX_CTX.matcher(before).find()) return new long[]{-1, v};
        if (MIN_CTX.matcher(before).find()) return new long[]{v, -1};
        // "khoảng 5 triệu", "tầm 5 triệu" hoặc không nói gì -> lấy ±20%
        return new long[]{v * 80 / 100, v * 120 / 100};
    }

    /** Giá trị VND của một lần khớp; -1 nếu không phải tiền (vd "ps5", "256gb"). */
    private static long moneyValue(Matcher m) {
        String num = m.group(1), unitText = m.group(2), frac = m.group(3);
        long unit = unitText == null ? 1 : unitOf(unitText);
        try {
            BigDecimal value;
            if (unit == 1 && num.matches("\\d{1,3}([.,]\\d{3})+")) {
                value = new BigDecimal(num.replaceAll("[.,]", ""));     // 1.500.000
            } else {
                value = new BigDecimal(num.replace(',', '.'));          // 2,5
            }
            value = value.multiply(BigDecimal.valueOf(unit));
            if (frac != null && unit > 1) {                              // 2tr5 = 2,5 triệu
                value = value.add(new BigDecimal(frac)
                        .divide(BigDecimal.TEN.pow(frac.length()))
                        .multiply(BigDecimal.valueOf(unit)));
            }
            long result = value.longValue();
            if (result < 10_000) return -1;                              // "ps5", "4k", "256" không phải giá
            return result <= 10_000_000_000L ? result : -1;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private static long toVnd(String num, long unit) {
        return new BigDecimal(num.replace(',', '.')).multiply(BigDecimal.valueOf(unit)).longValue();
    }

    private static long unitOf(String u) {
        return switch (u) {
            case "trieu", "tr", "m" -> 1_000_000L;
            case "nghin", "ngan", "k" -> 1_000L;
            case "ty" -> 1_000_000_000L;
            default -> 1L;          // vnd, dong, d
        };
    }

    /* ---------- loại, nền tảng, tình trạng, sắp xếp ---------- */

    private static boolean has(String f, String regex) {
        return Pattern.compile(regex, FLAGS).matcher(f).find();
    }

    private static String category(String f) {
        if (has(f, "\\b(tay cam|controller|gamepad|joystick)\\b")) return "Tay cầm";
        if (has(f, "\\b(the nho|memory card|sd card|microsd)\\b")) return "Thẻ nhớ";
        if (has(f, "\\b(phu kien|accessor(y|ies))\\b")) return "Phụ kiện";

        String noConsole = f.replaceAll("may (choi )?game", "may");
        if (has(noConsole, "\\b(dia|tro choi|game)\\b")) return "Đĩa game";
        if (has(noConsole, "\\b(may|console)\\b")) return "Máy console";
        return null;
    }

    private static String platform(String f) {
        if (has(f, "\\b(ps ?[2345]|ps|playstation|play station|sony)\\b")) return "PlayStation";
        if (has(f, "\\b(xbox|x box|series [xs])\\b")) return "Xbox";
        if (has(f, "\\b(switch|nintendo)\\b")) return "Nintendo Switch";
        if (has(f, "\\b(steam|pc)\\b")) return "PC / Steam";
        return null;
    }

    private static String condition(String text, String f) {
        boolean noAccent = text.equals(f);   // khách gõ không dấu thì "moi"/"cu" mới đáng tin
        if (has(text, "(?<!\\p{L})(cũ|đã qua sử dụng|second ?hand|2nd)(?!\\p{L})")
                || (noAccent && has(f, "\\b(cu|da qua su dung)\\b"))) return "CU";
        if (has(text, "(?<!\\p{L})(mới|nguyên seal|chính hãng)(?!\\p{L})")
                || (noAccent && has(f, "\\b(moi|nguyen seal|chinh hang)\\b"))) return "MOI";
        return null;
    }

    private static String sort(String f) {
        if (has(f, "\\b(re nhat|thap nhat|gia thap nhat)\\b")) return "priceAsc";
        if (has(f, "\\b(dat nhat|cao nhat|cao cap nhat|xin nhat|gia cao nhat|mac nhat)\\b")) return "priceDesc";
        return null;
    }
}
