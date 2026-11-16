package com.example.consolestore.Service;

import com.example.consolestore.Dto.ProductForm;
import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.*;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ProductManagementService {

    private final SanPhamRepository products;
    private final LoaiSanPhamRepository categories;
    private final NenTangRepository platforms;
    private final TaiKhoanRepository accounts;

    public ProductManagementService(
            SanPhamRepository products,
            LoaiSanPhamRepository categories,
            NenTangRepository platforms,
            TaiKhoanRepository accounts
    ) {
        this.products = products;
        this.categories = categories;
        this.platforms = platforms;
        this.accounts = accounts;
    }

    public record ProductRow(
            Long id,
            String name,
            String category,
            String platform,
            String condition,
            BigDecimal price,
            String image,
            boolean deleted,
            String version
    ) {
    }

    public record Option(Long id, String name) {
    }

    public Page<ProductRow> list(
            String username,
            String keyword,
            String status,
            String order,
            int page
    ) {
        admin(username);

        String query = clean(keyword);

        if (query.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Từ khóa tối đa 100 ký tự."
            );
        }

        Specification<SanPham> specification =
                (root, criteriaQuery, builder) -> {
                    List<jakarta.persistence.criteria.Predicate>
                            predicates = new ArrayList<>();

                    if ("deleted".equals(status)) {
                        predicates.add(
                                builder.isTrue(root.get("deleted"))
                        );
                    } else if (!"all".equals(status)) {
                        predicates.add(
                                builder.isFalse(root.get("deleted"))
                        );
                    }

                    if (!query.isBlank()) {
                        String pattern = "%"
                                + escape(query.toLowerCase(Locale.ROOT))
                                + "%";

                        predicates.add(
                                builder.like(
                                        builder.lower(root.get("tenSP")),
                                        pattern,
                                        '~'
                                )
                        );
                    }

                    return builder.and(
                            predicates.toArray(
                                    jakarta.persistence.criteria.Predicate[]
                                            ::new
                            )
                    );
                };

        Sort sort = switch (clean(order)) {
            case "name" -> Sort.by("tenSP").ascending()
                    .and(Sort.by("maSP"));
            case "price-asc" -> Sort.by("giaBan").ascending()
                    .and(Sort.by("maSP"));
            case "price-desc" -> Sort.by("giaBan").descending()
                    .and(Sort.by("maSP"));
            default -> Sort.by("maSP").descending();
        };

        return products.findAll(
                specification,
                PageRequest.of(
                        Math.max(0, page),
                        12,
                        sort
                )
        ).map(product -> new ProductRow(
                product.getMaSP(),
                product.getTenSP(),
                product.getLoaiSanPham().getName(),
                product.getNenTang() == null
                        ? "—"
                        : product.getNenTang().getName(),
                "CU".equals(product.getTinhTrang()) ? "Cũ" : "Mới",
                product.getGiaBan(),
                CatalogService.imagePath(product.getHinhAnh()),
                product.isDeleted(),
                ProductForm.versionOf(product)
        ));
    }

    public ProductForm form(String username, Long id) {
        admin(username);

        SanPham product = products
                .findByMaSPAndDeletedFalse(id)
                .orElseThrow(this::notFound);

        return ProductForm.from(product);
    }

    public List<Option> categoryOptions(String username) {
        admin(username);

        return categories.findAll(Sort.by("maLoai"))
                .stream()
                .map(category -> new Option(
                        category.getId(),
                        category.getName()
                ))
                .toList();
    }

    public List<Option> platformOptions(String username) {
        admin(username);

        return platforms.findAll(Sort.by("maNenTang"))
                .stream()
                .map(platform -> new Option(
                        platform.getId(),
                        platform.getName()
                ))
                .toList();
    }

    public List<Option> baseProductOptions(
            String username,
            Long editingId
    ) {
        admin(username);

        return products
                .findByDeletedFalseAndTinhTrangOrderByMaSPAsc("MOI")
                .stream()
                .filter(product -> !Objects.equals(
                        product.getMaSP(),
                        editingId
                ))
                .map(product -> new Option(
                        product.getMaSP(),
                        "SP" + product.getMaSP()
                                + " — " + product.getTenSP()
                ))
                .toList();
    }

    @Transactional(timeout = 20)
    public Long save(
            String username,
            Long id,
            ProductForm form,
            BindingResult errors
    ) {
        admin(username);

        validate(form, errors);

        if (errors.hasErrors()) {
            return null;
        }

        SanPham current = null;

        if (id != null) {
            current = products.lockCatalogProduct(id)
                    .orElseThrow(this::notFound);

            if (current.isDeleted()) {
                throw new IllegalArgumentException(
                        "Sản phẩm đã bị xóa."
                );
            }

            if (!Objects.equals(
                    ProductForm.versionOf(current),
                    form.version()
            )) {
                errors.reject(
                        "product.changed",
                        "Sản phẩm đã được cập nhật. "
                                + "Hãy mở lại trang chỉnh sửa."
                );

                return null;
            }
        }

        LoaiSanPham category = categories
                .findById(form.categoryId())
                .orElse(null);

        NenTang platform = form.platformId() == null
                ? null
                : platforms.findById(form.platformId()).orElse(null);

        if (category == null) {
            errors.rejectValue(
                    "categoryId",
                    "category.invalid",
                    "Loại sản phẩm không tồn tại."
            );
        }

        if (form.platformId() != null && platform == null) {
            errors.rejectValue(
                    "platformId",
                    "platform.invalid",
                    "Nền tảng không tồn tại."
            );
        }

        if (errors.hasErrors()) {
            return null;
        }

        if ("Đĩa game".equalsIgnoreCase(category.getName())
                && !"MOI".equals(form.condition())) {
            errors.rejectValue(
                    "condition",
                    "condition.disc",
                    "Đĩa game chỉ được khai báo là hàng mới."
            );
        }

        List<SanPham> children = id == null
                ? List.of()
                : products.findByMaSPGoc(id);

        if (current != null) {
            boolean structuralChange =
                    !Objects.equals(
                            current.getLoaiSanPham().getId(),
                            form.categoryId()
                    )
                            || !Objects.equals(
                            current.getNenTang() == null
                                    ? null
                                    : current.getNenTang().getId(),
                            form.platformId()
                    )
                            || !Objects.equals(
                            current.getTinhTrang(),
                            form.condition()
                    )
                            || !Objects.equals(
                            current.getMaSPGoc(),
                            form.baseProductId()
                    );

            if (structuralChange
                    && (products.hasBusinessReferences(id) == 1
                    || !children.isEmpty())) {
                errors.reject(
                        "product.structure",
                        "Sản phẩm đã có dữ liệu kho, giao dịch "
                                + "hoặc mã hàng cũ liên kết. "
                                + "Không thể đổi loại, nền tảng, "
                                + "tình trạng hoặc sản phẩm gốc."
                );
            }
        }

        for (SanPham child : children) {
            if (form.price().compareTo(child.getGiaBan()) <= 0) {
                errors.rejectValue(
                        "price",
                        "price.base",
                        "Giá hàng mới phải cao hơn giá "
                                + "của mã hàng cũ liên kết."
                );

                break;
            }
        }

        if (form.baseProductId() != null) {
            validateBaseProduct(id, form, errors);
        }

        if (errors.hasErrors()) {
            return null;
        }

        if (current == null) {
            current = new SanPham(
                    clean(form.name()),
                    nullable(form.description()),
                    form.price(),
                    nullable(form.image()),
                    form.condition(),
                    form.baseProductId(),
                    category,
                    platform
            );
        } else {
            current.updateCatalog(
                    clean(form.name()),
                    nullable(form.description()),
                    form.price(),
                    nullable(form.image()),
                    form.condition(),
                    form.baseProductId(),
                    category,
                    platform
            );
        }

        return products.saveAndFlush(current).getMaSP();
    }

    @Transactional(timeout = 20)
    public void delete(
            String username,
            Long id,
            String version
    ) {
        admin(username);

        SanPham product = products.lockCatalogProduct(id)
                .orElseThrow(this::notFound);

        if (product.isDeleted()) {
            return;
        }

        if (!Objects.equals(
                ProductForm.versionOf(product),
                version
        )) {
            throw new IllegalArgumentException(
                    "Sản phẩm đã được cập nhật. "
                            + "Hãy tải lại danh sách trước khi xóa."
            );
        }

        boolean activeChild = products.findByMaSPGoc(id)
                .stream()
                .anyMatch(child -> !child.isDeleted());

        if (activeChild) {
            throw new IllegalArgumentException(
                    "Hãy xóa mềm mã hàng cũ liên kết trước."
            );
        }

        product.markDeleted();

        products.flush();
    }

    @Transactional(timeout = 20)
    public void saveCategory(
            String username,
            Long id,
            String name
    ) {
        admin(username);

        String value = validateName(name);

        LoaiSanPham duplicate = categories
                .findByTenLoai(value)
                .orElse(null);

        if (duplicate != null
                && !Objects.equals(duplicate.getId(), id)) {
            throw new IllegalArgumentException(
                    "Tên loại sản phẩm đã tồn tại."
            );
        }

        if (id == null) {
            categories.saveAndFlush(new LoaiSanPham(value));
            return;
        }

        LoaiSanPham category = categories.lockById(id)
                .orElseThrow(this::notFound);

        if (!Objects.equals(category.getName(), value)
                && (reservedCategory(category.getName())
                || reservedCategory(value))) {
            throw new IllegalArgumentException(
                    "Không đổi tên loại Máy console và Đĩa game."
            );
        }

        category.rename(value);

        categories.flush();
    }

    @Transactional(timeout = 20)
    public void deleteCategory(
            String username,
            Long id
    ) {
        admin(username);

        LoaiSanPham category = categories.lockById(id)
                .orElseThrow(this::notFound);

        if (reservedCategory(category.getName())) {
            throw new IllegalArgumentException(
                    "Không thể xóa loại Máy console và Đĩa game."
            );
        }

        if (products.existsByLoaiSanPham_MaLoai(id)) {
            throw new IllegalArgumentException(
                    "Loại sản phẩm đang được sử dụng."
            );
        }

        categories.delete(category);
        categories.flush();
    }

    @Transactional(timeout = 20)
    public void savePlatform(
            String username,
            Long id,
            String name
    ) {
        admin(username);

        String value = validateName(name);

        NenTang duplicate = platforms
                .findByTenNenTang(value)
                .orElse(null);

        if (duplicate != null
                && !Objects.equals(duplicate.getId(), id)) {
            throw new IllegalArgumentException(
                    "Tên nền tảng đã tồn tại."
            );
        }

        if (id == null) {
            platforms.saveAndFlush(new NenTang(value));
            return;
        }

        NenTang platform = platforms.lockById(id)
                .orElseThrow(this::notFound);

        platform.rename(value);

        platforms.flush();
    }

    @Transactional(timeout = 20)
    public void deletePlatform(
            String username,
            Long id
    ) {
        admin(username);

        NenTang platform = platforms.lockById(id)
                .orElseThrow(this::notFound);

        if (products.existsByNenTang_MaNenTang(id)) {
            throw new IllegalArgumentException(
                    "Nền tảng đang được sử dụng."
            );
        }

        platforms.delete(platform);
        platforms.flush();
    }

    private void validateBaseProduct(
            Long id,
            ProductForm form,
            BindingResult errors
    ) {
        if (!"CU".equals(form.condition())
                || Objects.equals(id, form.baseProductId())) {
            errors.rejectValue(
                    "baseProductId",
                    "base.invalid",
                    "Chỉ hàng cũ được liên kết với "
                            + "một sản phẩm mới khác."
            );

            return;
        }

        SanPham base = products
                .lockCatalogProduct(form.baseProductId())
                .orElse(null);

        if (base == null
                || base.isDeleted()
                || !"MOI".equals(base.getTinhTrang())) {
            errors.rejectValue(
                    "baseProductId",
                    "base.missing",
                    "Sản phẩm gốc phải là hàng mới đang hoạt động."
            );

            return;
        }

        Long basePlatform = base.getNenTang() == null
                ? null
                : base.getNenTang().getId();

        if (!Objects.equals(
                base.getLoaiSanPham().getId(),
                form.categoryId()
        ) || !Objects.equals(
                basePlatform,
                form.platformId()
        )) {
            errors.rejectValue(
                    "baseProductId",
                    "base.different",
                    "Hàng cũ phải cùng loại và nền tảng "
                            + "với sản phẩm gốc."
            );
        }

        if (form.price().compareTo(base.getGiaBan()) >= 0) {
            errors.rejectValue(
                    "price",
                    "price.used",
                    "Giá hàng cũ phải thấp hơn giá hàng mới."
            );
        }

        boolean duplicate = products
                .findByMaSPGoc(form.baseProductId())
                .stream()
                .anyMatch(product -> !Objects.equals(
                        product.getMaSP(),
                        id
                ));

        if (duplicate) {
            errors.rejectValue(
                    "baseProductId",
                    "base.duplicate",
                    "Sản phẩm gốc đã có mã hàng cũ liên kết."
            );
        }
    }

    private void validate(
            ProductForm form,
            BindingResult errors
    ) {
        String name = clean(form.name());

        if (name.isBlank() || name.length() > 100) {
            errors.rejectValue(
                    "name",
                    "name.invalid",
                    "Tên sản phẩm phải có từ 1 đến 100 ký tự."
            );
        }

        if (clean(form.description()).length() > 255) {
            errors.rejectValue(
                    "description",
                    "description.length",
                    "Mô tả tối đa 255 ký tự."
            );
        }

        BigDecimal price = form.price();

        if (price == null
                || price.signum() <= 0
                || price.stripTrailingZeros().scale() > 0
                || price.compareTo(
                new BigDecimal("1000000000000000000")
        ) >= 0) {
            errors.rejectValue(
                    "price",
                    "price.invalid",
                    "Giá bán phải là số nguyên dương, tối đa 18 chữ số."
            );
        }

        String image = clean(form.image());

        if (!image.isBlank()
                && !image.matches("SP[1-9][0-9]*\\.jpg")) {
            errors.rejectValue(
                    "image",
                    "image.invalid",
                    "Tên ảnh phải có dạng SP1.jpg, SP2.jpg..."
            );
        }

        if (form.categoryId() == null) {
            errors.rejectValue(
                    "categoryId",
                    "category.required",
                    "Hãy chọn loại sản phẩm."
            );
        }

        if (!Set.of("MOI", "CU").contains(
                clean(form.condition())
        )) {
            errors.rejectValue(
                    "condition",
                    "condition.invalid",
                    "Tình trạng không hợp lệ."
            );
        }
    }

    private String validateName(String name) {
        String value = clean(name);

        if (value.isBlank() || value.length() > 100) {
            throw new IllegalArgumentException(
                    "Tên phải có từ 1 đến 100 ký tự."
            );
        }

        return value;
    }

    private boolean reservedCategory(String name) {
        return "Máy console".equalsIgnoreCase(name)
                || "Đĩa game".equalsIgnoreCase(name);
    }

    private void admin(String username) {
        if (username == null
                || accounts.paymentAdmin(username).isEmpty()) {
            throw new AccessDeniedException(
                    "Bạn không có quyền quản lý sản phẩm."
            );
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy dữ liệu."
        );
    }

    private static String escape(String value) {
        return value.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[");
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static String nullable(String value) {
        String result = clean(value);

        return result.isBlank() ? null : result;
    }
}