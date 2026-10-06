package com.example.turnos_service.shared.infrastructure.security.filter;

import com.example.turnos_service.shared.domain.ports.out.TokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    TokenValidator tokenValidator;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenValidator);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void debeAutenticar_cuandoElTokenEsValido() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(tokenValidator.isTokenValid("token-valido")).thenReturn(true);
        when(tokenValidator.getLoginFromToken("token-valido")).thenReturn("juan.perez");

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("juan.perez");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void noDebeAutenticar_cuandoNoHayHeaderAuthorization() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenValidator, never()).isTokenValid(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void noDebeAutenticar_cuandoElHeaderNoTieneElPrefijoBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic algo");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenValidator, never()).isTokenValid(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void noDebeAutenticar_cuandoElTokenEsInvalido() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(tokenValidator.isTokenValid("token-invalido")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenValidator, never()).getLoginFromToken(anyString());
        verify(filterChain).doFilter(request, response);
    }
}