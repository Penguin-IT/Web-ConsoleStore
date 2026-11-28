/* bg-ripple.js
 * Nền gợn sóng bằng các hình thoi xếp sát nhau. Rê chuột (hoặc kéo ngón tay)
 * tới đâu, sóng lan tới đó: hình thoi phồng lên ở đỉnh sóng, thu nhỏ ở đáy sóng.
 * Chỉ chạy trên trang có <body class="customer-page">. Tự dừng khi sóng đã lặng.
 * Đổi màu bằng biến CSS: :root { --ripple-rgb: 121, 85, 213; }
 */
(() => {
    'use strict';

    if (!document.body || !document.body.classList.contains('customer-page')) return;

    const SIZE = 26;            // đường chéo một hình thoi (px)
    const H = SIZE / 2;         // bước lưới mô phỏng
    const GAP = 0.8;            // 1 = các hình thoi chạm nhau, nhỏ hơn = có khe hở
    const DAMP = 0.975;         // sóng tắt dần (gần 1 = lan lâu hơn)
    const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;

    const cv = document.createElement('canvas');
    cv.setAttribute('aria-hidden', 'true');
    cv.style.cssText =
        'position:fixed;inset:0;width:100%;height:100%;z-index:-1;pointer-events:none';
    document.body.prepend(cv);
    const ctx = cv.getContext('2d');

    let W, VH, cols, rows, cur, prev, dpr, rgb, raf = 0, idle = 0;

    function resize() {
        dpr = Math.min(window.devicePixelRatio || 1, 2);
        W = innerWidth;
        VH = innerHeight;
        cv.width = W * dpr;
        cv.height = VH * dpr;
        cols = Math.ceil(W / H) + 2;
        rows = Math.ceil(VH / H) + 2;
        cur = new Float32Array(cols * rows);
        prev = new Float32Array(cols * rows);
        wake();
    }

    function diamond(cx, cy, r) {
        ctx.moveTo(cx + r, cy);
        ctx.lineTo(cx, cy + r);
        ctx.lineTo(cx - r, cy);
        ctx.lineTo(cx, cy - r);
        ctx.closePath();
    }

    // Phương trình sóng 2D trên lưới: mỗi điểm lấy trung bình 4 điểm lân cận.
    function step() {
        let energy = 0;
        for (let y = 1; y < rows - 1; y++) {
            for (let x = 1; x < cols - 1; x++) {
                const i = y * cols + x;
                const v = ((cur[i - 1] + cur[i + 1] + cur[i - cols] + cur[i + cols]) * 0.5 - prev[i]) * DAMP;
                prev[i] = v;
                energy += v < 0 ? -v : v;
            }
        }
        const t = cur; cur = prev; prev = t;
        return energy;
    }

    function draw() {
        const r0 = H * GAP;
        const active = [];

        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, W, VH);

        // Hình thoi đứng yên: vẽ gộp một lần cho nhanh.
        ctx.fillStyle = 'rgba(' + rgb + ', .07)';
        ctx.beginPath();
        for (let y = 0; y < rows; y++) {
            for (let x = y & 1; x < cols; x += 2) {
                const v = cur[y * cols + x];
                if (v > -0.15 && v < 0.15) diamond(x * H, y * H, r0);
                else active.push(x, y, v);
            }
        }
        ctx.fill();

        // Hình thoi đang gợn: đỉnh sóng phồng + đậm, đáy sóng nhỏ lại.
        for (let k = 0; k < active.length; k += 3) {
            const v = active[k + 2];
            const r = Math.max(r0 * 0.35, Math.min(H * 1.05, r0 * (1 + v * 0.12)));
            ctx.fillStyle = 'rgba(' + rgb + ',' + (0.07 + Math.min(Math.abs(v) * 0.14, 0.7)) + ')';
            ctx.beginPath();
            diamond(active[k] * H, active[k + 1] * H, r);
            ctx.fill();
        }
    }

    function loop() {
        const e = step();
        draw();
        idle = e < 0.4 ? idle + 1 : 0;
        raf = idle > 45 ? 0 : requestAnimationFrame(loop);   // lặng rồi thì dừng
    }

    function wake() {
        rgb = getComputedStyle(document.documentElement)
            .getPropertyValue('--ripple-rgb').trim() || '121, 85, 213';
        idle = 0;
        if (reduce) { draw(); return; }
        if (!raf) raf = requestAnimationFrame(loop);
    }

    function splash(px, py) {
        const gx = Math.round(px / H), gy = Math.round(py / H);
        for (let dy = -2; dy <= 2; dy++) {
            for (let dx = -2; dx <= 2; dx++) {
                const x = gx + dx, y = gy + dy;
                if (x < 1 || y < 1 || x >= cols - 1 || y >= rows - 1) continue;
                const w = 1 - Math.hypot(dx, dy) / 3;
                if (w > 0) cur[y * cols + x] += 3.2 * w;
            }
        }
    }

    let lx = -1, ly = -1;

    function move(x, y) {
        const dx = x - lx, dy = y - ly;
        const n = lx < 0 ? 1 : Math.max(1, Math.ceil(Math.hypot(dx, dy) / H));
        for (let k = 1; k <= n; k++) splash(lx < 0 ? x : lx + (dx * k) / n, lx < 0 ? y : ly + (dy * k) / n);
        lx = x; ly = y;
        wake();
    }

    if (!reduce) {
        addEventListener('pointermove', e => {
            if (e.pointerType !== 'touch') move(e.clientX, e.clientY);
        }, { passive: true });
        addEventListener('touchmove', e => {
            const t = e.touches[0];
            if (t) move(t.clientX, t.clientY);
        }, { passive: true });
        const stop = () => { lx = ly = -1; };
        addEventListener('touchend', stop, { passive: true });
        document.addEventListener('pointerleave', stop);
    }

    let timer;
    addEventListener('resize', () => { clearTimeout(timer); timer = setTimeout(resize, 150); });

    resize();
})();
