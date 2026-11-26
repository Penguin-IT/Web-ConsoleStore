package com.example.consolestore.Service;

import com.example.consolestore.DTO.AiChatResponse;
import com.example.consolestore.DTO.AiIntent;
import com.example.consolestore.DTO.CatalogSearch;
import com.example.consolestore.DTO.ProductCard;
import com.example.consolestore.Entity.LoaiSanPham;
import com.example.consolestore.Entity.NenTang;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Nhận câu khách gõ -> hiểu ý -> tìm trong database -> trả lời + danh sách sản phẩm. */
@Service
public class AiShopService {

    private static final int SHOW = 6;

    private static final String HELP = """
            Mình chưa rõ bạn cần gì. Bạn thử nói ngân sách, loại hoặc nền tảng nhé, ví dụ: \
            "tay cầm PS5 dưới 1 triệu", "máy Switch cũ dưới 5 triệu", "đĩa game Xbox".""";

    private final CatalogService catalog;
    private final AiIntentClient ai;

    public AiShopService(CatalogService catalog, AiIntentClient ai) {
        this.catalog = catalog;
        this.ai = ai;
    }

    public AiChatResponse chat(String message) {
        List<String> categories = catalog.categories().stream().map(LoaiSanPham::getName).toList();
        List<String> platforms = catalog.platforms().stream().map(NenTang::getName).toList();

        // 1) Hiểu ý: ưu tiên AI, không được thì dùng quy tắc.
        Optional<AiIntent> fromAi = ai.extract(message, categories, platforms);
        AiIntent intent = fromAi.orElseGet(() -> AiIntentParser.parse(message));
        if (!intent.hasFilter()) intent = AiIntentParser.parse(message);
        if (!intent.hasFilter()) return new AiChatResponse(HELP, List.of(), null);

        // 2) Tìm trong database.
        String note = "";
        List<ProductCard> found = find(intent);

        if (found.isEmpty() && intent.keyword() != null) {
            intent = intent.withoutKeyword();
            found = find(intent);
            note = "Mình chưa thấy đúng tên bạn nhắc, nên lọc theo các điều kiện còn lại. ";
        }

        // 3) Không có món nào trong tầm giá -> gợi ý món gần nhất.
        if (found.isEmpty() && intent.hasPrice()) {
            boolean cheapFirst = intent.maxPrice() != null;
            List<ProductCard> near = find(intent.withoutPrice());
            if (!near.isEmpty()) {
                near = new ArrayList<>(near);
                near.sort(cheapFirst ? byPrice() : byPrice().reversed());
                List<ProductCard> top = near.stream().limit(3).toList();
                return new AiChatResponse(
                        "Chưa có sản phẩm nào trong mức giá đó. Đây là vài lựa chọn gần nhất:",
                        top, moreUrl(intent.withoutPrice()));
            }
        }

        if (found.isEmpty()) {
            return new AiChatResponse(
                    "Mình chưa tìm thấy sản phẩm phù hợp. Bạn thử đổi ngân sách hoặc loại sản phẩm nhé.",
                    List.of(), moreUrl(null));
        }

        // 4) Còn hàng lên trước, rồi theo giá.
        Comparator<ProductCard> price = "priceDesc".equals(intent.sort()) ? byPrice().reversed() : byPrice();
        List<ProductCard> sorted = new ArrayList<>(found);
        sorted.sort(Comparator.comparingInt((ProductCard p) -> p.stock() > 0 ? 0 : 1).thenComparing(price));
        List<ProductCard> shown = sorted.stream().limit(SHOW).toList();

        String reply = note + "Mình tìm được " + found.size() + " sản phẩm (" + describe(intent) + ")"
                + (found.size() > shown.size() ? ", đây là " + shown.size() + " món nổi bật:" : ":");
        return new AiChatResponse(reply, shown, moreUrl(intent));
    }

    private List<ProductCard> find(AiIntent in) {
        CatalogSearch f = new CatalogSearch();
        f.setSize(100);
        f.setSort("priceAsc");
        if (in.category() != null) f.setCategory(in.category());
        if (in.platform() != null) f.setPlatform(in.platform());
        if (in.condition() != null) f.setCondition(in.condition());
        if (in.keyword() != null) f.setQ(in.keyword());
        if (in.minPrice() != null) f.setMinPrice(BigDecimal.valueOf(in.minPrice()));
        if (in.maxPrice() != null) f.setMaxPrice(BigDecimal.valueOf(in.maxPrice()));
        return catalog.search(f).getContent();
    }

    private static Comparator<ProductCard> byPrice() {
        return Comparator.comparing(ProductCard::price);
    }

    private static String describe(AiIntent in) {
        List<String> parts = new ArrayList<>();
        if (in.category() != null) parts.add(in.category());
        if (in.platform() != null) parts.add(in.platform());
        if (in.condition() != null) parts.add("MOI".equals(in.condition()) ? "hàng mới" : "hàng cũ");
        if (in.keyword() != null) parts.add(in.keyword());
        if (in.minPrice() != null && in.maxPrice() != null) {
            parts.add("từ " + vnd(in.minPrice()) + " đến " + vnd(in.maxPrice()));
        } else if (in.maxPrice() != null) {
            parts.add("dưới " + vnd(in.maxPrice()));
        } else if (in.minPrice() != null) {
            parts.add("trên " + vnd(in.minPrice()));
        }
        return String.join(", ", parts);
    }

    private static String vnd(long value) {
        return String.format(Locale.US, "%,d đ", value);     // 2,000,000 đ giống các trang khác
    }

    /** Link sang trang /products với đúng bộ lọc, để khách xem đầy đủ. */
    private static String moreUrl(AiIntent in) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath("/products");
        if (in != null) {
            if (in.category() != null) url.queryParam("category", in.category());
            if (in.platform() != null) url.queryParam("platform", in.platform());
            if (in.condition() != null) url.queryParam("condition", in.condition());
            if (in.keyword() != null) url.queryParam("q", in.keyword());
            if (in.minPrice() != null) url.queryParam("minPrice", in.minPrice());
            if (in.maxPrice() != null) url.queryParam("maxPrice", in.maxPrice());
        }
        return url.build().encode().toUriString();
    }
}
