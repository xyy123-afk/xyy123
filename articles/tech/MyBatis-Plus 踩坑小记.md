用 MyBatis-Plus 写了几个星期的 CRUD，记录几个踩过的坑，都是实际撞上才长记性的那种。

**坑一：逻辑删除和唯一索引打架**

给 username 加了唯一索引，又配了逻辑删除。第一个用户注销后，同名的永远注册不回来——因为逻辑删除只是把 deleted 改成 1，旧记录还占着唯一索引。

解法：要么唯一索引里带上 deleted 字段，要么注册前先物理检查。学习项目我选了后者，简单直接。

**坑二：updateById 不更新 null 字段**

想把某个字段清空，用 updateById 传 null，结果发现压根没生效。这不是 bug，是默认策略：`field-strategy` 默认 NOT_NULL，null 字段直接被跳过。

解法：用 `UpdateWrapper` 的 `set("字段", null)`，别跟全局策略较劲。

**坑三：分页插件忘了配**

`selectPage` 返回的数据永远是全量，页码参数像没收到一样。查了半天代码，最后发现是 `MybatisPlusInterceptor` 没注册 `PaginationInnerInterceptor`——不加这个，分页就是摆设。

配置加回来之后，一次就好了。

**坑四：驼峰映射**

实体里的 `createdAt` 查出来一直是 null。数据库列叫 `created_at`，MyBatis-Plus 默认开启了驼峰转换，但我自己写的 XML 里手滑用了 `select *` 加了别名，把映射关系打乱了。删掉多余的 `resultMap`，一切恢复正常。

总结下来其实就一句话：框架把重复劳动都干了，但配置那一屏你得真的读一遍。我现在的习惯是，新建项目先把 `application.properties` 里 MyBatis-Plus 那几行过一遍再开工，比事后翻文档快多了。
