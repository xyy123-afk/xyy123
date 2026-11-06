package com.fenglin.springboottest.util;

/**
 * 本站图片地址的合法性判断。
 *
 * <p>数据库里存的图片地址只有两种形态：
 * <ul>
 *   <li>/api/images/{数字} —— 新上传，二进制存在 images 表，由 ImageController 读</li>
 *   <li>/uploads/xxx —— 老数据，WebMvcConfig 还留着静态映射做兼容</li>
 * </ul>
 *
 * <p>头像、表情包这类"用户能自己填地址"的字段都要过一遍这里，
 * 只放行本站相对路径，防止有人存个外链钓鱼图（或是 javascript: 之类的伪协议）。
 * 判断做成静态方法，AuthController 和 ChatService 直接调，不用各自抄一遍正则。
 */
public final class ImageUrls {

    /** /api/images/ 后面必须是纯数字 id，别的一概不认 */
    private static final String API_PREFIX = "/api/images/";

    /** 老图片的目录，注意要连斜杠一起匹配，避免 /uploadsXxx 这种混淆 */
    private static final String LEGACY_PREFIX = "/uploads/";

    private ImageUrls() {
        // 纯工具类，不给实例化
    }

    /**
     * 是不是本站图片地址。
     *
     * @param url 待校验地址，允许 null
     * @return true 表示可以存
     */
    public static boolean isLocal(String url) {
        if (url == null) {
            return false;
        }
        String u = url.trim();
        if (u.isEmpty()) {
            return false;
        }
        if (u.startsWith(API_PREFIX)) {
            String id = u.substring(API_PREFIX.length());
            return !id.isEmpty() && isAllDigits(id);
        }
        if (u.startsWith(LEGACY_PREFIX)) {
            // 老数据：/uploads/ 之后要有内容，且不能出现 .. 之类的穿越
            String rest = u.substring(LEGACY_PREFIX.length());
            return !rest.isEmpty() && !rest.contains("..") && !rest.contains("\\");
        }
        return false;
    }

    private static boolean isAllDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
