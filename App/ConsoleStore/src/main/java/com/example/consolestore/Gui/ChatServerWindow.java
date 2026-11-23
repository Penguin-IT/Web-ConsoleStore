package com.example.consolestore.Gui;

import com.example.consolestore.Service.ChatSocketServer;
import com.example.consolestore.Service.ChatSocketServer.PeerInfo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Cửa sổ quản lý Chat Socket Server: trạng thái, kết nối, nhật ký. */
public class ChatServerWindow extends JFrame {

    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final Color GREEN = new Color(0x1E8E3E);
    private static final Color RED = new Color(0xC5221F);
    private static final Color BORDER = new Color(0xDADCE0);

    private static final String[] COLUMNS =
            {"Vai trò", "ID", "Tên", "Địa chỉ", "Kết nối lúc"};

    private static final int MAX_LOG_LINES = 1000;

    private final ChatSocketServer server;

    private final JLabel statusLabel = new JLabel();
    private final JLabel addressLabel = new JLabel();
    private final JButton startButton = new JButton("Khởi động");
    private final JButton stopButton = new JButton("Dừng");

    private final JLabel peersValue = new JLabel("0");
    private final JLabel customersValue = new JLabel("0");
    private final JLabel staffValue = new JLabel("0");
    private final JLabel roomsValue = new JLabel("0");
    private final JLabel messagesValue = new JLabel("0");
    private final JLabel uptimeValue = new JLabel("00:00:00");

    private final DefaultTableModel tableModel =
            new DefaultTableModel(COLUMNS, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTextArea logArea = new JTextArea();
    private final Timer refreshTimer;

    public ChatServerWindow(ChatSocketServer server) {
        super("Chat Socket Server");

        this.server = server;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 640);
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.add(buildHeader(), BorderLayout.NORTH);
        top.add(buildStats(), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                buildTable(),
                buildLog()
        );
        split.setResizeWeight(0.45);
        split.setBorder(null);

        content.add(top, BorderLayout.NORTH);
        content.add(split, BorderLayout.CENTER);
        setContentPane(content);

        startButton.addActionListener(event -> startServer());
        stopButton.addActionListener(event -> server.close());

        // Nhật ký từ server (chạy ở luồng khác) -> đưa về luồng giao diện
        server.setLogSink(line ->
                SwingUtilities.invokeLater(() -> appendLog(line))
        );

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                refreshTimer.stop();
                server.setLogSink(null);
            }
        });

        refreshTimer = new Timer(1000, event -> refresh());
        refreshTimer.setInitialDelay(0);
        refreshTimer.start();

        appendLog("Đã mở giao diện quản lý.");
    }

    // ------------------------------------------------------------ giao diện

    private JComponent buildHeader() {
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 20f));
        addressLabel.setForeground(Color.GRAY);
        addressLabel.setText("127.0.0.1:" + server.getPort() + "   (TCP)");

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(statusLabel);
        left.add(Box.createVerticalStrut(2));
        left.add(addressLabel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(startButton);
        buttons.add(stopButton);

        JPanel header = new JPanel(new BorderLayout());
        header.add(left, BorderLayout.WEST);
        header.add(buttons, BorderLayout.EAST);
        return header;
    }

    private JComponent buildStats() {
        JPanel stats = new JPanel(new GridLayout(1, 6, 10, 0));
        stats.add(card("Kết nối", peersValue));
        stats.add(card("Khách", customersValue));
        stats.add(card("Nhân viên", staffValue));
        stats.add(card("Cuộc trò chuyện", roomsValue));
        stats.add(card("Tổng tin nhắn", messagesValue));
        stats.add(card("Thời gian chạy", uptimeValue));
        return stats;
    }

    private JPanel card(String title, JLabel value) {
        JLabel caption = new JLabel(title);
        caption.setForeground(Color.GRAY);

        value.setFont(value.getFont().deriveFont(Font.BOLD, 20f));

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        panel.add(caption, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildTable() {
        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Kết nối đang mở"));
        return scroll;
    }

    private JComponent buildLog() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JButton clear = new JButton("Xóa nhật ký");
        clear.addActionListener(event -> logArea.setText(""));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bar.add(clear);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Nhật ký"));
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        panel.add(bar, BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------- hành vi

    private void startServer() {
        try {
            server.start();
        } catch (IOException exception) {
            appendLog("Không khởi động được: " + exception.getMessage());

            JOptionPane.showMessageDialog(
                    this,
                    "Không mở được cổng " + server.getPort() + ".\n"
                            + "Có thể cổng đang được chương trình khác dùng.\n\n"
                            + exception.getMessage(),
                    "Lỗi khởi động",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /** Chạy mỗi giây trên luồng giao diện. */
    private void refresh() {
        boolean running = server.isRunning();

        statusLabel.setText(running ? "● ĐANG CHẠY" : "● ĐÃ DỪNG");
        statusLabel.setForeground(running ? GREEN : RED);
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);

        List<PeerInfo> peers = server.snapshot();

        int customers = 0;
        int staff = 0;

        tableModel.setRowCount(0);

        for (PeerInfo peer : peers) {
            if (peer.staff()) {
                staff++;
            } else {
                customers++;
            }

            tableModel.addRow(new Object[]{
                    peer.staff() ? "Nhân viên" : "Khách",
                    peer.id(),
                    peer.name(),
                    peer.address(),
                    time(peer.since())
            });
        }

        peersValue.setText(String.valueOf(peers.size()));
        customersValue.setText(String.valueOf(customers));
        staffValue.setText(String.valueOf(staff));
        roomsValue.setText(String.valueOf(server.roomCount()));
        messagesValue.setText(String.valueOf(server.totalMessages()));

        Duration up = running
                ? Duration.between(
                Instant.ofEpochMilli(server.startedAt()), Instant.now())
                : Duration.ZERO;

        uptimeValue.setText(String.format(
                "%02d:%02d:%02d",
                up.toHours(), up.toMinutesPart(), up.toSecondsPart()
        ));
    }

    private void appendLog(String message) {
        logArea.append(LocalDateTime.now().format(CLOCK) + "  " + message + "\n");

        // Giữ nhật ký không phình mãi
        int lines = logArea.getLineCount();

        if (lines > MAX_LOG_LINES) {
            try {
                logArea.replaceRange(
                        "", 0, logArea.getLineStartOffset(lines - MAX_LOG_LINES)
                );
            } catch (Exception ignored) {
                // bỏ qua
            }
        }

        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private static String time(long millis) {
        return LocalDateTime
                .ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(CLOCK);
    }
}