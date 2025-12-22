# 开发指南

## 概要

VAT（Vert.x Application Toolkit)是基于Vert.X的异步响应式渐进式应用工具包，具有以下特性：

1. 响应式非阻塞模式：通过Future/Promise模式的全异步非阻塞操作；
2. 领域模型驱动：基于核心DDD概念的模型化约束设计；
3. 元数据支持：基于注解处理的编译时代码生成和元数据生成；
4. 渐进式应用：基于Vert.X事件驱动模式，可以按需进行单体部署或微服务集群部署，依据业务承载量实现运行时成本最优化处理；
5. 最低支持JDK 21;

## 核心概念

1. `vat.api.Data`: 基础领域数据对象，包含领域事件`vat.api.Event` 和 持久化实体`vat.api.Enitity`两种子类型;
2. `vat.api.Actor`: 用户抽象模型，代表系统用户的基础模型，通常应当直接使用`vat.foundation.users.api.Users.User`的实现;
3. `vat.api.Ability`: 用户角色抽象模型，通常用于管理一个具体领域内系统用户的权限;
4. `vat.api.Record`: 领域记录模型，通常用于表达领域内需要持久化的实体数据;
5. `vat.api.Event`: 领域事件模型，通常用于跨领域事件通讯;
6. `vat.api.Activities`: 领域活动模型，一个领域的基础行为模型，等同于其他实现中描述的领域服务，不同领域之间应当主要通过领域活动进行交互；
7. `vat.api.Domain.Context`: 领域上下文模型，表达一个领域活动运行所需的上下文信息，如依赖的其他领域，配置信息，持久化库等；

## 项目结构

基于VAT的应用项目可以是多模块的应用服务项目或单领域的库模块项目。

+ 由多个领域和业务应用入口组成的多模块maven项目.结构示例如下:

```
项目名称/
├── pom.xml                     # 根POM, 应当继承io.github.zenliucn.vertx:vat来应用vat的依赖管理和自动处理功能
├── shops/                      # 商户领域模块
│  ├── shops-api/               # 商户领域定义
│  └── shops-domain/            # 商户领域实现
├── products/                   # 商品领域模块
│  ├── products-api/            # 商品领域定义
│  └── products-domain/         # 商品领域实现
├── orders/                     # 订单领域模块
│  ├── orders-api/              # 订单领域定义
│  └── orders-domain/           # 订单领域实现
├── customers/                  # 顾客账户领域模块: 通过底层基础领域组合实现的客户功能
│  ├── customers-api/           # 顾客账户领域定义
│  └── customers-domain/        # 顾客账户领域实现
├── merchants/                  # 商户领域: 通过底层基础领域组合实现的商户功能
│  ├── merchants-api/           # 商户领域定义
│  └── merchants-domain/        # 商户领域实现
├── merchant-node/              # 商户入口节点: 可部署的web服务节点
├── customer-node/              # 顾客入口节点: 可部署的web服务节点
├── compose-node/               # 组合入口节点: 同时包含商户和顾客入口的服务节点，在使用非分布式部署时使用
```

+ 采用单领域模块的组织方式，单个领域maven项目结构示例如下：

```
领域名称/
├── pom.xml                     # 根POM, 应当继承io.github.zenliucn.vertx:vat来应用vat的依赖管理和自动处理功能
├── 领域名称-api/                # 领域定义
└── 领域名称-domain/             # 领域实现
```

### 领域模块结构示例

```
customers/
├── pom.xml
├── customers-api/                                                      # 领域定义模块
│  ├── .enhance                                                         # 代码生成控制文件(必须)
│  └── src/main/java/some/package/api                                   # 领域定义包, 一个包下只能定义一个领域
│      └──Customers.java                                                # 领域定义文件
│         └──`Customers extends Activities`                             # 最外层定义: 一般为Activities
│            ├──`Customer extends Ability.Base`                         # 客户Ability定义
│            ├──`Account extends  Record.Base`                          # 账户Record定义
│            └──`Context extends Customers, Domain.Context`             # 领域上下文定义      
├── customers-domain/                                                   # 领域实现
   └── src/main/java/some/package/domain                                # 领域实现包
       └──CustomersImpl.java                                            # 领域实现文件
          └──`CustomersImpl extends CustomersDommain<CustomersImpl>`    # 领域实现
```

