package com.fenglin.springboottest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 站长通知邮件：有新评论时发一封提醒。
 *
 * <p>设计取舍：
 * - 默认关闭（blog.mail-enabled=false），配上 SMTP 账号并打开开关才生效，没配置时所有方法直接返回；
 * - 整个发送包在 try/catch 里：邮件只是锦上添花，SMTP 挂了不能影响评论主流程；
 * - @Async 异步发送，评论接口不等 SMTP 往返（那可能是几秒）。
 */
@Service
public class NotifyMailService {

    private static final Logger log = LoggerFactory.getLogger(NotifyMailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String from;
    private final String to;

    public NotifyMailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                             @Value("${blog.mail-enabled:false}") boolean enabled,
                             @Value("${spring.mail.username:}") String from,
                             @Value("${blog.notify-email:${spring.mail.username:}}") String to) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.from = from;
        this.to = to;
    }

    /**
     * 新评论提醒。站长自己发的评论不提醒（调用方负责过滤）。
     * 方法必须 public + 无返回值，@Async 才生效；异常全部吞掉只留 warn 日志。
     */
    @Async
    public void notifyOwnerNewComment(String postTitle, String slug, String nickname, String content) {
        if (!enabled || from == null || from.isBlank() || to == null || to.isBlank()) {
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("邮件通知已开启但 spring.mail.host 未配置，跳过发送");
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject("【枫林小筑】新评论：" + postTitle);
            msg.setText("""
                    %s 在文章《%s》发表了评论：

                    %s

                    查看文章：/post/%s
                    （系统邮件，请勿回复）
                    """.formatted(nickname, postTitle, content, slug));
            sender.send(msg);
            log.info("评论通知邮件已发送 -> {}", to);
        } catch (Exception e) {
            // SMTP 失败只记日志：评论已经入库成功了，不能因为邮件报错让前端收到失败提示
            log.warn("评论通知邮件发送失败: {}", e.getMessage());
        }
    }
}
