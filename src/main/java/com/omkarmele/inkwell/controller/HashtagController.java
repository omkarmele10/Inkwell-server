package com.omkarmele.inkwell.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import com.omkarmele.inkwell.model.Article;
import com.omkarmele.inkwell.model.ArticleStatus;
import com.omkarmele.inkwell.dto.Dtos.TagCount;
import org.springframework.http.ResponseEntity;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hashtags")
@RequiredArgsConstructor
public class HashtagController {
    private final MongoTemplate mongoTemplate;

    @GetMapping("/trending")
    public ResponseEntity<List<TagCount>> trending() {
        // return top 10 trending hashtags DESC
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").is(ArticleStatus.PUBLISHED.name())),
                Aggregation.unwind("hashtags"),
                Aggregation.group("hashtags").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.limit(10));
        return ResponseEntity.ok(mongoTemplate.aggregate(aggregation, Article.class, Document.class).getMappedResults().stream()
                .map(document -> new TagCount(document.getString("_id"),
                        ((Number) document.get("count")).longValue())).toList());
    }
}
