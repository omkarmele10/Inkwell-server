package com.omkarmele.inkwell.repository;

import java.util.Optional;
import com.omkarmele.inkwell.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmailIgnoreCase(String email);
}
