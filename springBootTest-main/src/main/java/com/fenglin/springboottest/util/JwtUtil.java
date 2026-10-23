package com.fenglin.springboottest.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：签发 token 和从 token 里读出用户名。
 *
 * <p>JWT 长这样（三段，用 . 隔开）：头.载荷.签名。
 * 载荷里我们只放用户名(subject)和过期时间，签名用密钥算，
 * 别人没有密钥就伪造不了，也改不动内容——改了签名就对不上。
 *
 * <p>注意：JWT 只是"签名"不是"加密"，载荷是 Base64，谁都能解开看，
 * 所以千万别往 token 里放密码之类的敏感信息。
 */
@Component
public class JwtUtil {

    /** 签名密钥，HS256 要求至少 32 字节；生产环境一定要换掉并通过环境变量注入 */
    @Value("${jwt.secret}")
    private String secret;

    /** 有效期（小时） */
    @Value("${jwt.expire-hours}")
    private long expireHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 登录 / 注册成功后签发 token */
    public String createToken(String username) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireHours * 60 * 60 * 1000L);
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expire)
                .signWith(key())
                .compact();
    }

    /** 常规解析：过期 / 被篡改 / 格式不对都返回 null */
    public String parseUsername(String token) {
        return parseUsername(token, 0L);
    }

    /**
     * 带宽限期的解析：签名正确但已过期的 token，只要过期时间在 graceMillis 之内，仍然返回用户名。
     *
     * <p>这是"自动续期"的关键——不然用户开着页面超过有效期，就会在操作时突然被踢下线。
     * 宽限期必须有上限（这里前端传 7 天），否则一个过期一年的旧 token 也能换到新 token，
     * 等于 token 永不过期，丢了就没有失效可言了。
     *
     * @param graceMillis 允许的过期时长（毫秒），0 表示不接受过期 token
     */
    public String parseUsername(String token, long graceMillis) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            // 签名是对的，只是过期了：ExpiredJwtException 里还能拿到原来的载荷
            long expiredFor = System.currentTimeMillis() - e.getClaims().getExpiration().getTime();
            if (expiredFor >= 0 && expiredFor <= graceMillis) {
                return e.getClaims().getSubject();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 读出 token 的过期时间；签名不对或解析失败返回 null（黑名单清理时会用到） */
    public Date getExpiration(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
        } catch (Exception e) {
            return null;
        }
    }
}
