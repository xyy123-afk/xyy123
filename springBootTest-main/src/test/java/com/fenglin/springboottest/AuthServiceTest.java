package com.fenglin.springboottest;

import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.dto.UserVO;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 注册 / 登录的集成测试：连真实的 MySQL（xyy_login）跑一遍。
 *
 * <p>类上的 @Transactional 是关键：每个测试方法跑完自动回滚，
 * 不会在数据库里留下一堆 zhangsan01、lisi02 之类的测试脏数据。
 *
 * <p>运行方式：mvn test，或者在 IDEA 里直接点方法左边的绿色箭头。
 */
@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /** 每次跑用不同用户名，避免撞上库里已存在的账号 */
    private String newUsername() {
        return "u" + System.nanoTime();
    }

    @Test
    void 注册成功_密码以BCrypt密文落库() {
        String username = newUsername();
        ApiResponse<UserVO> res = authService.register(username, "abc123456");

        assertEquals(0, res.code(), "注册应当成功");
        assertEquals(username, res.data().username());

        // 库里查出来的一定是 $2a$ 开头的 BCrypt 密文，绝不能是明文
        String stored = userMapper.selectByUsername(username).getPassword();
        assertNotNull(stored);
        assertEquals(true, stored.startsWith("$2a$"), "密码必须是 BCrypt 密文，实际是：" + stored);
        assertEquals(false, "abc123456".equals(stored), "绝不能明文存密码");
    }

    @Test
    void 用户名重复_注册失败并提示username字段() {
        String username = newUsername();
        assertEquals(0, authService.register(username, "abc123456").code());

        ApiResponse<UserVO> again = authService.register(username, "abc123456");
        assertEquals(1, again.code(), "重复用户名应当失败");
        assertEquals("username", again.field(), "要提示前端高亮用户名输入框");
    }

    @Test
    void 密码少于6位_注册失败并提示password字段() {
        ApiResponse<UserVO> res = authService.register(newUsername(), "123");
        assertEquals(1, res.code());
        assertEquals("password", res.field());
    }

    @Test
    void 登录_密码正确才通过() {
        String username = newUsername();
        authService.register(username, "abc123456");

        assertEquals(0, authService.login(username, "abc123456").code(), "正确密码应当登录成功");
        assertEquals(1, authService.login(username, "wrong-pwd").code(), "错误密码应当登录失败");
        assertEquals(1, authService.login("not-exist-" + username, "abc123456").code(), "不存在的用户应当登录失败");
    }

    @Test
    void 登录成功后签发JWT_且能解析出用户名() {
        String username = newUsername();
        authService.register(username, "abc123456");

        ApiResponse<UserVO> res = authService.login(username, "abc123456");
        assertEquals(0, res.code());

        String token = res.data().token();
        assertNotNull(token, "登录成功必须返回 token");
        assertFalse(token.isBlank());
        // token 里应当能还原出登录的用户名
        assertEquals(username, jwtUtil.parseUsername(token));
    }

    @Test
    void 伪造或残缺的Token_一律解析为null() {
        // 解析失败返回 null，由接口层统一提示"登录已过期"，不把具体原因暴露出去
        assertNull(jwtUtil.parseUsername("abc.def.ghi"));
        assertNull(jwtUtil.parseUsername(""));
        assertNull(jwtUtil.parseUsername(null));
    }

    @Test
    void 续期_旧token能换成新token() {
        String username = newUsername();
        String oldToken = authService.register(username, "abc123456").data().token();

        ApiResponse<UserVO> res = authService.refresh(oldToken);
        assertEquals(0, res.code(), "有效的 token 应当能续期");
        assertEquals(username, res.data().username());
        // 新 token 同样要能解析出同一个用户名
        assertEquals(username, jwtUtil.parseUsername(res.data().token()));
    }

    @Test
    void 续期_伪造或缺失的token一律拒绝() {
        assertEquals(1, authService.refresh("abc.def.ghi").code());
        assertEquals(1, authService.refresh(null).code());
        assertEquals(1, authService.refresh("").code());
    }
}
