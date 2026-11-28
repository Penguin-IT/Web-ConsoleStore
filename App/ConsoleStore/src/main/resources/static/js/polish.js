/* Console Store - hiệu ứng nhỏ: header kính mờ, ảnh sản phẩm hiện mượt. */
(function () {
    'use strict';

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
})();
