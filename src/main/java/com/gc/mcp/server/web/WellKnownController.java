package com.gc.mcp.server.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/.well-known")
public class WellKnownController {

    private final String resourceUrl;
    private final String issuer;

    public WellKnownController(
            @Value("${mcp.prm-url}") String resourceUrl,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer
    ) {
        this.resourceUrl = trimTrailingSlash(resourceUrl);
        this.issuer = trimTrailingSlash(issuer);
    }

    @GetMapping(value = "/oauth-protected-resource", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> oauthProtectedResource() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resource", this.resourceUrl);
        body.put("authorization_servers", List.of(this.issuer));
        body.put("scopes_supported", List.of("openid", "profile", "email", "mcp.read", "mcp.write"));
        return Mono.just(body);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
