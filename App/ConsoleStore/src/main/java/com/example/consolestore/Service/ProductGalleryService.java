package com.example.consolestore.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tạo danh sách nhiều ảnh cho trang chi tiết sản phẩm mà KHÔNG cần đổi cơ sở dữ liệu.
 *
 * Quy ước: ảnh chính nằm ở cột SanPham.hinhAnh (ví dụ SP1.jpg). Muốn thêm ảnh phụ,
 * chỉ cần bỏ file vào static/images/products với tên SP1-2.jpg, SP1-3.jpg, SP1-4.jpg...
 * (cùng tên gốc + "-" + số thứ tự). Thứ tự hiển thị theo số thứ tự.
 */
@Service
public class ProductGalleryService {
    private static final Logger log = LoggerFactory.getLogger(ProductGalleryService.class);

    private static final String URL_PREFIX = "/images/products/";
    private static final String CLASSPATH_DIR = "classpath*:static/images/products/";
    private static final Pattern FILE = Pattern.compile("^(.+)\\.(?i:jpg|jpeg|png|webp|avif|gif)$");
    private static final int MAX_IMAGES = 8;
    private static final long CACHE_MS = 30_000;

    private record Cached(long at, List<String> images) {}

    private record Extra(int order, String name) {}

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    /** primaryPath là đường dẫn ảnh chính (đã qua CatalogService.imagePath). Ảnh chính luôn đứng đầu. */
    public List<String> images(String primaryPath) {
        if (primaryPath == null || !primaryPath.startsWith(URL_PREFIX)) {
            return primaryPath == null ? List.of() : List.of(primaryPath);
        }

        long now = System.currentTimeMillis();
        Cached hit = cache.get(primaryPath);
        if (hit != null && now - hit.at() < CACHE_MS) {
            return hit.images();
        }

        List<String> result = scan(primaryPath);
        cache.put(primaryPath, new Cached(now, result));
        return result;
    }

    private List<String> scan(String primaryPath) {
        String fileName = primaryPath.substring(URL_PREFIX.length());
        Matcher primary = FILE.matcher(fileName);
        List<String> result = new ArrayList<>();
        result.add(primaryPath);

        if (!primary.matches()) {
            return List.copyOf(result);
        }

        String stem = primary.group(1);
        if (stem.matches(".*[*?{}\\[\\]].*")) {
            return List.copyOf(result);
        }

        Pattern extra = Pattern.compile(
                "^" + Pattern.quote(stem) + "-(\\d+)\\.(?i:jpg|jpeg|png|webp|avif|gif)$");
        List<Extra> found = new ArrayList<>();

        try {
            for (Resource resource : resolver.getResources(CLASSPATH_DIR + stem + "-*")) {
                String name = resource.getFilename();
                if (name == null) {
                    continue;
                }
                Matcher m = extra.matcher(name);
                if (m.matches()) {
                    found.add(new Extra(Integer.parseInt(m.group(1)), name));
                }
            }
        } catch (IOException | NumberFormatException exception) {
            log.warn("Cannot read extra images for {}: {}",
                    fileName, exception.getClass().getSimpleName());
        }

        found.stream()
                .sorted(Comparator.comparingInt(Extra::order).thenComparing(Extra::name))
                .map(e -> URL_PREFIX + e.name())
                .distinct()
                .limit(MAX_IMAGES - 1L)
                .forEach(result::add);

        return List.copyOf(result);
    }
}
