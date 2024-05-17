package com.andreas.helloreactiveserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

@RestController
public class HelloController {

    @GetMapping("/hello")
    Mono<String> hello() throws ExecutionException, InterruptedException {
        return Mono.delay(Duration.ofSeconds(1))
                .thenReturn("Hello");
    }

}
