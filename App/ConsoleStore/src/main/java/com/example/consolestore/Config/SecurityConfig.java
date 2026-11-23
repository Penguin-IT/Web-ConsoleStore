package com.example.consolestore.Config;

import com.example.consolestore.Repository.TaiKhoanRepository;
import com.example.consolestore.Service.AuthService;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import com.example.consolestore.Service.CartStore;
import com.example.consolestore.Service.GuestCartCookie;

import java.util.List;
import java.util.stream.Stream;

@Configuration
public class SecurityConfig {
    // Các trang dành cho khách hàng
    private static final List<String> CUSTOMER_PAGES = List.of(
            "checkout",
            "orders",
            "account"
    );

    // Các trang nghiệp vụ nhân viên
    private static final List<String> STAFF_PAGES = List.of(
            "pos",
            "online-orders",
            "trade-ins",
            "warranties",
            "inventory",
            "invoices"
    );

    // Nhân viên bán hàng
    private static final List<String> SALES_PAGES = List.of(
            "pos",
            "online-orders",
            "invoices"
    );

    // Nhân viên kho
    private static final List<String> WAREHOUSE_PAGES = List.of(
            "inventory",
            "purchases"
    );

    // Nhân viên bảo hành
    private static final List<String> WARRANTY_PAGES = List.of(
            "warranties",
            "trade-ins"
    );

    // Quản trị viên
    private static final List<String> ADMIN_PAGES = List.of(
            "customers",
            "payments",
            "purchases",
            "suppliers",
            "manage-products",
            "staff",
            "revenue",
            "purchase-costs",
            "stock-reports"
    );

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            AuthService authService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(authService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider,
            TaiKhoanRepository taiKhoanRepository,
            CartStore cartStore                      // thêm
    ) throws Exception {

        http.authenticationProvider(authenticationProvider);
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/sandbox/**"));

        http.authorizeHttpRequests(authorize -> authorize

                // Cho phép xử lý lỗi và chuyển tiếp
                .dispatcherTypeMatchers(
                        DispatcherType.ERROR,
                        DispatcherType.FORWARD,
                        DispatcherType.ASYNC
                ).permitAll()

                // Tài nguyên công khai
                .requestMatchers(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/favicon.ico"
                ).permitAll()

                // Đăng nhập, đăng ký
                .requestMatchers(
                        "/login",
                        "/pages/login.html",
                        "/register",
                        "/pages/register.html"
                ).permitAll()
                .requestMatchers("/sandbox/**").permitAll()
                // Trang công khai
                .requestMatchers(HttpMethod.GET, "/chat/config").permitAll()
                .requestMatchers("/chat/stream", "/chat/send")
                .hasAnyRole("KHACHHANG", "ADMIN", "NHANVIEN", "BANHANG")
                .requestMatchers(
                        HttpMethod.GET,
                        "/",
                        "/home",
                        "/pages/home.html",
                        "/products",
                        "/products/**",
                        "/api/products",
                        "/api/products/**",
                        "/pages/catalog.html",
                        "/pages/product-detail.html",
                        "/auth/session"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/products/*/reviews")
                .hasRole("KHACHHANG")
                // Giỏ hàng: khách vãng lai cũng dùng được
                .requestMatchers("/cart", "/cart/**", "/pages/cart.html").permitAll()
                // Trang HTML của nhân viên bán hàng
                .requestMatchers(pagePaths(SALES_PAGES))
                .hasAnyRole(
                        "ADMIN",
                        "NHANVIEN",
                        "BANHANG"
                )

                // Trang HTML của nhân viên kho
                .requestMatchers(pagePaths(WAREHOUSE_PAGES))
                .hasAnyRole(
                        "ADMIN",
                        "NHANVIEN",
                        "KHO"
                )

                // Trang HTML của nhân viên bảo hành
                .requestMatchers(pagePaths(WARRANTY_PAGES))
                .hasAnyRole(
                        "ADMIN",
                        "NHANVIEN",
                        "BAOHANH"
                )

                // Trang HTML quản trị
                .requestMatchers(pagePaths(ADMIN_PAGES))
                .hasRole("ADMIN")

                // Trang HTML khách hàng
                .requestMatchers(pagePaths(CUSTOMER_PAGES))
                .hasRole("KHACHHANG")

                // Chức năng khách hàng
                .requestMatchers(
                        "/cart",
                        "/cart/**",
                        "/checkout",
                        "/checkout/**",
                        "/orders",
                        "/orders/**",
                        "/account",
                        "/account/**"
                ).hasRole("KHACHHANG")

                // Chức năng bán hàng
                .requestMatchers(
                        "/pos",
                        "/pos/**",
                        "/online-orders",
                        "/online-orders/**",
                        "/invoices",
                        "/invoices/**"
                ).hasAnyRole(
                        "ADMIN",
                        "NHANVIEN",
                        "BANHANG"
                )

                // Chức năng kho
                .requestMatchers(
                        "/inventory",
                        "/inventory/**",
                        "/purchases",
                        "/purchases/**"
                ).hasAnyRole(
                        "ADMIN",
                        "NHANVIEN",
                        "KHO"
                )

                // Chức năng bảo hành
                .requestMatchers(
                        "/warranties",
                        "/warranties/**",
                        "/trade-ins",
                        "/trade-ins/**"
                ).hasAnyRole(
                        "ADMIN",
                        "NHANVIEN",
                        "BAOHANH"
                )

                // Chức năng quản trị
                .requestMatchers(
                        "/customers",
                        "/customers/**",
                        "/payments",
                        "/payments/**",
                        "/suppliers",
                        "/suppliers/**",
                        "/manage-products",
                        "/manage-products/**",
                        "/staff",
                        "/staff/**",
                        "/revenue",
                        "/revenue/**",
                        "/purchase-costs",
                        "/purchase-costs/**",
                        "/stock-reports",
                        "/stock-reports/**"
                ).hasRole("ADMIN")

                // Các đường dẫn chưa khai báo
                .anyRequest().denyAll()
        );

