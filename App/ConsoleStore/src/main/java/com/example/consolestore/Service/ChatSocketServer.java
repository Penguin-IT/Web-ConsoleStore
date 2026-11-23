package com.example.consolestore.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Máy chủ chat TCP thuần (ServerSocket) giữa khách hàng và nhân viên.
 *
 * Giao thức: mỗi dòng là một lệnh, các trường cách nhau bằng TAB,
 * nội dung chữ được mã hóa Base64 (UTF-8) giống LookupSocketServer.
 *
 *  Client -> Server
 *    HELLO  <CUSTOMER|STAFF> <b64 id> <b64 tên>
 *    SAY    <b64 phòng> <b64 nội dung>      (khách: phòng luôn là của chính mình)
 *    PING
 *    QUIT
 *
 *  Server -> Client
 *    OK     <b64 lời chào>
 *    MSG    <b64 phòng> <b64 tên phòng> <C|S> <b64 người gửi> <thời gian ms> <b64 nội dung>
 *    READY                                  (đã gửi xong lịch sử)
 *    ERR    <b64 lý do>
 *    PONG
 */
public class ChatSocketServer implements AutoCloseable {

    private static final Logger log =
            LoggerFactory.getLogger(ChatSocketServer.class);

    private static final int MAX_TEXT = 500;
    private static final int MAX_HISTORY = 200;
    private static final int MAX_PEERS = 300;
    private static final int MAX_LINE = 4096;
    private static final int IDLE_TIMEOUT_MS = 90_000;

    private record Message(
            String room,
            String roomName,
            char from,
            String sender,
            long time,
            String text
    ) {
    }

    /** Một kết nối TCP (một tab trình duyệt hoặc một client Java). */
    private static final class Peer {
        final Socket socket;
        final BufferedWriter out;
        boolean staff;
        String id = "";
        String name = "";

        Peer(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new BufferedWriter(
                    new OutputStreamWriter(
                            socket.getOutputStream(),
                            StandardCharsets.UTF_8
                    )
            );
        }

        void send(String line) throws IOException {
            synchronized (out) {
                out.write(line);
                out.write('\n');
                out.flush();
            }
        }
    }

    private final int port;

    /** Khóa chung: bảo vệ lịch sử và việc phát tin để thứ tự luôn đúng. */
    private final Object lock = new Object();
    private final Map<String, ArrayDeque<Message>> history = new LinkedHashMap<>();
    private final Map<String, String> roomNames = new HashMap<>();

    private final Set<Peer> peers = ConcurrentHashMap.newKeySet();
    private final ExecutorService workers;

    private volatile boolean running;
    private ServerSocket serverSocket;

    public ChatSocketServer(int port) {
        if (port < 1024 || port > 65535) {
            throw new IllegalArgumentException(
                    "Chat port must be between 1024 and 65535."
            );
        }

        this.port = port;

        AtomicInteger sequence = new AtomicInteger();

        // Mỗi kết nối chat sống lâu nên cần một luồng riêng cho mỗi kết nối
        this.workers = Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(
                    task,
                    "chat-worker-" + sequence.incrementAndGet()
            );
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", port), 50);

        running = true;

        Thread accept = new Thread(this::acceptConnections, "chat-accept");
        accept.setDaemon(true);
        accept.start();

        log.info("Chat socket server started at 127.0.0.1:{}", port);
    }

    private void acceptConnections() {
        try {
            while (running) {
                Socket socket = serverSocket.accept();

                if (peers.size() >= MAX_PEERS) {
                    closeQuietly(socket);
                    log.warn("Chat client rejected: too many connections");
                    continue;
                }

                try {
                    socket.setTcpNoDelay(true);
                    socket.setSoTimeout(IDLE_TIMEOUT_MS);
                    workers.execute(() -> handle(socket));
                } catch (IOException | RuntimeException exception) {
                    closeQuietly(socket);
                }
            }
        } catch (IOException exception) {
            if (running) {
                log.error("Chat socket accept failed", exception);
            }
        }
    }

