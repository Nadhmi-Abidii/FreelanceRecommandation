package com.towork.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")               // Base64-encoded secret (>= 256 bits)
    private String secretKey;

    @Value("${jwt.expiration}")           // in milliseconds (e.g. 1800000 for 30 min)
    private long jwtExpirationMs;

    // ---------- Public API ----------
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Returns subject even if token is expired (for /auth/refresh use-cases). */
    public String extractUsernameAllowExpired(String token) {
        Claims claims = extractAllClaims(token, /*allowExpired*/ true);
        return claims.getSubject();
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpirationMs);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username;
        try {
            username = extractUsername(token); // throws if expired
        } catch (ExpiredJwtException ex) {
            return false;
        }
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    // ---------- Internals ----------
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationMs) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token, /*allowExpired*/ false);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, boolean allowExpired) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .setAllowedClockSkewSeconds(60) // optional skew tolerance
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            if (allowExpired) {
                return ex.getClaims(); // let caller decide
            }
            throw ex;
        }
    }

    private Key getSignInKey() {
        // Correct: use decoded bytes, not raw UTF-8 string
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Optional helper if you prefer minutes in properties:
    public static long minutesToMillis(long minutes) {
        return Duration.ofMinutes(minutes).toMillis();
    }
}