### 启动节点结构示例

```
customers-node/
└── pom.xml
    └──`<properties>
               <module.name>customers.node</module.name>
               <node.shade>true</node.shade>
        </properties>` # 指定指定模块名称和打包模式
```

## 模型定义

### 可序列化类型

可序列化类型指可转换为JSON和Buffer结构的数据类型，当前包含以下几种：

1. 继承`vat.api.Data`的数据类型,包含手动实现的类型和自动生成的类型
2. JVM原始类型和包装类型: `int`,`long`,`float`,`double`,`boolean` 等
3. 基础类型:
    + 字符串: `String`,
    + 数学类型: `BigDecimal`,
    + 时间日期类型: `Instant`,`LocalDateTime`,`LocalDate`,`LocalTime`,`OffsetDateTime`,`OffsetTime`,`Duration`,`Period`
    + JSON类型: `JsonObject`,`JsonArray`,
    + 二进制类型: `Buffer`,`byte[]`,
    + 特殊类型: `Class<?>`,`Enum<?>`,`UUID`等
4. 容器类型(仅包含可序列化类型): `Map`,`List`,`Optional`

### 可持久化类型:

1. JVM原始类型和包装类型: `int`,`long`,`float`,`double`,`boolean` 等
2. 基础类型:
    + 字符串: `String`,
    + 数学类型: `BigDecimal`,
    + 时间日期类型: `Instant`,`LocalDateTime`,`LocalDate`,`LocalTime`,`OffsetDateTime`,`OffsetTime`,`Duration`,`Period`
    + JSON类型: `JsonObject`,`JsonArray`,
    + 二进制类型: `Buffer`,`byte[]`,
    + 特殊类型: `Class<?>`,`Enum<?>`,`UUID`等
3. 容器类型(仅可持久化类型): `Optional`

### 领域活动: Activities

1. 应当继承`vat.api.Activities`.
2. 应当是接口.
3. 所有活动方法均应当满足:
    + 公开的非`default` 方法,方法名唯一不重复;
    + 返回值为`io.vertx.core.Future`,其类型参数必须为可序列化类型;
    + **单个参数**或**无参数**,参数必须是可序列化类型.
4. 应当在类型上使用`@vat.api.meta.Enhance`来启动代码生成.

### 领域数据: Data

公共约束

1. 只包含jvm record 风格的getter方法定义:
    + 方法名不处理`get` 或 `is` 前缀
    + 方法返回值是可序列化类型,且不为 `void` 或 `Void`
    + 方法没有入参
    + 方法不是`default`:
        + 例外1: `@vat.api.meta.Computed` 注解的计算值方法必须是`default`方法
        + 例外2: 即时计算值的方法应当是`default`, 该方法的值不会保存于序列化对象中
    + 方法可以通过`@vat.api.meta.Alias` 来修改Json中的键名: 默认使用定义的getter名称,无需使用`@Alias`来重复定义
    + 可以通过 `@vat.api.meta.Valiate` 来定义属性校验器
    + 可以通过 `@vat.api.meta.Intercept` 来定义转换器器
    + 可以通过 `@vat.api.meta.Vitrual` 来定义实际包含于另一个 JsonObject类型的属性中的虚拟字段
    + 对应枚举类型字段可以通过`@vat.api.meta.EnumName`来指定使用枚举名称作为值,否则使用索引作为值
2. 非手工实现类型
    + 需要在类型上使用`@vat.api.meta.Enhance`注解来触发代码生成:**注意必须标记**
    + 如需要生成二进制序列化支持,需要继承`vat.api.Data.Binary`, 而非`vat.api.Data`;
    + 如需要额外生成POJO类,需要使用`@vat.api.meta.Enhance(pojo=true)`;
    + 如需要生成非record类,需要使用`@vat.api.meta.Enhance(record=false)`;
    + 默认生成的数据类型自动添加后缀`Data`，并存放于当前包内；
    + 默认生成的POJO数据类型自动添加后缀`Object`，并存放于当前包内；
    + 数据使用到的编解码器作为静态字段存放在`pkg.name.Codecs`类上,编解码器同样存放于当前包内;
