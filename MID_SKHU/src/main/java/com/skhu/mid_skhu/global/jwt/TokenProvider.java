package com.skhu.mid_skhu.global.jwt;

import com.skhu.mid_skhu.app.entity.student.Student;
import com.skhu.mid_skhu.app.repository.TokenBlacklistRepository;
import com.skhu.mid_skhu.global.exception.ErrorCode;
import com.skhu.mid_skhu.global.exception.model.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class TokenProvider {

    private final Key key;
    private final long accessTokenValidityTime;
    private final long refreshTokenValidityTime;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    public TokenProvider(TokenBlacklistRepository tokenBlacklistRepository,
                         @Value("${jwt.secret}") String secretKey,
                         @Value("${jwt.access-token-validity-in-milliseconds}") long accessTokenValidityTime,
                         @Value("${jwt.refresh-token-validity-in-milliseconds}") long refreshTokenValidityTime) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityTime = accessTokenValidityTime;
        this.refreshTokenValidityTime = refreshTokenValidityTime;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    public String createRefreshToken(Student student) {
        long nowTime = (new Date().getTime());

        Date refreshTokenExpiredTime = new Date(nowTime + refreshTokenValidityTime);

        return Jwts.builder()
                .setSubject(student.getUserId().toString())
                .setIssuedAt(new Date())
                .claim("Role", student.getRoleType().getCode())
                .setExpiration(refreshTokenExpiredTime)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createAccessToken(Student student) {
        long nowTime = (new Date().getTime());

        Date accessTokenExpiredTime = new Date(nowTime + accessTokenValidityTime);

        return Jwts.builder()
                .setSubject(student.getUserId().toString())
                .claim("Role", student.getRoleType().getCode())
                .setExpiration(accessTokenExpiredTime)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN_EXCEPTION, ErrorCode.EXPIRED_TOKEN_EXCEPTION.getMessage());
        } catch (SignatureException e) {
            throw new CustomException(ErrorCode.INVALID_SIGNATURE_EXCEPTION, ErrorCode.INVALID_SIGNATURE_EXCEPTION.getMessage());
        }
    }

    public Claims getClaimsFromToken(String token) {
        return parseClaims(token);
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get("Role") == null) {
            throw new CustomException(ErrorCode.FORBIDDEN_AUTH_EXCEPTION, ErrorCode.FORBIDDEN_AUTH_EXCEPTION.getMessage());
        }

        // 사용자의 권한 정보를 securityContextHolder에 담아준다
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get("Role").toString().split(","))
                // 해당 hasRole이 권한 정보를 식별하기 위한 전처리 작업
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), "", authorities);
        authentication.setDetails(claims);

        return authentication;
    }

    public String resolveToken(HttpServletRequest request) { //토큰 분해/분석
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    public boolean validateToken(String token) {
        try {
            if (tokenBlacklistRepository.isTokenBlacklisted(token)) {
                return false;
            }
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (UnsupportedJwtException | ExpiredJwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public void invalidateToken(String token) {
        tokenBlacklistRepository.addTokenToBlacklist(token, refreshTokenValidityTime);
    }
}