    private void handle(Socket socket) {
        Peer peer = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.UTF_8
                )
        )) {
            peer = new Peer(socket);

            // ---- Bước 1: HELLO ----
            String first = readLine(reader);

            if (first == null) {
                return;
            }

            String[] hello = first.split("\t", -1);

            if (hello.length != 4 || !"HELLO".equals(hello[0])) {
                peer.send(frame("ERR", b64("Cần HELLO trước.")));
                return;
            }

            peer.staff = "STAFF".equals(hello[1]);
            peer.id = decode(hello[2]);
            peer.name = decode(hello[3]);

            if (peer.id.isBlank() || peer.id.length() > 50) {
                peer.send(frame("ERR", b64("Danh tính không hợp lệ.")));
                return;
            }

            register(peer);

            // ---- Bước 2: vòng lặp lệnh ----
            while (true) {
                String line = readLine(reader);

                if (line == null || "QUIT".equals(line)) {
                    return;
                }

                String[] fields = line.split("\t", -1);

                switch (fields[0]) {
                    case "PING" -> peer.send("PONG");
                    case "SAY" -> {
                        if (fields.length != 3) {
                            peer.send(frame("ERR", b64("Lệnh SAY không hợp lệ.")));
                        } else {
                            say(peer, decode(fields[1]), decode(fields[2]));
                        }
                    }
                    default -> peer.send(frame("ERR", b64("Lệnh không hợp lệ.")));
                }
            }
        } catch (SocketTimeoutException exception) {
            log.debug("Chat client idle, disconnected");
        } catch (IOException | IllegalArgumentException exception) {
            log.debug("Chat connection closed: {}", exception.toString());
        } finally {
            if (peer != null) {
                peers.remove(peer);
            }

            closeQuietly(socket);
        }
    }

    /** Gửi lịch sử rồi mới đưa kết nối vào danh sách nhận tin mới. */
    private void register(Peer peer) throws IOException {
        peer.send(frame("OK", b64(
                peer.staff
                        ? "Đã kết nối với tư cách nhân viên."
                        : "Đã kết nối với cửa hàng."
        )));

        synchronized (lock) {
            if (peer.staff) {
                for (ArrayDeque<Message> room : history.values()) {
                    for (Message message : room) {
                        peer.send(toLine(message));
                    }
                }
            } else {
                ArrayDeque<Message> room = history.get(peer.id);

                if (room != null) {
                    for (Message message : room) {
                        peer.send(toLine(message));
                    }
                }
            }

            peer.send("READY");
            peers.add(peer);
        }

        log.info("Chat {} connected: {}", peer.staff ? "staff" : "customer", peer.id);
    }

    private void say(Peer peer, String requestedRoom, String rawText)
            throws IOException {
        String text = rawText.strip();

        if (text.isEmpty()) {
            return;
        }

        if (text.length() > MAX_TEXT) {
            peer.send(frame("ERR", b64("Tin nhắn tối đa " + MAX_TEXT + " ký tự.")));
            return;
        }

        List<Peer> dead = new ArrayList<>();

        synchronized (lock) {
            String room;
            String roomName;

            if (peer.staff) {
                room = requestedRoom;
                roomName = roomNames.get(room);

                if (roomName == null) {
                    peer.send(frame("ERR", b64("Cuộc trò chuyện không tồn tại.")));
                    return;
                }
            } else {
                // Khách chỉ được nhắn vào phòng của chính mình
                room = peer.id;
                roomName = peer.name;
                roomNames.putIfAbsent(room, roomName);
            }

            Message message = new Message(
                    room,
                    roomName,
                    peer.staff ? 'S' : 'C',
                    peer.name,
                    System.currentTimeMillis(),
                    text
            );

            ArrayDeque<Message> messages =
                    history.computeIfAbsent(room, key -> new ArrayDeque<>());

            messages.addLast(message);

            while (messages.size() > MAX_HISTORY) {
                messages.removeFirst();
            }

            String line = toLine(message);

            // Nhân viên nhận mọi phòng; khách chỉ nhận phòng của mình
            for (Peer target : peers) {
                if (target.staff || target.id.equals(room)) {
                    try {
                        target.send(line);
                    } catch (IOException exception) {
                        dead.add(target);
                    }
                }
            }
        }

        for (Peer target : dead) {
            peers.remove(target);
            closeQuietly(target.socket);
        }
    }

    private String toLine(Message m) {
        return String.join("\t",
                "MSG",
                b64(m.room()),
                b64(m.roomName()),
                String.valueOf(m.from()),
                b64(m.sender()),
                String.valueOf(m.time()),
                b64(m.text())
        );
    }

    private String frame(String status, String payload) {
        return status + "\t" + payload;
    }

    private String readLine(BufferedReader reader) throws IOException {
        StringBuilder result = new StringBuilder();

        while (true) {
            int character = reader.read();

            if (character == -1) {
                return result.isEmpty() ? null : result.toString();
            }

            if (character == '\n') {
                return result.toString();
            }

            if (character == '\r') {
                continue;
            }

            if (result.length() >= MAX_LINE) {
                throw new IOException("Request is too long.");
            }

            result.append((char) character);
        }
    }

    private static String b64(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String decode(String value) {
        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }

    private static void closeQuietly(Closeable resource) {
        if (resource == null) {
            return;
        }

        try {
            resource.close();
        } catch (IOException ignored) {
            // bỏ qua
        }
    }

    @Override
    public synchronized void close() {
        running = false;

        closeQuietly(serverSocket);

        for (Peer peer : peers) {
            closeQuietly(peer.socket);
        }

        peers.clear();
        workers.shutdownNow();
    }
}