package dev.lapysh.in.controller;

import dev.lapysh.in.model.PlayerGameData;
import dev.lapysh.core.PlayerService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import reactor.core.publisher.Mono;

import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Controller("/save")
public class GameDataIngestionController {

    private final PlayerService service;

    public GameDataIngestionController(PlayerService service) {
        this.service = service;
    }

    @Post(consumes = APPLICATION_JSON, produces = APPLICATION_JSON)
    public Mono<MutableHttpResponse<Object>> savePlayerData(@Body PlayerGameData playerGameData) {
        return service.savePlayerData(playerGameData)
            .then(Mono.just(HttpResponse.ok()))
            .onErrorResume(IllegalArgumentException.class,
                e -> Mono.just(HttpResponse.badRequest(new ErrorResponse(e.getMessage())))
            );
    }

    public record ErrorResponse(String error) {
    }
}
