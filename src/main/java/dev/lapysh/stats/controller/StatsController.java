package dev.lapysh.stats.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lapysh.core.PlayerService;
import dev.lapysh.stats.model.StatisticsResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller("/stats")
public class StatsController {

    private final PlayerService service;
    private final ObjectMapper objectMapper;

    public StatsController(PlayerService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Get(uri = "/teams", produces = MediaType.APPLICATION_JSON_STREAM)
    public Flux<String> getTeamStatistics(String season) {
        return service.getTeamStatistics(season)
            .flatMapMany(Flux::fromIterable)
            .flatMap(statistics ->
                Mono.fromCallable(() -> convertToJsonWithNewline(statistics))
                    .subscribeOn(Schedulers.boundedElastic())
            );
    }

    @Get(uri = "/players", produces = MediaType.APPLICATION_JSON_STREAM)
    public Flux<String> getPlayerStatistics(String season) {
        return service.getPlayerStatistics(season)
            .flatMapMany(Flux::fromIterable)
            .flatMap(statistics ->
                Mono.fromCallable(() -> convertToJsonWithNewline(statistics))
                    .subscribeOn(Schedulers.boundedElastic())
            );
    }

    private <T extends StatisticsResponse> String convertToJsonWithNewline(T playerStatistics) throws Exception {
        return objectMapper.writeValueAsString(playerStatistics) + "\n";
    }
}
