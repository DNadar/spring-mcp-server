package com.gc.mcp.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class SecurityConfig {

    private final String issuer;
    private final String prmUrl;

    public SecurityConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${mcp.prm-url:http://localhost:8080/.well-known/oauth-protected-resource}") String prmUrl
    ) {
        this.issuer = issuer;
        this.prmUrl = prmUrl;
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        ReactiveJwtDecoder decoder = ReactiveJwtDecoders.fromIssuerLocation(this.issuer);

        JwtTypeValidator typ = new JwtTypeValidator("JWT", "application/okta-internal-at+jwt");
        if (decoder instanceof NimbusReactiveJwtDecoder nimbusDecoder) {
            nimbusDecoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                    new JwtTimestampValidator(),
                    new JwtIssuerValidator(this.issuer),
                    typ
            ));
        }
        return decoder;
    }

    // ------------------------------
    // 1) MCP/API chain: MUST return 401 (not 302) and include WWW-Authenticate
    // ------------------------------
    @Bean
    @Order(0)
    public SecurityWebFilterChain mcpApiSecurity(ServerHttpSecurity http,
                                                 ReactiveJwtDecoder reactiveJwtDecoder) {

        ServerAuthenticationEntryPoint mcpEntryPoint =
                (exchange, ex) -> unauthorizedWithWwwAuthenticate(exchange, prmUrl);

        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/sse", "/sse/**", "/mcp/**", "/.well-known/oauth-protected-resource"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(ex -> ex
                        .pathMatchers("/.well-known/oauth-protected-resource").permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(eh -> eh.authenticationEntryPoint(mcpEntryPoint))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder)))
                // IMPORTANT: no oauth2Login() here; resource server keeps SSE in reactive security context
                .build();
    }

    private static Mono<Void> unauthorizedWithWwwAuthenticate(ServerWebExchange exchange, String prmUrl) {
        var res = exchange.getResponse();
        res.setStatusCode(HttpStatus.UNAUTHORIZED);
        res.getHeaders().add(
                "WWW-Authenticate",
                "Bearer realm=\"mcp\", resource_metadata=\"" + prmUrl + "\""
        );
        return res.setComplete();
    }

    // ------------------------------
    // 2) Browser/login chain: keep your existing /connect flow
    // ------------------------------
    @Bean
    @Order(1)
    public SecurityWebFilterChain browserSecurity(ServerHttpSecurity http) {
        var successHandler = new RedirectServerAuthenticationSuccessHandler("/connected");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/.well-known/oauth-protected-resource").permitAll()
                        .pathMatchers("/connect", "/connected", "/login/**", "/oauth2/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2.authenticationSuccessHandler(successHandler))
                .oauth2Client(Customizer.withDefaults())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(java.util.List.of("https://chatgpt.com"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
