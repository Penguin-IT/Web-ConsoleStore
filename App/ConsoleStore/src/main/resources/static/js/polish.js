/* Console Store - hiệu ứng nhỏ: header kính mờ, ảnh hiện mượt, thẻ hiện dần khi cuộn. */
(function () {
    'use strict';

    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    var header = document.querySelector('.site-header');
    if (header) {
        var ticking = false;
        var update = function () {
            header.classList.toggle('is-scrolled', window.scrollY > 24);
            ticking = false;
        };
        window.addEventListener('scroll', function () {
            if (!ticking) { ticking = true; requestAnimationFrame(update); }
        }, { passive: true });
        update();
    }

    // Ảnh sản phẩm: mờ -> rõ khi tải xong, bỏ khung shimmer.
    function watch(img) {
        var frame = img.closest('.product-image');
        if (!frame) { return; }
        var done = function () {
            img.removeAttribute('data-pending');
            frame.classList.add('is-loaded');
        };
        if (img.complete && img.naturalWidth > 0) { done(); return; }
        img.setAttribute('data-pending', '');
        img.addEventListener('load', done, { once: true });
        img.addEventListener('error', done, { once: true });
    }
    document.querySelectorAll('.product-card img').forEach(watch);

    // Thẻ sản phẩm và khối form hiện dần khi cuộn tới.
    if (!reduceMotion && 'IntersectionObserver' in window) {
        var io = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add('is-visible');
                    io.unobserve(entry.target);
                }
            });
        }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });

        document
            .querySelectorAll('.product-card, .checkout-panel:not(.checkout-summary)')
            .forEach(function (el, i) {
                el.classList.add('cs-reveal');
                el.style.setProperty('--reveal-delay', (i % 6) * 60 + 'ms');
                io.observe(el);
            });
    }
})();