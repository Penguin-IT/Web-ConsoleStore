/* Thư viện ảnh + lightbox + chọn số lượng cho trang chi tiết sản phẩm. */
(function () {
    'use strict';

    /* ---------- Thư viện ảnh ---------- */

    function initGallery(root) {
        const stage = root.querySelector('[data-gallery-stage]');
        const main = root.querySelector('[data-gallery-main]');
        if (!stage || !main) {
            return;
        }

        const thumbs = Array.from(root.querySelectorAll('.pd-thumb'));
        const counter = root.querySelector('[data-gallery-counter]');
        const alt = main.getAttribute('alt') || '';

        // Nếu chỉ có 1 ảnh (không có thumbnail) thì dùng chính ảnh chính.
        const sources = thumbs.length
            ? thumbs.map((t) => t.dataset.src)
            : [main.getAttribute('src')];

        const lightbox = document.querySelector('[data-lightbox]');
        const lbImg = lightbox && lightbox.querySelector('[data-lightbox-img]');
        const lbCaption = lightbox && lightbox.querySelector('[data-lightbox-caption]');

        let index = 0;

        function wrap(i) {
            return (i + sources.length) % sources.length;
        }

        function show(i, immediate) {
            index = wrap(i);

            const apply = () => {
                main.src = sources[index];
                main.removeAttribute('srcset');
                main.classList.remove('is-swapping');
            };

            if (immediate || sources.length === 1) {
                apply();
            } else {
                main.classList.add('is-swapping');
                window.setTimeout(apply, 120);
            }

            thumbs.forEach((thumb, n) => {
                const active = n === index;
                thumb.classList.toggle('is-active', active);
                thumb.setAttribute('aria-current', active ? 'true' : 'false');
                if (active) {
                    thumb.scrollIntoView({block: 'nearest', inline: 'nearest'});
                }
            });

            if (counter) {
                counter.textContent = String(index + 1);
            }

            if (lightbox && lightbox.open) {
                syncLightbox();
            }
        }

        function syncLightbox() {
            if (!lbImg) {
                return;
            }
            lbImg.src = sources[index];
            lbImg.alt = alt;
            if (lbCaption) {
                lbCaption.textContent = sources.length > 1
                    ? alt + ' — ' + (index + 1) + '/' + sources.length
                    : alt;
            }
        }

        function openLightbox() {
            if (!lightbox || typeof lightbox.showModal !== 'function') {
                window.open(sources[index], '_blank', 'noopener');
                return;
            }
            const single = sources.length < 2;
            lightbox.querySelectorAll('[data-lightbox-prev],[data-lightbox-next]')
                .forEach((btn) => { btn.hidden = single; });
            syncLightbox();
            lightbox.showModal();
        }

        thumbs.forEach((thumb, n) => thumb.addEventListener('click', () => show(n)));

        const prev = root.querySelector('[data-gallery-prev]');
        const next = root.querySelector('[data-gallery-next]');
        if (prev) prev.addEventListener('click', () => show(index - 1));
        if (next) next.addEventListener('click', () => show(index + 1));

        // Phóng to bằng nút hoặc bấm vào ảnh
        const openBtn = root.querySelector('[data-gallery-open]');
        if (openBtn) openBtn.addEventListener('click', openLightbox);
        main.addEventListener('click', openLightbox);

        // Phím mũi tên khi đang focus trong thư viện
        root.addEventListener('keydown', (event) => {
            if (sources.length < 2) return;
            if (event.key === 'ArrowLeft') { event.preventDefault(); show(index - 1); }
            if (event.key === 'ArrowRight') { event.preventDefault(); show(index + 1); }
        });

        // Zoom theo vị trí con trỏ (chỉ có tác dụng khi CSS bật hover zoom)
        stage.addEventListener('pointermove', (event) => {
            if (event.pointerType !== 'mouse') return;
            const rect = stage.getBoundingClientRect();
            const x = ((event.clientX - rect.left) / rect.width) * 100;
            const y = ((event.clientY - rect.top) / rect.height) * 100;
            main.style.setProperty('--zx', x.toFixed(1) + '%');
            main.style.setProperty('--zy', y.toFixed(1) + '%');
        });

        // Vuốt trái/phải trên điện thoại
        let startX = null;
        stage.addEventListener('pointerdown', (event) => {
            if (event.pointerType === 'touch') startX = event.clientX;
        });
        stage.addEventListener('pointerup', (event) => {
            if (startX === null || sources.length < 2) { startX = null; return; }
            const dx = event.clientX - startX;
            startX = null;
            if (Math.abs(dx) > 45) show(dx < 0 ? index + 1 : index - 1);
        });
        stage.addEventListener('pointercancel', () => { startX = null; });

        // Lightbox
        if (lightbox) {
            const lbPrev = lightbox.querySelector('[data-lightbox-prev]');
            const lbNext = lightbox.querySelector('[data-lightbox-next]');
            const lbClose = lightbox.querySelector('[data-lightbox-close]');

            if (lbPrev) lbPrev.addEventListener('click', () => show(index - 1, true));
            if (lbNext) lbNext.addEventListener('click', () => show(index + 1, true));
            if (lbClose) lbClose.addEventListener('click', () => lightbox.close());

            // Bấm ra ngoài ảnh thì đóng
            lightbox.addEventListener('click', (event) => {
                if (event.target === lightbox) lightbox.close();
            });

            lightbox.addEventListener('keydown', (event) => {
                if (sources.length < 2) return;
                if (event.key === 'ArrowLeft') show(index - 1, true);
                if (event.key === 'ArrowRight') show(index + 1, true);
            });
        }
    }

    /* ---------- Chọn số lượng ---------- */

    function initQuantity(root) {
        const input = root.querySelector('input[type="number"]');
        const minus = root.querySelector('[data-qty-minus]');
        const plus = root.querySelector('[data-qty-plus]');
        if (!input) {
            return;
        }

        const min = Number(input.min) || 1;
        const max = Number(input.max) || Infinity;

        function clamp() {
            let value = Math.floor(Number(input.value));
            if (!Number.isFinite(value)) value = min;
            input.value = String(Math.min(max, Math.max(min, value)));
        }

        function step(delta) {
            input.value = String((Math.floor(Number(input.value)) || min) + delta);
            clamp();
        }

        if (minus) minus.addEventListener('click', () => step(-1));
        if (plus) plus.addEventListener('click', () => step(1));
        input.addEventListener('change', clamp);
    }

    function init() {
        document.querySelectorAll('[data-gallery]').forEach(initGallery);
        document.querySelectorAll('[data-qty]').forEach(initQuantity);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
