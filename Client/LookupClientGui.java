import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class LookupClientGui extends JFrame {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9000;

    private final JTextField username = new JTextField(12);
    private final JPasswordField password = new JPasswordField(12);
    private final JButton login = new JButton("Đăng nhập");
    private final JButton logout = new JButton("Thoát phiên");
    private final JComboBox<Command> commands = new JComboBox<>();
    private final JLabel argumentLabel = new JLabel("Mã hoặc tên sản phẩm");
    private final JTextField argument = new JTextField(15);
    private final JButton lookup = new JButton("Tra cứu");
    private final JTextArea output = new JTextArea(11, 50);
    private final JLabel status = new JLabel("Chưa kết nối");

    private Connection connection;
    private boolean busy;
    private int requests;

    public LookupClientGui() {
        super("Console Store | Trạm tra cứu ");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(new EmptyBorder(20, 22, 18, 22));

        JLabel heading = new JLabel("Tra cứu tồn kho và bảo hành");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 24f));

        JPanel loginRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel userLabel = new JLabel("Tài khoản");
        userLabel.setLabelFor(username);
        JLabel passwordLabel = new JLabel("Mật khẩu");
        passwordLabel.setLabelFor(password);
        loginRow.add(userLabel);
        loginRow.add(username);
        loginRow.add(passwordLabel);
        loginRow.add(password);
        loginRow.add(login);
        loginRow.add(logout);

        JPanel lookupRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel commandLabel = new JLabel("Chức năng");
        commandLabel.setLabelFor(commands);
        argumentLabel.setLabelFor(argument);
        commands.setPrototypeDisplayValue(Command.BAOHANH);
        lookupRow.add(commandLabel);
        lookupRow.add(commands);
        lookupRow.add(argumentLabel);
        lookupRow.add(argument);
        lookupRow.add(lookup);

        JPanel controls = new JPanel(new GridLayout(0, 1, 0, 10));
        controls.add(heading);
        controls.add(loginRow);
        controls.add(lookupRow);

        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setMargin(new Insets(12, 12, 12, 12));
        output.setFont(output.getFont().deriveFont(14f));

        JScrollPane scroll = new JScrollPane(output);
        scroll.setBorder(BorderFactory.createTitledBorder("Kết quả"));
        panel.add(controls, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(status, BorderLayout.SOUTH);
        setContentPane(panel);

        login.addActionListener(event -> signIn());
        password.addActionListener(event -> signIn());
        lookup.addActionListener(event -> search());
        argument.addActionListener(event -> search());
        logout.addActionListener(event -> signOut());
        commands.addActionListener(event -> {
            Command command = (Command) commands.getSelectedItem();
            argumentLabel.setText(command == Command.BAOHANH
                    ? "Serial thiết bị" : "Mã hoặc tên sản phẩm");
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeConnection();
            }
        });

        updateControls();
        pack();
        setMinimumSize(getSize());
        setLocationByPlatform(true);
    }

    private void signIn() {
        if (busy || connection != null) {
            return;
        }

        String user = username.getText().strip();
        char[] secret = password.getPassword();
        password.setText("");

        if (user.isEmpty() || user.length() > 100 || secret.length == 0) {
            Arrays.fill(secret, '\0');
            showResult("Nhập tài khoản và mật khẩu hợp lệ.");
            return;
        }

        Connection next = new Connection();
        connection = next;

        runRequest("Đang đăng nhập...", () -> {
            try {
                next.connect();
                return next.request("AUTH\t" + encode(user)
                        + "\t" + encode(new String(secret)));
            } finally {
                Arrays.fill(secret, '\0');
            }
        }, reply -> {
            showResult(reply.message());

            if (!reply.ok()) {
                reset("Đăng nhập thất bại");
                return;
            }

            List<String> allowed = reply.message().lines()
                    .filter(line -> line.startsWith("Lệnh được phép:"))
                    .findFirst()
                    .map(line -> Arrays.stream(line.substring(line.indexOf(':') + 1).split(","))
                            .map(String::strip)
                            .toList())
                    .orElseGet(List::of);

            for (Command command : Command.values()) {
                if (allowed.contains(command.name())) {
                    commands.addItem(command);
                }
            }

            if (commands.getItemCount() == 0) {
                reset("Server chưa cung cấp danh sách lệnh tra cứu");
                return;
            }

            requests = 0;
            status.setText("Đã kết nối " + HOST + ":" + PORT);
            SwingUtilities.invokeLater(argument::requestFocusInWindow);
        });
    }

    private void search() {
        Command command = (Command) commands.getSelectedItem();
        if (busy || connection == null || command == null) {
            return;
        }

        String value = argument.getText().strip();
        if (value.isEmpty() || value.length() > 100) {
            showResult("Nhập nội dung tra cứu từ 1 đến 100 ký tự.");
            return;
        }

        Connection current = connection;
        runRequest("Đang tra cứu...", () ->
                current.request(command.name() + "\t" + encode(value)), reply -> {
            showResult(reply.message());
            requests++;

            if (!reply.ok() && reply.message().startsWith("Phiên không còn quyền")) {
                reset("Phiên đã kết thúc. Hãy đăng nhập lại.");
            } else if (requests >= 100) {
                reset("Phiên đã đạt 100 lệnh. Hãy đăng nhập lại.");
            } else {
                status.setText("Đã kết nối " + HOST + ":" + PORT);
            }
        });
    }

    private void signOut() {
        if (busy || connection == null) {
            return;
        }

        Connection current = connection;
        runRequest("Đang đóng phiên...", () -> current.request("QUIT"), reply -> {
            showResult(reply.message());
            reset("Đã thoát phiên");
        });
    }

    private void runRequest(String progress, Callable<Reply> task, Consumer<Reply> onReply) {
        busy = true;
        status.setText(progress);
        updateControls();

        new SwingWorker<Reply, Void>() {
            @Override
            protected Reply doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                busy = false;
                if (!isDisplayable()) {
                    return;
                }

                try {
                    onReply.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    reset("Yêu cầu bị gián đoạn");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();

                    if (cause instanceof ConnectException) {
                        showResult("Không kết nối được. Hãy mở Console Store và kiểm tra cổng 9000.");
                    } else if (cause instanceof SocketTimeoutException) {
                        showResult("Server phản hồi quá thời gian chờ. Hãy đăng nhập lại.");
                    } else {
                        showResult("Kết nối đã đóng hoặc phản hồi không hợp lệ. Hãy đăng nhập lại.");
                    }

                    reset("Đã ngắt kết nối");
                } finally {
                    updateControls();
                }
            }
        }.execute();
    }

    private void showResult(String text) {
        output.setText(text);
        output.setCaretPosition(0);
    }

    private void reset(String message) {
        closeConnection();
        requests = 0;
        commands.removeAllItems();
        argument.setText("");
        status.setText(message);
    }

    private void closeConnection() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    private void updateControls() {
        boolean signedIn = connection != null && commands.getItemCount() > 0;
        boolean canLogin = !busy && connection == null;
        username.setEnabled(canLogin);
        password.setEnabled(canLogin);
        login.setEnabled(canLogin);
        logout.setEnabled(!busy && signedIn);
        commands.setEnabled(!busy && signedIn);
        argument.setEnabled(!busy && signedIn);
        lookup.setEnabled(!busy && signedIn);
    }

    private enum Command {
        TONKHO("Tra tồn kho"),
        BAOHANH("Tra bảo hành");

        private final String label;

        Command(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record Reply(boolean ok, String message) {
    }

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static final class Connection implements AutoCloseable {
        private final Socket socket = new Socket();
        private BufferedReader reader;
        private BufferedWriter writer;

        private void connect() throws IOException {
            socket.connect(new InetSocketAddress(HOST, PORT), 5_000);
            socket.setSoTimeout(20_000);
            reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        private Reply request(String message) throws IOException {
            writer.write(message);
            writer.write('\n');
            writer.flush();

            String[] parts = readResponse().split("\t", 2);
            if (parts.length != 2 || (!"OK".equals(parts[0]) && !"ERR".equals(parts[0]))) {
                throw new IOException("Invalid response");
            }

            try {
                byte[] decoded = Base64.getDecoder().decode(parts[1]);
                return new Reply("OK".equals(parts[0]), new String(decoded, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid response encoding", exception);
            }
        }

        private String readResponse() throws IOException {
            StringBuilder line = new StringBuilder();

            for (int ch; (ch = reader.read()) != -1;) {
                if (ch == '\n') {
                    return line.toString();
                }
                if (ch != '\r') {
                    if (line.length() >= 65_536) {
                        throw new IOException("Response too long");
                    }
                    line.append((char) ch);
                }
            }

            throw new EOFException("Server disconnected");
        }

        @Override
        public void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LookupClientGui().setVisible(true));
    }
}