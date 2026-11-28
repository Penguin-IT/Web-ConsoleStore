/* store-fx.js - Hiệu ứng cửa hàng:
 *  1) Thêm vào giỏ: ảnh bay vào giỏ + toast + mini-cart trượt từ bên phải
 *  2) Thẻ sản phẩm nghiêng 3D + ánh sáng chạy theo con trỏ
 *  3) Trang chi tiết: thanh mua hàng dính ở đáy
 * Nạp bằng <script ... defer>. Không có JS thì form vẫn gửi bình thường.
 */
(() => {
    'use strict';

    const BASE = new URL('../', document.currentScript.src);
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)');
    const finePointer = window.matchMedia('(hover: hover) and (pointer: fine)');

    const AUTO_OPEN_DRAWER = false;   // true = thêm xong tự mở mini-cart
    const TILT_MAX = 7;               // độ nghiêng tối đa của thẻ (độ)

    /* ------------------------------------------------------------ tiện ích */

    const url = path => new URL(String(path).replace(/^\/+/, ''), BASE).href;

    const imageUrl = src => /^(https?:|data:)/.test(src || '') ? src : url(src || '');

    const money = value =>
        new Intl.NumberFormat('vi-VN').format(Math.round(Number(value) || 0)) + ' đ';

    function el(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }

    const onCartPage = () => /\/(cart|checkout)\/?$/.test(location.pathname);

    /* --------------------------------------------------------------- toast */

    let toastBox = null;

    function toast({ title, text, image, error = false, action }) {
        if (!toastBox) {
            toastBox = el('div', 'cs-toasts');
            toastBox.setAttribute('aria-live', 'polite');
            document.body.append(toastBox);
        }

        const node = el('div', 'cs-toast' + (error ? ' cs-toast--error' : ''));
        node.setAttribute('role', error ? 'alert' : 'status');

        if (image) {
            const img = el('img', 'cs-toast__img');
            img.src = image;
            img.alt = '';
            node.append(img);
        } else {
            node.append(el('span', 'cs-toast__icon', error ? '!' : '✓'));
        }

        const body = el('div', 'cs-toast__body');
        body.append(el('strong', '', title));
        if (text) body.append(el('span', '', text));
        node.append(body);

        let timer = 0;
        const dismiss = () => {
            clearTimeout(timer);
            node.classList.add('is-leaving');
            setTimeout(() => node.remove(), 300);
        };

        if (action) {
            const button = el('button', 'cs-toast__action', action.label);
            button.type = 'button';
            button.addEventListener('click', () => {
                dismiss();
                action.run();
            });
            node.append(button);
        }

        toastBox.append(node);
        while (toastBox.children.length > 3) toastBox.firstElementChild.remove();

        timer = setTimeout(dismiss, error ? 5500 : 4200);
    }

    /* ------------------------------------------------ dữ liệu giỏ hàng nhỏ */

    let lastMini = null;
    let csrfPair = null;

    async function fetchMini() {
        const response = await fetch(url('cart/mini'), {
            credentials: 'same-origin',
            cache: 'no-store',
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw new Error('HTTP ' + response.status);
        return response.json();
    }

    async function csrf() {
        const input = document.querySelector('input[name="_csrf"]');
        if (input) return [input.name, input.value];

        if (!csrfPair) {
            const response = await fetch(url('auth/session'), {
                credentials: 'same-origin',
                cache: 'no-store',
                headers: { Accept: 'application/json' }
            });
            const state = await response.json();
            csrfPair = [state.csrfParameter, state.csrfToken];
        }
        return csrfPair;
    }

    function applyMini(mini) {
        lastMini = mini;

        const quantity = Number(mini.totalQuantity) || 0;

        document.querySelectorAll('[data-cart-count]').forEach(badge => {
            badge.textContent = String(quantity);
        });
        window.ConsoleStoreSession?.setCartCount(quantity);

        if (drawer) renderDrawer(mini);
    }

    function popBadge() {
        if (reduce.matches) return;
        document.querySelectorAll('[data-cart-count]').forEach(badge => {
            badge.classList.remove('is-pop');
            void badge.offsetWidth;          // khởi động lại animation
            badge.classList.add('is-pop');
        });
    }

    /* -------------------------------------------------------- ảnh bay vào giỏ */

    function flyToCart(source) {
        const target = document.querySelector('[data-store-header] .cart-button')
            || document.querySelector('.cart-button');

        if (reduce.matches || !source || !target) return Promise.resolve();

        const from = source.getBoundingClientRect();
        const to = target.getBoundingClientRect();

        if (!from.width || !to.width) return Promise.resolve();

        const size = Math.max(60, Math.min(from.width, 150));
        const startX = from.left + from.width / 2 - size / 2;
        const startY = from.top + from.height / 2 - size / 2;
        const endX = to.left + to.width / 2 - size / 2;
        const endY = to.top + to.height / 2 - size / 2;

        const wrap = el('div', 'cs-fly');       // chạy theo trục X
        const lift = el('div', 'cs-fly__lift'); // chạy theo trục Y (đường cong)
        const img = el('img', 'cs-fly__img');

        img.src = source.currentSrc || source.src;
        img.alt = '';
        wrap.style.width = wrap.style.height = size + 'px';
        lift.style.width = lift.style.height = size + 'px';

        lift.append(img);
        wrap.append(lift);
        document.body.append(wrap);

        const duration = 850;

        wrap.animate(
            [{ transform: `translateX(${startX}px)` }, { transform: `translateX(${endX}px)` }],
            { duration, easing: 'cubic-bezier(.35, .1, .25, 1)', fill: 'forwards' }
        );
        lift.animate(
            [{ transform: `translateY(${startY}px)` }, { transform: `translateY(${endY}px)` }],
            { duration, easing: 'cubic-bezier(.55, 0, .85, .45)', fill: 'forwards' }
        );

        return img.animate(
            [
                { transform: 'scale(1) rotate(0deg)', opacity: 1 },
                { transform: 'scale(.16) rotate(25deg)', opacity: .35 }
            ],
            { duration, easing: 'ease-in', fill: 'forwards' }
        ).finished.catch(() => {}).then(() => wrap.remove());
    }

    /* ------------------------------------------------------ thêm vào giỏ hàng */

    function isAddForm(form) {
        if (!(form instanceof HTMLFormElement)) return false;
        if (form.closest('.cart-page') || form.querySelector('[name="next"]')) return false;

        try {
            return form.method.toLowerCase() === 'post'
                && /\/cart\/items\/?$/.test(new URL(form.action, location.href).pathname);
        } catch {
            return false;
        }
    }

    function sourceImage(form) {
        if (form.classList.contains('pd-cart-form')) {
            return document.querySelector('[data-gallery-main]');
        }
        return form.closest('.product-card, article')
            ?.querySelector('.product-image img, img') ?? null;
    }

    function productName(form) {
        if (form.classList.contains('pd-cart-form')) {
            return document.getElementById('productTitle')?.textContent.trim();
        }
        return form.closest('.product-card, article')
            ?.querySelector('h2, h3, h4')?.textContent.trim();
    }

    async function addToCart(form, submitter) {
        if (form.dataset.busy) return;
        form.dataset.busy = '1';
        if (submitter) submitter.disabled = true;

        const image = sourceImage(form);
        const name = productName(form) || 'Sản phẩm';
        const quantity = Number(form.querySelector('[name="quantity"]')?.value) || 1;

        try {
            const response = await fetch(form.action, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams(new FormData(form))
            });

            if (!response.ok) throw new Error('HTTP ' + response.status);

            // Bị chuyển sang trang khác (đăng nhập...): đi theo
            if (response.redirected && !/\/cart\/?$/.test(new URL(response.url).pathname)) {
                location.assign(response.url);
                return;
            }

            // Server trả về trang /cart kèm thông báo thành công hoặc lỗi tồn kho
            const doc = new DOMParser().parseFromString(await response.text(), 'text/html');
            const error = doc.querySelector('.cart-message--error');

            if (error) {
                toast({ title: 'Chưa thêm được vào giỏ', text: error.textContent.trim(), error: true });
                return;
            }

            const mini = fetchMini().catch(() => null);   // chạy song song với hiệu ứng bay

            await flyToCart(image);

            const data = await mini;
            if (data) applyMini(data);
            popBadge();

            toast({
                title: 'Đã thêm vào giỏ hàng',
                text: quantity > 1 ? `${name} × ${quantity}` : name,
                image: image ? (image.currentSrc || image.src) : null,
                action: { label: 'Xem giỏ', run: openDrawer }
            });

            if (AUTO_OPEN_DRAWER) openDrawer();
        } catch {
            toast({ title: 'Không thêm được vào giỏ', text: 'Vui lòng thử lại.', error: true });
        } finally {
            delete form.dataset.busy;
            if (submitter) submitter.disabled = false;
        }
    }

    document.addEventListener('submit', event => {
        if (!isAddForm(event.target)) return;
        event.preventDefault();
        addToCart(event.target, event.submitter);
    }, true);

    /* ---------------------------------------------------- mini-cart bên phải */

    let drawer = null;

    function buildDrawer() {
        drawer = document.createElement('dialog');
        drawer.className = 'cs-drawer';
        drawer.setAttribute('aria-label', 'Giỏ hàng nhanh');
        drawer.innerHTML =
            '<header class="cs-drawer__head">' +
            '<h2>Giỏ hàng <span data-drawer-count></span></h2>' +
            '<button class="cs-drawer__close" type="button" aria-label="Đóng giỏ hàng">✕</button>' +
            '</header>' +
            '<div class="cs-drawer__body" data-drawer-body></div>' +
            '<footer class="cs-drawer__foot" data-drawer-foot></footer>';

        document.body.append(drawer);

        drawer.addEventListener('click', event => {
            if (event.target === drawer || event.target.closest('.cs-drawer__close')) {
                closeDrawer();
                return;
            }

            const button = event.target.closest('[data-act]');
            if (!button) return;

            const line = button.closest('.cs-line');
            const id = line.dataset.id;
            const quantity = Number(line.dataset.qty);

            if (button.dataset.act === 'remove') {
                changeCart(`cart/items/${id}/remove`, {});
            } else if (button.dataset.act === 'inc') {
                changeCart(`cart/items/${id}/quantity`, { quantity: quantity + 1 });
            } else if (button.dataset.act === 'dec') {
                changeCart(`cart/items/${id}/quantity`, { quantity: Math.max(1, quantity - 1) });
            }
        });

        drawer.addEventListener('close', () => {
            drawer.classList.remove('is-closing');
            document.documentElement.classList.remove('cs-lock');
        });
    }

    function closeDrawer() {
        if (!drawer || !drawer.open) return;

        if (reduce.matches) {
            drawer.close();
            return;
        }

        drawer.classList.add('is-closing');

        const done = event => {
            if (event.target !== drawer) return;   // bỏ qua animation của phần tử con
            drawer.removeEventListener('animationend', done);
            if (drawer.open) drawer.close();
        };
        drawer.addEventListener('animationend', done);
        setTimeout(() => drawer.open && drawer.close(), 450);   // phòng khi không có animationend
    }

    function showSkeleton() {
        const body = drawer.querySelector('[data-drawer-body]');
        body.replaceChildren();
        for (let i = 0; i < 3; i++) {
            const row = el('div', 'cs-skel');
            row.append(el('span', 'cs-skel__img'), el('span', 'cs-skel__line'));
            body.append(row);
        }
        drawer.querySelector('[data-drawer-foot]').replaceChildren();
    }

    function lineNode(line) {
        const li = el('li', 'cs-line' + (line.available ? '' : ' is-unavailable'));
        li.dataset.id = line.productId;
        li.dataset.qty = line.quantity;

        const href = url('products/' + line.productId);

        const thumb = el('a', 'cs-line__img');
        thumb.href = href;
        const img = el('img');
        img.src = imageUrl(line.image);
        img.alt = '';
        img.loading = 'lazy';
        thumb.append(img);

        const info = el('div', 'cs-line__info');
        const name = el('a', 'cs-line__name', line.name);
        name.href = href;

        const controls = el('div', 'cs-line__controls');
        const qty = el('div', 'cs-qty');

        const minus = el('button', '', '−');
        minus.type = 'button';
        minus.dataset.act = 'dec';
        minus.setAttribute('aria-label', 'Giảm số lượng');
        minus.disabled = line.quantity <= 1;

        const value = el('span', 'cs-qty__value', String(line.quantity));

        const plus = el('button', '', '+');
        plus.type = 'button';
        plus.dataset.act = 'inc';
        plus.setAttribute('aria-label', 'Tăng số lượng');
        plus.disabled = Number(line.stock) > 0 && line.quantity >= Number(line.stock);

        qty.append(minus, value, plus);

        const remove = el('button', 'cs-line__remove', 'Xóa');
        remove.type = 'button';
        remove.dataset.act = 'remove';

        controls.append(qty, remove);
        info.append(name, el('span', 'cs-line__price', money(line.price)), controls);

        li.append(thumb, info);
        return li;
    }

    function renderDrawer(mini) {
        const body = drawer.querySelector('[data-drawer-body]');
        const foot = drawer.querySelector('[data-drawer-foot]');
        const count = Number(mini.totalQuantity) || 0;

        drawer.querySelector('[data-drawer-count]').textContent = count ? `(${count})` : '';
        body.replaceChildren();
        foot.replaceChildren();

        if (!mini.lines || !mini.lines.length) {
            const empty = el('div', 'cs-drawer__empty');
            empty.append(el('strong', '', 'Giỏ hàng đang trống'),
                el('span', '', 'Chọn một món để bắt đầu nhé.'));
            body.append(empty);

            const shop = el('a', 'cs-btn cs-btn--primary', 'Tiếp tục mua sắm');
            shop.href = url('products');
            foot.append(shop);
            return;
        }

        const list = el('ul', 'cs-drawer__list');
        mini.lines.forEach(line => list.append(lineNode(line)));
        body.append(list);

        const total = el('div', 'cs-drawer__total');
        total.append(el('span', '', 'Tạm tính'), el('strong', '', money(mini.totalAmount)));

        const actions = el('div', 'cs-drawer__actions');
        const view = el('a', 'cs-btn', 'Xem giỏ hàng');
        view.href = url('cart');
        const pay = el('a', 'cs-btn cs-btn--primary', 'Thanh toán');
        pay.href = url('checkout');
        actions.append(view, pay);

        foot.append(total, actions);
    }

    async function changeCart(path, params) {
        drawer.classList.add('is-busy');

        try {
            const [name, token] = await csrf();
            const response = await fetch(url(path), {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({ ...params, [name]: token })
            });

            if (!response.ok) throw new Error('HTTP ' + response.status);

            const doc = new DOMParser().parseFromString(await response.text(), 'text/html');
            const error = doc.querySelector('.cart-message--error');

            if (error) {
                toast({ title: 'Chưa cập nhật được', text: error.textContent.trim(), error: true });
            }

            applyMini(await fetchMini());
        } catch {
            toast({ title: 'Không cập nhật được giỏ hàng', text: 'Vui lòng thử lại.', error: true });
        } finally {
            drawer.classList.remove('is-busy');
        }
    }

    async function openDrawer() {
        if (typeof HTMLDialogElement === 'undefined') {
            location.assign(url('cart'));
            return;
        }

        if (!drawer) buildDrawer();
        if (drawer.open) return;

        if (lastMini) renderDrawer(lastMini);
        else showSkeleton();

        drawer.classList.remove('is-closing');
        drawer.showModal();
        document.documentElement.classList.add('cs-lock');

        try {
            applyMini(await fetchMini());
        } catch {
            drawer.querySelector('[data-drawer-body]').replaceChildren(
                el('div', 'cs-drawer__empty', 'Không tải được giỏ hàng. Vui lòng thử lại.')
            );
        }
    }

    // Bấm biểu tượng giỏ trên header -> mở mini-cart (trừ khi đang ở trang giỏ/thanh toán)
    document.addEventListener('click', event => {
        const link = event.target.closest?.('.cart-button');

        if (!link || event.button !== 0
            || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) {
            return;
        }
        if (onCartPage() || document.querySelector('.ops-main')) return;

        event.preventDefault();
        openDrawer();
    }, true);

    /* ------------------------------------------ thẻ sản phẩm nghiêng 3D */

    function initTilt() {
        if (!finePointer.matches || reduce.matches) return;

        let frame = 0;

        document.addEventListener('pointermove', event => {
            if (event.pointerType !== 'mouse' || frame) return;

            const card = event.target.closest?.('.product-card');
            if (!card) return;

            frame = requestAnimationFrame(() => {
                frame = 0;

                const rect = card.getBoundingClientRect();
                const x = (event.clientX - rect.left) / rect.width;
                const y = (event.clientY - rect.top) / rect.height;

                card.style.setProperty('--mx', (x * 100).toFixed(1) + '%');
                card.style.setProperty('--my', (y * 100).toFixed(1) + '%');
                card.style.setProperty('--ry', ((x - .5) * 2 * TILT_MAX).toFixed(2) + 'deg');
                card.style.setProperty('--rx', ((.5 - y) * 2 * TILT_MAX).toFixed(2) + 'deg');
            });
        }, { passive: true });
    }

    /* ------------------------------------- thanh mua hàng dính (chi tiết) */

    function initStickyBuy() {
        const form = document.querySelector('.pd-cart-form');
        const title = document.getElementById('productTitle');
        const trigger = form?.querySelector('[type="submit"]');

        if (!form || !title || !trigger) return;

        const image = document.querySelector('[data-gallery-main]');
        const price = document.querySelector('.pd-price');

        const bar = el('div', 'cs-buybar');
        bar.setAttribute('role', 'region');
        bar.setAttribute('aria-label', 'Mua hàng nhanh');
        bar.setAttribute('aria-hidden', 'true');
        bar.inert = true;

        const thumb = el('img', 'cs-buybar__img');
        thumb.alt = '';

        const info = el('div', 'cs-buybar__info');
        info.append(
            el('strong', '', title.textContent.trim()),
            el('span', '', price ? price.textContent.replace(/\s+/g, ' ').trim() : '')
        );

        const button = el('button', 'cs-btn cs-btn--primary', 'Thêm vào giỏ');
        button.type = 'button';
        button.addEventListener('click', () => form.requestSubmit());

        bar.append(thumb, info, button);
        document.body.append(bar);

        new IntersectionObserver(([entry]) => {
            const show = !entry.isIntersecting && entry.boundingClientRect.top < 0;

            if (show && image) thumb.src = image.currentSrc || image.src;

            bar.classList.toggle('is-visible', show);
            bar.inert = !show;
            bar.setAttribute('aria-hidden', String(!show));
        }).observe(trigger);
    }

    initTilt();
    initStickyBuy();
})();