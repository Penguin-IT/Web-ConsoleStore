package com.example.consolestore.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Trình duyệt không mở được TCP thô, nên mỗi tab trình duyệt được một
 * "cầu nối": một kết nối TCP thật tới ChatSocketServer, còn dữ liệu từ
 * server được đẩy xuống trình duyệt bằng Server-Sent Events (SSE).
 *
 *   Trình duyệt --POST /chat/send--> ChatBridge --TCP--> ChatSocketServer
 *   Trình duyệt <--SSE /chat/stream-- ChatBridge <--TCP-- ChatSocketServer
 */
@Service
public class ChatBridge implements DisposableBean {

    private static final Logger log =
            LoggerFactory.getLogger(ChatBridge.class);

    private static final class Tab {
        final String id;
        final String user;
        final boolean staff;
        final Socket socket;
        final BufferedWriter out;
        final SseEmitter emitter;

        Tab(String id, String user, boolean staff,
            Socket socket, SseEmitter emitter) throws IOException {
            this.id = id;
            this.user = user;
            this.staff = staff;
            this.socket = socket;
            this.emitter = emitter;
            this.out = new BufferedWriter(
                    new OutputStreamWriter(
                            socket.getOutputStream(),
                            StandardCharsets.UTF_8
                    )
            );
        }

        void write(String line) throws IOException {
            synchronized (out) {
                out.write(line);
                out.write('\n');
                out.flush();
            }
        }
    }

    private final int port;
    private final ConcurrentMap<String, Tab> tabs = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeat;

    public ChatBridge(@Value("${store.chat.port:9100}") int port) {
        this.port = port;

        this.heartbeat = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "chat-bridge-heartbeat");
            thread.setDaemon(true);
            return thread;
        });

        // Giữ kết nối sống: ping TCP + dòng chú thích SSE
        heartbeat.scheduleWithFixedDelay(
                this::beat, 25, 25, TimeUnit.SECONDS
        );
    }

    /** Mở kết nối TCP cho một tab và trả về luồng SSE tương ứng. */
    public SseEmitter open(String tabId, String user, boolean staff)
            throws IOException {

        Tab previous = tabs.get(tabId);

        if (previous != null) {
            release(previous);
        }

        Socket socket = new Socket();

        try {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 3000);
            socket.setTcpNoDelay(true);
        } catch (IOException exception) {
            closeQuietly(socket);
            throw exception;
        }

        SseEmitter emitter = new SseEmitter(0L);   // 0 = không hết hạn
        Tab tab = new Tab(tabId, user, staff, socket, emitter);

        tabs.put(tabId, tab);

        emitter.onCompletion(() -> release(tab));
        emitter.onTimeout(() -> release(tab));
        emitter.onError(error -> release(tab));

        try {
            tab.write(
                    "HELLO\t" + (staff ? "STAFF" : "CUSTOMER")
                            + "\t" + b64(user)
                            + "\t" + b64(user)
            );
        } catch (IOException exception) {
            release(tab);
            throw exception;
        }

        Thread pump = new Thread(() -> pump(tab), "chat-bridge-" + tabId);
        pump.setDaemon(true);
        pump.start();

        return emitter;
    }

    /** Chuyển tin nhắn từ trình duyệt vào socket TCP của đúng tab đó. */
    public void say(String tabId, String user, String room, String text)
            throws IOException {

        Tab tab = tabs.get(tabId);

        // Tab phải thuộc về đúng người đang đăng nhập
        if (tab == null || !tab.user.equals(user)) {
            throw new IllegalStateException(
                    "Kết nối chat đã đóng. Hãy tải lại trang."
            );
        }

        // Khách luôn nhắn vào phòng của chính mình
        String target = tab.staff ? room : user;

        tab.write("SAY\t" + b64(target) + "\t" + b64(text));
    }

    /** Đọc từng dòng từ server TCP và đẩy xuống trình duyệt. */
    private void pump(Tab tab) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        tab.socket.getInputStream(),
                        StandardCharsets.UTF_8
                )
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                if ("PONG".equals(line)) {
                    continue;
                }

                tab.emitter.send(
                        SseEmitter.event().name("chat").data(line)
                );
            }
        } catch (IOException | IllegalStateException exception) {
            log.debug("Chat bridge closed, tab={}", tab.id);
        } finally {
            release(tab);

            try {
                tab.emitter.complete();
            } catch (RuntimeException ignored) {
                // đã hoàn tất trước đó
            }
        }
    }

    private void beat() {
        for (Tab tab : tabs.values()) {
            try {
                tab.write("PING");
                tab.emitter.send(SseEmitter.event().comment("hb"));
            } catch (IOException | RuntimeException exception) {
                release(tab);
            }
        }
    }

    private void release(Tab tab) {
        tabs.remove(tab.id, tab);   // chỉ xóa nếu vẫn là chính tab này
        closeQuietly(tab.socket);
    }

    private static void closeQuietly(Closeable resource) {
        try {
            resource.close();
        } catch (IOException ignored) {
            // bỏ qua
        }
    }

    private static String b64(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void destroy() {
        heartbeat.shutdownNow();
        tabs.values().forEach(this::release);
    }
}