(() => {
    'use strict';

    // data-ai-base="/ai" (hoặc "/tenapp/ai" nếu app chạy trong thư mục con)
    const script = document.querySelector('script[data-ai-base]');
    const BASE = script ? script.dataset.aiBase : '/ai';
    const ROOT = BASE.replace(/\/ai$/, '');
    const STORE_KEY = 'cs-ai-chat';
    const MAX_SAVED = 30;
    const TIMEOUT_MS = 25000;

    const WELCOME = {
        role: 'bot',
        text: 'Xin chào! Mình là trợ lý mua sắm của Console Store. ' +
              'Bạn cứ nói ngân sách hoặc món cần tìm, mình sẽ gợi ý giúp nhé.'
    };

    const SUGGESTIONS = [
        'Tôi muốn dưới 2 triệu',
        'Tay cầm PS5 dưới 2 triệu',
        'Máy Switch cũ dưới 5 triệu',
        'Đĩa game Xbox'
    ];

    const state = {open: false, busy: false, messages: []};
    const ui = {};

    /* ---------- tiện ích ---------- */

    const el = (tag, cls, text) => {
        const node = document.createElement(tag);
        if (cls) node.className = cls;
        if (text != null) node.textContent = text;   // luôn textContent, không innerHTML
        return node;
    };

    const money = (n) => Number(n).toLocaleString('en-US') + ' đ';   // 2,000,000 đ giống các trang khác

    function save() {
        try {
            sessionStorage.setItem(STORE_KEY, JSON.stringify({
                open: state.open,
                messages: state.messages.slice(-MAX_SAVED)
            }));
        } catch (ignored) { /* trình duyệt chặn storage */ }
    }

    function load() {
        try {
            const data = JSON.parse(sessionStorage.getItem(STORE_KEY)) || {};
            const list = Array.isArray(data.messages) ? data.messages : [];
            state.messages = list.filter((m) => m && typeof m.text === 'string' &&
                (m.role === 'bot' || m.role === 'user'));
            state.open = data.open === true;
        } catch (ignored) {
            state.messages = [];
        }
        if (!state.messages.length) state.messages = [WELCOME];
    }

    /* ---------- vẽ tin nhắn ---------- */

    function productCard(p) {
        const link = el('a', 'cs-ai-card');
        link.href = `${ROOT}/products/${Number(p.id)}`;

        const placeholder = `${ROOT}/images/ui/product-placeholder.svg`;
        const img = el('img');
        img.alt = '';
        img.width = 64;
        img.height = 64;
        img.loading = 'lazy';
        img.src = typeof p.image === 'string' && p.image.startsWith('/images/')
            ? ROOT + p.image : placeholder;
        img.addEventListener('error', () => { img.src = placeholder; }, {once: true});

        const info = el('span', 'cs-ai-card-info');
        const meta = [p.platform, p.condition === 'CU' ? 'Hàng cũ' : 'Mới'].filter(Boolean).join(' · ');
        const foot = el('span', 'cs-ai-card-foot');
        foot.append(
            el('b', null, money(p.price)),
            el('em', p.stock > 0 ? 'cs-ai-in' : 'cs-ai-out', p.stock > 0 ? 'Còn hàng' : 'Hết hàng')
        );
        info.append(el('strong', null, p.name), el('small', null, meta), foot);

        link.append(img, info);
        return link;
    }

    function messageRow(msg) {
        const row = el('div', `cs-ai-row cs-ai-${msg.role}`);
        row.append(el('div', msg.error ? 'cs-ai-bubble is-error' : 'cs-ai-bubble', msg.text));

        if (Array.isArray(msg.products) && msg.products.length) {
            const list = el('div', 'cs-ai-cards');
            msg.products.forEach((p) => list.append(productCard(p)));
            row.append(list);
        }

        if (typeof msg.moreUrl === 'string' && msg.moreUrl.startsWith('/products')) {
            const more = el('a', 'cs-ai-more', 'Xem tất cả kết quả →');
            more.href = ROOT + msg.moreUrl;
            row.append(more);
        }
        return row;
    }

    function scrollDown() {
        ui.body.scrollTop = ui.body.scrollHeight;
    }

    function updateChips() {
        ui.chips.hidden = state.messages.some((m) => m.role === 'user');
    }

    function push(msg) {
        state.messages.push(msg);
        ui.body.append(messageRow(msg));
        updateChips();
        scrollDown();
        save();
    }

    function renderAll() {
        ui.body.replaceChildren(...state.messages.map(messageRow));
        updateChips();
        scrollDown();
    }

    function showTyping() {
        const row = el('div', 'cs-ai-row cs-ai-bot');
        const dots = el('div', 'cs-ai-typing');
        dots.setAttribute('aria-label', 'Trợ lý đang trả lời');
        dots.append(el('i'), el('i'), el('i'));
        row.append(dots);
        ui.body.append(row);
        scrollDown();
        return row;
    }

    /* ---------- gọi server ---------- */

    async function ask(text) {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);

        try {
            const res = await fetch(`${BASE}/chat`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({message: text}),
                signal: controller.signal
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) throw new Error(data.message || 'Có lỗi xảy ra, bạn thử lại sau nhé.');
            return data;
        } finally {
            clearTimeout(timer);
        }
    }

    async function send(raw) {
        const text = raw.trim();
        if (!text || state.busy) return;

        push({role: 'user', text});
        ui.input.value = '';
        setBusy(true);
        const typing = showTyping();

        try {
            const data = await ask(text);
            typing.remove();
            push({
                role: 'bot',
                text: data.reply || 'Mình chưa tìm được kết quả phù hợp.',
                products: data.products || [],
                moreUrl: data.moreUrl || null
            });
        } catch (error) {
            typing.remove();
            const timedOut = error && error.name === 'AbortError';
            push({
                role: 'bot',
                error: true,
                text: timedOut ? 'Trợ lý phản hồi quá lâu, bạn thử lại nhé.'
                               : (error.message || 'Không kết nối được, bạn thử lại nhé.')
            });
        } finally {
            setBusy(false);
            ui.input.focus();
        }
    }

    function setBusy(value) {
        state.busy = value;
        ui.submit.disabled = value;
    }

    /* ---------- mở / đóng ---------- */

    function setOpen(value) {
        state.open = value;
        ui.panel.hidden = !value;
        ui.root.classList.toggle('is-open', value);
        ui.fab.setAttribute('aria-expanded', String(value));

        if (value) {
            // Đóng khung chat nhân viên (nếu đang mở) để 2 khung không đè nhau
            const staffClose = document.querySelector('.cs-chat-panel:not([hidden]) .cs-chat-close');
            if (staffClose) staffClose.click();
            scrollDown();
            ui.input.focus();
        }
        save();
    }

    function reset() {
        state.messages = [WELCOME];
        renderAll();
        save();
    }

    /* ---------- dựng giao diện ---------- */

    function build() {
        const root = el('div', 'cs-ai');

        root.innerHTML = `
            <button class="cs-ai-fab" type="button"
                    aria-expanded="false" aria-controls="cs-ai-panel">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true">
                    <path d="M12 2l1.9 5.6L19.5 9.5l-5.6 1.9L12 17l-1.9-5.6L4.5 9.5l5.6-1.9z"/>
                    <path d="M19 15l.9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9z"/>
                </svg>
                <span class="cs-ai-fab-label">Trợ lý AI</span>
            </button>

            <section class="cs-ai-panel" id="cs-ai-panel" role="dialog"
                     aria-label="Trợ lý mua sắm AI" hidden>
                <header class="cs-ai-head">
                    <div class="cs-ai-heading">
                        <strong>Trợ lý mua sắm<span class="cs-ai-tag">AI</span></strong>
                        <small>Hỏi theo ngân sách, loại, nền tảng</small>
                    </div>
                    <button class="cs-ai-icon cs-ai-reset" type="button" aria-label="Cuộc trò chuyện mới" title="Cuộc trò chuyện mới">↻</button>
                    <button class="cs-ai-icon cs-ai-close" type="button" aria-label="Đóng">×</button>
                </header>

                <div class="cs-ai-body" aria-live="polite"></div>
                <div class="cs-ai-chips"></div>

                <form class="cs-ai-form" autocomplete="off">
                    <input type="text" maxlength="200"
                           placeholder="Ví dụ: tôi muốn dưới 2 triệu" aria-label="Tin nhắn">
                    <button type="submit" aria-label="Gửi">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none"
                             stroke="currentColor" stroke-width="2"
                             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M22 2 11 13M22 2l-7 20-4-9-9-4z"/>
                        </svg>
                    </button>
                </form>
                <p class="cs-ai-note">Gợi ý tự động. Giá và tồn kho lấy từ dữ liệu cửa hàng.</p>
            </section>`;

        ui.root = root;
        ui.fab = root.querySelector('.cs-ai-fab');
        ui.panel = root.querySelector('.cs-ai-panel');
        ui.body = root.querySelector('.cs-ai-body');
        ui.chips = root.querySelector('.cs-ai-chips');
        ui.form = root.querySelector('.cs-ai-form');
        ui.input = root.querySelector('.cs-ai-form input');
        ui.submit = root.querySelector('.cs-ai-form button');

        SUGGESTIONS.forEach((text) => {
            const chip = el('button', 'cs-ai-chip', text);
            chip.type = 'button';
            chip.addEventListener('click', () => send(text));
            ui.chips.append(chip);
        });

        ui.fab.addEventListener('click', () => setOpen(!state.open));
        root.querySelector('.cs-ai-close').addEventListener('click', () => setOpen(false));
        root.querySelector('.cs-ai-reset').addEventListener('click', reset);

        ui.form.addEventListener('submit', (event) => {
            event.preventDefault();
            send(ui.input.value);
        });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && state.open) setOpen(false);
        });

        document.body.append(root);
    }

    /* ---------- khởi động ---------- */

    async function boot() {
        // Server cho biết có hiện khung AI không (ẩn với nhân viên / admin)
        try {
            const res = await fetch(`${BASE}/config`, {headers: {Accept: 'application/json'}});
            const config = res.ok ? await res.json() : {};
            if (config.enabled !== true) return;
        } catch (ignored) {
            return;
        }

        load();
        build();
        renderAll();
        if (state.open) setOpen(true);   // giữ khung mở khi khách bấm sang trang khác
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }
})();
