package com.test;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/hello")
public class HelloTestController {

    private static AtomicLong helloWaitCounter = new AtomicLong();
    private static AtomicLong helloWaitWithErrorCounter = new AtomicLong();
    private static AtomicLong randExceptionCounter = new AtomicLong();

    @GetMapping
    public Mono<String> helloWait() throws InterruptedException {
        Thread.sleep(helloWaitCounter.addAndGet(1)%5*1000);
        return Mono.just("Hello");
    }

    @GetMapping("/withError")
    public Mono<String> helloWithError() throws InterruptedException {
        Thread.sleep(helloWaitWithErrorCounter.addAndGet(1)%5*1000);
        if(randExceptionCounter.addAndGet(1)%3 == 0){
            throw new RuntimeException();
        }
        return Mono.just("Hello");
    }

}
