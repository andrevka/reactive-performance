package com.andreas.helloreactiveclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;

@RestController
public class GreeterController {

    private static Logger logger = LoggerFactory.getLogger(GreeterController.class);

    List<String> sourceSystems = List.of();
    WebClient webClient;

    public GreeterController(@Value("${hello-server}") String helloServer) {
        HttpClient httpClient = HttpClient.create(
                ConnectionProvider.builder("hello")
                        .maxConnections(Integer.MAX_VALUE)
                        .pendingAcquireMaxCount(Integer.MAX_VALUE)
                        .build()
        );
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        this.webClient = WebClient.builder()
                .baseUrl(helloServer)
                .clientConnector(connector)
                .build();
    }

    @GetMapping(value = "/greet", produces = "application/json")
    Mono<List<String>> greet() throws ExecutionException, InterruptedException {
        return Flux.range(1, 10)
                .flatMap(i -> sendHello())
                //.retryWhen(Retry.backoff(Integer.MAX_VALUE, Duration.ofSeconds(1)))
                .collectList();
    }

    private Mono<String> sendHello() {
        return webClient.get()
                .uri("/hello")
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(Integer.MAX_VALUE,Duration.ofMillis(10)));
    }

    private static Mono<Double> calculate() {
        return Flux.range(1, 10000)
                .map(i -> Math.random())
                .parallel()
                .reduce((aDouble, aDouble2) -> aDouble + aDouble2);
    }



}
