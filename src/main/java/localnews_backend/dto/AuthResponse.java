package localnews_backend.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private final String token;
    private final String type = "Bearer";

    public AuthResponse(String token) {
        this.token = token;
    }
}
