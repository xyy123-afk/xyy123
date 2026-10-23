package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传：头像和表情包共用。
 *
 * <p>前端用 multipart 单独 fetch（api.js 的 uploadImage），字段名固定 file。
 * 存到项目根目录 uploads/yyyy-MM/uuid.ext，返回相对路径 /uploads/...，
 * 由 WebMvcConfig 的静态映射对外提供访问，Vite 已把 /uploads 代理到本服务。
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    /** 允许的图片扩展名白名单 */
    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp");

    /** 单文件上限（与 application-dev.properties 的 multipart 配置呼应，双保险） */
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    /** 保存目录：项目根下的 uploads/（随工作目录走，不用配置绝对路径） */
    private final Path uploadRoot = Paths.get("uploads").toAbsolutePath().normalize();

    /**
     * 上传一张图片，返回 { url }。
     * 必须登录（WebMvcConfig 把 /api/upload 挂到了 LoginInterceptor）。
     */
    @PostMapping
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
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
        if (!ALLOWED_EXT.contains(ext)) {
            return ApiResponse.fail("只支持 png / jpg / gif / webp 图片", "");
        }

        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = uploadRoot.resolve(month);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename));
        } catch (IOException e) {
            return ApiResponse.fail("保存失败：" + e.getMessage(), "");
        }

        // 只返回相对路径，前端拼成 /uploads/yyyy-MM/uuid.ext，同源可访问
        return ApiResponse.ok(Map.of("url", "/uploads/" + month + "/" + filename));
    }
}
