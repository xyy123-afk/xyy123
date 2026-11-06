package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.entity.Image;
import com.fenglin.springboottest.mapper.ImageMapper;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 读图接口：把 images 表里的二进制原样吐给浏览器。
 *
 * <p>UploadController 存图后返回 /api/images/{id}，页面上的 &lt;img src&gt; 直接用它。
 * <b>本接口必须匿名可读</b>——&lt;img&gt; 不会带 Authorization 头，
 * 一旦挂上 LoginInterceptor，头像和表情包会全裂，所以 WebMvcConfig 里刻意没挂。
 *
 * <p>图片按 id 不可变（同一个 id 永远是同一张图），可以放心让浏览器长期缓存，
 * 省掉来回查库。
 */
@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageMapper imageMapper;

    public ImageController(ImageMapper imageMapper) {
        this.imageMapper = imageMapper;
    }

    /**
     * 按 id 取图。找不到返回 404，前端 &lt;img&gt; 就显示破图，这是预期行为。
     *
     * @param id images.id
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.notFound().build();
        }
        Image image = imageMapper.selectById(id);
        if (image == null || image.getContent() == null) {
            return ResponseEntity.notFound().build();
        }

        // contentType 存的就是 image/png 这种合法 MIME；万一是脏数据就退回 octet-stream，别让浏览器乱猜
        MediaType type;
        try {
            type = MediaType.parseMediaType(image.getContentType());
        } catch (Exception e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(image.getContent());
    }
}
