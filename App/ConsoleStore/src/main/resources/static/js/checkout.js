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
        'province', 'ward', 'street',
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
     * 3. Chọn địa chỉ theo mô hình hiện hành: Tỉnh -> Phường/Xã
     * API provinces.open-api.vn phiên bản v2 (sau 07/2025).
     * Dropdown luôn nằm trong HTML; lỗi API không đổi nó thành input.
     * --------------------------------------------------------- */

    var ADDRESS_API = 'https://provinces.open-api.vn/api/v2';
    var REQUEST_TIMEOUT_MS = 7000;
    var cache = {};

    function getJson(url) {
        if (cache[url]) return Promise.resolve(cache[url]);

        var controller = new AbortController();
        var timer = window.setTimeout(function () {
            controller.abort();
        }, REQUEST_TIMEOUT_MS);

        return fetch(url, {
            signal: controller.signal,
            headers: { Accept: 'application/json' }
        })
            .then(function (response) {
                if (!response.ok) throw new Error('HTTP ' + response.status);
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

    var LEVELS = {
        province: { label: 'Tỉnh / Thành phố', placeholder: 'Chọn Tỉnh / Thành phố' },
        ward: { label: 'Phường / Xã', placeholder: 'Chọn Phường / Xã' }
    };

    function nameKey(value) {
        return normalizeText(value).toLocaleLowerCase('vi')
            .replace(/^(thành phố|tỉnh|phường|xã|đặc khu|tp\.?)\s+/i, '');
    }

    function findByName(names, wanted) {
        var candidates = [].concat(wanted || []).filter(Boolean);
        for (var i = 0; i < candidates.length; i++) {
            var candidate = nameKey(candidates[i]);
            for (var j = 0; j < names.length; j++) {
                if (names[j] && nameKey(names[j]) === candidate) return j;
            }
        }
        return -1;
    }

    function sortByName(items) {
        return items.slice().sort(function (a, b) {
            return a.name.localeCompare(b.name, 'vi', { numeric: true });
        });
    }

    function sortProvinces(items) {
        return items.slice().sort(function (a, b) {
            var aPinned = /Hà Nội|Hồ Chí Minh/.test(a.name) ? 0 : 1;
            var bPinned = /Hà Nội|Hồ Chí Minh/.test(b.name) ? 0 : 1;
            return aPinned - bPinned || a.name.localeCompare(b.name, 'vi');
        });
    }

    function field(name) {
        return form.elements[name];
    }

    function fillSelect(select, items, selectedName) {
        var placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = LEVELS[select.name].placeholder;
        select.replaceChildren(placeholder);

        var names = items.map(function (item) { return item.name; });
        var matchIndex = findByName(names, selectedName);
        var matched = matchIndex >= 0;

        items.forEach(function (item, index) {
            var option = document.createElement('option');
            option.value = item.name;
            option.textContent = item.name;
            option.dataset.code = item.code;
            if (index === matchIndex) option.selected = true;
            select.appendChild(option);
        });

        select.disabled = false;
        return matched;
    }

    function resetWard() {
        var ward = field('ward');
        ward.replaceChildren(new Option(LEVELS.ward.placeholder, ''));
        ward.disabled = true;
    }

    function codeOf(name) {
        var select = field(name);
        var option = select && select.selectedOptions[0];
        return option && option.dataset.code ? option.dataset.code : '';
    }

    function setHint(message) {
        var hint = form.querySelector('[data-address-hint]');
        if (!hint) return;
        hint.textContent = message || '';
        hint.hidden = !message;
    }

    function legacyWardNames(wanted, provinceCode) {
        var candidates = [].concat(wanted || []).filter(Boolean);
        function tryNext(index) {
            if (index >= candidates.length) return Promise.resolve([]);
            var url = ADDRESS_API + '/w/from-legacy/?legacy_name=' +
                encodeURIComponent(candidates[index]);
            return getJson(url).then(function (rows) {
                var matches = (rows || []).filter(function (row) {
                    return row.ward && String(row.ward.province_code) === String(provinceCode);
                });
                if (matches.length === 1) return [matches[0].ward.name];
                return tryNext(index + 1);
            }).catch(function () {
                return tryNext(index + 1);
            });
        }
        return tryNext(0);
    }

    function loadWards(selectedName) {
        var provinceCode = codeOf('province');
        resetWard();
        if (!provinceCode) return Promise.resolve();

        var url = ADDRESS_API + '/p/' + encodeURIComponent(provinceCode) + '?depth=2';
        return getJson(url).then(function (data) {
            var wards = sortByName(data.wards || []);
            var wardSelect = field('ward');
            var found = fillSelect(wardSelect, wards, selectedName);

            if (found || !selectedName) return;

            return legacyWardNames(selectedName, provinceCode).then(function (newNames) {
                if (newNames.length) {
                    fillSelect(wardSelect, wards, newNames);
                } else {
                    // Giữ được địa chỉ đã lưu cũ nếu không thể tự ánh xạ tên phường.
                    var oldName = [].concat(selectedName || [])[0];
                    if (oldName) {
                        var option = new Option(oldName, oldName, true, true);
                        wardSelect.add(option);
                    }
                }
            });
        }).catch(function (error) {
            console.error('Không tải được danh sách phường/xã:', error);
            setHint('Không tải được danh sách phường/xã. Hãy tải lại trang hoặc thử lại sau.');
        });
    }

    function setupAddress() {
        var provinceSelect = field('province');
        var wardSelect = field('ward');
        var initialProvince = valueOf('province');
        var initialWard = valueOf('ward');
        var districtField = field('district');
        if (districtField) districtField.value = districtField.value || '';

        provinceSelect.addEventListener('change', function () {
            setError('province', '');
            setError('ward', '');
            if (districtField) districtField.value = '';
            setHint('');
            loadWards('');
        });

        wardSelect.addEventListener('change', function () {
            setError('ward', '');
        });

        return getJson(ADDRESS_API + '/p/').then(function (items) {
            if (!Array.isArray(items) || !items.length) {
                throw new Error('Danh sách tỉnh/thành rỗng');
            }
            fillSelect(provinceSelect, sortProvinces(items), initialProvince);
            resetWard();
            setHint('');

            if (initialProvince && provinceSelect.value) {
                return loadWards(initialWard);
            }
        }).catch(function (error) {
            console.error('Không tải được danh sách tỉnh/thành:', error);
            // Vẫn giữ nguyên các control SELECT, không đổi về input.
            provinceSelect.disabled = false;
            wardSelect.disabled = true;
            setHint('Không tải được dữ liệu địa chỉ. Vui lòng tải lại trang hoặc kiểm tra kết nối mạng.');
        });
    }

    function applyAddress(a) {
        a = a || {};
        var provinceSelect = field('province');
        var wardSelect = field('ward');
        var districtField = field('district');

        if (districtField) {
            districtField.value = [].concat(a.district || [])[0] || '';
        }

        function done() {
            form.dispatchEvent(new CustomEvent('address:applied'));
        }

        var provinceNames = Array.prototype.map.call(provinceSelect.options, function (o) {
            return o.value;
        });
        var found = findByName(provinceNames, a.province);
        provinceSelect.selectedIndex = found >= 0 ? found : 0;

        if (!provinceSelect.value) {
            resetWard();
            wardSelect.value = '';
            done();
            return Promise.resolve();
        }

        return loadWards(a.ward).then(done);
    }

    root.CheckoutAddress = { apply: applyAddress };

// Hàm cập nhật số ký tự ghi chú
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
        // setupAddress().then(focusFirstServerError);
        root.CheckoutAddress.ready = setupAddress().then(focusFirstServerError);
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
