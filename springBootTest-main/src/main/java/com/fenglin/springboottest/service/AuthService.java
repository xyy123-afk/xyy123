package com.fenglin.springboottest.service;

import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.dto.UserVO;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.util.JwtUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证业务：注册 / 登录。持久层换成 MyBatis-Plus 后，
 * 查询走 UserMapper 的自定义 SQL，插入走 BaseMapper 自带的 insert()。
 */
@Service
public class AuthService {

    /** 过期 7 天之内还允许续期，超过就得重新登录 */
    private static final long REFRESH_GRACE_MILLIS = 7 * 24 * 60 * 60 * 1000L;

    /** BCrypt 每次加密都会生成随机盐，同一个密码两次密文不同，校验只能用 matches() */
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    /** 注册：后端兜底校验（前端已校验过一遍），密码 BCrypt 加密后落库 */
    public ApiResponse<UserVO> register(String username, String password) {
        if (username == null || username.isBlank()) {
            return ApiResponse.fail("请输入用户名或邮箱", "username");
        }
        username = username.trim();
        if (username.length() < 2) {
            return ApiResponse.fail("用户名至少 2 个字符", "username");
        }
        if (username.length() > 32) {
            return ApiResponse.fail("用户名不能超过 32 个字符", "username");
        }
        if (password == null || password.isBlank()) {
            return ApiResponse.fail("请输入密码", "password");
        }
        if (password.length() < 6) {
            return ApiResponse.fail("密码长度至少 6 位", "password");
        }
        if (password.length() > 72) {
            // BCrypt 只取前 72 字节，超长直接拒绝更稳妥
            return ApiResponse.fail("密码长度不能超过 72 位", "password");
        }
        if (userMapper.countByUsername(username) > 0) {
            return ApiResponse.fail("该用户名已被注册", "username");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());
        try {
            // BaseMapper.insert()：自增主键会回填到 user.getId()
            userMapper.insert(user);
        } catch (DataIntegrityViolationException e) {
            // 极端并发下两人同时注册同一用户名，数据库唯一索引是最后一道防线
            return ApiResponse.fail("该用户名已被注册", "username");
        }
        // 注册完直接算登录，签发 token，前端拿着它去访问待办等需要身份的接口
        return ApiResponse.ok(new UserVO(user.getUsername(), jwtUtil.createToken(user.getUsername())));
    }

    /** 登录：校验 BCrypt 密文；不区分「用户不存在」和「密码错误」，避免被枚举用户名 */
    public ApiResponse<UserVO> login(String username, String password) {
        if (username == null || username.isBlank()) {
            return ApiResponse.fail("请输入用户名或邮箱", "username");
        }
        if (password == null || password.isBlank()) {
            return ApiResponse.fail("请输入密码", "password");
        }

        User user = userMapper.selectByUsername(username.trim());
        if (user == null || !encoder.matches(password, user.getPassword())) {
            return ApiResponse.fail("用户名或密码错误", "");
        }
        // 校验通过，签发 token
        return ApiResponse.ok(new UserVO(user.getUsername(), jwtUtil.createToken(user.getUsername())));
    }

    /**
     * 修改密码：校验原密码（BCrypt matches）→ 校验新密码强度 → 加密后更新。
     * 成功后重新签发 token（旧 token 是否立即失效由调用方决定，比如登出黑名单）。
     */
    public ApiResponse<UserVO> changePassword(String username, String oldPassword, String newPassword) {
        if (username == null || username.isBlank()) {
            return ApiResponse.fail("登录已过期，请重新登录", "");
        }
        if (oldPassword == null || oldPassword.isBlank()) {
            return ApiResponse.fail("请输入原密码", "oldPassword");
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ApiResponse.fail("请输入新密码", "newPassword");
        }
        if (newPassword.length() < 6) {
            return ApiResponse.fail("新密码长度至少 6 位", "newPassword");
        }
        if (newPassword.length() > 72) {
            return ApiResponse.fail("新密码长度不能超过 72 位", "newPassword");
        }
        User user = userMapper.selectByUsername(username.trim());
        if (user == null) {
            return ApiResponse.fail("登录已过期，请重新登录", "");
        }
        if (!encoder.matches(oldPassword, user.getPassword())) {
            return ApiResponse.fail("原密码不正确", "oldPassword");
        }
        user.setPassword(encoder.encode(newPassword));
        userMapper.updateById(user);
        return ApiResponse.ok(new UserVO(user.getUsername(), jwtUtil.createToken(user.getUsername())));
    }

    /**
     * 续期：拿旧 token 换一张新 token。
     * 旧 token 可以是"刚过期"的（宽限期 7 天内），签名必须对得上，用户也必须还存在。
     * 这样用户连续使用几天不会被突然踢下线；超过 7 天没来就得重新登录。
     */
    public ApiResponse<UserVO> refresh(String token) {
        String username = jwtUtil.parseUsername(token, REFRESH_GRACE_MILLIS);
        if (username == null) {
            return ApiResponse.fail("登录已过期，请重新登录", "");
        }
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            // 用户可能已被删除，这时不能再续期
            return ApiResponse.fail("登录已过期，请重新登录", "");
        }
        return ApiResponse.ok(new UserVO(user.getUsername(), jwtUtil.createToken(user.getUsername())));
    }
}
