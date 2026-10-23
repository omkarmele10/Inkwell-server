package com.omkarmele.inkwell.repository;

import com.omkarmele.inkwell.model.Article;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ArticleRepository extends MongoRepository<Article, String> {}
