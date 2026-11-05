package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResponse;
import com.fenglin.springboottest.entity.Image;
import com.fenglin.springboottest.interceptor.LoginInterceptor;
import com.fenglin.springboottest.mapper.ImageMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 图片上传：头像、表情包、文章里的图片共用。
 *
 * <p>前端用 multipart 单独 fetch（api.js 的 uploadImage），字段名固定 file。
 * 图片二进制存进 MySQL 的 images 表，返回引用地址 /api/images/{id}，
 * 由 ImageController 负责把图片读出来。之前是写项目根 uploads/ 目录，
 * 备份数据库不带图片、换机器部署容易丢，所以改成存库。
 *
 * <p>旧数据里已有的 /uploads/... 地址仍然能显示：WebMvcConfig 保留了
 * /uploads/** 的静态映射做兼容，新旧两套 URL 可以共存。
 *
 * <p>必须登录（WebMvcConfig 把 /api/upload 挂到了 LoginInterceptor）。
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    /** 扩展名 -> 回给浏览器的 Content-Type，白名单之外一律拒绝 */
    private static final Map<String, String> ALLOWED = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "gif", "image/gif",
            "webp", "image/webp"
    );

    /** 单文件上限（与 application-dev.properties 的 multipart 配置呼应，双保险） */
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final ImageMapper imageMapper;

    public UploadController(ImageMapper imageMapper) {
        this.imageMapper = imageMapper;
    }

    /**
     * 上传一张图片，返回 { url: "/api/images/{id}" }。
     * 前端拿到 url 后原样存进 users.avatar / stickers.url / 消息内容，不需要改前端。
     */
    @PostMapping
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                   HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail("没有收到文件", "");
        }
        if (file.getSize() > MAX_BYTES) {
            return ApiResponse.fail("图片不能超过 10MB", "");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        String contentType = ALLOWED.get(ext);
        if (contentType == null) {
            return ApiResponse.fail("只支持 png / jpg / gif / webp 图片", "");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            return ApiResponse.fail("读取图片失败：" + e.getMessage(), "");
        }

        Image image = new Image();
        image.setContentType(contentType);
        image.setFileName(original.length() > 255 ? original.substring(0, 255) : original);
        image.setContent(bytes);
        image.setSizeBytes((long) bytes.length);
        image.setCreatedAt(LocalDateTime.now());
        // 上传者：LoginInterceptor 验过 token 后塞进 request 的属性，没登录时这里是 null
        Object uid = request.getAttribute(LoginInterceptor.ATTR_USER_ID);
        if (uid instanceof Long l) {
            image.setUploaderId(l);
        }

        try {
            // insert 后 MyBatis-Plus 会把自增 id 回填到 image.id
            imageMapper.insert(image);
        } catch (Exception e) {
            // 常见原因：images 表还没建（跑一次一键启动.bat 或 docs/schema.sql 即可）
            return ApiResponse.fail("保存失败：" + e.getMessage(), "");
        }

        return ApiResponse.ok(Map.of("url", "/api/images/" + image.getId()));
    }
}