3. 手工实现类型需要同时实现公开的特殊构造函数:
    + `public SomeData(JsonObject v)`: 用于从JSON反序列化
    + `public SomeData(JsonObject v,Void ignore)`: 用于从JS兼容的JSON反序列化
    + `public SomeData(Buf v)`: 用于从二进制数据反序列化,该模式需要类型实现`vat.api.Data.Binary`

#### 非持久化数据

1. `vat.api.Event`: 领域事件,在满足公共约束前提下满足以下两条之一
    + 包含一个`vat.api.meta.EventKind`注解的整数(int,long)或枚举属性,作为事件类型标记
    + 或者是不具有`vat.api.meta.EventKind`注解,但字段名称为`kind`的属性
2. `vat.api.Data`: 领域数据对象,只需要满足公共约束;

#### 持久化数据

公共约束: 在满足领域数据的公共约束下,额外满足以下约束

1. 字段类型只能是可持久化类型
2. 其他可序列化类型只能通过`@vat.api.meta.Vitrual`作为虚拟字段来定义
3. 类型上必须使用`@vat.api.meta.Table`来指定使用的表名
4. 属性上可选使用`@vat.api.meta.Column`来指定列约束
    + 对于变长数据类型，必须使用`@vat.api.meta.Column`指定字段最大长度
5. 持久化数据会在当前包下生成 `Store`后缀的仓库访问工具类
6. 所有`*.Base`基础模型均包含以下基础字段,基础字段由Storage引擎自动处理,无需手动处理
    + `id:Long` 实体自增ID
    + `removed:boolean` 软删除标记
    + `version:int` 乐观锁
    + `creator:long` 创建用户ID
    + `createdAt:Instant` 创建时间
    + `modifier:long` 最后更新用户ID
    + `modifiedAt:Instant` 最后更新时间
7. 可选继承`vat.api.trait.History` 实现变更历史记录,Storage引擎将自动将每次变更时的数据保存到`history`字段
    + 注意: 查询时默认不加载`history`字段, 如需加载需要查询前使用``

特定模型约束

1. `vat.api.Actor`: 一般不需要手动实现该类型，而是使用`vat.foundation.users.api.Users.User`;
2. `vat.api.Ability`: 必须继承`vat.api.Ability.Base`;
3. `vat.api.Record`: 必须继承`vat.api.Record.Base`;

### 领域上下文: Domain.Context

领域上下文用于定义领域运行时依赖项:

1. 类型定义:
    + 必须是公开接口,且首先继承对应的领域活动定义接口,然后继承`Domain.Context`
    + 必须在类型上使用`@vat.api.meta.Enhance`启动代码生成,生成的辅助类型命名为`领域活动Domain`
2. 配置信息:
    + `@vat.api.meta.Config` 注解的默认方法,用于从配置中读取指定值,若配置值不存在则调用该默认方法
    + 方法必须是默认方法: `default`方法
    + 方法必须无参数
    + 方法必须返回以下类型之一:
        + `int`,`boolean` 等原始类型
        + `String`,`JsonArray`,`JsonObject`
        + `BigDecimal`
        + `Buffer`
        + `Instant`
    + 返回值可以使用 `Optional` 进行包装
    + 实现应当直接使用生成的方法
3. 错误工厂
    + `@vat.api.meta.Errors`注解的默认方法,用于从配置中读取错误提示配置,若配置值不存在则调用该默认方法
    + 方法必须是默认方法: `default`方法
    + 方法可以无参数或包含若干参数用于格式化错误信息
    + 方法必须返回`vat.api.DomainError`
    + 实现应当直接使用生成的方法
