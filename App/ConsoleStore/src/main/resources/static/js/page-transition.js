/* page-transition.js
 * Hiệu ứng chuyển trang cho Console Store:
 *  1) Trang mới "loang" ra từ đúng chỗ vừa bấm (View Transitions API).
 *  2) Thanh gradient chạy trên đầu trang ngay khi bấm (mọi trình duyệt).
 * Đặt trong <head>, KHÔNG thêm defer/async (pagereveal phải đăng ký sớm).
 */
(() => {
    'use strict';

    const KEY = 'cs:nav-origin';
    const root = document.documentElement;
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)');
    /* ---------- Màu nhấn theo nền tảng ---------- */
    (function applyPlatform() {
        const KEYS = ['playstation', 'xbox', 'nintendo', 'pc'];

        const keyOf = name => {
            const s = String(name || '').toLowerCase();
            if (s.includes('playstation') || /\bps\d?\b/.test(s)) return 'playstation';
            if (s.includes('xbox')) return 'xbox';
            if (s.includes('nintendo') || s.includes('switch')) return 'nintendo';
            if (s.includes('pc') || s.includes('steam')) return 'pc';
            return null;
        };

        try {
            const home = new URL('../', document.currentScript.src).pathname;
            const asked = new URLSearchParams(location.search).get('platform');
            const onHome = location.pathname === home || location.pathname === home + 'home';

            let current = null;

            if (asked) {
                current = keyOf(asked);                               // /products?platform=Xbox
            } else if (!onHome) {
                current = localStorage.getItem('cs:platform');        // trang khác: giữ màu đã chọn
            }                                                         // trang chủ: về màu tím mặc định

            if (KEYS.includes(current)) {
                localStorage.setItem('cs:platform', current);
                root.dataset.platform = current;
            } else {
                localStorage.removeItem('cs:platform');
            }
        } catch { /* bỏ qua */ }
    })();

    // Trình duyệt chưa hỗ trợ View Transitions -> CSS sẽ dùng fade-in đơn giản
    if (!('onpagereveal' in window)) {
        root.classList.add('cs-no-vt');
    }

    /* ---------------- Thanh tiến trình ---------------- */
    let bar = null;

    function hideBar() {
        bar?.remove();
        bar = null;
    }

    function showBar() {
        if (reduce.matches || !document.body) return;
        hideBar();
        bar = document.createElement('div');
        bar.className = 'cs-nav-bar';
        bar.setAttribute('aria-hidden', 'true');
        document.body.appendChild(bar);
        bar.getBoundingClientRect();        // ép tính layout để transition chạy
        bar.classList.add('is-running');
    }

    // Chỉ hiện nếu sự kiện không bị code khác huỷ (confirm, fetch...)
    function showBarUnlessCancelled(event) {
        setTimeout(() => {
            if (!event.defaultPrevented) showBar();
        }, 0);
    }

    // Quay lại bằng nút Back (bfcache) -> bỏ thanh cũ
    window.addEventListener('pageshow', hideBar);
    window.csShowNavBar = showBar;
    function isPageLink(a, event) {
        if (!a || event.button !== 0) return false;
        if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return false;
        if (a.target || a.hasAttribute('download')) return false;
        if (a.getAttribute('href').startsWith('#')) return false;
        if (a.origin !== location.origin) return false;
        // chỉ đổi #hash trong cùng trang thì không tính
        return !(a.pathname === location.pathname && a.search === location.search && a.hash);
    }

    /* ---------------- Trang cũ: nhớ chỗ vừa bấm ---------------- */
    let lastClick = null;

    document.addEventListener('click', event => {
        const target = event.target instanceof Element ? event.target : null;
        if (!target) return;

        let x = event.clientX;
        let y = event.clientY;

        if (event.detail === 0) {           // bấm bằng bàn phím (Enter)
            const r = target.getBoundingClientRect();
            x = r.left + r.width / 2;
            y = r.top + r.height / 2;
        }

        lastClick = { x, y, t: Date.now() };

        if (isPageLink(target.closest('a[href]'), event)) {
            showBarUnlessCancelled(event);
        }
    }, { capture: true });

    document.addEventListener('submit', event => {
        if (event.target.target !== '_blank') showBarUnlessCancelled(event);
    });

    window.addEventListener('pageswap', event => {
        if (!event.viewTransition || !lastClick) return;
        if (event.activation?.navigationType === 'traverse') return;   // nút Back/Forward
        if (Date.now() - lastClick.t > 10000) return;

        try {
            sessionStorage.setItem(KEY, JSON.stringify(lastClick));
        } catch { /* bỏ qua */ }
    });

    /* ---------------- Trang mới: loang tròn từ chỗ đã bấm ---------------- */
    window.addEventListener('pagereveal', event => {
        const transition = event.viewTransition;
        if (!transition || reduce.matches) return;

        let origin = null;
        try {
            origin = JSON.parse(sessionStorage.getItem(KEY) || 'null');
            sessionStorage.removeItem(KEY);
        } catch { /* bỏ qua */ }

        // Không rõ chỗ bấm -> để CSS dùng hiệu ứng mặc định
        if (!origin || Date.now() - origin.t > 15000) return;

        const { x, y } = origin;
        const radius = Math.hypot(
            Math.max(x, innerWidth - x),
            Math.max(y, innerHeight - y)
        );

        root.classList.add('cs-circle');
        const clear = () => root.classList.remove('cs-circle');
        transition.finished.then(clear, clear);

        transition.ready.then(() => {
            root.animate(
                {
                    clipPath: [
                        `circle(0px at ${x}px ${y}px)`,
                        `circle(${radius}px at ${x}px ${y}px)`
                    ]
                },
                {
                    duration: 700,
                    easing: 'cubic-bezier(0.22, 1, 0.36, 1)',
                    pseudoElement: '::view-transition-new(root)'
                }
            );
        }).catch(() => {});
    });
})();