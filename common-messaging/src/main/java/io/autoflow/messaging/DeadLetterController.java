package io.autoflow.messaging;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/dead-letters")
public class DeadLetterController {
    private final DeadLetterService service;
    public DeadLetterController(DeadLetterService service) { this.service = service; }
    @GetMapping public List<Map<String, Object>> list() { return service.list(); }
    @PostMapping("/{id}/replay") @ResponseStatus(HttpStatus.ACCEPTED)
    public void replay(@PathVariable String id) { service.replay(id); }
}