4. 储存工厂
    + `@vat.api.meta.Storage`注解的默认方法,用于构造存储仓库
    + 方法必须是默认方法: `default`方法
    + 方法可以无参数或包含一个可空的`SqlConnection` 参数,当包含该参数时,该方法支持事务操作
        + 建议始终包含`SqlConnection`参数
    + 方法必须返回`vat.api.Store<本领域内定义的Entity子类型>`
    + 方法体应当直接抛出错误,建议抛出 `IllegalStateException`, 正常情形不会触发
    + 实现应当直接使用生成的方法
    + 生成的方法将返回值替换为对应生成的`xxxStore`实现类型
5. 领域依赖
    + `@vat.api.meta.Uses`注解的默认方法,用于获取其他领域活动代理
    + 方法必须是默认方法: `default`方法
    + 方法必须没有参数
    + 方法必须返回一个非本领域活动的领域活动
    + 方法体应当直接抛出错误,建议抛出 `IllegalStateException`, 正常情形不会触发
    + 实现应当直接使用生成的方法

### 注解说明

1. Activities相关注解

   | 注解             | 描述                        | 参数                                                                                                                                                          |
   |----------------|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
   | @Enhance  (必须) | 代码生成标记                    | 无参数                                                                                                                                                         |
   | @Access        | 将Activities的方法标记为标准存储访问方法 | entity: 实体类型; value: 复制策略名称;                                                                                                                                |
   | @Auditing      | 标记Activities操作应进行审计       | topic: 主题                                                                                                                                                   |
   | @Authorized    | 将Activities的方法标记为授权方法     | ability: 需要验证的ability类型; <br/>authorize: 授权方法全名;<br/> allowSystem: 是否允许系统调用;<br/> holder: 方法持有者; value: 授权方法字段; badRequest: 上下文错误提供者; forbidden: 上下文禁止错误提供者 |

2. Domain.Context相关注解

   | 注解             | 描述                            | 参数                                                           |
   |----------------|-------------------------------|--------------------------------------------------------------|
   | @Enhance  (必须) | 代码生成标记                        | endpoint: 是否使用端点模型(限Domain.Context)                          |
   | @Config        | 将Domain.Context默认方法标记为配置读取    | value: 配置值指针; mapping: 映射字段名; holder: 映射函数持有者; once: 是否仅使用一次 |
   | @Errors        | 将Domain.Context默认方法标记为错误配置    | value: 配置对象指针                                                |
   | @Storage       | 将Domain.Context默认方法标记为存储访问器   | value: 模式字符串或JSON指针                                          |
   | @Publish       | 将Domain.Context默认方法标记为事件发布器   | value: 地址字符串或JSON指针                                          |
   | @Subscribe     | 将Domain.Context默认方法标记为事件订阅器   | value: 地址字符串或JSON指针                                          |
   | @Uses          | 将Activities域上下文默认方法标记为使用外部域活动 | value: 活动地址或配置指针                                             |

3. Entity(Actor,Ability,Record)相关注解: 注意该类别注解不应当出现在非Entity类型上

   | 注解            | 描述     | 参数                                                                                                                                                                             |
   |---------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
   | @Enhance (必须) | 代码生成标记 | record: 是否生成记录实现(默认是); pojo: 是否也生成POJO实现;  internal: 内部使用标记                                                                                                                    |
   | @Column       | 列定义    | value: 列名; indexed: 索引字段; unique: 唯一约束字段; size: 存储大小; max: 最大长度/值; min: 最小长度/值; precision: 精度; scale: 小数位数; enumName: 是否使用枚举名称; interceptField: 拦截器字段名;interceptHolder: 拦截器持有类 |
   | @Table        | 表名定义   | value: 表名                                                                                                                                                                      |

4. Entity(Actor,Ability,Record)扩展注解: 一般情况下无需使用

   | 注解              | 描述                 | 参数  |
   |-----------------|--------------------|-----|
   | @Audit.Creator  | 标记身份类型的列为创建者审计     | 无参数 |
   | @Audit.Created  | 标记时间戳紧凑类型列为创建时间审计  | 无参数 |
   | @Audit.Modifier | 标记身份类型的列为修改者审计     | 无参数 |
   | @Audit.Modified | 标记时间戳紧凑类型列为修改时间审计  | 无参数 |
   | @Identity       | 标记时实体ID            | 无参数 |
   | @OptimisticLock | 标记int列为乐观锁         | 无参数 |
   | @SoftRemoved    | 标记布尔列为软删除标记        | 无参数 |
   | @Historic       | 标记JsonObject列为历史存储 | 无参数 |

