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

    function setText(key, text) {
        var el = document.querySelector('[data-sum="' + key + '"]');
        if (el) el.textContent = text;
    }

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


    function refresh() {
        var mine = ++seq;
        var provinceField = form.querySelector('[name="province"]');
        var body = new URLSearchParams();

        body.set('province', provinceField ? provinceField.value : '');
        body.set('couponCode', input.value.trim());
        if (csrf) body.set('_csrf', csrf.value);
        if (applyBtn) applyBtn.disabled = true;

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
                if (msg) {
                    msg.textContent = 'Không kiểm tra được mã lúc này. Vui lòng thử lại.';
                    msg.className = 'coupon-msg err';
                }
            })
            .finally(function () { if (applyBtn) applyBtn.disabled = false; });
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
})();