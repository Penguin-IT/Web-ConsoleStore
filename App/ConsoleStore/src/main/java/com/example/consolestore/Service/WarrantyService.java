package com.example.consolestore.Service;

import com.example.consolestore.DTO.*;
import com.example.consolestore.Entity.PhieuBaoHanh;
import com.example.consolestore.Repository.*;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class WarrantyService {

    private static final Set<String> STATUSES = Set.of(
            "TIEP_NHAN",
            "DANG_XU_LY",
            "HOAN_TAT"
    );

    private final PhieuBaoHanhRepository cases;
    private final SerialMayRepository serials;
    private final TaiKhoanRepository accounts;

    public WarrantyService(
            PhieuBaoHanhRepository cases,
            SerialMayRepository serials,
            TaiKhoanRepository accounts
    ) {
        this.cases = cases;
        this.serials = serials;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public Page<PhieuBaoHanhRepository.Row> list(
            String username,
            String keyword,
            String status,
            int page
    ) {
        staff(username);

        if (!status.isEmpty() && !STATUSES.contains(status)) {
            throw bad("Trạng thái không hợp lệ.");
        }

        String query = clean(keyword);

        if (query.length() > 100) {
            throw bad("Từ khóa tối đa 100 ký tự.");
        }

        query = "%"
                + query.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[")
                + "%";

        return cases.search(
                status,
                query,
                PageRequest.of(Math.max(0, page), 10)
        );
    }

    @Transactional(readOnly = true)
    public PhieuBaoHanhRepository.Lookup lookup(
            String username,
            String number
    ) {
        staff(username);

        String value = clean(number);

        if (value.isEmpty()) {
            return null;
        }

        if (value.length() > 100) {
            throw bad("Serial tối đa 100 ký tự.");
        }

        return cases.lookup(value).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<Long> existing(
            String username,
            String token
    ) {
        Long employeeId = staff(username);

        return cases.findByTag(tag(token)).map(receipt -> {
            if (!employeeId.equals(receipt.getEmployeeId())) {
                throw new AccessDeniedException(
                        "Warranty owner required"
                );
            }

            return receipt.getId();
        });
    }

    @Transactional(readOnly = true)
    public WarrantyDetail detail(
            String username,
            Long id
    ) {
        staff(username);

        var receipt = cases.findById(id)
                .orElseThrow(WarrantyService::notFound);

        var serial = serials.findById(receipt.getSerialId())
                .orElseThrow(WarrantyService::notFound);

        var lookup = cases.lookup(serial.getNumber())
                .orElseThrow(WarrantyService::notFound);

        return new WarrantyDetail(
                receipt,
                lookup,
                visibleNotes(receipt.getNotes())
        );
    }

    @Transactional(timeout = 20)
    public Long receive(
            String username,
            WarrantyRequest form
    ) {
        Long employeeId = staff(username);
        String requestTag = tag(form.getToken());

        var old = cases.lockByTag(requestTag);

        if (old.isPresent()) {
            if (!employeeId.equals(old.get().getEmployeeId())) {
                throw new AccessDeniedException(
                        "Warranty owner required"
                );
            }

            return old.get().getId();
        }

        String number = clean(form.getSerial());
        String description = clean(form.getDescription());

        if (number.isEmpty() || number.length() > 100) {
            throw bad("Nhập serial tối đa 100 ký tự.");
        }

        if (description.isEmpty() || description.length() > 255) {
            throw bad(
                    "Mô tả lỗi bắt buộc và tối đa 255 ký tự."
            );
        }

        var serial = serials.lockByNumber(number)
                .orElseThrow(() -> bad(
                        "Không tìm thấy serial."
                ));

        var lookup = cases.lookup(serial.getNumber())
                .orElseThrow(WarrantyService::notFound);

        if (!"DA_BAN".equals(serial.getStatus())
                || lookup.getActiveId() != null) {
            throw bad(
                    "Máy chưa bán hoặc đang có phiếu bảo hành chưa hoàn tất."
            );
        }

        if (!Boolean.TRUE.equals(lookup.getCovered())) {
            throw bad("Máy không còn trong hạn bảo hành.");
        }

        if (form.getDueAt() == null
                || form.getDueAt().isBefore(
                lookup.getServerNow()
        )) {
            throw bad(
                    "Ngày hẹn trả phải từ thời điểm hiện tại trở đi."
            );
        }

        Long id = cases.receive(
                serial.getNumber(),
                employeeId,
                description,
                form.getDueAt()
        );

        if (id == null) {
            throw bad("Chưa tạo được phiếu bảo hành.");
        }

        cases.stamp(
                id,
                employeeId,
                requestTag + "|"
        );

        PhieuBaoHanh created = cases.findById(id)
                .orElseThrow(WarrantyService::notFound);

        if (!created.isCovered()
                || !(requestTag + "|").equals(
                created.getNotes()
        )) {
            throw bad(
                    "Không thể xác nhận hạn bảo hành hoặc phiên tiếp nhận."
            );
        }

        return id;
    }

    @Transactional(timeout = 20)
    public void update(
            String username,
            Long id,
            WarrantyRequest form
    ) {
        staff(username);

        var receipt = cases.lockById(id)
                .orElseThrow(WarrantyService::notFound);

        if ("HOAN_TAT".equals(receipt.getStatus())) {
            throw bad("Phiếu đã hoàn tất, không thể sửa.");
        }

        if (!Objects.equals(
                form.getExpectedStatus(),
                receipt.getStatus()
        )) {
            throw bad(
                    "Phiếu vừa thay đổi trạng thái. Vui lòng tải lại."
            );
        }

        String target = switch (clean(form.getAction())) {
            case "save" -> receipt.getStatus();

            case "start" -> {
                if (!"TIEP_NHAN".equals(receipt.getStatus())) {
                    throw bad(
                            "Phiếu không còn ở bước tiếp nhận."
                    );
                }

                yield "DANG_XU_LY";
            }

            case "complete" -> {
                if (!"DANG_XU_LY".equals(receipt.getStatus())) {
                    throw bad(
                            "Cần chuyển sang đang xử lý trước."
                    );
                }

                yield "HOAN_TAT";
            }

            default -> throw bad("Thao tác không hợp lệ.");
        };

        String notes = clean(form.getNotes());
        String costText = clean(form.getCost());

        if (notes.length() > 180) {
            throw bad(
                    "Nội dung xử lý tối đa 180 ký tự."
            );
        }

        if (!costText.matches("[0-9]{1,18}")) {
            throw bad(
                    "Chi phí phải là số nguyên không âm."
            );
        }
        BigDecimal cost;
        try {
            cost = new BigDecimal(costText).setScale(0, java.math.RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException e) {
            throw bad("Chi phí không hợp lệ.");
        }
        var serial = serials.findById(receipt.getSerialId())
                .orElseThrow(WarrantyService::notFound);

        var lookup = cases.lookup(serial.getNumber())
                .orElseThrow(WarrantyService::notFound);

        if (form.getDueAt() == null) {
            throw bad("Nhập ngày hẹn trả.");
        }

        if (!form.getDueAt().equals(receipt.getDueAt())
                && form.getDueAt().isBefore(
                lookup.getServerNow()
        )) {
            throw bad(
                    "Ngày hẹn trả mới không được ở quá khứ."
            );
        }

        String prefix = tagPrefix(receipt.getNotes());
        if (prefix.isEmpty()) {
            throw bad("Dữ liệu phiếu bị lỗi (thiếu mã phiên). Liên hệ quản trị.");
        }
        cases.updateCase(
                id,
                receipt.getStatus(),
                target,
                prefix + notes,
                cost,
                form.getDueAt()
        );

        if (!target.equals(cases.status(id))) {
            throw bad("Chưa cập nhật được phiếu.");
        }
    }
    private Long staff(String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("Warranty staff access required");
        }

        TaiKhoanRepository.LoginState state = accounts.loginState(username)
                .orElseThrow(() -> new AccessDeniedException(
                        "Warranty staff access required"
                ));

        if (!state.isUsable()
                || !Set.of("ADMIN", "NHANVIEN", "BAOHANH")
                .contains(state.getRole())
                || state.getEmployeeId() == null) {
            throw new AccessDeniedException("Warranty staff access required");
        }

        return state.getEmployeeId();
    }

    private static String tag(String value) {
        try {
            if (value == null
                    || !UUID.fromString(value)
                    .toString()
                    .equals(value)) {
                throw new IllegalArgumentException();
            }

            return "CN12:" + value;
        } catch (IllegalArgumentException exception) {
            throw bad("Phiên tiếp nhận không hợp lệ.");
        }
    }

    private static String tagPrefix(String value) {
        if (value != null
                && value.length() >= 42
                && value.substring(0, 42).matches(
                "^CN12:[0-9a-f-]{36}\\|$"
        )) {
            return value.substring(0, 42);
        }

        return "";
    }

    private static String visibleNotes(String value) {
        String text = clean(value);

        return text.substring(
                tagPrefix(text).length()
        );
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private static IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND
        );
    }

}