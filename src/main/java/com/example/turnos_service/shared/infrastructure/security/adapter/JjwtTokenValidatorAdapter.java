package com.example.turnos_service.shared.infrastructure.security.adapter;

import com.example.turnos_service.shared.domain.ports.out.TokenValidator;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

// Adapter que implementa TokenValidator validando el JWT con la clave compartida con catalogo-service
@Component
public class JjwtTokenValidatorAdapter implements TokenValidator {

    private final SecretKey key;

    // Arma la clave a partir del secreto en Base64
    public JjwtTokenValidatorAdapter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    // Verifica la firma y la vigencia del token
    @Override
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Devuelve el login guardado en el subject del token
    @Override
    public String getLoginFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}