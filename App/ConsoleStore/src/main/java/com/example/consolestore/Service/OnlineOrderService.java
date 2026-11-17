package com.example.consolestore.Service;

import com.example.consolestore.Dto.OnlineOrderData;
import com.example.consolestore.Entity.HoaDon;
import com.example.consolestore.Entity.SerialMay;
import com.example.consolestore.Repository.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class OnlineOrderService {

    private static final Set<String> STATUSES = Set.of(
            "CHO_XAC_NHAN",
            "DA_XAC_NHAN",
            "DANG_GIAO",
            "HOAN_TAT",
            "HUY"
    );

    private final HoaDonRepository invoices;
    private final SerialMayRepository serials;
    private final TaiKhoanRepository accounts;

    public OnlineOrderService(
            HoaDonRepository invoices,
            SerialMayRepository serials,
            TaiKhoanRepository accounts
    ) {
        this.invoices = invoices;
        this.serials = serials;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public Page<HoaDonRepository.OnlineRow> list(
            String username,
            String status,
            String keyword,
            int page
    ) {
        staff(username);

        if (!status.isEmpty() && !STATUSES.contains(status)) {
            throw bad("Trạng thái không hợp lệ.");
        }

        String query = keyword.strip();

        if (query.length() > 100) {
            throw bad("Từ khóa tối đa 100 ký tự.");
        }

        query = "%"
                + query.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[")
                + "%";

        return invoices.onlineOrders(
                status,
                query,
                PageRequest.of(Math.max(0, page), 10)
        );
    }

    @Transactional(readOnly = true)
    public OnlineOrderData.Detail detail(
            String username,
            Long id
    ) {
        staff(username);

        HoaDon order = invoices.findById(id)
                .filter(invoice -> "ONLINE".equals(invoice.getChannel()))
                .orElseThrow(OnlineOrderService::notFound);

        var lines = invoices.onlineLines(id);

        Map<Long, List<SerialMay>> choices = new HashMap<>();
        Map<Long, List<SerialMay>> byWarehouse = new HashMap<>();

        for (var line : lines) {
            List<SerialMay> available = List.of();

            if ("DA_XAC_NHAN".equals(order.getStatus())
                    && Boolean.TRUE.equals(line.getMachine())
                    && line.getSerialId() == null
                    && line.getWarehouseId() != null) {

                available = byWarehouse.computeIfAbsent(
                        line.getWarehouseId(),
                        serials::available
                ).stream().filter(serial ->
                        Objects.equals(
                                serial.getProductId(),
                                line.getProductId()
                        )
                                && Objects.equals(
                                serial.getCondition(),
                                line.getCondition()
                        )
                ).toList();
            }

            choices.put(line.getId(), available);
        }

        return new OnlineOrderData.Detail(
                order,
                invoices.payment(id).orElse(null),
                lines,
                choices
        );
    }

    @Transactional(timeout = 20)
    public void transition(
            String username,
            Long id,
            String action
    ) {
        Long employeeId = staff(username);

        HoaDon order = invoices.lockOnlineOrder(id)
                .orElseThrow(OnlineOrderService::notFound);

        String expected;
        String target;

        switch (action) {
            case "confirm" -> {
                expected = "CHO_XAC_NHAN";
                target = "DA_XAC_NHAN";
            }

            case "ship" -> {
                expected = "DA_XAC_NHAN";
                target = "DANG_GIAO";
            }

            case "complete" -> {
                expected = "DANG_GIAO";
                target = "HOAN_TAT";
            }

            case "cancel" -> {
                expected = "CHO_XAC_NHAN";
                target = "HUY";
            }

            default -> throw bad("Thao tác không hợp lệ.");
        }

        if (target.equals(order.getStatus())) {
            return;
        }

        if (!expected.equals(order.getStatus())) {
            throw bad(
                    "Đơn đã thay đổi trạng thái. Vui lòng kiểm tra lại."
            );
        }

        var payment = invoices.lockOrderPayment(id).orElse(null);

        if ("cancel".equals(action)) {
            if (payment != null
                    && !"CHUA_THANH_TOAN".equals(payment.getStatus())) {
                throw bad(
                        "Đơn đã thanh toán không thể hủy tại đây."
                );
            }
        } else {
            if (payment == null
                    || !"DA_THANH_TOAN".equals(payment.getStatus())
                    || payment.getAmount() == null
                    || payment.getAmount().compareTo(
                    order.getTotalAmount()
            ) != 0) {
                throw bad(
                        "Đơn chưa được xác nhận thanh toán đủ."
                );
            }

            var lines = invoices.onlineLines(id);

            if (lines.isEmpty()) {
                throw bad("Đơn chưa có sản phẩm.");
            }

            if (!"confirm".equals(action)
                    && lines.stream().anyMatch(line ->
                    Boolean.TRUE.equals(line.getMachine())
                            && (
                            line.getSerialId() == null
                                    || !Integer.valueOf(1).equals(
                                    line.getQuantity()
                            )
                    )
            )) {
                throw bad(
                        "Cần gán đủ serial máy trước khi giao hàng."
                );
            }
        }

        invoices.changeOnlineStatus(
                id,
                expected,
                target,
                employeeId
        );

        if (!target.equals(invoices.onlineStatus(id))) {
            throw bad("Không thể cập nhật trạng thái đơn.");
        }
    }

    @Transactional(timeout = 20)
    public void assign(
            String username,
            Long id,
            Long lineId,
            Long serialId
    ) {
        staff(username);

        if (lineId == null || serialId == null || serialId < 1) {
            throw bad("Vui lòng chọn serial.");
        }

        HoaDon order = invoices.lockOnlineOrder(id)
                .orElseThrow(OnlineOrderService::notFound);

        if (!"DA_XAC_NHAN".equals(order.getStatus())) {
            throw bad(
                    "Chỉ gán serial khi đơn đã xác nhận."
            );
        }

        var lines = invoices.onlineLines(id);

        var line = lines.stream()
                .filter(item -> item.getId().equals(lineId))
                .findFirst()
                .orElseThrow(OnlineOrderService::notFound);

        if (!Boolean.TRUE.equals(line.getMachine())
                || !Integer.valueOf(1).equals(line.getQuantity())) {
            throw bad(
                    "Dòng sản phẩm không phù hợp để gán serial."
            );
        }

        if (line.getSerialId() != null) {
            if (line.getSerialId().equals(serialId)) {
                return;
            }

            throw bad("Dòng sản phẩm đã có serial.");
        }

        SerialMay serial = serials.lockIds(
                List.of(serialId)
        ).stream().findFirst().orElseThrow(
                () -> bad("Không tìm thấy serial.")
        );

        if (!"TRONG_KHO".equals(serial.getStatus())
                || !Objects.equals(
                serial.getProductId(),
                line.getProductId()
        )
                || !Objects.equals(
                serial.getWarehouseId(),
                line.getWarehouseId()
        )
                || line.getWarehouseId() == null
                || !Objects.equals(
                serial.getCondition(),
                line.getCondition()
        )
                || lines.stream().anyMatch(item ->
                serialId.equals(item.getSerialId())
        )) {
            throw bad(
                    "Serial không còn khả dụng cho sản phẩm và kho này."
            );
        }

        invoices.assignOnlineSerial(
                id,
                lineId,
                serialId
        );

        boolean assigned = invoices.onlineLines(id)
                .stream()
                .anyMatch(item ->
                        item.getId().equals(lineId)
                                && serialId.equals(item.getSerialId())
                );

        if (!assigned) {
            throw bad(
                    "Chưa gán được serial. Vui lòng tải lại."
            );
        }
    }

    private Long staff(String username) {
        return accounts.posStaff(username)
                .map(row -> row.getEmployeeId())
                .orElseThrow(
                        () -> new AccessDeniedException(
                                "Staff access required"
                        )
                );
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