package com.example.consolestore.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.access.AccessDeniedException;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LookupSocketServer implements AutoCloseable {

    private static final Logger log =
            LoggerFactory.getLogger(LookupSocketServer.class);

    private final LookupService service;
    private final int port;

    private final Set<Socket> connections =
            ConcurrentHashMap.newKeySet();

    private final ThreadPoolExecutor workers;

    private volatile boolean running;

    private ServerSocket serverSocket;
    private Thread acceptThread;

    public LookupSocketServer(
            LookupService service,
            int port
    ) {
        if (port < 1024 || port > 65535) {
            throw new IllegalArgumentException(
                    "Lookup port must be between 1024 and 65535."
            );
        }

        this.service = service;
        this.port = port;

        AtomicInteger sequence = new AtomicInteger();

        this.workers = new ThreadPoolExecutor(
                4,
                4,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(16),
                task -> {
                    Thread thread = new Thread(
                            task,
                            "lookup-worker-" + sequence.incrementAndGet()
                    );

                    thread.setDaemon(true);

                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket();

        try {
            serverSocket.bind(
                    new InetSocketAddress("127.0.0.1", port),
                    20
            );
        } catch (IOException exception) {
            closeSocket(serverSocket);
            workers.shutdownNow();

            throw exception;
        }

        running = true;

        acceptThread = new Thread(
                this::acceptConnections,
                "lookup-accept"
        );

        acceptThread.setDaemon(true);
        acceptThread.start();

        log.info(
                "Store socket server started at 127.0.0.1:{}",
                port
        );
    }

    private void acceptConnections() {
        try {
            while (running) {
                Socket socket = serverSocket.accept();

                try {
                    socket.setSoTimeout(600_000);
                    socket.setTcpNoDelay(true);

                    connections.add(socket);

                    workers.execute(() -> handle(socket));
                } catch (RejectedExecutionException exception) {
                    connections.remove(socket);
                    closeSocket(socket);

                    log.warn("Store client rejected: worker queue is full");
                } catch (IOException exception) {
                    connections.remove(socket);
                    closeSocket(socket);

                    log.warn(
                            "Store cannot prepare client connection",
                            exception
                    );
                }
            }
        } catch (IOException exception) {
            if (running) {
                log.error("Store socket accept failed", exception);
            }
        } finally {
            close();
        }
    }

    private void handle(Socket socket) {
        String thread = Thread.currentThread().getName();

        log.info("Store client connected, thread={}", thread);

        try (
                socket;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(
                                socket.getOutputStream(),
                                StandardCharsets.UTF_8
                        )
                )
        ) {
            LookupService.StaffSession session;

            try {
                String authentication = readLine(reader, 1024);

                if (authentication == null) {
                    return;
                }

                String[] fields = authentication.split("\t", -1);

                if (fields.length != 3
                        || !"AUTH".equals(fields[0])) {
                    send(writer, "ERR", "Cần đăng nhập trước.");
                    return;
                }

                session = service.authenticate(
                        decode(fields[1]),
                        decode(fields[2])
                );
            } catch (AccessDeniedException
                     | IllegalArgumentException exception) {
                send(
                        writer,
                        "ERR",
                        "Đăng nhập thất bại hoặc tài khoản không có quyền."
                );

                return;
            } catch (RuntimeException exception) {
                log.error(
                        "Store authentication failed, thread={}",
                        thread,
                        exception
                );

                send(writer, "ERR", "Tạm thời không thể đăng nhập.");

                return;
            }

            send(
                    writer,
                    "OK",
                    "Đăng nhập thành công (" + session.role() + ").\n"
                            + "Lệnh được phép: "
                            + String.join(", ", LookupService.commandsForRole(session.role()))
                            + ", QUIT"
            );

            for (int request = 0; request < 100; request++) {
                String line = readLine(reader, 1024);

                if (line == null) {
                    return;
                }

                if ("QUIT".equals(line)) {
                    send(writer, "OK", "Đã đóng phiên tra cứu.");
                    return;
                }

                String[] fields = line.split("\t", -1);

                if (fields.length != 2) {
                    send(writer, "ERR", "Lệnh không hợp lệ.");
                    continue;
                }

                try {
                    String argument = decode(fields[1]);

                    String result = switch (fields[0]) {
                        case "TONKHO" ->
                                service.stock(session, argument);
                        case "BAOHANH" ->
                                service.warranty(session, argument);
                        default ->
                                throw new IllegalArgumentException(
                                        "Chỉ hỗ trợ TONKHO và BAOHANH."
                                );
                    };

                    send(writer, "OK", result);
                } catch (LookupService.CommandDeniedException exception) {
                    send(writer, "ERR", exception.getMessage());
                } catch (AccessDeniedException exception) {
                    send(
                            writer,
                            "ERR",
                            "Phiên không còn quyền tra cứu. Hãy đăng nhập lại."
                    );
                    return;
                } catch (IllegalArgumentException exception) {
                    send(writer, "ERR", exception.getMessage());
                } catch (RuntimeException exception) {
                    log.error(
                            "Store lookup failed, thread={}",
                            thread,
                            exception
                    );

                    send(
                            writer,
                            "ERR",
                            "Tạm thời không truy vấn được dữ liệu."
                    );
                }
            }

            send(
                    writer,
                    "ERR",
                    "Phiên đã đạt giới hạn 100 lệnh. Hãy kết nối lại."
            );
        } catch (SocketTimeoutException exception) {
            log.info(
                    "Store idle client disconnected, thread={}",
                    thread
            );
        } catch (IOException exception) {
            log.debug(
                    "Store connection closed, thread={}",
                    thread,
                    exception
            );
        } finally {
            connections.remove(socket);

            log.info("Store client disconnected, thread={}", thread);
        }
    }

    private String readLine(
            BufferedReader reader,
            int maximum
    ) throws IOException {
        StringBuilder result = new StringBuilder();

        while (true) {
            int character = reader.read();

            if (character == -1) {
                if (result.isEmpty()) {
                    return null;
                }

                throw new EOFException("Incomplete request.");
            }

            if (character == '\n') {
                return result.toString();
            }

            if (character == '\r') {
                continue;
            }

            if (result.length() >= maximum) {
                throw new IOException("Request is too long.");
            }

            result.append((char) character);
        }
    }

    private String decode(String value) {
        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }

    private void send(
            BufferedWriter writer,
            String status,
            String message
    ) throws IOException {
        String encoded = Base64.getEncoder().encodeToString(
                message.getBytes(StandardCharsets.UTF_8)
        );

        writer.write(status);
        writer.write('\t');
        writer.write(encoded);
        writer.write('\n');
        writer.flush();
    }

    @Override
    public synchronized void close() {
        running = false;

        closeSocket(serverSocket);

        connections.forEach(this::closeSocket);
        connections.clear();

        workers.shutdownNow();
    }

    private void closeSocket(Closeable resource) {
        if (resource == null) {
            return;
        }

        try {
            resource.close();
        } catch (IOException exception) {
            log.debug("Store resource close failed", exception);
        }
    }
}