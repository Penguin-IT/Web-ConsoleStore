(() => {
    "use strict";

    const showError = (field, message) => {
        field.classList.add("is-invalid");
        field.setAttribute("aria-invalid", "true");
        const error = document.querySelector(`[data-error-for="${field.name}"]`);
        if (error) error.textContent = message;
    };

    const clearError = (field) => {
        field.classList.remove("is-invalid");
        field.removeAttribute("aria-invalid");
        const error = document.querySelector(`[data-error-for="${field.name}"]`);
        if (error) error.textContent = "";
    };

    const validateField = (field) => {
        clearError(field);
        const value = field.value.trim();

        if (field.required && !value) {
            showError(field, "Trường này không được để trống.");
            return false;
        }
        if (!value) return true;

        if (field.name === "serial" && !/^[A-Za-z0-9._/-]{3,100}$/.test(value)) {
            showError(field, "Serial phải từ 3–100 ký tự, chỉ gồm chữ, số và . _ / -.");
            return false;
        }
        if (field.name === "description" && value.length < 5) {
            showError(field, "Mô tả lỗi cần có ít nhất 5 ký tự.");
            return false;
        }
        if (field.name === "dueAt") {
            const chosen = new Date(value);
            if (Number.isNaN(chosen.getTime())) {
                showError(field, "Vui lòng nhập ngày hẹn trả hợp lệ.");
                return false;
            }
            if (chosen.getTime() < Date.now()) {
                showError(field, "Ngày hẹn trả không được ở trong quá khứ.");
                return false;
            }
        }
        if (field.name === "cost") {
            const amount = Number(value);
            if (!Number.isFinite(amount) || amount < 0 || !Number.isInteger(amount)) {
                showError(field, "Chi phí phải là số nguyên không âm.");
                return false;
            }
        }
        if (field.name === "notes" && value.length > 180) {
            showError(field, "Nội dung xử lý không được quá 180 ký tự.");
            return false;
        }
        if (!field.checkValidity()) {
            showError(field, field.validationMessage || "Giá trị nhập chưa hợp lệ.");
            return false;
        }
        return true;
    };

    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll("form.validation-form").forEach((form) => {
            const fields = [...form.querySelectorAll("input, textarea, select")]
                .filter((field) => field.type !== "hidden" && !field.disabled);

            fields.forEach((field) => {
                field.addEventListener("blur", () => validateField(field));
                field.addEventListener("input", () => {
                    if (field.classList.contains("is-invalid")) validateField(field);
                });
                field.addEventListener("change", () => {
                    if (field.classList.contains("is-invalid")) validateField(field);
                });
            });

            form.addEventListener("submit", (event) => {
                // Form tra cứu serial có formnovalidate nên không bị chặn bởi các trường chưa nhập.
                if (event.submitter && event.submitter.hasAttribute("formnovalidate")) return;
                const valid = fields.map(validateField).every(Boolean);
                if (!valid) {
                    event.preventDefault();
                    const firstInvalid = form.querySelector(".is-invalid");
                    if (firstInvalid) firstInvalid.focus();
                    const summary = form.querySelector("[data-validation-summary]");
                    if (summary) {
                        summary.hidden = false;
                        summary.textContent = "Vui lòng kiểm tra và sửa các trường được đánh dấu.";
                    }
                }
            });
        });

        // Khi đổi serial sau tra cứu, không cho gửi thông tin tra cứu cũ.
        const serial = document.getElementById("serial-number");
        const result = document.getElementById("serial-result");
        const receiveButton = document.getElementById("receive-warranty");
        if (serial && receiveButton) {
            const initiallyEnabled = !receiveButton.disabled;
            serial.addEventListener("input", () => {
                const changed = serial.value !== serial.defaultValue;
                if (result) result.hidden = changed;
                receiveButton.disabled = changed || !initiallyEnabled;
            });
        }
    });
})();
