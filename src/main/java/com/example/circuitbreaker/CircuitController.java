package com.example.circuitbreaker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CircuitController {

    private final CircuitService circuitService;

    @GetMapping("/str")
    public String getStrRestTemplate(){
        log.info("Starting BLOCKING Controller!");
        String result = circuitService.getStringBlocking();
        log.info(result);
        log.info("Exiting BLOCKING Controller!");
        return result;
    }

    @GetMapping(value = "/str/webclient", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<String> getStrWebClient(){
        log.info("Starting NON-BLOCKING Controller!");
        Mono<String> result = circuitService.getStringNonBlocking();
        result.subscribe(s -> log.info(s.toString()));
        log.info("Exiting NON-BLOCKING Controller!");
        return result;
    }

}
