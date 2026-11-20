/*
 * Trang đặt hàng: kiểm tra dữ liệu nhập và chọn địa chỉ theo Tỉnh/Thành -
 * Quận/Huyện - Phường/Xã.
 *
 * Quy tắc kiểm tra phải khớp với CheckoutValidator.java. Máy chủ luôn kiểm
 * tra lại, nên đoạn mã này chỉ giúp người dùng sửa lỗi ngay khi nhập.
 */
(function (root) {
    'use strict';

    /* ---------------------------------------------------------
     * 1. Quy tắc kiểm tra (không phụ thuộc trình duyệt)
     * --------------------------------------------------------- */

    var NAME_RE = /^\p{L}[\p{L}\p{M}\s.'-]*$/u;
    var PHONE_RE = /^0(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$/;
    var PLACE_RE = /^[\p{L}\p{N}][\p{L}\p{N}\p{M}\s.,'()/-]*$/u;
    var STREET_RE = /^[\p{L}\p{N}][\p{L}\p{N}\p{M}\s.,'()/#-]*$/u;
    var METHODS = ['CHUYEN_KHOAN', 'MOMO', 'ZALOPAY', 'TIEN_MAT'];

    function normalizeText(value) {
        return String(value == null ? '' : value)
            .normalize('NFC')
            .replace(/\s+/g, ' ')
            .trim();
    }

    function normalizePhone(value) {
        var digits = String(value == null ? '' : value)
            .replace(/[\s.()-]/g, '');

        if (digits.indexOf('+84') === 0) {
            digits = '0' + digits.slice(3);
        } else if (digits.indexOf('84') === 0 && digits.length === 11) {
            digits = '0' + digits.slice(2);
        }

        return digits;
    }

    function place(label, max) {
        return function (raw) {
            var value = normalizeText(raw);

            if (!value) {
                return 'Vui lòng chọn ' + label + '.';
            }

            if (value.length > max) {
                return label + ' tối đa ' + max + ' ký tự.';
            }

            if (!PLACE_RE.test(value)) {
                return label + ' chứa ký tự không hợp lệ.';
            }

            return '';
        };
    }

    var rules = {
        fullName: function (raw) {
            var value = normalizeText(raw);

            if (!value) {
                return 'Vui lòng nhập họ và tên.';
            }

            if (value.length < 2) {
                return 'Họ và tên quá ngắn.';
            }

            if (value.length > 100) {
                return 'Họ và tên tối đa 100 ký tự.';
            }

            if (!NAME_RE.test(value)) {
                return 'Họ và tên chỉ gồm chữ cái và khoảng trắng.';
            }

            return '';
        },

        phone: function (raw) {
            var value = normalizePhone(raw);

            if (!value) {
                return 'Vui lòng nhập số điện thoại.';
            }

            if (!PHONE_RE.test(value)) {
                return 'Số điện thoại không hợp lệ. Ví dụ: 0912345678.';
            }

            return '';
        },

        province: place('Tỉnh / Thành phố', 40),
        district: place('Quận / Huyện', 50),
        ward: place('Phường / Xã', 50),

        street: function (raw) {
            var value = normalizeText(raw);

            if (!value) {
                return 'Vui lòng nhập số nhà, tên đường.';
            }

            if (value.length < 5) {
                return 'Địa chỉ quá ngắn. Ví dụ: 12 Nguyễn Huệ.';
            }

            if (value.length > 100) {
                return 'Số nhà, tên đường tối đa 100 ký tự.';
            }

            if (!STREET_RE.test(value)) {
                return 'Địa chỉ chứa ký tự không hợp lệ.';
            }

            return '';
        },

        note: function (raw) {
            return String(raw == null ? '' : raw).length > 255
                ? 'Ghi chú tối đa 255 ký tự.'
                : '';
        },

        paymentMethod: function (raw) {
            return METHODS.indexOf(raw) >= 0
                ? ''
                : 'Vui lòng chọn phương thức thanh toán.';
        }
    };

    var api = {
        rules: rules,
        normalizeText: normalizeText,
        normalizePhone: normalizePhone
    };

    // Cho phép kiểm thử bằng Node.
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }

    if (typeof document === 'undefined') {
        return;
    }

    root.CheckoutRules = api;

    /* ---------------------------------------------------------
     * 2. Kiểm tra trên form
     * --------------------------------------------------------- */

    document.documentElement.classList.add('has-js');

    var ORDER = [
        'fullName', 'phone',
        'province', 'district', 'ward', 'street',
        'note', 'paymentMethod'
    ];

    var form;
    var alertBox;

    function control(name) {
        if (name === 'paymentMethod') {
            return form.querySelector(
                'input[name="paymentMethod"]:checked'
            );
        }

        return form.elements[name] || null;
    }

    function valueOf(name) {
        var element = control(name);

        return element ? element.value : '';
    }

    function wrapper(name) {
        return form.querySelector('[data-field="' + name + '"]');
    }

    function setError(name, message) {
        var box = wrapper(name);
        var text = document.getElementById(name + '-error');
        var element = name === 'paymentMethod'
            ? null
            : form.elements[name];

        if (text) {
            text.textContent = message;
        }

        if (box) {
            box.classList.toggle('has-error', Boolean(message));
        }

        if (element && element.setAttribute) {
            element.setAttribute(
                'aria-invalid',
                message ? 'true' : 'false'
            );
        }
    }

    function check(name) {
        var message = rules[name](valueOf(name));

        setError(name, message);

        return message;
    }

    function showAlert(message) {
        if (!alertBox) {
            return;
        }

        alertBox.textContent = message || '';
        alertBox.hidden = !message;
    }

    function focusField(name) {
        var element = name === 'paymentMethod'
            ? form.querySelector('input[name="paymentMethod"]')
            : form.elements[name];

        if (!element) {
            return;
        }

        element.focus({ preventScroll: true });

        element.scrollIntoView({
            block: 'center',
            behavior: window.matchMedia('(prefers-reduced-motion: reduce)')
                .matches ? 'auto' : 'smooth'
        });
    }

    function validateAll() {
        var first = null;

        ORDER.forEach(function (name) {
            if (check(name) && !first) {
                first = name;
            }
        });

        return first;
    }

    /* ---------------------------------------------------------
     * 3. Chọn địa chỉ theo cấp hành chính
     *
     * Dữ liệu lấy từ API công khai provinces.open-api.vn (bộ 63 tỉnh,
     * 3 cấp). Nếu không tải được, các ô tự chuyển về nhập tay để
     * khách vẫn đặt hàng được.
     * --------------------------------------------------------- */

    var ADDRESS_API = 'https://provinces.open-api.vn/api';
    var REQUEST_TIMEOUT_MS = 5000;
    var cache = {};

    function getJson(url) {
        if (cache[url]) {
            return Promise.resolve(cache[url]);
        }

        var controller = new AbortController();
        var timer = window.setTimeout(function () {
            controller.abort();
        }, REQUEST_TIMEOUT_MS);

        return fetch(url, {
            signal: controller.signal,
            headers: { Accept: 'application/json' }
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }

                return response.json();
            })
            .then(function (data) {
                cache[url] = data;

                return data;
            })
            .finally(function () {
                window.clearTimeout(timer);
            });
    }

    var OPEN_API = 'https://provinces.open-api.vn/api';
    var ESGOO_API = 'https://esgoo.net/api-tinhthanh/';

    function esgoo(path) {
        return getJson(ESGOO_API + path + '.htm').then(function (data) {
            if (!data || data.error !== 0 || !Array.isArray(data.data)) {
                throw new Error('esgoo: dữ liệu không hợp lệ');
            }

            return data.data.map(function (item) {
                return { name: item.full_name || item.name, code: item.id };
            });
        });
    }

    var PROVIDERS = [
        {
            provinces: function () {
                return getJson(OPEN_API + '/p/');
            },
            districts: function (code) {
                return getJson(
                    OPEN_API + '/p/' + encodeURIComponent(code) + '?depth=2'
                ).then(function (data) {
                    return data.districts || [];
                });
            },
            wards: function (code) {
                return getJson(
                    OPEN_API + '/d/' + encodeURIComponent(code) + '?depth=2'
                ).then(function (data) {
                    return data.wards || [];
                });
            }
        },
        {
            provinces: function () { return esgoo('1/0'); },
            districts: function (code) {
                return esgoo('2/' + encodeURIComponent(code));
            },
            wards: function (code) {
                return esgoo('3/' + encodeURIComponent(code));
            }
        }
    ];

    // Nguồn đã tải Tỉnh thành công; Quận/Phường phải dùng cùng nguồn đó
    // vì mã (code) của mỗi nguồn khác nhau.
    var active = 0;

    var source = {
        provinces: function () {
            var index = 0;

            function next() {
                if (index >= PROVIDERS.length) {
                    return Promise.reject(
                        new Error('Không có nguồn địa chỉ nào hoạt động')
                    );
                }

                var current = index++;

                return PROVIDERS[current].provinces().then(function (items) {
                    if (!Array.isArray(items) || !items.length) {
                        throw new Error('Danh sách rỗng');
                    }

                    active = current;

                    return items;
                }).catch(function (error) {
                    console.warn('Nguồn địa chỉ #' + current + ' lỗi:', error);

                    return next();
                });
            }

            return next();
        },

        districts: function (code) {
            return PROVIDERS[active].districts(code);
        },

        wards: function (code) {
            return PROVIDERS[active].wards(code);
        }
    };

    var LEVELS = {
        province: { label: 'Tỉnh / Thành phố', placeholder: 'Chọn Tỉnh / Thành phố' },
        district: { label: 'Quận / Huyện', placeholder: 'Chọn Quận / Huyện' },
        ward: { label: 'Phường / Xã', placeholder: 'Chọn Phường / Xã' }
    };

    function copyAttributes(from, to, names) {
        names.forEach(function (name) {
            if (from.hasAttribute(name)) {
                to.setAttribute(name, from.getAttribute(name));
            }
        });
    }

    var SHARED = [
        'id', 'name', 'required', 'aria-describedby',
        'aria-invalid', 'autocomplete'
    ];

    // Đổi ô nhập tay thành ô chọn, giữ nguyên id/name để form vẫn gửi đúng.
    function asSelect(name) {
        var current = form.elements[name];

        if (current && current.tagName === 'SELECT') {
            return current;
        }

        var select = document.createElement('select');

        copyAttributes(current, select, SHARED);
        select.removeAttribute('autocomplete');
        current.replaceWith(select);

        return select;
    }

    function asInput(name) {
        var current = form.elements[name];

        if (current && current.tagName === 'INPUT') {
            current.disabled = false;

            return current;
        }

        var input = document.createElement('input');

        input.type = 'text';
        input.maxLength = name === 'province' ? 40 : 50;
        input.placeholder = 'Nhập ' + LEVELS[name].label.toLowerCase();

        copyAttributes(current, input, SHARED);
        current.replaceWith(input);

        return input;
    }

    function fillSelect(select, items, selectedName) {
        var level = select.name;

        select.innerHTML = '';

        var first = document.createElement('option');

        first.value = '';
        first.textContent = LEVELS[level].placeholder;
        select.appendChild(first);

        var chosen = false;

        items.forEach(function (item) {
            var option = document.createElement('option');

            option.value = item.name;
            option.textContent = item.name;
            option.dataset.code = item.code;

            if (selectedName && item.name === selectedName) {
                option.selected = true;
                chosen = true;
            }

            select.appendChild(option);
        });

        select.disabled = false;

        return chosen;
    }

    function resetSelect(name) {
        var select = asSelect(name);

        select.innerHTML = '';

        var option = document.createElement('option');

        option.value = '';
        option.textContent = LEVELS[name].placeholder;
        select.appendChild(option);

        select.disabled = true;
    }

    function codeOf(name) {
        var select = form.elements[name];

        if (!select || select.tagName !== 'SELECT') {
            return '';
        }

        var option = select.selectedOptions[0];

        return option && option.dataset.code ? option.dataset.code : '';
    }

    function sortProvinces(items) {
        var pinned = /(Hà Nội|Hồ Chí Minh)$/;

        return items.slice().sort(function (a, b) {
            var pa = pinned.test(a.name) ? 0 : 1;
            var pb = pinned.test(b.name) ? 0 : 1;

            if (pa !== pb) {
                return pa - pb;
            }

            return a.name.localeCompare(b.name, 'vi');
        });
    }

    function sortByName(items) {
        return items.slice().sort(function (a, b) {
            return a.name.localeCompare(b.name, 'vi', { numeric: true });
        });
    }

    function setHint(message) {
        var hint = form.querySelector('[data-address-hint]');

        if (!hint) {
            return;
        }

        hint.textContent = message || '';
        hint.hidden = !message;
    }

    function fallbackToText(names, message) {
        names.forEach(function (name) {
            asInput(name);
        });

        setHint(message);
    }

    function loadDistricts(selectedName, selectedWard) {
        var code = codeOf('province');

        resetSelect('district');
        resetSelect('ward');

        if (!code) {
            return Promise.resolve();
        }

        return source.districts(code).then(function (items) {
            var select = asSelect('district');
            var found = fillSelect(
                select,
                sortByName(items),
                selectedName
            );

            if (found) {
                return loadWards(selectedWard);
            }
        }).catch(function (error) {
            console.error('Không tải được địa chỉ:', error);
            // Giữ nguyên các ô nhập tay do máy chủ dựng sẵn.
            setHint(
                'Không tải được danh sách địa phương. '
                + 'Vui lòng nhập Tỉnh / Quận / Phường thủ công.'
            );
        });
    }

    function loadWards(selectedName) {
        var code = codeOf('district');

        resetSelect('ward');

        if (!code) {
            return Promise.resolve();
        }

        return source.wards(code).then(function (items) {
            fillSelect(
                asSelect('ward'),
                sortByName(items),
                selectedName
            );
        }).catch(function (error) {
            console.error('Không tải được địa chỉ:', error);
            setHint(
                'Không tải được danh sách địa phương. '
                + 'Vui lòng nhập Tỉnh / Quận / Phường thủ công.'
            );
        });
    }

    function setupAddress() {
        var initial = {
            province: valueOf('province'),
            district: valueOf('district'),
            ward: valueOf('ward')
        };

        // Đăng ký trước để không bỏ lỡ sự kiện đổi Tỉnh / Quận.
        form.addEventListener('change', function (event) {
            var target = event.target;

            if (!target || target.tagName !== 'SELECT') {
                return;
            }

            if (target.name === 'province') {
                setError('district', '');
                setError('ward', '');
                loadDistricts('', '');
            } else if (target.name === 'district') {
                setError('ward', '');
                loadWards('');
            }
        });

        return source.provinces().then(function (items) {
            var select = asSelect('province');

            fillSelect(select, sortProvinces(items), initial.province);

            resetSelect('district');
            resetSelect('ward');

            setHint('');

            if (initial.province && select.value) {
                return loadDistricts(initial.district, initial.ward);
            }
        }).catch(function () {
            // Giữ nguyên các ô nhập tay do máy chủ dựng sẵn.
            setHint(
                'Không tải được danh sách địa phương. '
                    + 'Vui lòng nhập Tỉnh / Quận / Phường thủ công.'
            );
        });
    }

    /* ---------------------------------------------------------
     * 4. Gắn sự kiện
     * --------------------------------------------------------- */

    function setupNoteCounter() {
        var counter = form.querySelector('[data-note-count]');
        var note = form.elements.note;

        if (!counter || !note) {
            return;
        }

        function update() {
            counter.textContent = note.value.length + '/255';
        }

        note.addEventListener('input', update);
        update();
    }

    function init() {
        form = document.querySelector('[data-checkout-form]');

        if (!form) {
            return;
        }

        alertBox = document.querySelector('[data-form-alert]');

        var submit = form.querySelector('[data-submit]');
        var submitLabel = submit ? submit.textContent : '';
        var touched = {};

        // Kiểm tra khi rời ô, sau đó kiểm tra lại ngay khi người dùng sửa.
        form.addEventListener('focusout', function (event) {
            var name = event.target && event.target.name;

            if (!name || !rules[name] || name === 'paymentMethod') {
                return;
            }

            if (event.target.tagName === 'INPUT'
                && (name === 'fullName' || name === 'street')) {
                event.target.value = normalizeText(event.target.value);
            }

            if (name === 'phone') {
                event.target.value = normalizePhone(event.target.value);
            }

            touched[name] = true;
            check(name);
        });

        form.addEventListener('input', function (event) {
            var name = event.target && event.target.name;

            if (!name || !rules[name]) {
                return;
            }

            if (name === 'phone') {
                event.target.value = event.target.value
                    .replace(/[^\d+\s.()-]/g, '');
            }

            if (touched[name]) {
                check(name);
            }
        });

        form.addEventListener('change', function (event) {
            var name = event.target && event.target.name;

            if (!name || !rules[name]) {
                return;
            }

            touched[name] = true;
            check(name);
        });

        form.addEventListener('submit', function (event) {
            var first = validateAll();

            if (first) {
                event.preventDefault();

                showAlert(
                    'Vui lòng kiểm tra lại các thông tin '
                        + 'được đánh dấu bên dưới.'
                );

                focusField(first);

                return;
            }

            showAlert('');

            // Chặn bấm nhiều lần; mã yêu cầu cũng đã chống trùng đơn.
            if (submit) {
                submit.disabled = true;
                submit.textContent = 'Đang xử lý…';
            }
        });

        // Quay lại bằng nút Back: bỏ trạng thái "đang xử lý".
        window.addEventListener('pageshow', function () {
            if (submit && submit.textContent === 'Đang xử lý…') {
                submit.disabled = false;
                submit.textContent = submitLabel;
            }
        });

        setupNoteCounter();

        // Nếu máy chủ trả lỗi, đưa con trỏ tới ô lỗi đầu tiên sau khi
        // các ô địa chỉ đã được dựng xong (chúng có thể bị thay thế).
        setupAddress().then(focusFirstServerError);
    }

    function focusFirstServerError() {
        var box = form.querySelector('.has-error');

        if (!box) {
            return;
        }

        var field = box.querySelector('input, select, textarea');

        if (field) {
            field.focus({ preventScroll: true });
        }

        box.scrollIntoView({ block: 'center' });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})(typeof window !== 'undefined' ? window : globalThis);
