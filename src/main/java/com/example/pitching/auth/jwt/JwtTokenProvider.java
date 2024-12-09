package com.example.pitching.auth.jwt;

import com.example.pitching.auth.domain.TokenStatus;
import com.example.pitching.auth.dto.TokenInfo;
import com.example.pitching.call.exception.ErrorCode;
import com.example.pitching.call.exception.UnAuthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token.expiration}")
    private Duration accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private Duration refreshTokenExpiration;

    public TokenInfo createTokenInfo(String email, Long userId) {
        return new TokenInfo(
                createAccessToken(email, userId),
                createRefreshToken(email, userId)
        );
    }

    public TokenInfo recreateAccessToken(String email, Long userId) {
        return new TokenInfo(
                createAccessToken(email, userId),
                null
        );
    }

    private String createAccessToken(String email, Long userId) {
        return createToken(email, userId, accessTokenExpiration, "access");
    }

    private String createRefreshToken(String email, Long userId) {
        return createToken(email, userId, refreshTokenExpiration, "refresh");
    }

    private String createToken(String email, Long userId, Duration expiration, String tokenType) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .setSubject(email)
                .claim("type", tokenType)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String validateAndGetEmail(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has expired");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    public Mono<String> validateAndGetUserId(String token) {
        return Mono.fromCallable(() ->
                        Jwts.parserBuilder()
                                .setSigningKey(getSecretKey())
                                .build()
                                .parseClaimsJws(token)
                                .getBody()
                                .get("userId", Long.class))
                                .map(String::valueOf)
                .onErrorMap(throwable -> {
                    log.error("ERROR : {}", throwable.getMessage());
                    return new UnAuthorizedException(ErrorCode.UNAUTHORIZED_ACCESS_TOKEN, token);
                });
    }

    public TokenStatus validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Refresh 토큰 타입 검증
            if (!"refresh".equals(claims.get("type", String.class))) {
                return TokenStatus.INVALID;
            }

            // 토큰 만료 시간 검증
            if (claims.getExpiration().before(new Date())) {
                return TokenStatus.EXPIRED;
            }

            return TokenStatus.VALID;

        } catch (ExpiredJwtException e) {
            return TokenStatus.EXPIRED;
        } catch (Exception e) {
            return TokenStatus.INVALID;
        }
    }

    public String extractEmail(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}