package com.example.telegramclient.auth;

import dev.voroby.springframework.telegram.client.updates.ClientAuthorizationState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ClientAuthorizationState state;

    public AuthController(ClientAuthorizationState state) {
        this.state = state;
    }

    @GetMapping("/state")
    public ResponseEntity<String> getState() {
        if (state.isStateClosed()) return ResponseEntity.ok("CLOSED");
        if (state.haveAuthorization()) return ResponseEntity.ok("READY");
        if (state.isWaitAuthenticationPassword()) return ResponseEntity.ok("WAIT_PASSWORD");
        if (state.isWaitEmailAddress()) return ResponseEntity.ok("WAIT_EMAIL");
        if (state.isWaitAuthenticationCode()) return ResponseEntity.ok("WAIT_CODE");
        return ResponseEntity.ok("UNKNOWN");
    }

    @PostMapping("/code")
    public ResponseEntity<String> sendCode(@RequestParam("value") String code) {
        state.checkAuthenticationCode(code);
        return ResponseEntity.ok("CODE_SENT");
    }

    @PostMapping("/password")
    public ResponseEntity<String> sendPassword(@RequestParam("value") String password) {
        state.checkAuthenticationPassword(password);
        return ResponseEntity.ok("PASSWORD_SENT");
    }

    @PostMapping("/email")
    public ResponseEntity<String> sendEmail(@RequestParam("value") String email) {
        state.checkEmailAddress(email);
        return ResponseEntity.ok("EMAIL_SENT");
    }
}
