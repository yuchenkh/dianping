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

### 防止缓存穿透
缓存穿透问题是指用户反复请求数据库和缓存中都不存在的数据时，给数据库造成较大压力的情况。针对缓存穿透，有以下几种解决方案：
* 用户请求这类数据时，在缓存中缓存空值
* 布隆过滤
* 增强数据 ID 的复杂度，同时在后端进行请求数据 ID 的格式校验
* 对数据请求做权限管理

我们这里采取简单的「缓存空值」方案，它具有实现简单的优点，但同时会带来一定的内存成本和数据不一致的可能。

### 防止缓存击穿
缓存击穿问题也称为「热点 Key」问题，指一个**被高并发访问**且缓存重建过程较复杂的 key 失效时，无数的请求会在瞬间给数据库造成巨大的冲击。
常见的解决方案有两种：
* 互斥锁：只让一个请求线程做缓存重建工作。
* 逻辑过期：对热点 key 不设置过期时间，相反，在数据中使用一个字段表示逻辑过期时间并在后端判断缓存是否过期。
* 一个线程发现缓存过期时，不会等待缓存重建，而是直接拿过期的数据，并使用一个新的线程去做缓存重建工作（当然这个过程也需要使用互斥锁）。

互斥锁实现起来比较简单，没有额外的内存消耗，但会有互相等待的问题，性能较差，且有发生死锁的可能。
使用逻辑过期的策略，线程无需等待所以性能较好，但是实现复杂，有额外内存消耗，且不保证数据的一致性。
这两种方案的选择更多的是一致性和可用性之间的权衡，应根据自己的需求和侧重点来选择。

关于缓存击穿到底是什么样子，可以将查询商户信息的接口设置为调用 `queryShopWithoutPenetration` 方法，删除 Redis 中某一个商户的缓存，
然后使用 JMeter 模拟高并发请求进行测试。我设置在 1 秒之内进行 100 次请求，结果有 57 条请求未命中缓存并去查询了 MySQL 然后将其写入 Redis，
只有剩下 43 次请求命中了 Redis。 可以看到在高并发场景下，热点 key 失效会对 MySQL 造成巨大压力。

作为对比，调用 `queryShopWithoutBreakdown` 方法时，面对同样的 100 次请求，只有第一次请求去 MySQL 中查询，后面的 99 次请求均在 Redis 中命中，
防止了缓存击穿的问题；调用 `queryShopWithoutBreakdown2` 方法时，所有的请求都因为缓存过期而在请求互斥锁，但是只有一个线程获取锁成功并刷新缓存，
其他的线程不会等待，拿到了过时的数据。


## 优惠券秒杀
### 全局唯一 ID
在我们的业务中，订单等实体需要一个全局唯一的 ID，即不同订单的 ID 不能冲突。通常，我们在使用 MySQL 存储业务数据时会通过主键自增的方式来配置 ID。
但随着业务规模的扩大，MySQL 这类数据库可能会需要分库分表以及集群化的部署，这时在不同的 MySQL 服务器以及不同的表中保证实体 ID 的唯一性将会很难。 

Redis 并不存在表的概念，所有的数据都存放在一起，同时其 `INCR` 命令可以轻松实现 ID 自增，因此可以使用 Redis 来生成全局唯一 ID。其实还有其他的方案
可以满足我们的需求，比如 UUID。UUID 是一串 32 个字符（128 bit）的字符串，但是相比于我们的方案生成的 `long` 型 ID，在 MySQL 中的查询效率会更低。

对于某一类业务实体，这里用订单举例，我们用一个 `long` 来存放其全局唯一 ID。一个长整形数是 8 个字节，64 位，我们用最高位作为符号位，中间的 31 位
表示时间戳（指该订单创建时间与指定的开始时间之间的时间差），低位的 32 位存放同一秒内的订单序号（即可以保证一秒内有最多 2^32 个订单产生）。

### 超售问题
对于优惠券下单操作，一种简单的做法是：
1. 去 MySQL 中查询某一优惠券的信息，检查售卖的开始结束时间以及库存，如果不在设定时间内或库存不足则返回错误信息；
2. 更新库存；
3. 创建一个新订单，这时即说明下单成功。

但是这种做法会产生「超售」的问题：在高并发的状态下，大量请求同时查询库存，并且更新库存的操作相比于查询操作更加耗时，
从而导致了"查询到还有库存但实际已经没有库存"的情况的发生——即卖出了超出库存数量的优惠券。

我使用 JMeter 进行测试，设置 100 个库存，然后用 200 个并发线程去下单优惠券，最终售出了 109 张优惠券，超售 9 张。

#### 一个小细节
更新库存的步骤，我自己写的代码是：
```
voucher.setStock(voucher.getStock() - 1);
limitedVoucherService.updateById(voucher);
```
这种写法更新效率比较低。更好的写法是：
```
limitedVoucherService.update()
   .setSql("stock = stock - 1")
   .eq("voucher_id", voucherId)
   .update();
```
另外，运行在本机的 MySQL 和运行在远程服务器上的 MySQL 之间有非常大的性能差距。同样是两百个线程同时进行下单优惠券的操作，
把 MySQL 运行在本机时只需要 4 秒，而把 MySQL 运行在远程服务器上时则需要 12 秒。