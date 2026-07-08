package com.bookranker.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey signingKey;
  private final long expirationMillis;

  public JwtService(
      @Value(
              "${bookranker.jwt.secret:bookranker-local-development-secret-change-before-production}")
          String secret,
      @Value("${bookranker.jwt.expiration-millis:86400000}") long expirationMillis) {
    this.signingKey = Keys.hmacShaKeyFor(sha256(secret));
    this.expirationMillis = expirationMillis;
  }

  public String generateToken(String subject) {
    Instant now = Instant.now();
    Instant expiration = now.plusMillis(expirationMillis);

    return Jwts.builder()
        .subject(subject)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiration))
        .signWith(signingKey)
        .compact();
  }

  public String getSubject(String token) {
    return parseClaims(token).getSubject();
  }

  public boolean isValid(String token) {
    parseClaims(token);
    return true;
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }
}
