(() => {
    'use strict';

    const script = document.querySelector('script[data-chat-base]');
    const BASE = script ? script.dataset.chatBase : '/chat';
    const LOGIN_URL = BASE.replace(/\/chat$/, '') + '/login';
    const STORE_KEY = 'cs-chat-ui';

    const state = {
        mode: 'GUEST',          // GUEST | CUSTOMER | STAFF
        me: '',
        csrfHeader: '',
        csrfToken: '',
        tab: newId(),
        stream: null,
        rooms: new Map(),       // id phòng -> {id, name, msgs[], unread, last}
        current: null,          // phòng đang xem
        open: false,
        ready: false            // đã nhận xong lịch sử từ server
    };

    const ui = {};
    let noticeTimer = 0;

    /* ---------- tiện ích ---------- */

    function newId() {
        const raw = (window.crypto && crypto.randomUUID)
            ? crypto.randomUUID()
            : Math.random().toString(36).slice(2) + Date.now().toString(36);
        return raw.replace(/[^A-Za-z0-9-]/g, '');
    }

    const el = (tag, cls, text) => {
        const node = document.createElement(tag);
        if (cls) node.className = cls;
        if (text != null) node.textContent = text;
        return node;
    };

    // Base64 (UTF-8) -> chuỗi, khớp với server Java
    const decode = (value) =>
        new TextDecoder().decode(
            Uint8Array.from(atob(value), (c) => c.charCodeAt(0))
        );

    const clock = (time) =>
        new Date(time).toLocaleTimeString('vi-VN', {
            hour: '2-digit',
            minute: '2-digit'
        });

    function saveUi() {
        try {
            sessionStorage.setItem(
                STORE_KEY,
                JSON.stringify({open: state.open, current: state.current})
            );
        } catch (ignored) { /* trình duyệt chặn storage */ }
    }

    function loadUi() {
        try {
            return JSON.parse(sessionStorage.getItem(STORE_KEY)) || {};
        } catch (ignored) {
            return {};
        }
    }

    /* ---------- dữ liệu phòng chat ---------- */

    function room(id, name) {
        let r = state.rooms.get(id);

        if (!r) {
            r = {id, name: name || id, msgs: [], unread: 0, last: 0};
            state.rooms.set(id, r);
        } else if (name) {
            r.name = name;
        }

        return r;
    }

    function resetRooms() {
        state.rooms.clear();
        state.ready = false;

        // Khách luôn có đúng một phòng của mình
        if (state.mode === 'CUSTOMER') {
            room(state.me, state.me);
            state.current = state.me;
        }
    }

    function addMessage(m) {
        const r = room(m.room, m.roomName);

        r.msgs.push(m);
        r.last = m.time;

        if (!state.ready) return;     // đang nạp lịch sử, không tính chưa đọc

        const incoming = state.mode === 'STAFF'
            ? m.from === 'C'
            : m.from === 'S';

        const viewing = state.open && state.current === r.id;

        if (incoming && !viewing) r.unread++;

        render();
    }

    /* ---------- nhận dữ liệu từ server (qua SSE) ---------- */

    function handle(line) {
        const p = line.split('\t');

        switch (p[0]) {
            case 'MSG':
                addMessage({
                    room: decode(p[1]),
                    roomName: decode(p[2]),
                    from: p[3],
                    sender: decode(p[4]),
                    time: Number(p[5]),
                    text: decode(p[6])
                });
                break;

            case 'READY':
                state.ready = true;

                // Nhân viên: phòng đã lưu có còn không?
                if (state.mode === 'STAFF'
                    && state.current
                    && !state.rooms.has(state.current)) {
                    state.current = null;
                }

                render();
                break;

            case 'ERR':
                notice(decode(p[1]));
                break;

            default:
                break;   // OK và các dòng khác
        }
    }

    function connect() {
        if (state.stream) state.stream.close();

        const stream = new EventSource(
            BASE + '/stream?tab=' + encodeURIComponent(state.tab)
        );

        // Mỗi lần (tái) kết nối, server gửi lại toàn bộ lịch sử
        stream.onopen = () => {
            resetRooms();
            render();
        };

        stream.addEventListener('chat', (event) => handle(event.data));

        stream.onerror = () => {
            // CLOSED = trình duyệt bỏ cuộc (server tắt / hết phiên): thử lại sau
            if (stream.readyState === EventSource.CLOSED) {
                state.stream = null;
                setTimeout(boot, 5000);
            }
        };

        state.stream = stream;
    }

    /* ---------- gửi tin ---------- */

    async function send() {
        const text = ui.input.value.trim();

        if (!text || !state.current) return;

        ui.input.value = '';

        try {
            const response = await fetch(BASE + '/send', {
                method: 'POST',
                headers: {[state.csrfHeader]: state.csrfToken},
                body: new URLSearchParams({
                    tab: state.tab,
                    room: state.current,
                    text
                })
            });

            if (!response.ok) throw new Error('HTTP ' + response.status);
        } catch (error) {
            ui.input.value = text;
            notice('Không gửi được tin nhắn. Hãy tải lại trang và thử lại.');
        }
    }

    function notice(message) {
        ui.notice.textContent = message;
        ui.notice.hidden = false;

        clearTimeout(noticeTimer);
        noticeTimer = setTimeout(() => { ui.notice.hidden = true; }, 4000);
    }

    /* ---------- hiển thị ---------- */

    function render() {
        const total = [...state.rooms.values()]
            .reduce((sum, r) => sum + r.unread, 0);

        ui.badge.hidden = total === 0;
        ui.badge.textContent = total > 99 ? '99+' : String(total);

        if (!state.open) return;

        if (state.mode === 'GUEST') {
            renderGuest();
        } else if (!state.ready) {
            renderLoading();
        } else if (state.mode === 'STAFF' && !state.current) {
            renderList();
        } else {
            renderThread();
        }
    }

    function setHead(title, subtitle, canGoBack) {
        ui.title.textContent = title;
        ui.subtitle.textContent = subtitle;
        ui.back.hidden = !canGoBack;
    }

    function renderGuest() {
        setHead('Liên hệ cửa hàng', 'Cần đăng nhập để trò chuyện', false);
        ui.form.hidden = true;
        ui.body.dataset.view = 'guest';

        const box = el('div', 'cs-chat-empty');
        box.append(el('p', null, 'Vui lòng đăng nhập để nhắn tin với nhân viên Console Store.'));

        const link = el('a', 'cs-chat-login', 'Đăng nhập');
        link.href = LOGIN_URL;
        box.append(link);

        ui.body.replaceChildren(box);
    }

    function renderLoading() {
        setHead(
            state.mode === 'STAFF' ? 'Tin nhắn khách hàng' : 'Chat với Console Store',
            'Đang kết nối…',
            false
        );
        ui.form.hidden = true;
        ui.body.dataset.view = 'loading';
        ui.body.replaceChildren(el('p', 'cs-chat-empty', 'Đang kết nối…'));
    }

    // Nhân viên: danh sách các cuộc trò chuyện
    function renderList() {
        const rooms = [...state.rooms.values()].sort((a, b) => b.last - a.last);

        setHead('Tin nhắn khách hàng', rooms.length + ' cuộc trò chuyện', false);
        ui.form.hidden = true;
        ui.body.dataset.view = 'list';

        if (!rooms.length) {
            ui.body.replaceChildren(
                el('p', 'cs-chat-empty', 'Chưa có khách nào nhắn tin.')
            );
            return;
        }

        const items = rooms.map((r) => {
            const last = r.msgs[r.msgs.length - 1];

            const item = el('button', 'cs-chat-room');
            item.type = 'button';
            item.addEventListener('click', () => openRoom(r.id));

            item.append(el('span', 'cs-chat-avatar', r.name.charAt(0).toUpperCase()));

            const text = el('span', 'cs-chat-room-text');
            text.append(el('strong', null, r.name));
            text.append(el('small', null,
                (last.from === 'S' ? 'Bạn: ' : '') + last.text));
            item.append(text);

            const meta = el('span', 'cs-chat-room-meta');
            meta.append(el('time', null, clock(r.last)));

            if (r.unread) meta.append(el('b', 'cs-chat-badge', String(r.unread)));

            item.append(meta);

            return item;
        });

        ui.body.replaceChildren(...items);
    }

    // Một cuộc trò chuyện (khách: duy nhất; nhân viên: phòng đang chọn)
    function renderThread() {
        const r = room(state.current);
        const staff = state.mode === 'STAFF';

        r.unread = 0;

        if (staff) {
            setHead(r.name, 'Khách hàng', true);
        } else {
            setHead('Chat với Console Store', 'Nhân viên sẽ phản hồi sớm nhất có thể', false);
        }

        ui.form.hidden = false;

        const view = 'thread:' + r.id;
        const body = ui.body;
        const stick = body.dataset.view !== view
            || body.scrollHeight - body.scrollTop - body.clientHeight < 80;

        body.dataset.view = view;

        const nodes = [];

        if (!r.msgs.length) {
            nodes.push(el('p', 'cs-chat-empty',
                'Xin chào! Console Store có thể giúp gì cho bạn? ' +
                'Hãy nhắn tin, nhân viên sẽ trả lời ngay khi có thể.'));
        }

        for (const m of r.msgs) {
            const mine = staff
                ? (m.from === 'S' && m.sender === state.me)
                : m.from === 'C';

            const row = el('div', 'cs-chat-msg ' + (mine ? 'is-mine' : 'is-theirs'));

            if (!mine) {
                row.append(el('span', 'cs-chat-sender',
                    m.from === 'S' ? m.sender + ' · Nhân viên' : m.sender));
            }

            row.append(el('p', 'cs-chat-bubble', m.text));
            row.append(el('time', 'cs-chat-time', clock(m.time)));
            nodes.push(row);
        }

        body.replaceChildren(...nodes);

        if (stick) body.scrollTop = body.scrollHeight;
    }

    function openRoom(id) {
        state.current = id;

        const r = state.rooms.get(id);
        if (r) r.unread = 0;

        saveUi();
        render();
        ui.input.focus();
    }

    function setOpen(open) {
        state.open = open;

        ui.panel.hidden = !open;
        ui.fab.setAttribute('aria-expanded', String(open));

        if (open && state.mode === 'CUSTOMER') {
            state.current = state.me;
            const r = state.rooms.get(state.me);
            if (r) r.unread = 0;
        }

        saveUi();
        render();

        if (open && !ui.form.hidden) ui.input.focus();
    }

    /* ---------- dựng giao diện ---------- */

    function build() {
        if (ui.root) return;

        const root = el('div', 'cs-chat');

        root.innerHTML = `
            <button class="cs-chat-fab" type="button"
                    aria-expanded="false" aria-controls="cs-chat-panel">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none"
                     stroke="currentColor" stroke-width="1.8"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 9 9 0 0 1-3.6-.8L3 21l1.9-5.1A8.4 8.4 0 0 1 3 11.5 8.5 8.5 0 0 1 12 3a8.5 8.5 0 0 1 9 8.5z"/>
                </svg>
                <span class="cs-chat-fab-label">Liên hệ</span>
                <b class="cs-chat-badge" hidden>0</b>
            </button>

            <section class="cs-chat-panel" id="cs-chat-panel" role="dialog"
                     aria-label="Chat với cửa hàng" hidden>
                <header class="cs-chat-head">
                    <button class="cs-chat-back" type="button" aria-label="Quay lại" hidden>‹</button>
                    <div class="cs-chat-heading">
                        <strong></strong>
                        <small></small>
                    </div>
                    <button class="cs-chat-close" type="button" aria-label="Đóng">×</button>
                </header>

                <div class="cs-chat-body" aria-live="polite"></div>
                <p class="cs-chat-notice" role="alert" hidden></p>

                <form class="cs-chat-form" autocomplete="off">
                    <input type="text" maxlength="500"
                           placeholder="Nhập tin nhắn…" aria-label="Tin nhắn">
                    <button type="submit" aria-label="Gửi">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none"
                             stroke="currentColor" stroke-width="2"
                             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M22 2 11 13M22 2l-7 20-4-9-9-4z"/>
                        </svg>
                    </button>
                </form>
            </section>`;

        ui.root = root;
        ui.fab = root.querySelector('.cs-chat-fab');
        ui.badge = root.querySelector('.cs-chat-fab .cs-chat-badge');
        ui.panel = root.querySelector('.cs-chat-panel');
        ui.title = root.querySelector('.cs-chat-heading strong');
        ui.subtitle = root.querySelector('.cs-chat-heading small');
        ui.back = root.querySelector('.cs-chat-back');
        ui.close = root.querySelector('.cs-chat-close');
        ui.body = root.querySelector('.cs-chat-body');
        ui.notice = root.querySelector('.cs-chat-notice');
        ui.form = root.querySelector('.cs-chat-form');
        ui.input = root.querySelector('.cs-chat-form input');

        ui.fab.addEventListener('click', () => setOpen(!state.open));
        ui.close.addEventListener('click', () => setOpen(false));

        ui.back.addEventListener('click', () => {
            state.current = null;
            saveUi();
            render();
        });

        ui.form.addEventListener('submit', (event) => {
            event.preventDefault();
            send();
        });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && state.open) setOpen(false);
        });

        window.addEventListener('pagehide', () => {
            if (state.stream) state.stream.close();
        });

        document.body.append(root);
    }

    /* ---------- khởi động ---------- */

    async function boot() {
        let fields;

        try {
            const response = await fetch(BASE + '/config', {
                headers: {Accept: 'text/plain'}
            });

            fields = (await response.text()).split('\t');
        } catch (error) {
            setTimeout(boot, 10000);
            return;
        }

        if (fields.length !== 4
            || !['GUEST', 'CUSTOMER', 'STAFF'].includes(fields[0])) {
            return;     // không phải phản hồi của /chat/config
        }

        [state.mode, state.me, state.csrfHeader, state.csrfToken] = fields;

        build();

        if (state.mode === 'GUEST') return;

        const saved = loadUi();

        resetRooms();

        if (state.mode === 'STAFF') state.current = saved.current || null;

        connect();

        if (saved.open && !state.open) setOpen(true);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }
})();