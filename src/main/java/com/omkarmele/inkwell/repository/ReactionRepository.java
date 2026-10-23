package com.omkarmele.inkwell.repository;

import java.util.Collection;
import java.util.List;
import com.omkarmele.inkwell.model.Reaction;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReactionRepository extends MongoRepository<Reaction, String> {
    List<Reaction> findByUserIdAndArticleIdIn(String userId, Collection<String> articleIds);
    void deleteByArticleId(String articleId);
}
