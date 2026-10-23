package com.fenglin.springboottest.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版 token 黑名单，用于「退出登录」让 token 立即失效。
 *
 * <p>JWT 本身是无状态的——只要签名对、没过期就一直有效，没法"主动作废"。
 * 要让退出登录马上生效，最常见的轻量做法就是加一张黑名单：登出时把 token 记下来，
 * 之后凡是带着这个 token 的请求都当作"未登录"。
 *
 * <p>这里用内存 Map 存 "token -> 它的过期时间(ms)"，校验时命中就拒绝；
 * 没有接 Redis，所以<b>服务重启黑名单会清空</b>（已登出的 token 重启后又可用）。
 * 练手项目足够，真要生产再换成 Redis 这类集中存储即可。
 *
 * <p>顺手做了个小清理：token 过期后留着也没意义，校验时若发现已过期就直接移除，
 * 避免 Map 一直涨。
 */
@Component
public class TokenBlacklist {

    // token -> 过期时间戳(ms)，用 ConcurrentHashMap 保证多线程安全
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /** 登出：把 token 和它的过期时间记下来 */
    public void add(String token, long expireAtMillis) {
        if (token != null && !token.isBlank()) {
            blacklist.put(token, expireAtMillis);
        }
    }

    /** 是否已被登出（已过期的不算，顺手清掉） */
    public boolean contains(String token) {
        Long expireAt = blacklist.get(token);
        if (expireAt == null) {
            return false;
        }
        if (System.currentTimeMillis() > expireAt) {
            blacklist.remove(token); // 过期的没必要留着
            return false;
        }
        return true;
    }
}
