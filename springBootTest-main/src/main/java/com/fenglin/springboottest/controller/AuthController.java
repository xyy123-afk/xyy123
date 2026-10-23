package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.common.TokenBlacklist;
import com.fenglin.springboottest.dto.AuthRequest;
import com.fenglin.springboottest.dto.AvatarUpdateRequest;
import com.fenglin.springboottest.dto.ChangePasswordRequest;
import com.fenglin.springboottest.dto.UserProfileVO;
import com.fenglin.springboottest.dto.UserVO;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 认证接口。
 * 前端统一用相对路径 /api/auth/xxx 发 POST JSON，开发环境由 Vite 代理转到 8081，
 * 所以后端不需要配 CORS。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER = "Bearer ";

    private final AuthService authService;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist blacklist;

    /** 单作者博客默认关闭注册；想加账号时把 application-dev.properties 里改成 true */
    @Value("${blog.registration-enabled:false}")
    private boolean registrationEnabled;

    public AuthController(AuthService authService, UserMapper userMapper, JwtUtil jwtUtil, TokenBlacklist blacklist) {
        this.authService = authService;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
    }

    /**
     * 注册：请求体 { username, password }，成功后直接签发 token（注册即登录）。
     * 博客只给站长一个人写，默认关掉注册入口——保留接口只是为了以后想加协作者时改配置即可。
     */
    @PostMapping("/register")
    public ApiResponse<UserVO> register(@RequestBody AuthRequest req) {
        if (!registrationEnabled) {
            return ApiResponse.fail("本站未开放注册", "");
        }
        return authService.register(req.username(), req.password());
    }

    /** 登录：请求体 { username, password } */
    @PostMapping("/login")
    public ApiResponse<UserVO> login(@RequestBody AuthRequest req) {
        return authService.login(req.username(), req.password());
    }

    /**
     * 续期：把旧 token 放在 Authorization 头里（api.js 会自动带），换一张新 token。
     * 前端每次打开页面时静默调一次，用户就不会用到一半突然过期。
     * 失败时返回 code=1 + "登录已过期，请重新登录"，前端清掉凭证回登录页即可。
     */
    @PostMapping("/refresh")
    public ApiResponse<UserVO> refresh(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = extractToken(authorization);
        // 已登出的 token 不能再续期（否则 logout 就白做了）
        if (token != null && blacklist.contains(token)) {
            return ApiResponse.fail("登录已过期，请重新登录", "");
        }
        return authService.refresh(token);
    }

    /**
     * 修改密码：{ oldPassword, newPassword }。必须带有效 token（走 token 识别身份，不接受 username 回退）。
     * 成功后旧 token 立即失效（加黑名单），返回的新 token 才是有效的。
     */
    @PostMapping("/change-password")
    public ApiResponse<UserVO> changePassword(@RequestBody ChangePasswordRequest req,
                                             @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = extractToken(authorization);
        String username = token != null ? jwtUtil.parseUsername(token) : null;
        ApiResponse<UserVO> res = authService.changePassword(username, req.oldPassword(), req.newPassword());
        // 改密成功 → 旧 token 立刻作废，前端用返回的新 token 替换
        if (res.code() == 0 && token != null) {
            Date exp = jwtUtil.getExpiration(token);
            if (exp != null) {
                blacklist.add(token, exp.getTime());
            }
        }
        return res;
    }

    /** 获取当前登录用户资料（不含密码） */
    @GetMapping("/me")
    public ApiResponse<UserProfileVO> me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = extractToken(authorization);
        String username = token != null ? jwtUtil.parseUsername(token) : null;
        if (username == null) {
            return ApiResponse.fail("登录已过期，请重新登录", "");
        }
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return ApiResponse.fail("登录已过期，请重新登录", "");
        }
        return ApiResponse.ok(new UserProfileVO(user.getId(), user.getUsername(),
                user.getNickname(), user.getAvatar(), user.getCreatedAt()));
    }

    /**
     * 更新我的头像：{ avatar: "/uploads/..." }，空串表示清除头像。
     * 只接受本站上传的相对路径，防止存外链钓鱼图。
     * 返回最新资料，前端直接就地刷新，不用再拉一次 /me。
     */
    @PostMapping("/avatar")
    public ApiResponse<UserProfileVO> updateAvatar(@RequestBody AvatarUpdateRequest req,
                                                   @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = extractToken(authorization);
        String username = token != null ? jwtUtil.parseUsername(token) : null;
        if (username == null) {
            return ApiResponse.fail("登录已过期，请重新登录", "");
        }
        String avatar = req == null || req.avatar() == null ? "" : req.avatar().trim();
        if (!avatar.isEmpty() && !avatar.startsWith("/uploads/")) {
            return ApiResponse.fail("头像地址不合法", "");
        }
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return ApiResponse.fail("登录已过期，请重新登录", "");
        }
        user.setAvatar(avatar.isEmpty() ? null : avatar);
        userMapper.updateById(user);
        return ApiResponse.ok(new UserProfileVO(user.getId(), user.getUsername(),
                user.getNickname(), user.getAvatar(), user.getCreatedAt()));
    }

    /**
     * 退出登录：把当前 token 加入黑名单，使其立即失效。
     * JWT 本身无状态，这里是"主动作废"的轻量实现；前端收到成功后清掉本地 token 即可。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = extractToken(authorization);
        if (token != null) {
            Date exp = jwtUtil.getExpiration(token);
            if (exp != null) {
                blacklist.add(token, exp.getTime());
            }
        }
        return ApiResponse.ok(null);
    }

    /** 从 Authorization 头里取出 token（去掉 "Bearer " 前缀），没有或不合法返回 null */
    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith(BEARER)) {
            String token = authorization.substring(BEARER.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
