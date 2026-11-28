(function () {
    'use strict';

    var form = document.querySelector('[data-checkout-form]');
    if (!form) return;

    var book = document.querySelector('[data-address-book]');
    var saveBox = form.querySelector('[data-addr-save-box]');
    var newOpts = form.querySelector('[data-addr-new-opts]');
    var updateBtn = form.querySelector('[data-addr-update]');
    var csrf = form.querySelector('input[name="_csrf"]');
    var BASE = form.getAttribute('action').replace(/\/$/, '') + '/addresses';
    var FIELDS = ['fullName', 'phone', 'province', 'district', 'ward', 'street'];
    var editingId = null;

    function post(url, data) {
        var body = new URLSearchParams(data || {});
        if (csrf) body.set('_csrf', csrf.value);

        return fetch(url, {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'Accept': 'application/json'
            },
            body: body.toString()
        }).then(function (r) {
            return r.text().then(function (text) {
                var json = text ? JSON.parse(text) : {};
                if (!r.ok) {
                    var error = new Error('rejected');
                    error.errors = json.errors || {};
                    throw error;
                }
                return json;
            });
        });
    }

    function reload() {
        // GET lại trang checkout: giỏ hàng nằm trong session nên không mất.
        window.location.assign(form.getAttribute('action'));
    }

    function showError(error) {
        var messages = error && error.errors ? Object.values(error.errors) : [];
        window.alert(messages[0] || 'Không lưu được địa chỉ. Vui lòng thử lại.');
    }

    // mode: 'saved' (đang dùng địa chỉ đã lưu) | 'edit' (sửa địa chỉ đã lưu) | 'new'
    function setMode(mode) {
        if (!saveBox) return;
        saveBox.hidden = mode === 'saved';
        newOpts.hidden = mode !== 'new';
        updateBtn.hidden = mode !== 'edit';

        if (mode !== 'new') {
            form.elements.saveAddress.checked = false;
            form.elements.makeDefaultAddress.checked = false;
        }
    }

    function markSelected(card) {
        book.querySelectorAll('.addr-card').forEach(function (c) {
            c.classList.toggle('is-selected', c === card);
        });
    }

    function dataOf(card) {
        var d = card.dataset;
        return {
            fullName: d.fullName, phone: d.phone,
            province: d.province, district: d.district,
            ward: d.ward, street: d.street
        };
    }

    function applyToForm(a) {
        form.elements.fullName.value = a.fullName || '';
        form.elements.phone.value = a.phone || '';
        form.elements.street.value = a.street || '';

        if (window.CheckoutAddress) {
            return window.CheckoutAddress.apply(a);
        }
    }

    function currentFields() {
        var out = {};
        FIELDS.forEach(function (n) { out[n] = form.elements[n].value; });
        return out;
    }

    if (book) {
        book.addEventListener('click', function (event) {
            var btn = event.target.closest('button');
            var card = event.target.closest('.addr-card');

            if (btn && btn.hasAttribute('data-addr-new')) {
                book.querySelectorAll('.addr-card').forEach(function (c) {
                    c.classList.remove('is-selected');
                });
                editingId = null;
                applyToForm({
                    fullName: form.elements.fullName.value,
                    phone: form.elements.phone.value,
                    province: '', district: '', ward: '', street: ''
                });
                setMode('new');
                form.elements.street.focus();
                return;
            }

            if (!card) return;
            var id = card.dataset.id;

            if (btn && btn.hasAttribute('data-addr-delete')) {
                if (window.confirm('Xóa địa chỉ này?')) {
                    post(BASE + '/' + id + '/delete').then(reload).catch(showError);
                }
                return;
            }

            if (btn && btn.hasAttribute('data-addr-default')) {
                post(BASE + '/' + id + '/default').then(reload).catch(showError);
                return;
            }

            markSelected(card);
            applyToForm(dataOf(card));

            if (btn && btn.hasAttribute('data-addr-edit')) {
                editingId = id;
                setMode('edit');
            } else {
                editingId = null;
                setMode('saved');
            }
        });
    }

    if (updateBtn) {
        updateBtn.addEventListener('click', function () {
            if (!editingId) return;
            post(BASE + '/' + editingId, currentFields())
                .then(reload)
                .catch(showError);
        });
    }
    form.addEventListener('address:map-picked', function () {
        if (!book || editingId) return;
        book.querySelectorAll('.addr-card').forEach(function (c) {
            c.classList.remove('is-selected');
        });
        setMode('new');
    });
    // Có địa chỉ đã lưu: mặc định ở chế độ "saved" (form đã được server điền sẵn).
    // Nếu server render lại sau lỗi validate và khách đã tick "Lưu", giữ chế độ "new".
    var ticked = form.elements.saveAddress && form.elements.saveAddress.checked;
    setMode(book && !ticked ? 'saved' : 'new');
})();