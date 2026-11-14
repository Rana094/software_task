package com.example.university.security;

import com.example.university.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AccountJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository users;

    public AccountJwtAuthenticationConverter(UserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            var user = users.findById(Long.valueOf(jwt.getSubject()))
                    .orElseThrow(() -> new InvalidBearerTokenException("Account no longer exists"));
            Object version = jwt.getClaim("ver");
            if (jwt.getExpiresAt() == null || jwt.getIssuedAt() == null || jwt.getId() == null
                    || !(version instanceof Number number) || number.longValue() != user.getTokenVersion()
                    || !user.getEmail().equals(jwt.getClaimAsString("email"))
                    || !user.getRole().name().equals(jwt.getClaimAsString("role"))) {
                throw new InvalidBearerTokenException("Token has been revoked or account changed");
            }
            return new JwtAuthenticationToken(jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())), user.getEmail());
        } catch (NumberFormatException | NullPointerException exception) {
            throw new InvalidBearerTokenException("Invalid account identifier");
        }
    }
}