        // Cấu hình đăng nhập
        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")

                .successHandler((request, response, authentication) -> {
                    String role = roleOf(authentication);
                    String target = landingPage(role);

                    // Gộp giỏ vãng lai vào giỏ tài khoản
                    if ("KHACHHANG".equals(role)) {
                        String guestToken = GuestCartCookie.read(request);

                        if (guestToken != null) {
                            try {
                                var owner = cartStore.customerOwner(
                                        authentication.getName()
                                );

                                if (cartStore.mergeGuestCart(
                                        guestToken,
                                        owner.customerId()
                                )) {
                                    target = "/cart";
                                }

                                GuestCartCookie.clear(request, response);
                            } catch (RuntimeException exception) {
                                // Lỗi gộp giỏ không được chặn đăng nhập; giữ cookie để thử lại
                            }
                        }
                    }

                    response.sendRedirect(request.getContextPath() + target);
                })

                .failureHandler((request, response, exception) -> {
                    response.sendRedirect(
                            request.getContextPath() + "/login?error"
                    );
                })

                .permitAll()
        );

        // Cấu hình đăng xuất
        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        );

        // Quản lý phiên đăng nhập
        http.sessionManagement(session -> session
                .sessionFixation(fixation ->
                        fixation.changeSessionId()
                )
        );

        http.requestCache(cache -> cache.disable());

        // Kiểm tra trạng thái tài khoản
        http.addFilterBefore(
                new AccountStatusFilter(taiKhoanRepository),
                AuthorizationFilter.class
        );

        return http.build();
    }

    // Chuyển tên trang thành đường dẫn HTML
    private static String[] pagePaths(List<String> pages) {
        return pages.stream()
                .map(page -> "/pages/" + page + ".html")
                .toArray(String[]::new);
    }

    // Lấy vai trò của tài khoản đăng nhập
    public static String roleOf(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "GUEST";
        }

        return authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .orElse("GUEST");
    }

    // Chuyển hướng theo vai trò
    public static String landingPage(String role) {
        return switch (role) {
            case "ADMIN" -> "/revenue";
            case "NHANVIEN", "BANHANG" -> "/pos";
            case "KHO" -> "/inventory";
            case "BAOHANH" -> "/warranties";
            default -> "/";
        };
    }

    public static List<String> allowedPages(String role) {
        return switch (role) {
            case "ADMIN" -> Stream.concat(
                    STAFF_PAGES.stream(),
                    ADMIN_PAGES.stream()
            ).distinct().toList();

            case "NHANVIEN" -> Stream.concat(
                    STAFF_PAGES.stream(),
                    Stream.of("purchases")
            ).distinct().toList();

            case "BANHANG" -> SALES_PAGES;

            case "KHO" -> WAREHOUSE_PAGES;

            case "BAOHANH" -> WARRANTY_PAGES;

            case "KHACHHANG" -> CUSTOMER_PAGES;

            default -> List.of();
        };
    }

    public static List<String> restrictedPages() {
        return Stream.of(
                        CUSTOMER_PAGES,
                        STAFF_PAGES,
                        WAREHOUSE_PAGES,
                        WARRANTY_PAGES,
                        ADMIN_PAGES
                )
                .flatMap(List::stream)
                .distinct()
                .toList();
    }
}
