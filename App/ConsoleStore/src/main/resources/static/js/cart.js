/*
 * Giỏ hàng: nút tăng/giảm số lượng, tự lưu khi đổi số lượng, xác nhận khi xóa.
 * Khi tắt JavaScript, trang vẫn dùng được với nút "Cập nhật" của từng dòng.
 */
(function () {
    'use strict';

    document.documentElement.classList.add('has-js');

    var SAVE_DELAY_MS = 600;

    function clamp(input) {
        var min = Number(input.min) || 1;
        var max = Number(input.max);
        var value = parseInt(input.value, 10);

        if (!Number.isFinite(value) || value < min) {
            value = min;
        }

        if (Number.isFinite(max) && max > 0 && value > max) {
            value = max;
        }

        input.value = String(value);

        return value;
    }

    function refreshButtons(form) {
        var input = form.querySelector('input[name="quantity"]');
        var minus = form.querySelector('[data-qty-step="-1"]');
        var plus = form.querySelector('[data-qty-step="1"]');

        if (!input || input.disabled) {
            return;
        }

        var value = Number(input.value);
        var max = Number(input.max);

        if (minus) {
            minus.disabled = value <= (Number(input.min) || 1);
        }

        if (plus) {
            plus.disabled = Number.isFinite(max) && max > 0 && value >= max;
        }
    }

    function setupQuantity(form) {
        var input = form.querySelector('input[name="quantity"]');
        var timer = null;
        var lastSaved = input ? input.value : '';

        if (!input) {
            return;
        }

        function scheduleSave() {
            window.clearTimeout(timer);

            timer = window.setTimeout(function () {
                if (input.value === lastSaved) {
                    return;
                }

                lastSaved = input.value;
                form.classList.add('is-saving');

                if (typeof form.requestSubmit === 'function') {
                    form.requestSubmit();
                } else {
                    form.submit();
                }
            }, SAVE_DELAY_MS);
        }

        form.addEventListener('click', function (event) {
            var button = event.target.closest('[data-qty-step]');

            if (!button || button.disabled) {
                return;
            }

            var step = Number(button.getAttribute('data-qty-step'));

            input.value = String(
                (parseInt(input.value, 10) || 1) + step
            );

            clamp(input);
            refreshButtons(form);
            scheduleSave();
        });

        input.addEventListener('input', function () {
            // Cho phép người dùng xóa trắng để gõ số mới.
            if (input.value === '') {
                return;
            }

            clamp(input);
            refreshButtons(form);
            scheduleSave();
        });

        input.addEventListener('blur', function () {
            clamp(input);
            refreshButtons(form);
            scheduleSave();
        });

        input.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                clamp(input);
                window.clearTimeout(timer);
                lastSaved = input.value;
                form.classList.add('is-saving');
                form.requestSubmit();
            }
        });

        refreshButtons(form);
    }

    function init() {
        document
            .querySelectorAll('[data-cart-quantity]')
            .forEach(setupQuantity);

        document.addEventListener('submit', function (event) {
            var form = event.target;

            if (!(form instanceof HTMLFormElement)) {
                return;
            }

            var message = form.getAttribute('data-confirm');

            if (message && !window.confirm(message)) {
                event.preventDefault();
            }
        });
    }

    // Trang được khôi phục từ bộ nhớ đệm khi bấm nút Back.
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) {
            window.location.reload();
        }
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
