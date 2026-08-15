package com.vidacotidiana.user.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /me — Documentacion/openapi/openapi.yaml. */
@RestController
@RequestMapping("/api/v1/me")
public class UserController {

    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public UserController(UserRepository userRepository, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public UserResponse getCurrentUser() {
        // UserSyncFilter already guarantees this row exists for any authenticated request.
        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user row missing after sync filter."));
        return UserResponse.from(user);
    }
}
