/*
 * Giỏ hàng: nút tăng/giảm, tự lưu, xác nhận khi xóa.
 * Cập nhật bằng fetch rồi thay nội dung <main> (không tải lại cả trang).
 * Khi tắt JavaScript vẫn dùng được với nút "Cập nhật" của từng dòng.
 */
(function () {
    'use strict';

    document.documentElement.classList.add('has-js');

    var SAVE_DELAY_MS = 500;
    var timers = {};                 // key = form.action
    var chain = Promise.resolve();   // xếp hàng: mỗi lần chỉ gửi 1 request

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

    function isCartForm(form) {
        if (!(form instanceof HTMLFormElement) || !form.closest('.cart-page')) {
            return false;
        }
        try {
            return /\/cart\/items/.test(new URL(form.action).pathname);
        } catch (e) {
            return false;
        }
    }

    function syncCartCount() {
        var heading = document.querySelector('.cart-heading-count');
        var quantity = heading
            ? parseInt(heading.textContent.replace(/\D/g, ''), 10)
            : 0;

        if (window.ConsoleStoreSession && Number.isFinite(quantity)) {
            window.ConsoleStoreSession.setCartCount(quantity);
        }
    }

    function fadeMessages() {
        document
            .querySelectorAll('.cart-message:not(.cart-message--error)')
            .forEach(function (el) {
                setTimeout(function () {
                    el.classList.add('is-leaving');
                    setTimeout(function () { el.remove(); }, 350);
                }, 3500);
            });
    }

    function swapFrom(html) {
        var next = new DOMParser()
            .parseFromString(html, 'text/html')
            .getElementById('main-content');
        var current = document.getElementById('main-content');

        if (!next || !current) {
            return false;
        }

        var y = window.scrollY;
        var focusId = document.activeElement && document.activeElement.id;

        current.replaceChildren.apply(current, Array.from(next.childNodes));
        window.scrollTo(0, y);

        if (focusId) {
            var again = document.getElementById(focusId);
            if (again) {
                again.focus({ preventScroll: true });
            }
        }

        current.querySelectorAll('[data-cart-quantity]').forEach(refreshButtons);
        syncCartCount();
        fadeMessages();
        return true;
    }

    function enqueue(form, submitter) {
        var action = form.action;
        var body = new URLSearchParams(
            submitter ? new FormData(form, submitter) : new FormData(form)
        ).toString();

        form.classList.add('is-saving');

        chain = chain.then(function () {
            return fetch(action, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: body
            })
                .then(function (response) {
                    // Bị chuyển sang trang khác (đăng nhập, thanh toán...): đi theo.
                    if (!/\/cart\/?$/.test(new URL(response.url).pathname)) {
                        window.csShowNavBar?.();
                        window.location.assign(response.url);
                        return;
                    }
                    if (!response.ok) {
                        throw new Error('HTTP ' + response.status);
                    }
                    return response.text().then(function (html) {
                        if (!swapFrom(html)) {
                            window.location.reload();
                        }
                    });
                })
                .catch(function () {
                    // Không gửi lại (tránh cộng dồn mua kèm); tải lại để thấy trạng thái thật.
                    window.location.reload();
                });
        });
    }

    function scheduleSave(form) {
        var input = form.querySelector('input[name="quantity"]');

        window.clearTimeout(timers[form.action]);
        timers[form.action] = window.setTimeout(function () {
            // defaultValue = số lượng server đã render; chưa đổi thì khỏi gửi.
            if (input && input.value !== input.defaultValue) {
                enqueue(form);
            }
        }, SAVE_DELAY_MS);
    }

    document.addEventListener('click', function (event) {
        var button = event.target.closest('[data-qty-step]');
        if (!button || button.disabled) {
            return;
        }

        var form = button.closest('[data-cart-quantity]');
        var input = form && form.querySelector('input[name="quantity"]');
        if (!input) {
            return;
        }

        input.value = String(
            (parseInt(input.value, 10) || 1)
            + Number(button.getAttribute('data-qty-step'))
        );
        clamp(input);
        refreshButtons(form);
        scheduleSave(form);
    });

    document.addEventListener('input', function (event) {
        var input = event.target;
        if (!input.matches
            || !input.matches('[data-cart-quantity] input[name="quantity"]')
            || input.value === '') {   // cho phép xóa trắng để gõ số mới
            return;
        }

        var form = input.closest('[data-cart-quantity]');
        clamp(input);
        refreshButtons(form);
        scheduleSave(form);
    });

    document.addEventListener('focusout', function (event) {
        var input = event.target;
        if (!input.matches
            || !input.matches('[data-cart-quantity] input[name="quantity"]')) {
            return;
        }

        var form = input.closest('[data-cart-quantity]');
        clamp(input);
        refreshButtons(form);
        scheduleSave(form);
    });

    document.addEventListener('keydown', function (event) {
        var input = event.target;
        if (event.key !== 'Enter'
            || !input.matches
            || !input.matches('[data-cart-quantity] input[name="quantity"]')) {
            return;
        }

        event.preventDefault();
        var form = input.closest('[data-cart-quantity]');
        window.clearTimeout(timers[form.action]);
        clamp(input);
        if (input.value !== input.defaultValue) {
            enqueue(form);
        }
    });

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }

        var message = form.getAttribute('data-confirm');
        if (message && !window.confirm(message)) {
            event.preventDefault();
            return;
        }

        if (isCartForm(form)) {
            event.preventDefault();
            enqueue(form, event.submitter);
        }
    });

    function init() {
        document
            .querySelectorAll('[data-cart-quantity]')
            .forEach(refreshButtons);
        fadeMessages();
    }

    // Trang được khôi phục từ bộ nhớ đệm khi bấm Back.
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