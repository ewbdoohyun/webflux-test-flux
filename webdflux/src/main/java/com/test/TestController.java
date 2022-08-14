package com.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/test")
public class TestController {

    private final TestClient testClient;

    @GetMapping
    public void sequentialTest() {

        AtomicInteger atomicSum = new AtomicInteger();

        List<Integer> priceList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            priceList.add(100);
        }
        OrderInfo orderInfo = Flux.fromStream(priceList.stream())
                .buffer(200)
                .map(it -> {
                    log.info("Buffer Size : {}", it.size());
                    int price = it.stream().mapToInt(Integer::intValue).sum();
                    return new OrderInfo(price,atomicSum.addAndGet(price));
                })
                .flatMap(it -> testClient.hello().thenReturn(it))
                .bufferTimeout(1000000, Duration.ofSeconds(10))
                .map(list -> list.get(list.size() - 1))
                .blockLast();
        log.info("last orderInfo : {}", orderInfo);
    }

    @GetMapping("/fix")
    public void sequentialCorrect() {

        AtomicInteger atomicSum = new AtomicInteger();
        AtomicInteger atomicErrorCounter = new AtomicInteger();

        List<Integer> priceList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            priceList.add(100);
        }
        OrderInfo orderInfo = Flux.fromStream(priceList.stream())
                .buffer(200)
                .map(it -> {
                    int price = it.stream().mapToInt(Integer::intValue).sum();
                    return Optional.of(new OrderInfo(price,0));
                })
                .flatMap(it -> Mono.fromCompletionStage(() ->
                        testClient.helloWithError().thenReturn(it)
                                .doOnError(error -> atomicErrorCounter.addAndGet(1))
                                .onErrorReturn(Optional.empty()).toFuture())
                )
                .bufferTimeout(1000000, Duration.ofSeconds(10))
                .map(list -> {
                    log.info("list size : {}", list.size());
                    Integer accumulatePrice = list.stream().filter(Optional::isPresent).mapToInt(it->it.get().price).sum();
                    OrderInfo lastOrderInfo= list.get(list.size()-1).get();
                    lastOrderInfo.setAccumulatePrice(atomicSum.addAndGet(accumulatePrice));
                    return list.get(list.size()-1);
                })
                .blockLast().get();
        log.info("last orderInfo : {}, fail count : {},fail sum = {}", orderInfo,atomicErrorCounter.get(),atomicErrorCounter.get()*20000);
    }

    @GetMapping("/seq")
    public void sequential() {

        AtomicInteger atomicSum = new AtomicInteger();
        AtomicInteger atomicErrorCounter = new AtomicInteger();

        List<Integer> priceList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            priceList.add(100);
        }
        OrderInfo orderInfo = Flux.fromStream(priceList.stream())
                .buffer(200)
                .map(it -> {
                    int price = it.stream().mapToInt(Integer::intValue).sum();
                    return Optional.of(new OrderInfo(price,0));
                })
                .flatMap(it -> Mono.fromCompletionStage(() ->
                        testClient.helloWithError().thenReturn(it)
                                .doOnError(error -> atomicErrorCounter.addAndGet(1))
                                .onErrorReturn(Optional.empty()).toFuture())
                )
                .bufferTimeout(1000000, Duration.ofSeconds(10))
                .map(list -> {
                    log.info("list size : {}", list.size());
                    Integer accumulatePrice = list.stream().filter(Optional::isPresent).mapToInt(it->it.get().price).sum();
                    OrderInfo lastOrderInfo= list.get(list.size()-1).get();
                    lastOrderInfo.setAccumulatePrice(atomicSum.addAndGet(accumulatePrice));
                    return list.get(list.size()-1);
                })
                .blockLast().get();
        log.info("last orderInfo : {}, fail count : {},fail sum = {}", orderInfo,atomicErrorCounter.get(),atomicErrorCounter.get()*20000);
    }


    @Data
    @ToString
    @Setter
    @AllArgsConstructor
    public static class OrderInfo {
        private Integer price;
        private Integer accumulatePrice;
    }
}
