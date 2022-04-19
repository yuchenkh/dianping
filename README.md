# Dianping：点评网站后端服务
[黑马程序员 Redis 课程](https://www.bilibili.com/video/BV1cr4y1671t?p=25&share_source=copy_web)

## 主要内容
* 用户登录
  * 密码登录
  * 手机号登录
  * 基于 Session 和拦截器的登录状态校验

- [ ] 用户登录
- [ ] 商户查询缓存
- [ ] 优惠券秒杀
- [ ] 分布式锁
- [ ] 消息队列
- [ ] 达人探店
- [ ] 好友关注
- [ ] 附近商铺
- [ ] 用户签到
- [ ] UV 统计
- [ ] 分布式缓存
- [ ] 多级缓存

## 用户登录

后端视角下的登录动作，其实是向 Redis 中写入相应用户的记录，并设定一定的过期时间，代表一个 session。
一旦用户在这段时间内没有任何访问或操作，则将 Redis 中的记录删除，代表 session 结束。

### Redis 在这一部分的作用
我们选择用 Redis 而非 MySQL 来存放发送给用户的验证码以及用户的 token。理由是这类数据都是有时效性的，超过设定时间会过期，
而这一性质正好符合 Redis 的使用场景。

### 使用拦截器校验用户登录状态
这里使用的拦截器基于 Spring MVC 提供的 [HandlerInterceptor](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-handlermapping-interceptor)
，我们定义了两个拦截器：
1. `AccessInterceptor`：拦截对所有后端接口的请求，如果带有合法 token 则刷新其过期时间，否则放行到下一拦截器。
2. `LoginInterceptor`：拦截对需要登录的接口的请求，避免每个功能都得单独校验用户的登录状态。如果未登录则不予放行。

这里同时用到了 `ThreadLocal` 类，属于 Java 并发的知识。

## 商户信息缓存
将之前从 MySQL 中查询到的商户信息存放至 Redis 作为缓存，以提高访问速度，增加并发能力。

当然，不是所有数据都适合放到缓存中，我们需要权衡成本和收益。对于经常请求而不经常变动的数据就很适合放在缓存中。

### 缓存更新策略
采取主动更新的策略以保持 MySQL 和 Redis 中数据的一致性。
具体地，每次修改 MySQL 数据时，同时删除 Redis 中相应记录（而非更新）。
注意这两步操作也是有先后顺序的：先修改 MySQL 记录，后删除 Redis 记录，这样能**最大限度地**减少出现数据不一致的可能性。
除此之外，对于 Redis 中的缓存设置过期时间，进一步保证数据的一致性。