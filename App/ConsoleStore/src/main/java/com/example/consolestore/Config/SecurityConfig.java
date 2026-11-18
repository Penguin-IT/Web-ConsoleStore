package com.example.consolestore.Config;
import com.example.consolestore.Repository.TaiKhoanRepository;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import com.example.consolestore.Service.AuthService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.stream.Stream;

@Configuration
public class SecurityConfig {

    private static final List<String> CUSTOMER_PAGES = List.of(
            "cart",
            "checkout",
            "orders",
            "account"
    );

    private static final List<String> STAFF_PAGES = List.of(
            "pos",
            "online-orders",
            "trade-ins",
            "warranties",
            "inventory",
            "invoices"
    );

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
            TaiKhoanRepository taiKhoanRepository
    ) throws Exception {
        http.authenticationProvider(authenticationProvider);

        http.authorizeHttpRequests(authorize -> authorize
                .dispatcherTypeMatchers(
                        DispatcherType.ERROR,
                        DispatcherType.FORWARD
                ).permitAll()

                .requestMatchers(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/favicon.ico"
                ).permitAll()

                .requestMatchers(
                        "/login",
                        "/pages/login.html",
                        "/register",
                        "/pages/register.html"
                ).permitAll()

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

                .requestMatchers(pagePaths(ADMIN_PAGES))
                .hasRole("ADMIN")

                .requestMatchers(pagePaths(STAFF_PAGES))
                .hasAnyRole("ADMIN", "NHANVIEN")

                .requestMatchers(pagePaths(CUSTOMER_PAGES))
                .hasRole("KHACHHANG")

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
                .requestMatchers(
                        "/pos",
                        "/pos/**",
                        "/online-orders",
                        "/online-orders/**",
                        "/trade-ins",
                        "/trade-ins/**",
                        "/warranties",
                        "/warranties/**",
                        "/inventory",
                        "/inventory/**",
                        "/invoices",
                        "/invoices/**"
                ).hasAnyRole("ADMIN", "NHANVIEN")
                .requestMatchers(
                        "/customers",
                        "/customers/**",
                        "/payments",
                        "/payments/**",
                        "/purchases",
                        "/purchases/**",
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
                ).hasAnyRole("ADMIN")
                .anyRequest().denyAll()
        );

        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    String target = landingPage(
                            roleOf(authentication)
                    );

                    response.sendRedirect(
                            request.getContextPath() + target
                    );
                })
                .failureHandler((request, response, exception) -> {
                    response.sendRedirect(
                            request.getContextPath()
                                    + "/login?error"
                    );
                })
                .permitAll()
        );

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        );

        http.sessionManagement(session -> session
                .sessionFixation(fixation -> fixation
                        .changeSessionId()
                )
        );

        http.requestCache(cache -> cache.disable());
        http.addFilterBefore(
                new AccountStatusFilter(taiKhoanRepository),
                AuthorizationFilter.class
        );
        return http.build();
    }

    private static String[] pagePaths(List<String> pages) {
        return pages.stream()
                .map(page -> "/pages/" + page + ".html")
                .toArray(String[]::new);
    }

    public static String roleOf(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                instanceof AnonymousAuthenticationToken) {
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

    public static String landingPage(String role) {
        return switch (role) {
            case "ADMIN" -> "/revenue";
            case "NHANVIEN" -> "/pos";
            default -> "/";
        };
    }

    public static List<String> allowedPages(String role) {
        return switch (role) {
            case "ADMIN" -> Stream.concat(
                    STAFF_PAGES.stream(),
                    ADMIN_PAGES.stream()
            ).toList();

            case "NHANVIEN" -> STAFF_PAGES;

            case "KHACHHANG" -> CUSTOMER_PAGES;

            default -> List.of();
        };
    }

    public static List<String> restrictedPages() {
        return Stream.of(
                        CUSTOMER_PAGES,
                        STAFF_PAGES,
                        ADMIN_PAGES
                )
                .flatMap(List::stream)
                .distinct()
                .toList();
    }

}