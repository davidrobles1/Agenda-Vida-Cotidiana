package com.vidacotidiana.user.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.user.application.AccountDeletionService;
import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET/DELETE /me — Documentacion/openapi/openapi.yaml. */
@RestController
@RequestMapping("/api/v1/me")
public class UserController {

    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final AccountDeletionService accountDeletionService;

    public UserController(UserRepository userRepository, CurrentUser currentUser, AccountDeletionService accountDeletionService) {
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.accountDeletionService = accountDeletionService;
    }

    @GetMapping
    public UserResponse getCurrentUser() {
        // UserSyncFilter already guarantees this row exists for any authenticated request.
        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user row missing after sync filter."));
        return UserResponse.from(user);
    }

    /** BE-027/UC-13/AC-015. Always "me" — there is no path parameter, and no other user can be targeted. */
    @DeleteMapping
    public ResponseEntity<Void> deleteCurrentUser() {
        accountDeletionService.requestDeletion(currentUser.userId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
