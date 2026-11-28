package com.example.consolestore.Gui;

import com.example.consolestore.Service.ChatSocketServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/** Mở cửa sổ quản lý khi ứng dụng khởi động xong (store.chat.gui=true). */
@Component
@ConditionalOnProperty(
        prefix = "store.chat",
        name = "gui",
        havingValue = "true"
)
public class ChatServerLauncher {

    private static final Logger log =
            LoggerFactory.getLogger(ChatServerLauncher.class);

    private final ObjectProvider<ChatSocketServer> serverProvider;

    public ChatServerLauncher(ObjectProvider<ChatSocketServer> serverProvider) {
        this.serverProvider = serverProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void open() {
        if (GraphicsEnvironment.isHeadless()) {
            log.warn("Không có màn hình (headless), bỏ qua giao diện chat server.");
            return;
        }

        ChatSocketServer server = serverProvider.getIfAvailable();

        if (server == null) {
            log.warn("Chat socket server đang tắt (store.chat.enabled=false).");
            return;
        }

        log.info("Đang mở giao diện chat server...");

        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel(
                        javax.swing.UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
                // dùng giao diện mặc định
            }

            ChatServerWindow window = new ChatServerWindow(server);
            window.setLocationRelativeTo(null);   // luôn giữa màn hình chính
            window.setVisible(true);
            window.setAlwaysOnTop(true);          // nổi lên trên IntelliJ
            window.toFront();
            window.requestFocus();
            window.setAlwaysOnTop(false);         // rồi trả về bình thường

            log.info("Đã mở cửa sổ Chat Socket Server.");
        });
    }
}