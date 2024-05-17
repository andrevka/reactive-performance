package com.andreas.helloclient;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@RestController
public class GreeterController {

    RestTemplate restTemplate;

    public CloseableHttpClient httpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultMaxPerRoute(Integer.MAX_VALUE);

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    public GreeterController(@Value("${hello-server}") String helloServer) {
        this.restTemplate = new RestTemplateBuilder()
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient()))
                .rootUri(helloServer)
                .build();
    }

    static final ExecutorService executor = Executors.newFixedThreadPool(500);

    @GetMapping("/greet")
    List<String> greet() throws ExecutionException, InterruptedException {
        CompletableFuture<List<String>> greetings = IntStream.rangeClosed(1, 10)
                .boxed()
                .map(i -> CompletableFuture.supplyAsync(() -> restTemplate.getForObject("/hello", String.class), executor))
                .map(i -> i.thenApply(List::of))
                .reduce((f1, f2) -> f1.thenCombineAsync(f2, (l1, l2) -> Stream.of(l1, l2).flatMap(Collection::stream).toList(), executor))
                .get();

        return greetings.join();
    }


}