5. Data相关注解

   | 注解              | 描述                | 参数                                                                                                            |
   |-----------------|-------------------|---------------------------------------------------------------------------------------------------------------|
   | @Enhance (必须)   | 代码生成标记            | record: 是否生成记录实现(默认是); pojo: 是否也生成POJO实现;  internal: 内部使用标记                                                   |
   | @Alias          | JSON属性键覆盖         | value: 别名; strict: 是否严格模式                                                                                     |
   | @Computed       | 标记默认方法用于计算JSON写入值 | 无参数                                                                                                           |
   | @Copier         | 标记当前数据类型可以从其他类型复制 | name: 复制策略名称; value: 数据源类型;                                                                                   |
   | @Copier.Process | 复制值的处理策略          | withDefault: 是否使用默认值; strategy: 策略名称; from: 源字段; holders: 持有者类; provide: 提供者字段; validate: 验证字段; convert: 转换字段 |
   | @Intercept      | 数据属性拦截器用于检查或转换值   | construct: 是否在构造时调用; value: 拦截器字段名; holder: 拦截器持有类                                                            |
   | @Validate       | 数据属性拦截器用于验证值      | construct: 是否在构造时调用; value: 验证器字段名; holder: 验证器持有类                                                            |
   | @Virtual        |
   | @EnumName       | 标记枚举属性使用枚举名称作为值   | 无参数                                                                                                           |

6. Event专用注解

   | 注解         | 描述     | 参数  |
   |------------|--------|-----|
   | @EventKind | 事件类型标记 | 无参数 |

7. 通用注解

   | 注解        | 描述          | 参数                  |
   |-----------|-------------|---------------------|
   | @Describe | 描述域对象       | desc: 描述; value: 名称 |
   | @Nullable | 标记可空参数的替代方案 | 无参数                 |

8. 实现用注解

   | 注解        | 描述      | 参数                                                                                            |
   |-----------|---------|-----------------------------------------------------------------------------------------------|
   | @Activity | 标记领域实现类 | mode: 实现模式(FOUNDATION, COMPONENT, DOMAIN, ENDPOINT); order: 部署顺序,默认按模式进行排序; auto: 配置变更时是否自动重载 |

## 领域实现
  在领域定义模块完成后基于生成的工厂代码,进行领域实现

### 生成类型说明

1. `XxxData`: 生成的基于JsonObject的基础领域对象实现
   + 通过`new XxxData(JsonObjectValue)` 来将已知的JsonObject转换为领域数据类型 
   + 通过`XxxData.toJson()` 来将领域数据转换为JsonObject
   + 通过`XxxData.toJS()` 来将领域数据转换为JS兼容的JsonObject
   + 通过`new XxxData(JsonObjectValue,null)` 来将JS兼容的JsonObject转换为领域数据类型
   + `XxxObject`具备相同操作模式
