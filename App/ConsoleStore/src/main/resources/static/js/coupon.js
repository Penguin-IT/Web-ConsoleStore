(function () {
    'use strict';
    function copyText(text) {
        if (navigator.clipboard && window.isSecureContext) {
            return navigator.clipboard.writeText(text);
        }
        return new Promise(function (resolve, reject) {
            var ta = document.createElement('textarea');
            ta.value = text;
            ta.setAttribute('readonly', '');
            ta.style.position = 'fixed';
            ta.style.opacity = '0';
            document.body.appendChild(ta);
            ta.select();
            try { document.execCommand('copy') ? resolve() : reject(); }
            catch (e) { reject(e); }
            finally { document.body.removeChild(ta); }
        });
    }
    // ===== Trang chủ: bấm để sao chép mã =====
    document.addEventListener('click', function (event) {
        var btn = event.target.closest('[data-copy-code]');
        if (!btn) return;

        var code = btn.getAttribute('data-code');
        var label = btn.querySelector('em');
        var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        var MONEY_KEYS = { payable: true, saved: true };
        var lastKey = null;

        function parseMoney(text) {
            var digits = String(text || '').replace(/\D/g, '');
            return digits ? Number(digits) : null;
        }

        function flash(el) {
            if (reduceMotion) return;
            el.classList.remove('is-flash');
            void el.offsetWidth;            // khởi động lại animation
            el.classList.add('is-flash');
        }

        function animateMoney(el, targetText) {
            var from = parseMoney(el.textContent);
            var to = parseMoney(targetText);

            cancelAnimationFrame(el._raf);

            if (reduceMotion || from === null || to === null || from === to) {
                el.textContent = targetText;
                return;
            }

            var start = performance.now();
            var duration = 450;

            function step(now) {
                var t = Math.min(1, (now - start) / duration);
                var eased = 1 - Math.pow(1 - t, 3);

                el.textContent = t < 1
                    ? Math.round(from + (to - from) * eased).toLocaleString('vi-VN') + ' ₫'
                    : targetText;                // khung cuối dùng đúng chuỗi server trả

                if (t < 1) el._raf = requestAnimationFrame(step);
            }
            el._raf = requestAnimationFrame(step);
        }

        function setText(key, text) {
            var el = document.querySelector('[data-sum="' + key + '"]');
            text = text == null ? '' : String(text);
            if (!el || el.textContent === text) return;

            if (MONEY_KEYS[key]) {
                animateMoney(el, text);
            } else {
                el.textContent = text;
            }
            flash(el);
        }

        function refresh() {
            var provinceField = form.querySelector('[name="province"]');
            var province = provinceField ? provinceField.value : '';
            var code = input.value.trim();
            var key = province + '|' + code;

            if (key === lastKey) return;      // chưa đổi gì thì không hỏi server lại
            lastKey = key;

            var mine = ++seq;
            var body = new URLSearchParams();
            body.set('province', province);
            body.set('couponCode', code);
            if (csrf) body.set('_csrf', csrf.value);

            if (applyBtn) {
                applyBtn.disabled = true;
                applyBtn.dataset.label = applyBtn.dataset.label || applyBtn.textContent;
                applyBtn.textContent = 'Đang kiểm tra…';
            }

            fetch(box.getAttribute('data-url'), {
                method: 'POST',
                credentials: 'same-origin',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Accept': 'application/json'
                },
                body: body.toString()
            })
                .then(function (r) {
                    if (!r.ok) throw new Error(r.status);
                    return r.json();
                })
                .then(function (data) { if (mine === seq) render(data); })
                .catch(function () {
                    lastKey = null;           // cho phép thử lại cùng mã
                    if (msg) {
                        msg.textContent = 'Không kiểm tra được mã lúc này. Vui lòng thử lại.';
                        msg.className = 'coupon-msg err';
                    }
                })
                .finally(function () {
                    if (applyBtn) {
                        applyBtn.disabled = false;
                        applyBtn.textContent = applyBtn.dataset.label;
                    }
                });
        }
        function done() {
            if (!label) return;
            var old = label.textContent;
            label.textContent = 'Đã chép!';
            btn.classList.add('is-copied');
            setTimeout(function () {
                label.textContent = old;
                btn.classList.remove('is-copied');
            }, 1500);
        }
        copyText(code).then(done).catch(function () {
            window.prompt('Sao chép mã:', code);
        });
    });

    // ===== Trang thanh toán: áp mã, tính lại ship =====
    var form = document.querySelector('[data-checkout-form]');
    var input = document.querySelector('[data-coupon-input]');
    if (!form || !input) return;

    var box = document.querySelector('.coupon-box');
    var applyBtn = document.querySelector('[data-coupon-apply]');
    var msg = document.querySelector('[data-coupon-msg]');
    var csrf = form.querySelector('input[name="_csrf"]');
    var seq = 0;



    function render(data) {
        setText('shipping', data.shipping);
        setText('discount', data.discount);
        setText('payable', data.payable);
        setText('code', data.code ? '(' + data.code + ')' : '');
        setText('saved', data.totalSaved);
        var savedRow = document.querySelector('[data-sum-saved-row]');
        if (savedRow) savedRow.classList.toggle('is-hidden', !data.hasSaved);
        var row = document.querySelector('[data-sum-discount-row]');
        if (row) row.classList.toggle('is-hidden', !data.hasDiscount);

        if (msg) {
            msg.textContent = data.message || '';
            msg.classList.toggle('ok', !!data.couponValid);
            msg.classList.toggle('err', !data.couponValid && !!data.message);
        }
    }



    if (applyBtn) applyBtn.addEventListener('click', refresh);

    // Enter trong ô mã chỉ áp mã, không được gửi đơn hàng.
    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); refresh(); }
    });
    input.addEventListener('input', function () {
        input.value = input.value.toUpperCase();
    });

    document.querySelectorAll('[data-coupon-pick]').forEach(function (b) {
        b.addEventListener('click', function () {
            input.value = b.getAttribute('data-code');
            refresh();
        });
    });

    // Đổi tỉnh/thành → tính lại phí ship (select vẫn giữ name="province").
    form.addEventListener('change', function (e) {
        if (e.target && e.target.name === 'province') refresh();
    });
    form.addEventListener('address:applied', refresh);
})();