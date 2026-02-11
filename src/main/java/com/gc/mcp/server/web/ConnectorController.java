package com.gc.mcp.server.web;

import com.gc.mcp.server.connector.ConnectorRequest;
import com.gc.mcp.server.connector.ConnectorResponse;
import com.gc.mcp.server.connector.ConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/connector")
public class ConnectorController {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorController.class);

    private final ConnectorService connectorService;

    public ConnectorController(ConnectorService connectorService) {
        this.connectorService = connectorService;
    }

    @PostMapping(path = "/invoke", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ConnectorResponse>> invoke(@RequestBody ConnectorRequest request) {
        return Mono.fromCallable(() -> ResponseEntity.ok(connectorService.handle(request)));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ConnectorResponse> handleBadRequest(Exception ex) {
        logger.warn("Connector request rejected: {}", ex.getMessage());
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("intent", "error");
        meta.put("view", "compact");
        meta.put("source", "connector");

        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("error", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ConnectorResponse(meta, Map.of(), diagnostics));
    }
}