2. `XxxStore`: 生成的实体仓库类型,应当通过`Domain.Context`定义的`@Storage`方法来访问,而非直接使用;
   + 常用一级访问方法: S 代表当前仓库操作实体,T代表定义的Entity
     + `xxx()`: 字段访问器, 获取对应字段的存储访问器,用于构造存储表达式
     + 仓库操作方法:
       + 通用命名规则: `put` 存储新的实体, `set` 更新实体属性, `one` 加载一个实体 `many|any` 加载若干实体
       + `Future<Void> remove(long actor,long id)`: 按ID移除一个实体(逻辑删除)
       + `Future<Void> remove(long actor,long id,int version)`: 按ID和版本号移除一个实体(逻辑删除)
       + `Future<Integer> removeAny(long actor, Function<S, Value.BooleanValue> cond)`: 按条件表达式移除若干实体(逻辑删除)
         + cond: 通过`XxxStore` 构造匹配表达式, 例如 `t->t.id().eq(1)`
         + 返回值: 影响的实体数量
       + `Future<Void> justPut(long actor, Function<S, Collection<StmtAssign>> set)`: 储存一个新的实体
         + set: 通过store实例构造column设置列表, 例如 `t->List.of(t.name().value(someNameValue),t.gender().value(1))`
         + 不允许操作基础字段: id,removed,version,creator,createdAt,modifier,modifiedAt,removed,history(如果有)
       + `Future<Void> justPut(long actor, JsonObject set)`: 储存一个JsonObject表达的实体数据(key为属性名)
       + `Future<Long> putGetIdentity(long actor, Function<S, Collection<StmtAssign>> set)`: 储存并获取id,使用方法同`Future<Void> justPut(long actor, Function<S, Collection<StmtAssign>> set)`
       + `Future<Long> putGetIdentity(long actor, JsonObject set)`: 储存并获取id,使用方法同`Future<Void> justPut(long actor, JsonObject set)`
       + `Future<T> put(long actor, JsonObject set)`: 储存并返回实体,使用方法同`Future<Void> justPut(long actor, JsonObject set)`
       + `Future<T> put(long actor, Function<S, Collection<StmtAssign>> set)`: 储存并返回实体,使用方法同`Future<Void> justPut(long actor, Function<S, Collection<StmtAssign>> set)`
3. `XxxDomain<T>`: 领域实现上下文抽象类, 实现应当继承该类
    + 构造模式: 合计构造模式有5种:
      + `public Xxx(Vertx vertx,@Nullable String address)`: 没有定义`@Enhance(endpoint=true)`, 同时没有任何配置依赖和`@Storage`仓库依赖
      + `public Xxx(Vertx vertx,@Nullable String address，JsonObject conf)`： 只定义了配置依赖，例如 `@Config` 和/或 `@Errors`的上下文
      + `public Xxx(Vertx vertx,@Nullable String address，vat.api.implement.Web.Factory web, JsonObject conf)`： 定义了`@Enhance(endpoint=true)`的上下文,未定义`@Storage`仓库依赖
      + `public Xxx(Vertx vertx,@Nullable String address， Pool sql, Dialect dialect, JsonObject conf)`： 定义了`@Storage`仓库依赖的上下文,未定义`@Enhance(endpoint=true)`
      + `public Xxx(Vertx vertx,@Nullable String address，vat.api.implement.Web.Factory web, Pool sql, Dialect dialect, JsonObject conf)`： 定义了`@Storage`仓库依赖以及`@Enhance(endpoint=true)`依赖的上下文
    + 实现类应当包含以下两个注解
      + `@AutoService(Activities.class)`: 启用SPI自动生成 (或手动编码SPI协议)
      + `@Activity(mode=Activity.Mode.COMPOENT,autp=true)`: 部署模式配置
    + 实现类必须具有公开无参数构造函数: 用于满足SPI协议




基于以上框架描述，利用该框架实现一套离线除外协议管理模块,不要添加不存在的内容

## 领域清单

### 协议领域： OfflineContract

包含模型：

+ Contract：Record类型，离线协议记录
    + user: `long`, 外部引用的用户
    + card: `String`, 外部引用的卡号,最多32字符
    + file: `String`, 外部引用的文件ID,最多128字符
    + effect: `boolean`, 是否生效,默认为生效
+ RegisterContract:Data.Request类型, 创建协议记录请求数据
    + actor:`Optional<Long>` 用户ID
    + user: `long` 领域外部管理的用户
    + card: `String` 领域外部管理的卡号
    + files:`List<String>` 协议文件清单,需要限制最大5个
+ Contractor: Data 类型,协议查询信息请求参数
    + user: `long` 协议人ID
    + card: `String` 卡号
+ ContractInfo: Data 类型,已有协议摘要
    + id:`long` 协议ID
    + file: `String` 协议文件ID
    + effect: `boolean`
    + creator: `long` 创建用户
    + modifier: `long` 更新用户
    + createdAt: `Instant` 创建时间
    + modifiedAt: `Instant` 最后更新时间

### 协议行为:

+ `Future<Void> addContracts(RegisterContract data)`
    + 添加新的协议
+ `Future<List<ContractInfo>> contracts(Contractor data)`
    + 查询存在的协议
