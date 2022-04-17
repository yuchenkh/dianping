# Dianping：点评网站后端服务
[黑马程序员 Redis 课程](https://www.bilibili.com/video/BV1cr4y1671t?p=25&share_source=copy_web)

## 主要内容
* 用户登录
  * 密码登录
  * 手机号登录
  * 基于 Session 和拦截器的登录状态校验

## 用户登录
定义一个通用的拦截器，避免每个模块都得单独校验用户的登录状态。前端调用特定接口时，会被拦截器拦截并校验登录状态，然后选择放行或阻止访问。

这里用到了：
* Spring MVC 提供的 [HandlerInterceptor](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-handlermapping-interceptor) 
* `ThreadLocal` 类，属于 Java 并发的知识