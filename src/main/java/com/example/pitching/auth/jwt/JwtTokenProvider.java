package com.example.pitching.auth.jwt;

import com.example.pitching.auth.domain.TokenStatus;
import com.example.pitching.auth.dto.TokenInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.io.Decoders;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token.expiration}")
    private Duration accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private Duration refreshTokenExpiration;

    public TokenInfo createTokenInfo(String email) {
        return new TokenInfo(
                createAccessToken(email),
                createRefreshToken(email)
        );
    }

    public TokenInfo recreateAccessToken(String email) {
        return new TokenInfo(
                createAccessToken(email),
                null
        );
    }

    private String createAccessToken(String email) {
        return createToken(email, accessTokenExpiration, "access");
    }

    private String createRefreshToken(String email) {
        return createToken(email, refreshTokenExpiration, "refresh");
    }

    private String createToken(String email, Duration expiration, String tokenType) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .setSubject(email)
                .claim("type", tokenType)
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

            // TODO: 필요한 경우 검증 로직 추가

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

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}