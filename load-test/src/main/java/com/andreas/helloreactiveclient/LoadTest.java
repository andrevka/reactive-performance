package com.andreas.helloreactiveclient;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTest {

    public static void main(String[] args) {
        String url = args[0]; // URL to test
        int concurrency = 1000; // Number of concurrent requests
        int duration = 30; // Duration of the test in seconds

        ConnectionProvider loadTester = ConnectionProvider.builder("load tester")
                .maxConnections(Integer.MAX_VALUE)
                .pendingAcquireMaxCount(Integer.MAX_VALUE)
                .build();
        HttpClient httpClient = HttpClient.create(loadTester);
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        WebClient webClient = WebClient.builder()
                .clientConnector(connector)
                .build();

        CountDownLatch latch = new CountDownLatch(concurrency);
        AtomicLong totalRequests = new AtomicLong();
        AtomicLong totalTime = new AtomicLong();
        List<Integer> durations = Collections.synchronizedList(new LinkedList<>());

        //wait for service to be up
        webClient.get()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(1)))
                .block();

        Flux.range(0, concurrency)
                .flatMap(i -> sendRequests(webClient, url, duration, totalRequests, totalTime, latch, durations, i), 10000, 1000)
                .timeout(Duration.ofSeconds(duration), Mono.empty())
                .blockLast();

        long totalRequestsValue = totalRequests.get();
        long totalTimeValue = totalTime.get();
        double throughput = totalRequestsValue / (double) duration;
        double averageResponseTime = totalTimeValue / (double) totalRequestsValue;

        System.out.println("Total requests: " + totalRequestsValue);
        System.out.println("Throughput (req/s): " + throughput);
        System.out.println("Average response time (ms): " + averageResponseTime);
        System.out.println("Average latency (ms): " + durations.stream().reduce((a, b) -> a + b).get() / durations.size());
        System.out.println("Standard deviation latency (ms): " + calculateStandardDeviation(durations));
        System.out.println("Max latency (ms): " + durations.stream().max(Integer::compareTo).get());
    }

    private static Flux<Void> sendRequests(WebClient webClient, String url, int duration, AtomicLong totalRequests,
                                           AtomicLong totalTime, CountDownLatch latch, List<Integer> durations, Integer nth) {
        return Flux.defer(() -> {
            return webClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(String.class)
                    .timed()
                    .repeat()
                    .doOnNext(timed -> {
                        System.out.println(timed.get());
                        if (timed.get().getStatusCode().is2xxSuccessful()) {
                            durations.add((int) timed.elapsedSinceSubscription().toMillis());
                            totalRequests.incrementAndGet();
                        } else {
                            System.out.println("Failed request");
                        }
                        System.out.println(timed.elapsedSinceSubscription().toMillis());
                    })
                    .flatMap(i -> Mono.<Void>empty())
                    .doFinally(signalType -> latch.countDown());
        });
    }

    public static double calculateStandardDeviation(List<Integer> numbers) {
        // Step 1: Calculate the mean
        double sum = 0;
        for (int num : numbers) {
            sum += num;
        }
        double mean = sum / numbers.size();

        // Step 2: Calculate the sum of squared differences
        double sumSquaredDiff = 0;
        for (int num : numbers) {
            double diff = num - mean;
            sumSquaredDiff += diff * diff;
        }

        // Step 3: Calculate the mean of squared differences
        double meanSquaredDiff = sumSquaredDiff / numbers.size();

        // Step 4: Calculate the standard deviation (square root of the mean of squared differences)
        return Math.sqrt(meanSquaredDiff);
    }

}
