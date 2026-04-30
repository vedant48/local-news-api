package localnews_backend.controller;

import jakarta.validation.Valid;
import localnews_backend.dto.AuthRequest;
import localnews_backend.dto.AuthResponse;
import localnews_backend.dto.RegisterRequest;
import localnews_backend.model.User;
import localnews_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return new AuthResponse(authService.login(request));
    }
}