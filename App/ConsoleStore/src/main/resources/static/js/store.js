(() => {
    'use strict';

    const script = document.currentScript;
    const base = new URL('../', script.src);

    const appUrl = path => {
        return new URL(path.replace(/^\//, ''), base).href;
    };

    const fallbackProduct = appUrl(
        '/images/ui/product-placeholder.svg'
    );

    const fallbackBanner = appUrl(
        '/images/ui/banner-placeholder.svg'
    );



    const imageStates = new WeakMap();


    let sessionState = null;
    let updateScheduled = false;

    function productUrl(value) {
        const raw = String(value || '').trim();

        if (!raw || raw === 'null' || raw === 'undefined') {
            return fallbackProduct;
        }

        const clean = raw.replace(/\\/g, '/');

        if (/^https?:\/\//i.test(clean)) {
            try {
                return new URL(clean).href;
            } catch {
                return fallbackProduct;
            }
        }

        const filename = clean
            .split('/')
            .pop()
            .split('?')[0]
            .split('#')[0];



        const marker = clean.lastIndexOf('images/');

        if (marker >= 0) {
            return appUrl(clean.slice(marker));
        }

        if (!clean.includes('/')
            && /\.(jpe?g|png|webp|avif|gif|svg)$/i.test(filename)) {
            return appUrl(
                '/images/products/' + encodeURIComponent(filename)
            );
        }

        return fallbackProduct;
    }

    window.ConsoleStoreImages = {
        productUrl
    };

    /* Product image fitting and fallbacks */



    function fitProduct(img) {
        if (!(img instanceof HTMLImageElement)) {
            return;
        }

        img.classList.add('cs-product-photo');
    }

    function prepareImage(img) {
        if (!(img instanceof HTMLImageElement)
            || imageStates.has(img)) {
            return;
        }

        const value = img.getAttribute('data-product-image')
            || img.getAttribute('src')
            || '';

        const product = img.hasAttribute('data-product-image')
            || /\/images\/products\//i.test(value)
            || Boolean(img.closest(
                '.product-image, .ui-product-gallery, '
                + '.ui-cart-line, .ui-ranking'
            ));

        const banner = /\/images\/banners\//i.test(value)
            && !/logo\.png/i.test(value);

        if (!product && !banner) {
            return;
        }

        const state = {
            product,
            failed: false,
            rendered: null
        };

        imageStates.set(img, state);

        const showFallback = () => {
            if (state.failed) {
                return;
            }

            state.failed = true;

            img.classList.add('cs-image-fallback');
            img.src = product ? fallbackProduct : fallbackBanner;
        };

        img.addEventListener('error', showFallback);

        img.addEventListener('load', () => {
            if (product) {
                fitProduct(img);
            }
        });

        if (product) {
            img.classList.add('cs-product-photo');

            const normalized = productUrl(value);

            if (img.src !== normalized) {
                img.src = normalized;
            }
        }

        if (img.complete) {
            if (img.naturalWidth > 0) {
                if (product) {
                    fitProduct(img);
                }
            } else {
                showFallback();
            }
        }
    }

    function scanImages(node) {
        if (node instanceof HTMLImageElement) {
            prepareImage(node);
        }

        if (node.querySelectorAll) {
            node.querySelectorAll('img').forEach(prepareImage);
        }
    }

    /* Mobile navigation */

    function initializeNavigation() {
        document.addEventListener('click', event => {
            if (!(event.target instanceof Element)) {
                return;
            }

            const toggle = event.target.closest('[data-store-menu]');

            if (toggle) {
                const header = toggle.closest('.site-header');

                if (!header) {
                    return;
                }

                const open = header.classList.toggle('menu-open');

                toggle.setAttribute(
                    'aria-expanded',
                    String(open)
                );

                return;
            }

            const navigationLink = event.target.closest(
                '.home-nav-links a'
            );

            if (navigationLink) {
                const header = navigationLink.closest('.site-header');

                header?.classList.remove('menu-open');

                header?.querySelector('[data-store-menu]')
                    ?.setAttribute('aria-expanded', 'false');
            }

            document.querySelectorAll(
                '.session-account[open]'
            ).forEach(menu => {
                if (!menu.contains(event.target)) {
                    menu.open = false;
                }
            });
        });

        document.addEventListener('keydown', event => {
            if (event.key !== 'Escape') {
                return;
            }

            document.querySelectorAll(
                '.session-account[open]'
            ).forEach(menu => {
                menu.open = false;
                menu.querySelector('summary')?.focus();
            });

            document.querySelectorAll(
                '.site-header.menu-open'
            ).forEach(header => {
                header.classList.remove('menu-open');

                header.querySelector('[data-store-menu]')
                    ?.setAttribute('aria-expanded', 'false');
            });
        });
    }

    /* Hero carousel */

    function initializeCarousel(root) {
        const slides = Array.from(
            root.querySelectorAll('[data-hero-slide]')
        );

        if (!slides.length) {
            return;
        }

        const dotsContainer = root.querySelector('.cs-carousel-dots');
        const pauseButton = root.querySelector('[data-hero-pause]');
        const pauseIcon = root.querySelector('[data-hero-pause-icon]');
        const counter = root.querySelector('[data-hero-count]');

        const reducedMotion = window.matchMedia(
            '(prefers-reduced-motion: reduce)'
        );

        const configuredInterval = Number(root.dataset.interval);

        const interval = Number.isFinite(configuredInterval)
        && configuredInterval >= 3000
            ? configuredInterval
            : 6500;

        const dots = [];

        let index = Math.max(
            0,
            slides.findIndex(slide => {
                return slide.classList.contains('is-active');
            })
        );

        let timer = null;
        let userPaused = reducedMotion.matches;
        let keyboardFocused = false;
        let touchStartX = null;
        const track = root.querySelector('.cs-showcase-track');
        const finePointer = window.matchMedia('(hover: hover) and (pointer: fine)');
        let slideRequest = 0;
        let slideAnimations = [];
        let pointerFrame = 0;

        track?.addEventListener('pointermove', event => {
            if (event.pointerType !== 'mouse' || reducedMotion.matches || !finePointer.matches) {
                return;
            }

            cancelAnimationFrame(pointerFrame);
            pointerFrame = requestAnimationFrame(() => {
                const art = slides[index].querySelector('.cs-slide-art');
                const rect = art.getBoundingClientRect();
                const x = event.clientX - rect.left;
                const y = event.clientY - rect.top;

                if (x < 0 || x > rect.width || y < 0 || y > rect.height) {
                    track.removeAttribute('data-hero-pointer');
                    return;
                }

                art.style.setProperty('--light-x', x + 'px');
                art.style.setProperty('--light-y', y + 'px');
                art.style.setProperty('--image-x', ((x / rect.width - 0.5) * 14) + 'px');
                art.style.setProperty('--image-y', ((y / rect.height - 0.5) * 10) + 'px');
                track.setAttribute('data-hero-pointer', '');
            });
        });

        track?.addEventListener('pointerleave', () => {
            cancelAnimationFrame(pointerFrame);
            track.removeAttribute('data-hero-pointer');
        });
        if (dotsContainer) {
            dotsContainer.replaceChildren();

            slides.forEach((slide, slideIndex) => {
                const button = document.createElement('button');
                const indicator = document.createElement('span');

                const title = slide.querySelector('img')?.alt
                    || 'Banner';

                button.type = 'button';
                button.dataset.heroDot = String(slideIndex);

                button.setAttribute(
                    'aria-label',
                    `Xem banner ${slideIndex + 1}: ${title}`
                );

                button.append(indicator);

                button.addEventListener('click', () => {
                    showSlide(slideIndex);
                });

                dotsContainer.append(button);
                dots.push(button);
            });
        }

        function updatePauseButton() {
            if (!pauseButton) {
                return;
            }

            pauseButton.setAttribute(
                'aria-pressed',
                String(userPaused)
            );

            pauseButton.setAttribute(
                'aria-label',
                userPaused
                    ? 'Tiếp tục tự chuyển'
                    : 'Tạm dừng tự chuyển'
            );

            if (pauseIcon) {
                pauseIcon.textContent = userPaused ? '▶' : '❚❚';
            }
        }

        function schedule() {
            window.clearTimeout(timer);
            timer = null;

            if (userPaused
                || keyboardFocused
                || document.hidden
                || slides.length < 2) {
                return;
            }

            timer = window.setTimeout(() => {
                showSlide(index + 1);
            }, interval);
        }

        async function showSlide(nextIndex) {
            const next = (nextIndex % slides.length + slides.length) % slides.length;
            const request = ++slideRequest;
            const previousSlide = slides[index];
            const nextSlide = slides[next];
            const nextImage = nextSlide.querySelector('.cs-slide-art img');

            if (nextImage && next !== index) {
                nextImage.loading = 'eager';

                if (typeof nextImage.decode === 'function') {
                    try {
                        await nextImage.decode();
                    } catch {
                        // Keep the existing image fallback.
                    }
                }
            }

            if (request !== slideRequest) {
                return;
            }

            for (const animation of slideAnimations) {
                animation.cancel();
            }

            const wipe = next !== index
                && !reducedMotion.matches
                && nextImage?.naturalWidth > 0
                && typeof nextSlide.animate === 'function';

            const startClip = nextIndex < index
                ? 'inset(0 100% 0 0)'
                : 'inset(0 0 0 100%)';

            root.toggleAttribute('data-hero-wipe', wipe);
            track?.removeAttribute('data-hero-pointer');
            index = next;

            slides.forEach((slide, slideIndex) => {
                const active = slideIndex === index;
                slide.classList.toggle('is-active', active);
                slide.setAttribute('aria-hidden', String(!active));
                slide.inert = !active;
                slide.setAttribute('aria-label', String(slideIndex + 1) + ' / ' + slides.length);
            });

            if (wipe) {
                const duration = 560;

                slideAnimations = [
                    previousSlide.animate(
                        [{ visibility: 'visible', opacity: 1 }, { visibility: 'visible', opacity: 1 }],
                        { duration }
                    ),
                    nextSlide.animate(
                        [{ clipPath: startClip }, { clipPath: 'inset(0)' }],
                        { duration, easing: 'cubic-bezier(0.22, 1, 0.36, 1)' }
                    )
                ];
            }

            dots.forEach((dot, dotIndex) => {
                dot.setAttribute('aria-current', String(dotIndex === index));
            });

            if (counter) {
                const current = String(index + 1).padStart(2, '0');
                const total = String(slides.length).padStart(2, '0');
                counter.textContent = current + ' / ' + total;
            }

            schedule();
        }

        root.querySelector('[data-hero-prev]')
            ?.addEventListener('click', () => {
                showSlide(index - 1);
            });

        root.querySelector('[data-hero-next]')
            ?.addEventListener('click', () => {
                showSlide(index + 1);
            });

        pauseButton?.addEventListener('click', () => {
            userPaused = !userPaused;

            updatePauseButton();
            schedule();
        });

        root.addEventListener('focusin', event => {
            keyboardFocused = event.target.matches(':focus-visible');
            schedule();
        });

        root.addEventListener('focusout', event => {
            if (!root.contains(event.relatedTarget)) {
                keyboardFocused = false;
                schedule();
            }
        });

        root.addEventListener('pointerdown', () => {
            keyboardFocused = false;
            schedule();
        });

        root.addEventListener('keydown', event => {
            if (event.key === 'ArrowLeft') {
                event.preventDefault();
                showSlide(index - 1);
            } else if (event.key === 'ArrowRight') {
                event.preventDefault();
                showSlide(index + 1);
            }
        });

        root.addEventListener('touchstart', event => {
            touchStartX = event.touches[0].clientX;
        }, {passive: true});

        root.addEventListener('touchend', event => {
            if (touchStartX === null) {
                return;
            }

            const distance =
                event.changedTouches[0].clientX - touchStartX;

            if (Math.abs(distance) > 55) {
                showSlide(index + (distance < 0 ? 1 : -1));
            }

            touchStartX = null;
        }, {passive: true});

        root.addEventListener('touchcancel', () => {
            touchStartX = null;
        }, {passive: true});

        document.addEventListener('visibilitychange', schedule);

        reducedMotion.addEventListener('change', event => {
            userPaused = event.matches;

            updatePauseButton();
            schedule();
        });

        updatePauseButton();
        showSlide(index);
    }

    /* Session menu and cart count */

    function createLink(text, path, className = 'login-button') {
        const link = document.createElement('a');

        link.textContent = text;
        link.href = appUrl(path);
        link.className = className;

        return link;
    }

    function createLogoutForm(state) {
        const form = document.createElement('form');

        form.method = 'post';
        form.action = appUrl('/logout');
        form.className = 'session-logout';

        const token = document.createElement('input');

        token.type = 'hidden';
        token.name = state.csrfParameter;
        token.value = state.csrfToken;

        const button = document.createElement('button');

        button.type = 'submit';
        button.textContent = 'Đăng xuất';

        form.append(token, button);

        return form;
    }

    function createCartButton(state) {
        const link = document.createElement('a');

        link.className = 'cart-button';

        link.href = appUrl('/cart');

        link.setAttribute('aria-label', 'Xem giỏ hàng');

        const namespace = 'http://www.w3.org/2000/svg';
        const svg = document.createElementNS(namespace, 'svg');

        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('fill', 'none');
        svg.setAttribute('stroke', 'currentColor');
        svg.setAttribute('stroke-width', '1.6');
        svg.setAttribute('stroke-linecap', 'round');
        svg.setAttribute('stroke-linejoin', 'round');
        svg.setAttribute('aria-hidden', 'true');

        const path = document.createElementNS(namespace, 'path');

        path.setAttribute(
            'd',
            'M3 3h2l2.2 11h10.6l2-8H6M8 14l-1 3h12'
        );

        svg.append(path);

        [9, 18].forEach(x => {
            const circle = document.createElementNS(
                namespace,
                'circle'
            );

            circle.setAttribute('cx', String(x));
            circle.setAttribute('cy', '20');
            circle.setAttribute('r', '1');

            svg.append(circle);
        });

        const count = document.createElement('span');

        count.className = 'cart-count';
        count.dataset.cartCount = '';
        count.textContent = String(state.cartCount);

        link.append(svg, count);

        return link;
    }

    function createAccountMenu(state) {
        const roleLabels = {
            ADMIN: 'Quản trị viên',
            NHANVIEN: 'Nhân viên',
            BANHANG: 'Nhân viên bán hàng',
            KHO: 'Nhân viên kho',
            BAOHANH: 'Nhân viên bảo hành',
            KHACHHANG: 'Khách hàng'
        };

        const menu = document.createElement('details');

        menu.className = 'session-account';

        const summary = document.createElement('summary');
        const avatar = document.createElement('span');
        const identity = document.createElement('span');
        const username = document.createElement('strong');
        const role = document.createElement('small');
        const arrow = document.createElement('span');

        avatar.className = 'account-avatar';
        avatar.setAttribute('aria-hidden', 'true');

        avatar.textContent = String(state.username)
            .slice(0, 2)
            .toUpperCase();

        identity.className = 'account-identity';
        username.textContent = state.username;
        role.textContent = roleLabels[state.role] || 'Tài khoản';

        identity.append(username, role);

        arrow.className = 'account-arrow';
        arrow.setAttribute('aria-hidden', 'true');
        arrow.textContent = '⌄';

        summary.append(avatar, identity, arrow);

        const dropdown = document.createElement('div');

        dropdown.className = 'account-popover';

        if (state.role === 'KHACHHANG') {
            dropdown.append(
                createLink(
                    'Hồ sơ tài khoản',
                    '/account',
                    'account-menu-link'
                ),
                createLink(
                    'Đơn hàng của tôi',
                    '/orders',
                    'account-menu-link'
                )
            );
        } else {
            dropdown.append(
                createLink(
                    'Vào khu nghiệp vụ',
                    state.landingPage,
                    'account-menu-link'
                )
            );
        }

        dropdown.append(createLogoutForm(state));

        menu.append(summary, dropdown);

        return menu;
    }
    function createOperationsMenu(state) {
        const allowed = new Set(state.allowedPages || []);

        const groups = [
            {
                title: 'Bán hàng',
                items: [
                    ['pos', 'Bán hàng tại quầy', '/pos'],
                    ['online-orders', 'Đơn hàng online', '/online-orders'],
                    ['invoices', 'Hóa đơn', '/invoices']
                ]
            },
            {
                title: 'Kho & hậu mãi',
                items: [
                    ['inventory', 'Tồn kho', '/inventory'],
                    ['purchases', 'Nhập hàng', '/purchases'],
                    ['trade-ins', 'Thu mua thiết bị cũ', '/trade-ins'],
                    ['warranties', 'Bảo hành', '/warranties']
                ]
            },
            {
                title: 'Quản lý',
                adminOnly: true,
                items: [
                    ['customers', 'Khách hàng', '/customers'],
                    ['payments', 'Duyệt thanh toán', '/payments'],
                    ['purchases', 'Nhập hàng', '/purchases'],
                    ['suppliers', 'Nhà cung cấp', '/suppliers'],
                    ['manage-products', 'Sản phẩm', '/manage-products'],
                    ['staff', 'Nhân viên & tài khoản', '/staff']
                ]
            },
            {
                title: 'Báo cáo',
                adminOnly: true,
                items: [
                    ['revenue', 'Doanh thu', '/revenue'],
                    ['purchase-costs', 'Chi phí nhập hàng', '/purchase-costs'],
                    ['stock-reports', 'Báo cáo hàng hóa', '/stock-reports']
                ]
            }
        ];

        const menu = document.createElement('details');
        menu.className = 'session-account operations-menu';

        const summary = document.createElement('summary');
        summary.textContent = 'Nghiệp vụ';

        const arrow = document.createElement('span');
        arrow.textContent = '⌄';
        arrow.setAttribute('aria-hidden', 'true');

        summary.append(arrow);

        const panel = document.createElement('div');
        panel.className = 'account-popover operations-grid';

        for (const group of groups) {
            if (group.adminOnly && state.role !== 'ADMIN') {
                continue;
            }

            const items = group.items.filter(([key]) => allowed.has(key));

            if (items.length === 0) {
                continue;
            }

            const section = document.createElement('section');
            section.className = 'operations-group';

            const heading = document.createElement('h2');
            heading.textContent = group.title;

            section.append(heading);

            for (const [, label, route] of items) {
                const link = createLink(
                    label,
                    route,
                    'account-menu-link'
                );

                const pathname = new URL(appUrl(route)).pathname;

                if (
                    location.pathname === pathname
                    || location.pathname.startsWith(pathname + '/')
                ) {
                    link.setAttribute('aria-current', 'page');
                }

                section.append(link);
            }

            panel.append(section);
        }

        if (panel.childElementCount === 0) {
            return document.createDocumentFragment();
        }

        menu.append(summary, panel);

        return menu;
    }
    function updateActions(container, state) {
        if (container.dataset.sessionReady === 'true') {
            return;
        }

        container.dataset.sessionReady = 'true';

        const actions = document.createElement('div');

        actions.className = 'session-actions';

        if (state.authenticated) {
            if (['ADMIN', 'NHANVIEN', 'BANHANG', 'KHO', 'BAOHANH'].includes(state.role)) {
                actions.append(createOperationsMenu(state));
            }

            actions.append(createAccountMenu(state));
        } else {
            actions.append(
                createLink('Đăng nhập', '/login'),
                createLink(
                    'Tạo tài khoản ↗',
                    '/register',
                    'register-button'
                )
            );
        }

        if (!state.authenticated || state.role === 'KHACHHANG') {
            actions.append(createCartButton(state));
        }

        if (container.matches('.ops-topbar')) {
            container.append(actions);
        } else {
            container.replaceChildren(actions);
        }
    }

    function updateRestrictedLinks(state) {
        const allowed = new Set(state.allowedPages || []);
        const restricted = new Set(state.restrictedPages || []);

        document.querySelectorAll('a[href]').forEach(link => {
            const url = new URL(link.href, location.href);

            if (url.origin !== base.origin
                || !url.pathname.startsWith(base.pathname)) {
                return;
            }

            const path = url.pathname.slice(base.pathname.length);
            const match = path.match(/^pages\/([^/]+)\.html$/);

            let page = match ? match[1] : null;

            if (path === 'cart' || path.startsWith('cart/')) {
                page = 'cart';
            }

            if (page && restricted.has(page)) {
                link.hidden = !allowed.has(page);
            }
        });

        document.querySelectorAll(
            '.ops-sidebar nav section'
        ).forEach(section => {
            const hasVisibleLink = Array.from(
                section.querySelectorAll('a')
            ).some(link => !link.hidden);

            section.hidden = !hasVisibleLink;
        });
    }

    function applySessionUI() {
        if (!sessionState) {
            return;
        }

        document.querySelectorAll(
            '[data-store-header] .header-actions, .ops-topbar'
        ).forEach(container => {
            updateActions(container, sessionState);
        });

        updateRestrictedLinks(sessionState);
        document.querySelectorAll('[data-customer-link]').forEach(link => {
            link.hidden = sessionState.authenticated
                && sessionState.role !== 'KHACHHANG';
        });

        const count = String(sessionState.cartCount);

        document.querySelectorAll(
            '[data-cart-count]'
        ).forEach(element => {
            if (element.textContent !== count) {
                element.textContent = count;
            }
        });
    }

    function scheduleSessionUI() {
        if (updateScheduled) {
            return;
        }

        updateScheduled = true;

        window.requestAnimationFrame(() => {
            updateScheduled = false;
            applySessionUI();
        });
    }

    async function readJson(path) {
        const response = await fetch(
            appUrl(path),
            {
                credentials: 'same-origin',
                cache: 'no-store',
                headers: {
                    Accept: 'application/json'
                }
            }
        );

        const contentType =
            response.headers.get('content-type') || '';

        if (!response.ok
            || response.redirected
            || !contentType.includes('application/json')) {
            throw new Error('Cannot load ' + path);
        }

        return response.json();
    }

    async function initializeSession() {
        try {
            const state = await readJson('/auth/session');

            state.cartCount = 0;

            if (state.role === 'KHACHHANG' || state.role === 'GUEST') {
                try {
                    const cart = await readJson('/cart/count');
                    const quantity = Number(cart.quantity);

                    if (Number.isSafeInteger(quantity)
                        && quantity >= 0) {
                        state.cartCount = quantity;
                    }
                } catch {
                    // Bộ đếm hoạt động sau khi phần Java CN05 được áp dụng.
                }
            }

            sessionState = state;

            applySessionUI();
        } catch (error) {
            console.error('Cannot initialize session menu', error);
        }
    }
    function updateMainNavigation() {
        const url = new URL(location.href);

        const homePath = new URL(appUrl('/')).pathname;
        const productsPath = new URL(appUrl('/products')).pathname;

        const category = (
            url.searchParams.get('category') || ''
        ).toLocaleLowerCase('vi');

        let active = null;

        if (
            url.pathname === homePath
            || url.pathname === new URL(appUrl('/home')).pathname
            || url.pathname === new URL(appUrl('/pages/home.html')).pathname
        ) {
            active = 'home';
        } else if (
            url.pathname === productsPath
            || url.pathname.startsWith(productsPath + '/')
        ) {
            if (category === 'đĩa game') {
                active = 'games';
            } else if (category === 'tay cầm') {
                active = 'controllers';
            } else if (
                category === 'máy console'
                || category === 'máy chơi game'
            ) {
                active = 'consoles';
            }
        }

        document.querySelectorAll('[data-main-nav]').forEach(link => {
            if (link.dataset.mainNav === active) {
                link.setAttribute('aria-current', 'page');
            } else {
                link.removeAttribute('aria-current');
            }
        });
    }

    function initializeDropdowns() {
        document.addEventListener('click', event => {
            document.querySelectorAll(
                '.session-account[open]'
            ).forEach(menu => {
                if (!menu.contains(event.target)) {
                    menu.open = false;
                }
            });
        });

        document.addEventListener('keydown', event => {
            if (event.key !== 'Escape') {
                return;
            }

            document.querySelectorAll(
                '.session-account[open]'
            ).forEach(menu => {
                menu.open = false;
                menu.querySelector('summary')?.focus();
            });
        });

        document.addEventListener('toggle', event => {
            const opened = event.target;

            if (
                !(opened instanceof HTMLDetailsElement)
                || !opened.matches('.session-account')
                || !opened.open
            ) {
                return;
            }

            document.querySelectorAll(
                '.session-account[open]'
            ).forEach(menu => {
                if (menu !== opened) {
                    menu.open = false;
                }
            });
        }, true);
    }

    function initialize() {
        scanImages(document);
        initializeNavigation();
        updateMainNavigation();
        initializeDropdowns();
        document.querySelectorAll(
            '[data-hero-carousel]'
        ).forEach(initializeCarousel);

        const observer = new MutationObserver(records => {
            records.forEach(record => {
                record.addedNodes.forEach(scanImages);
            });

            scheduleSessionUI();
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });

        initializeSession();
    }

    if (document.readyState === 'loading') {
        document.addEventListener(
            'DOMContentLoaded',
            initialize,
            {once: true}
        );
    } else {
        initialize();
    }

    window.addEventListener('pageshow', event => {
        if (event.persisted) {
            location.reload();
        }
    });
})();

