package com.omkarmele.inkwell.repository;

import java.util.Collection;
import java.util.List;
import com.omkarmele.inkwell.model.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FollowRepository extends MongoRepository<Follow, String> {
    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);
    List<Follow> findByFollowerId(String followerId);
    Page<Follow> findByFollowerId(String followerId, Pageable pageable);
    Page<Follow> findByFollowingId(String followingId, Pageable pageable);
    Slice<Follow> findSliceByFollowingId(String followingId, Pageable pageable);
    List<Follow> findByFollowerIdAndFollowingIdIn(String followerId, Collection<String> followingIds);
}
