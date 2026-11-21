package com.example.consolestore.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class NewsService {

    public record Item(String title, String link, String published, String image) {}
    private static final Logger log =
            LoggerFactory.getLogger(NewsService.class);

    private static final String FEED = "https://vnexpress.net/rss/so-hoa.rss";
    private static final String LINK_PREFIX = "https://vnexpress.net/";
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final Pattern IMG = Pattern.compile(
            "<img[^>]+src=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
    );
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private volatile List<Item> cache = List.of();
    private volatile Instant fetchedAt = Instant.EPOCH;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        refreshAsync();
    }

    /** Trả về ngay bản đang có; không bao giờ chặn request để chờ mạng. */
    public List<Item> latest(int limit) {
        if (Duration.between(fetchedAt, Instant.now()).compareTo(TTL) > 0) {
            refreshAsync();
        }

        return cache.stream().limit(limit).toList();
    }

    private void refreshAsync() {
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                List<Item> items = fetch();

                if (!items.isEmpty()) {
                    cache = items;
                }

                fetchedAt = Instant.now();
            } catch (Exception exception) {
                log.warn("Cannot load tech news: {}", exception.toString());

                // Lỗi thì thử lại sau ~2 phút thay vì chờ hết TTL
                fetchedAt = Instant.now().minus(TTL).plusSeconds(120);
            } finally {
                refreshing.set(false);
            }
        });
    }

    private List<Item> fetch() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(FEED))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "ConsoleStore/1.0")
                .GET()
                .build();

        HttpResponse<InputStream> response =
                http.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl", true
        );
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        Document document;

        try (InputStream body = response.body()) {
            document = factory.newDocumentBuilder().parse(body);
        }

        NodeList nodes = document.getElementsByTagName("item");
        List<Item> items = new ArrayList<>();

        for (int i = 0; i < nodes.getLength() && items.size() < 12; i++) {
            Element node = (Element) nodes.item(i);

            String title = text(node, "title");
            String link = text(node, "link");

            // Chỉ nhận link về đúng báo gốc
            if (title.isBlank() || !link.startsWith(LINK_PREFIX)) {
                continue;
            }

            items.add(new Item(
                    title,
                    link,
                    date(text(node, "pubDate")),
                    image(node)
            ));
        }

        return List.copyOf(items);
    }

    private static String text(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);

        return list.getLength() == 0
                ? ""
                : list.item(0).getTextContent().strip();
    }

    private static String date(String rfc1123) {
        try {
            return ZonedDateTime
                    .parse(rfc1123, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception exception) {
            return "";
        }
    }
    private static String image(Element item) {
        // 1. <enclosure url="..." type="image/...">
        NodeList enclosures = item.getElementsByTagName("enclosure");

        for (int i = 0; i < enclosures.getLength(); i++) {
            Element enclosure = (Element) enclosures.item(i);
            String url = enclosure.getAttribute("url");

            if (enclosure.getAttribute("type").startsWith("image")
                    && url.startsWith("https://")) {
                return url;
            }
        }

        // 2. <media:thumbnail url="..."> hoặc <media:content url="...">
        for (String tag : List.of("media:thumbnail", "media:content")) {
            NodeList media = item.getElementsByTagName(tag);

            if (media.getLength() > 0) {
                String url = ((Element) media.item(0)).getAttribute("url");

                if (url.startsWith("https://")) {
                    return url;
                }
            }
        }

        // 3. Thẻ <img> đầu tiên trong <description>
        Matcher matcher = IMG.matcher(text(item, "description"));

        if (matcher.find()) {
            String url = matcher.group(1);

            if (url.startsWith("//")) {
                url = "https:" + url;
            }

            if (url.startsWith("https://")) {
                return url;
            }
        }

        return "";
    }
}