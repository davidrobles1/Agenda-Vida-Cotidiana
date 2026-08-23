package com.vidacotidiana.user.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.user.application.AccountDeletionService;
import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * BE-038/FR-016/UC-15/ADR-015: enables a mode (Personal or Laboral) for
     * the authenticated caller — only-additive, mirrors User.enableMode's
     * own doc comment on why there is no matching "disable" here yet.
     * POST, not PATCH /me: this is an action ("activate this mode"), the
     * same style already used by POST /reminders/{id}/complete rather than
     * a generic partial-resource-update endpoint.
     */
    @PostMapping("/modes")
    public UserResponse enableMode(@Valid @RequestBody EnableModeRequest request) {
        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user row missing after sync filter."));
        user.enableMode(request.mode());
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }
}
