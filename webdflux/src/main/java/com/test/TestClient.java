package com.test;

import org.springframework.web.bind.annotation.GetMapping;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;

@ReactiveFeignClient(name="test",url = "http://localhost:8080")
public interface TestClient {
    @GetMapping("/hello")
    Mono<String> hello();

    @GetMapping("/hello/withError")
    Mono<String> helloWithError();
}
