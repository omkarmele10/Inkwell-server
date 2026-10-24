package com.omkarmele.inkwell;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Optional;
import com.omkarmele.inkwell.exception.ApiException;
import com.omkarmele.inkwell.model.User;
import com.omkarmele.inkwell.repository.FollowRepository;
import com.omkarmele.inkwell.repository.UserRepository;
import com.omkarmele.inkwell.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceAuthTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserService service = new UserService(
            users, mock(FollowRepository.class), mock(MongoTemplate.class), passwordEncoder);

    @Test
    void registeringEmailAlreadyUsedByGoogleIsRejectedIgnoringCase() {
        User googleUser = new User();
        googleUser.setEmail("Person@example.com");
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(googleUser));

        assertThrows(ApiException.class, () -> service.register("person@example.com", "password123", "Person"));
        verify(users, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void googleOnlyAccountCannotAuthenticateWithPassword() {
        User googleUser = new User();
        googleUser.setEmail("person@example.com");
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(googleUser));

        assertThrows(ApiException.class, () -> service.login("person@example.com", "password123"));
        verifyNoInteractions(passwordEncoder);
    }
}
