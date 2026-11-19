import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

public class LookupClient {

    private static final Console CONSOLE = System.console();

    private static final BufferedReader KEYBOARD =
            new BufferedReader(
                    new InputStreamReader(
                            System.in,
                            StandardCharsets.UTF_8
                    )
            );

    public static void main(String[] args) {
        String host = args.length > 0
                ? args[0]
                : "127.0.0.1";

        int port;

        try {
            port = args.length > 1
                    ? Integer.parseInt(args[1])
                    : 9000;

            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            System.out.println("Cổng kết nối không hợp lệ.");
            return;
        }

        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(host, port),
                    5_000
            );

            socket.setSoTimeout(20_000);

            try (
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
                System.out.println("CONSOLE STORE — TRẠM TRA CỨU");

                String username = input("Tên đăng nhập: ");
                String password = password();

                if (username == null || password == null) {
                    return;
                }

                send(
                        writer,
                        "AUTH\t"
                                + encode(username)
                                + "\t"
                                + encode(password)
                );

                password = null;

                if (!response(reader)) {
                    return;
                }

                while (true) {
                    String line = input("\nTra cứu> ");

                    if (line == null) {
                        return;
                    }

                    line = line.strip();

                    if (line.isEmpty()) {
                        continue;
                    }

                    String[] parts = line.split("\\s+", 2);

                    String command = parts[0]
                            .toUpperCase(Locale.ROOT);

                    if ("QUIT".equals(command)) {
                        send(writer, "QUIT");
                        response(reader);
                        return;
                    }

                    if (!"TONKHO".equals(command)
                            && !"BAOHANH".equals(command)) {
                        System.out.println(
                                "Chỉ hỗ trợ TONKHO, BAOHANH và QUIT."
                        );

                        continue;
                    }

                    if (parts.length != 2
                            || parts[1].isBlank()
                            || parts[1].length() > 100) {
                        System.out.println(
                                "Nhập nội dung tra cứu từ 1 đến 100 ký tự."
                        );

                        continue;
                    }

                    send(
                            writer,
                            command + "\t" + encode(parts[1])
                    );

                    response(reader);
                }
            }
        } catch (ConnectException exception) {
            System.out.println(
                    "Không kết nối được server. "
                            + "Hãy mở ứng dụng Console Store "
                            + "và kiểm tra cổng kết nối."
            );
        } catch (SocketTimeoutException exception) {
            System.out.println(
                    "Kết nối hết thời gian chờ. Hãy chạy client lại."
            );
        } catch (IOException exception) {
            System.out.println(
                    "Kết nối đã đóng hoặc dữ liệu không hợp lệ. "
                            + "Hãy chạy client lại."
            );
        }
    }

    private static String input(String prompt) throws IOException {
        if (CONSOLE != null) {
            return CONSOLE.readLine("%s", prompt);
        }

        System.out.print(prompt);
        return KEYBOARD.readLine();
    }

    private static String password() throws IOException {
        if (CONSOLE == null) {
            return input("Mật khẩu: ");
        }

        char[] characters = CONSOLE.readPassword("Mật khẩu: ");

        if (characters == null) {
            return null;
        }

        try {
            return new String(characters);
        } finally {
            Arrays.fill(characters, '\0');
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static void send(
            BufferedWriter writer,
            String message
    ) throws IOException {
        writer.write(message);
        writer.write('\n');
        writer.flush();
    }

    private static boolean response(
            BufferedReader reader
    ) throws IOException {
        String line = readLine(reader, 65_536);

        if (line == null) {
            throw new EOFException("Server disconnected.");
        }

        String[] parts = line.split("\t", 2);

        if (parts.length != 2
                || (!"OK".equals(parts[0])
                && !"ERR".equals(parts[0]))) {
            throw new IOException("Invalid response.");
        }

        try {
            String message = new String(
                    Base64.getDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
            );

            System.out.println(message);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid response encoding.");
        }

        return "OK".equals(parts[0]);
    }

    private static String readLine(
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

                throw new EOFException("Incomplete response.");
            }

            if (character == '\n') {
                return result.toString();
            }

            if (character == '\r') {
                continue;
            }

            if (result.length() >= maximum) {
                throw new IOException("Response is too long.");
            }

            result.append((char) character);
        }
    }
}