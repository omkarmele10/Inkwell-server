package com.omkarmele.inkwell.repository;

import java.util.List;
import com.omkarmele.inkwell.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
// Spring Data JPA automatically generates the underlying implementation classes at runtime
// i just have to write the methodname according to naming convention of spring data jpa and it will automatically generate the implementation for me
public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByArticleIdOrderByCreatedAtAsc(String articleId);
    boolean existsByParentId(String parentId);
    void deleteByArticleId(String articleId);
}
