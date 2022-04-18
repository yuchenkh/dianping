# Dianping：点评网站后端服务
[黑马程序员 Redis 课程](https://www.bilibili.com/video/BV1cr4y1671t?p=25&share_source=copy_web)

## 主要内容
* 用户登录
  * 密码登录
  * 手机号登录
  * 基于 Session 和拦截器的登录状态校验

## 用户登录

后端视角下的登录动作，其实是向 Redis 中写入相应用户的记录，并设定一定的过期时间，代表一个 session。
一旦用户在这段时间内没有任何访问或操作，则将 Redis 中的记录删除，代表 session 结束。

### 使用拦截器校验用户登录状态
这里使用的拦截器基于 Spring MVC 提供的 [HandlerInterceptor](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-handlermapping-interceptor)
，我们定义了两个拦截器：
1. `AccessInterceptor`：拦截对所有后端接口的请求，如果带有合法 token 则刷新其过期时间，否则放行到下一拦截器。
2. `LoginInterceptor`：拦截对需要登录的接口的请求，避免每个功能都得单独校验用户的登录状态。如果未登录则不予放行。

这里同时用到了 `ThreadLocal` 类，属于 Java 并发的知识。