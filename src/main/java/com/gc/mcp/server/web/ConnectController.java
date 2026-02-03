package com.gc.mcp.server.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ConnectController {

    @GetMapping("/connect")
    public Mono<Void> connect(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create("/oauth2/authorization/okta"));
        return response.setComplete();
    }

    @GetMapping("/connected")
    public Mono<Map<String, Object>> connected(@AuthenticationPrincipal OidcUser oidcUser,
                                               @RegisteredOAuth2AuthorizedClient("okta") OAuth2AuthorizedClient client) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Use the accessToken as Bearer for /sse and /mcp/message");
        payload.put("subject", oidcUser.getSubject());
        payload.put("email", oidcUser.getEmail());
        payload.put("name", oidcUser.getFullName());
        payload.put("accessToken", client.getAccessToken().getTokenValue());
        payload.put("expiresAt", client.getAccessToken().getExpiresAt());
        return Mono.just(payload);
    }
}
