package com.example.university.security;

import com.example.university.dto.TokenResponse;
import com.example.university.dto.UserResponse;
import com.example.university.model.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final long ttlSeconds;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.ttl-seconds}") long ttlSeconds) {
        if (ttlSeconds < 1 || ttlSeconds > 86400) {
            throw new IllegalArgumentException("JWT_TTL_SECONDS must be between 1 and 86400");
        }
        this.encoder = encoder;
        this.ttlSeconds = ttlSeconds;
    }

    public TokenResponse issue(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(JwtConfig.ISSUER)
                .audience(List.of(JwtConfig.AUDIENCE)).subject(user.getId().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(ttlSeconds)).id(UUID.randomUUID().toString())
                .claim("email", user.getEmail()).claim("role", user.getRole().name())
                .claim("ver", user.getTokenVersion()).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", ttlSeconds, UserResponse.from(user));
    }
}
