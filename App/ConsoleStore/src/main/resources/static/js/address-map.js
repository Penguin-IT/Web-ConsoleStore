/*
 * Chọn địa chỉ giao hàng trên bản đồ (Leaflet + OpenStreetMap + Nominatim).
 *
 * Nhấn vào bản đồ, kéo ghim, tìm địa chỉ hoặc dùng "Vị trí của tôi"
 * → tự điền Tỉnh / Quận / Phường / Số nhà, tên đường vào form.
 * Không cần API key. Phụ thuộc checkout.js (window.CheckoutAddress).
 */
(function () {
    'use strict';

    var form = document.querySelector('[data-checkout-form]');
    var mapEl = document.querySelector('[data-addr-map]');

    if (!form || !mapEl) {
        return;
    }

    var statusEl = document.querySelector('[data-map-status]');
    var searchInput = document.querySelector('[data-map-search]');
    var searchBtn = document.querySelector('[data-map-search-btn]');
    var locateBtn = document.querySelector('[data-map-locate]');

    function say(message, isError) {
        if (!statusEl) {
            return;
        }

        statusEl.textContent = message;
        statusEl.classList.toggle('is-error', Boolean(isError));
    }

    if (typeof L === 'undefined') {
        say('Không tải được bản đồ. Vui lòng nhập địa chỉ thủ công.', true);
        return;
    }

    var NOMINATIM = 'https://nominatim.openstreetmap.org';
    var DEFAULT_CENTER = [10.7769, 106.7009]; // TP. Hồ Chí Minh
    var LEVELS = ['province', 'ward'];

    var map = L.map(mapEl).setView(DEFAULT_CENTER, 13);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    window.setTimeout(function () {
        map.invalidateSize();
    }, 200);

    var marker = null;
    var ticket = 0; // bỏ qua kết quả của yêu cầu cũ nếu khách đã chọn chỗ khác

    function putMarker(latlng) {
        if (marker) {
            marker.setLatLng(latlng);
            return;
        }

        marker = L.marker(latlng, { draggable: true }).addTo(map);
        marker.on('dragend', function () {
            pick(marker.getLatLng());
        });
    }

    function getJson(url) {
        return fetch(url, { headers: { Accept: 'application/json' } })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }

                return response.json();
            });
    }

    function reverse(latlng) {
        return getJson(
            NOMINATIM + '/reverse?format=jsonv2&addressdetails=1&zoom=18'
            + '&accept-language=vi&lat=' + latlng.lat + '&lon=' + latlng.lng
        );
    }

    function search(text) {
        return getJson(
            NOMINATIM + '/search?format=jsonv2&addressdetails=1&limit=1'
            + '&countrycodes=vn&accept-language=vi&q=' + encodeURIComponent(text)
        );
    }

    function list() {
        return Array.prototype.slice.call(arguments).filter(Boolean);
    }

    // Đổi kết quả Nominatim thành các ứng viên cho từng ô địa chỉ.
    function toAddress(data) {
        var a = data.address || {};

        var street = list(
            a.house_number,
            a.road || a.pedestrian || a.residential || a.path
        ).join(' ');

        if (!street) {
            street = data.name || (data.display_name || '').split(',')[0] || '';
        }

        // Giữ đúng bộ ký tự mà CheckoutRules.street cho phép.
        street = street
            .replace(/[^\p{L}\p{N}\p{M}\s.,'()\/#-]/gu, ' ')
            .replace(/\s+/g, ' ')
            .trim()
            .slice(0, 100);

        return {
            street: street,
            province: list(a.state, a.city, a.province),
            district: [], // Cấp quận/huyện không còn trong danh mục hành chính mới.
            ward: list(
                a.suburb, a.quarter, a.neighbourhood,
                a.village, a.hamlet, a.town
            )
        };
    }

    function fill(data) {
        var address = toAddress(data);
        var ready = (window.CheckoutAddress && window.CheckoutAddress.ready)
            || Promise.resolve();

        return ready.then(function () {
            form.elements.street.value = address.street;

            return window.CheckoutAddress.apply(address);
        }).then(function () {
            // Cho address-book.js biết khách đang nhập địa chỉ mới.
            form.dispatchEvent(new CustomEvent('address:map-picked'));

            // Kích hoạt kiểm tra từng ô để xóa/hiện lỗi đúng trạng thái.
            LEVELS.concat('street').forEach(function (name) {
                var element = form.elements[name];

                if (element) {
                    element.dispatchEvent(
                        new FocusEvent('focusout', { bubbles: true })
                    );
                }
            });

            var missing = LEVELS.filter(function (name) {
                return !form.elements[name].value;
            });

            if (missing.length) {
                say('Đã chọn vị trí nhưng chưa khớp Tỉnh/Phường/Xã. '
                    + 'Vui lòng chọn lại các ô còn trống bên dưới.', true);
            } else {
                say('Đã chọn: ' + (data.display_name || address.street));
            }
        });
    }

    function fail() {
        say('Không xác định được địa chỉ. Hãy thử vị trí khác '
            + 'hoặc nhập thủ công.', true);
    }

    function pick(latlng) {
        var id = ++ticket;

        putMarker(latlng);
        say('Đang xác định địa chỉ…');

        reverse(latlng).then(function (data) {
            if (id !== ticket) {
                return;
            }

            if (!data || data.error) {
                throw new Error('no address');
            }

            return fill(data);
        }).catch(function () {
            if (id === ticket) {
                fail();
            }
        });
    }

    function doSearch() {
        var text = searchInput.value.trim();

        if (!text) {
            return;
        }

        var id = ++ticket;

        say('Đang tìm…');

        search(text).then(function (rows) {
            if (id !== ticket) {
                return;
            }

            if (!rows.length) {
                say('Không tìm thấy. Hãy ghi rõ hơn '
                    + '(số nhà, đường, quận, tỉnh).', true);
                return;
            }

            var latlng = L.latLng(
                parseFloat(rows[0].lat),
                parseFloat(rows[0].lon)
            );

            putMarker(latlng);
            map.setView(latlng, 17);

            return fill(rows[0]);
        }).catch(function () {
            if (id === ticket) {
                fail();
            }
        });
    }

    map.on('click', function (event) {
        pick(event.latlng);
    });

    if (searchBtn) {
        searchBtn.addEventListener('click', doSearch);
    }

    if (searchInput) {
        // Enter trong ô tìm kiếm không được gửi form đặt hàng.
        searchInput.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                doSearch();
            }
        });
    }

    if (locateBtn) {
        locateBtn.addEventListener('click', function () {
            if (!navigator.geolocation) {
                say('Trình duyệt không hỗ trợ định vị.', true);
                return;
            }

            say('Đang lấy vị trí của bạn…');

            navigator.geolocation.getCurrentPosition(function (position) {
                var latlng = L.latLng(
                    position.coords.latitude,
                    position.coords.longitude
                );

                map.setView(latlng, 17);
                pick(latlng);
            }, function () {
                say('Không lấy được vị trí. Hãy cho phép truy cập vị trí '
                    + 'hoặc chọn trực tiếp trên bản đồ.', true);
            }, { enableHighAccuracy: true, timeout: 10000 });
        });
    }
})();