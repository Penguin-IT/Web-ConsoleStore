package com.example.consolestore.DTO;

import com.example.consolestore.Entity.SanPham;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public record ProductForm(
        String name,
        String description,
        BigDecimal price,
        String image,
        Long categoryId,
        Long platformId,
        String condition,
        Long baseProductId,
        String version
) {

    public static ProductForm empty() {
        return new ProductForm(
                "",
                "",
                null,
                "",
                null,
                null,
                "MOI",
                null,
                ""
        );
    }

    public static ProductForm from(SanPham product) {
        return new ProductForm(
                product.getTenSP(),
                product.getMoTa(),
                product.getGiaBan(),
                product.getHinhAnh(),
                product.getLoaiSanPham().getId(),
                product.getNenTang() == null
                        ? null
                        : product.getNenTang().getId(),
                product.getTinhTrang(),
                product.getMaSPGoc(),
                versionOf(product)
        );
    }

    public static String versionOf(SanPham product) {
        List<String> values = List.of(
                text(product.getMaSP()),
                text(product.getTenSP()),
                text(product.getMoTa()),
                text(product.getGiaBan()),
                text(product.getHinhAnh()),
                text(product.isDeleted()),
                text(product.getTinhTrang()),
                text(product.getMaSPGoc()),
                text(product.getLoaiSanPham().getId()),
                text(product.getNenTang() == null
                        ? null
                        : product.getNenTang().getId())
        );

        StringBuilder content = new StringBuilder();

        for (String value : values) {
            content.append(value.length())
                    .append(':')
                    .append(value);
        }

        try {
            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            content.toString()
                                    .getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }
}