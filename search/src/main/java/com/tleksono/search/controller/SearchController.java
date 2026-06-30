package com.tleksono.search.controller;

import com.tleksono.search.dto.EnrichedProductResponse;
import com.tleksono.search.repository.ElasticsearchProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ElasticsearchProductRepository esRepository;

    @Value("${app.elasticsearch.index}")
    private String index;

    @GetMapping
    public List<EnrichedProductResponse> search(@RequestParam String q,
                                        @RequestParam(defaultValue = "20") int size) throws Exception {
        return esRepository.search(q, size);
    }
}