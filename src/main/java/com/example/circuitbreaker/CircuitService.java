package com.example.circuitbreaker;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CircuitService {

    private static final String URL = "http://localhost:8080/question";

    public String getDefaultString(Throwable t) {
        log.error("Fallback : " + t.getMessage());
        return "default 입니다.";
    }

    public Mono<String> getDefaultStringWebFlux(Throwable t) {
        log.error("Fallback : " + t.getMessage());
        return Mono.just("default 입니다.");
    }

    /**
     * 방법 1 : restTemplate 사용 (Blocking HTTP Client)
     * * blocking
     * * 비반응형
     * * 예외를 발생시켜 오류를 처리함.
     * * 다른 외부 라이브러리를 통해 객체를 (역)직렬화함.
     */
    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "hji", fallbackMethod = "getDefaultString")
    public String getStringBlocking() {
        return restTemplate.exchange(URL, HttpMethod.GET, null, String.class).getBody();
    }
    /**
     * 결과
     * 2024-06-12T08:54:44.294+09:00  INFO 6248 --- [circuit-breaker] [nio-8081-exec-8] c.e.circuitbreaker.CircuitController     : Starting BLOCKING Controller!
     * 2024-06-12T08:54:44.299+09:00 ERROR 6248 --- [circuit-breaker] [nio-8081-exec-8] c.example.circuitbreaker.CircuitService  : Fallback : I/O error on GET request for "http://localhost:8080/v1/chatBot/question": Connection refused: connect
     * 2024-06-12T08:54:44.299+09:00  INFO 6248 --- [circuit-breaker] [nio-8081-exec-8] c.e.circuitbreaker.CircuitController     : default 입니다.
     * 2024-06-12T08:54:44.299+09:00  INFO 6248 --- [circuit-breaker] [nio-8081-exec-8] c.e.circuitbreaker.CircuitController     : Exiting BLOCKING Controller!
     *
     * 2024-06-12T08:54:44.999+09:00  INFO 6248 --- [circuit-breaker] [nio-8081-exec-1] c.e.circuitbreaker.CircuitController     : Starting BLOCKING Controller!
     * 2024-06-12T08:54:44.999+09:00 ERROR 6248 --- [circuit-breaker] [nio-8081-exec-1] c.example.circuitbreaker.CircuitService  : Fallback : CircuitBreaker 'hji' is OPEN and does not permit further calls
     * 2024-06-12T08:54:44.999+09:00  INFO 6248 --- [circuit-breaker] [nio-8081-exec-1] c.e.circuitbreaker.CircuitController     : default 입니다.
     * 2024-06-12T08:54:44.999+09:00  INFO 6248 --- [circuit-breaker] [nio-8081-exec-1] c.e.circuitbreaker.CircuitController     : Exiting BLOCKING Controller!
     * */

    /**
     * 방법 2 : webClient 사용 (Non-Blocking Reactive HTTP Client) - spring webflux 프레임워크의 일부
     * * Non-Blocking (메인 스레드를 차단하지 않고 여러 요청을 동시에 수행 가능함)
     * * 반응형 프로그래밍 (대용량 데이터를 보다 효율적으로 처리할 수 있음)
     * * 반응형 스트림을 사용하여 오류를 전파하기 때문에 오류를 좀 더 쉽게 처리할 수 있음
     * * Spring에 내장된 기능을 사용하여 (역)직렬화함.
     */
    private final WebClient webClient;

    @CircuitBreaker(name = "hji", fallbackMethod = "getDefaultStringWebFlux")
    public Mono<String> getStringNonBlocking() {
//        방법 1 : WebClient 의존성 추가 없이 바로 생성
        Mono<String> result = WebClient.create()
                .post()
                .uri(URL)
                .retrieve()
                .bodyToMono(String.class); // .bodyToFlux(), .bodyToMono() : body의 데이터로만 받고 싶다 || .toEntity(dto.class) : status, headers, body를 포함하는 ResponseEntity 타입으로 받고 싶다
        return result;

////        방법 2 : WebClient 의존성 주입 o
//        String url="/v1/chatBot/question";
//        return webClient.post()
//                .uri(url)
//                .bodyValue("body에 들어갈 Dto")
//                .retrieve()
//                .bodyToFlux(String.class);
    }
    /**
     * 결과
     * 2024-06-13T10:38:14.817+09:00  INFO 14168 --- [circuit-breaker] [nio-8081-exec-3] c.e.circuitbreaker.CircuitController     : Starting NON-BLOCKING Controller!
     * 2024-06-13T10:38:14.819+09:00  INFO 14168 --- [circuit-breaker] [nio-8081-exec-3] c.e.circuitbreaker.CircuitController     : Exiting NON-BLOCKING Controller!
     * 2024-06-13T10:38:14.825+09:00 ERROR 14168 --- [circuit-breaker] [ctor-http-nio-2] c.example.circuitbreaker.CircuitService  : Fallback : 400 Bad Request from POST http://localhost:8080/v1/chatBot/question
     * 2024-06-13T10:38:14.826+09:00 ERROR 14168 --- [circuit-breaker] [ctor-http-nio-1] c.example.circuitbreaker.CircuitService  : Fallback : 400 Bad Request from POST http://localhost:8080/v1/chatBot/question
     * 2024-06-13T10:38:14.826+09:00  INFO 14168 --- [circuit-breaker] [ctor-http-nio-1] c.e.circuitbreaker.CircuitController     : default 입니다.
     *
     * 2024-06-13T10:39:13.810+09:00  INFO 14168 --- [circuit-breaker] [nio-8081-exec-8] c.e.circuitbreaker.CircuitController     : Starting NON-BLOCKING Controller!
     * 2024-06-13T10:39:13.811+09:00 ERROR 14168 --- [circuit-breaker] [nio-8081-exec-8] c.example.circuitbreaker.CircuitService  : Fallback : CircuitBreaker 'hji' is OPEN and does not permit further calls
     * 2024-06-13T10:39:13.811+09:00  INFO 14168 --- [circuit-breaker] [nio-8081-exec-8] c.e.circuitbreaker.CircuitController     : default 입니다.
     * 2024-06-13T10:39:13.811+09:00  INFO 14168 --- [circuit-breaker] [nio-8081-exec-8] c.e.circuitbreaker.CircuitController     : Exiting NON-BLOCKING Controller!
     * 2024-06-13T10:39:13.811+09:00 ERROR 14168 --- [circuit-breaker] [nio-8081-exec-8] c.example.circuitbreaker.CircuitService  : Fallback : CircuitBreaker 'hji' is OPEN and does not permit further calls
     * */

}
