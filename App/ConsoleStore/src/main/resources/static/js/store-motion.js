(() => {
    const motion = window.matchMedia('(prefers-reduced-motion: no-preference)');
    const base = new URL('../', document.currentScript.src);

    function storeUrl(href) {
        const url = new URL(href);

        if (url.origin !== base.origin || !url.pathname.startsWith(base.pathname)) {
            return null;
        }

        const path = url.pathname.slice(base.pathname.length).replace(/\/$/, '');
        const productId = /^products\/([1-9]\d*)$/.exec(path)?.[1];
        const isListing = ['', 'products', 'catalog.html'].includes(path);

        return { productId, isListing };
    }

    let clickedLink = null;

    document.addEventListener('click', event => {
        clickedLink = event.target.closest?.('.product-card a[href]') ?? null;
    }, { capture: true });

    window.addEventListener('pageswap', event => {
        if (!motion.matches || !event.viewTransition || !event.activation?.entry) {
            return;
        }

        const current = storeUrl(location.href);
        const next = storeUrl(event.activation.entry.url);

        if (!current?.isListing || !next?.productId) {
            return;
        }

        const matchesProduct = link => storeUrl(link.href)?.productId === next.productId;
        const link = clickedLink?.isConnected && matchesProduct(clickedLink)
            ? clickedLink
            : Array.from(document.querySelectorAll('.product-card a[href]')).find(matchesProduct);

        const image = link?.closest('.product-card')?.querySelector('.product-image img');

        if (!image?.complete || !image.naturalWidth) {
            return;
        }

        image.style.viewTransitionName = 'console-product';
        const clearName = () => image.style.removeProperty('view-transition-name');
        event.viewTransition.finished.then(clearName, clearName);
    });

    window.addEventListener('pagereveal', event => {
        const from = window.navigation?.activation?.from;

        if (!motion.matches || !event.viewTransition || !from
            || !storeUrl(from.url)?.isListing || !storeUrl(location.href)?.productId) {
            return;
        }

        const image = document.querySelector('[data-gallery-main]');

        if (!image) {
            return;
        }

        image.style.viewTransitionName = 'console-product';
        const clearName = () => image.style.removeProperty('view-transition-name');
        event.viewTransition.finished.then(clearName, clearName);
    });

    document.addEventListener('DOMContentLoaded', () => {
        if (!motion.matches || !('IntersectionObserver' in window)
            || typeof Element.prototype.animate !== 'function') {
            return;
        }

        const targets = document.querySelectorAll([
            '.home-page .cs-home-heading',
            '.home-page .cs-decoration-heading',
            '.home-page .cs-decoration-card',
            '.home-page .cs-editorial-card',
            '.home-page .home-product-grid > div',
            '.home-page .home-accessory-grid > div',
            '.catalog-page .catalog-results > div',
            '.product-detail-page .pd-content-grid > .pd-panel'
        ].join(', '));

        const observer = new IntersectionObserver(entries => {
            let delay = 0;

            for (const entry of entries) {
                if (!entry.isIntersecting) {
                    continue;
                }

                const element = entry.target;
                observer.unobserve(element);

                if (!motion.matches || element.contains(document.activeElement)) {
                    continue;
                }

                element.animate(
                    [
                        { opacity: 0.3, translate: '0 28px' },
                        { opacity: 1, translate: '0 0' }
                    ],
                    {
                        duration: 500,
                        delay,
                        fill: 'backwards',
                        easing: 'cubic-bezier(0.22, 1, 0.36, 1)'
                    }
                );

                delay = Math.min(delay + 55, 165);
            }
        }, { threshold: 0.12 });

        const viewportBottom = document.documentElement.clientHeight;

        for (const element of targets) {
            if (element.getBoundingClientRect().top >= viewportBottom) {
                observer.observe(element);
            }
        }
    }, { once: true });
})();