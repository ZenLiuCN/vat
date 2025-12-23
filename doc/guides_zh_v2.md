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
│            ├──`Customer extends Ability.Base`                         # 客户Ability定义(可选)
│            ├──`Account extends  Record.Base`                          # 账户Record定义
│            └──`Context extends Customers, Domain.Context`             # 领域上下文定义      
├── customers-domain/                                                   # 领域实现
   └── src/main/java/some/package/domain                                # 领域实现包
       └──CustomersImpl.java                                            # 领域实现文件
          └──`CustomersImpl extends CustomersDommain<CustomersImpl>`    # 领域实现
```

### 启动节点结构示例

启动节点最小需要引入依赖 `io.github.zenliucn.vertx:core` 以及 `io.github.zenliucn.vertx:runtime-node`

```
customers-node/
├── src/main/resources
│   └── logback.xml # 如果使用logback logger
└── pom.xml
    └──`<properties>
               <module.name>customers.node</module.name>
               <node.shade>true</node.shade>
        </properties>` # 指定指定模块名称和打包模式
```

### `io.github.zenliucn.vertx:vat` 母包关键说明

1. 包含具体的依赖管理和maven配置信息,无需在实现中进行额外定义
2. 动态特性通过子模块的`<properties/>`进行控制
3. 标准启动信息已经包含在配置中,无特殊需求无需配置
4. 关键配置属性说明:
    + `module.name:String`: 配置本模块的自动JPMS名称
    + `node.shade:boolean`: 配置本模块需要打包为可启动的fat-jar,默认false
    + `node.lib:boolean`: 配置本模块需要打包为可启动的libs部署包,默认false

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
3. 应当在类型上使用`@vat.api.meta.Enhance`来启动代码生成.
4. 元数据说明:
    + `@Describe`: 元数据说明,可选, 可用于方法或类型
        + `value:String`: 名称
        + `desc:String?`: 详细说明
5. 领域活动方法: 所有活动方法均应当满足
    + 公开的非`default` 方法,方法名唯一不重复;
    + 返回值为`io.vertx.core.Future`,其类型参数必须为可序列化类型;
    + **单个参数**或**无参数**,参数必须是可序列化类型.
    + 可使用的扩展注解为:
        + `@Access`: 将Activities的方法标记为标准存储访问方法
            + entity: 实体类型class,当方法返回值不为实体本身时需要提供
            + value: 复制策略名称,当入参需要是Request实体时使用;
            + 支持的模式:
                + `identity`模式: 通过实体ID获取实体, 方法签名为`Future<Optional<E>> identityXXX(long entityId)`
                + `authorize`模式: 通过用户ID获取本领域定义的相关Ability, 方法签名为
                  `Future<Optional<A>> authorizeXXX(long userId)`
                + `create`模式: 创建新实体数据, 方法签名为`Future<E|Void> createXXX(CreateData createData)`, 其中
                  `CreateData` 需要实现 `vat.api.trait.Accessor.Creator`
                + `remove`模式: 根据实体ID和版本号移除实体, 方法签名为`Future<Void> createXXX(RemoveData  removeData)`,
                  其中`RemoveData` 需要实现 `vat.api.trait.Accessor.Remover`
                + `update`模式: 根据实体ID和版本号更新实体, 方法签名为
                  `Future<Void> createXXX(ModifiedData  modifiedData)`, 其中`ModifiedData` 需要实现
                  `vat.api.trait.Accessor.Modificator`
        + `@Authorize`: 将Activities的方法标记为需要授权验证方法
            + ability: 需要验证的ability类型class，当属于外部领域或无法在上下文信息中获取是应当提供;
            + authorize: 授权方法全名，当不是标准命名方式时需要提供，默认为`authroizeAblitiyName`;
            + allowSystem: 是否允许系统调用, 当未提供actor时,不进行权限校验;
            + holder: 处理器静态字段持有类,默认为本领域上下文接口类;
            + value: 授权方法静态字段名称,该字段应当符合签名`BiPredicate<? extends Data.Request<?>,@Nullable Ability>`;
            + badRequest: 错误请求提供静态字段,字段类型应当满足`BiFunction<InputType,Ability?,DomainError>` 或
              `Provider<DomainError>`;
            + forbidden: 禁止错误提供静态字段,字段类型应当满足`BiFunction<InputType,Ability?,DomainError>` 或
              `Provider<DomainError>`
        + `@Auditing`: 标记当前Activities操作应进行审计
            + `mode`: 模式,默认为 `vat.api.meta.Auditing.Mode.INVOKE`
                + `vat.api.meta.Auditing.Mode.INVOKE`: 调用审计,发送请求和结果到`vat.foundation.audits`领域
                + `vat.api.meta.Auditing.Mode.REQUEST`: 请求审计,发送请求信息到`vat.foundation.audits`领域
                + `vat.api.meta.Auditing.Mode.RESPONSE`: 响应审计,发送响应信息到`vat.foundation.audits`领域
                + `vat.api.meta.Auditing.Mode.FAILURE`: 错误审计,发送包含请求信息的错误记录到`vat.foundation.audits`领域
            + `topic`: 主题,默认为完整方法Identifier
            + 添加审计功能后,实际逻辑需要实现`doXxxx`方法来进行实现,审计功能自动包含在生成的原始方法逻辑中

### 领域上下文: Domain.Context

领域上下文用于定义领域运行时依赖项:

1. 类型定义:
    + 必须是公开接口,且首先继承对应的领域活动定义接口,然后继承`Domain.Context`
    + 必须在类型上使用`@vat.api.meta.Enhance`启动代码生成,生成的辅助类型命名为`领域活动Domain`
2. 元数据说明:
   + `@Describe`: 元数据说明,可选, 可用于方法或类型
       + `value:String`: 名称
       + `desc:String?`: 详细说明
3. 配置信息:
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
4. 错误工厂
    + `@vat.api.meta.Errors`注解的默认方法,用于从配置中读取错误提示配置,若配置值不存在则调用该默认方法
    + 方法必须是默认方法: `default`方法
    + 方法可以无参数或包含若干参数用于格式化错误信息
    + 方法必须返回`vat.api.DomainError`
    + 实现应当直接使用生成的方法
5. 事件订阅
    + `@vat.api.meta.Subscribe`注解的默认方法,用于订阅本领域或其他领域的事件
        + `value:String?`: 定义使用的事件地址或配置JsonPointer(以`/`开始),默认为事件完整Identifier
    + 方法必须是默认方法: `default`方法
    + 方法必须包含一个类型为`EventType`的入参
    + 方法必须返回`void`
    + 领域实现应当实现该方法的处理逻辑
6. 事件发布
    + `@vat.api.meta.Publish`注解的默认方法,用于发布本领域或其他领域的事件
        + `value:String?`: 定义使用的事件地址或配置JsonPointer(以`/`开始),默认为事件完整Identifier
    + 方法必须是默认方法: `default`方法
    + 方法必须包含一个类型为`Consumer<EventType>`的入参
    + 方法必须返回`void`
    + 领域实现应当使用生成的`XXXXPublish`的方法进行事件构造,生成函数的入参为`Consumer<EventTypeData>`,即替换成了生成的事件Data类型
7. 储存工厂
    + `@vat.api.meta.Storage`注解的默认方法,用于构造存储仓库
        + `value:String?`: 定义使用的数据库Schema或读取配置JsonPointer(以`/`开始),默认无需Schema
    + 方法必须是默认方法: `default`方法
    + 方法可以无参数或包含一个可空的`SqlConnection` 参数,当包含该参数时,该方法支持事务操作
        + 建议始终包含`SqlConnection`参数
    + 方法必须返回`vat.api.Store<本领域内定义的Entity子类型>`
    + 方法体应当直接抛出错误,建议抛出 `IllegalStateException`, 正常情形不会触发
    + 实现应当直接使用生成的方法
    + 生成的方法将返回值替换为对应生成的`xxxStore`实现类型
8. 领域依赖
    + `@vat.api.meta.Uses`注解的默认方法,用于获取其他领域活动代理
        + `value:String?`: 定义使用的领域服务地址或读取配置JsonPointer(以`/`开始),默认使用默认地址
    + 方法必须是默认方法: `default`方法
    + 方法必须没有参数
    + 方法必须返回一个非本领域活动的领域活动
    + 方法体应当直接抛出错误,建议抛出 `IllegalStateException`, 正常情形不会触发
    + 实现应当直接使用生成的方法

### 领域数据: Data, 非模型子类型一般用于数据传输

公共约束

1. 类型标记
    + `@Enhance`: 触发代码生成,非手工实现必须标记
        + `record:boolean?`: 是否生成记录实现(默认是);
        + `pojo:boolean?`: 是否也生成POJO实现;
        + `internal:boolean?`: 内部使用标记
    + `@Describe`: 元数据说明,可选
      + `value:String`: 名称
      + `desc:String?`: 详细说明
2. 属性定义: 通过jvm record 风格的getter方法进行属性定义
    + 方法名不处理`get` 或 `is` 前缀
    + 方法返回值是可序列化类型,且不为 `void` 或 `Void`
    + 方法没有参数
    + 方法不是`default`:
        + 例外1: `@vat.api.meta.Computed` 注解的计算值方法必须是`default`方法
        + 例外2: 即时计算值的方法应当是`default`, 该方法的值不会保存于序列化对象中
    + 支持的注解处理器:
        + `@Describe`: 元数据说明,可选
          + `value:String`: 名称
          + `desc:String?`: 详细说明
        + `@vat.api.meta.Alias`: 定义Json中的键名别名，读取时兼容别名读取，写入时只写入别名， 默认使用定义的getter名称,无需使用
          `@Alias`来重复定义
            + `value:String`: 别名
            + `strict:boolean?`: 是否严格模式,严格模式读取时也只读取别名指定的键
        + `@vat.api.meta.Computed`: 定义计算属性,此时方法必须为`default`
          方法,返回值即为默认值,在输出到JsonObject时,如对应键不存在,将默认值输出到JsonObject中;
        + `@vat.api.meta.Valiate`: 定义属性校验器
            + `construct:boolean?`: 是否在构造时进行验证, 默认false;
            + `holder:Class<?>?`: 验证器静态字段持有类,默认为`vat.api.implement.Validators`
            + `value:String`: 静态字段名称,该字段必须满足类型` Consumer<T>`或对应的原始类型Consumer
            + 可重复定义,执行时按定义顺序进行
        + `@vat.api.meta.Intercept`: 定义转换器
            + `construct:boolean?`: 是否在构造时进行转换, 默认false,只在设置值时进行转换;
            + `holder:Class<?>?`: 验证器静态字段持有类,默认为`vat.api.implement.Interceptors`
            + `value:String`: 静态字段名称,该字段必须满足类型` UnaryOperator<T>`或对应的原始类型UnaryOperator
            + 可重复定义,执行时按定义顺序进行
        + `@vat.api.meta.Vitrual`: 定义实际包含于另一个JsonObject类型的属性中的虚拟字段
            + `value:String`: 持有的JsonObject属性名
            + `key:String?`: 存储的键名, 默认为getter方法名
        + `@vat.api.meta.EnumName`: 指定使用枚举名称作为值,否则使用索引作为值,注解的属性类型必须是枚举类型
3. 非手工实现类型
    + 需要在类型上使用`@vat.api.meta.Enhance`注解来触发代码生成:**注意必须标记**
    + 如需要生成二进制序列化支持,需要继承`vat.api.Data.Binary`, 而非`vat.api.Data`;
    + 如需要额外生成POJO类,需要使用`@vat.api.meta.Enhance(pojo=true)`;
    + 如需要生成非record类,需要使用`@vat.api.meta.Enhance(record=false)`;
    + 默认生成的数据类型自动添加后缀`Data`，并存放于当前包内；
    + 默认生成的POJO数据类型自动添加后缀`Object`，并存放于当前包内；
    + 数据使用到的编解码器作为静态字段存放在`pkg.name.Codecs`类上,编解码器同样存放于当前包内;
4. 手工实现类型需要同时实现公开的特殊构造函数:
    + `public SomeData(JsonObject v)`: 用于从JSON反序列化
    + `public SomeData(JsonObject v,Void ignore)`: 用于从JS兼容的JSON反序列化
    + `public SomeData(Buf v)`: 用于从二进制数据反序列化,该模式需要类型实现`vat.api.Data.Binary`

#### 非持久化Data

1. `vat.api.Event`: 领域事件,在满足公共约束前提下满足以下两条之一
    + 包含一个`vat.api.meta.EventKind`注解的整数(int,long)或枚举属性,作为事件类型标记
    + 或者是不具有`vat.api.meta.EventKind`注解,但字段名称为`kind`的属性
2. `vat.api.Data`: 领域数据对象,只需要满足公共约束;

#### 持久化Data: 包含Actor,Ability,Record 三种模型

公共约束: 在满足领域数据的公共约束下,额外满足以下约束

1. 字段类型只能是可持久化类型
2. 其他可序列化类型只能通过`@vat.api.meta.Vitrual`作为虚拟字段来定义
3. 类型上必须使用`@vat.api.meta.Table`来指定使用的表名
4. 专用getter注解
    + `@vat.api.meta.Column`来指定列约束, 可选
        + `value:String?`: 指定列名,默认采用自动转换为`snake_case`
        + `indexed:String[]?`: 指定**参与索引的字段清单**,注意需要使用列名而非属性名,若同一个字段参与多个不同的索引,需要分别定义在不同的字段上
        + `unique:String[]?`: 指定**参与唯一索引的字段清单**,注意需要使用列名而非属性名,不能同`indexed`同时使用
        + `size:int?`: 指定数据大小
        + `max:int?`: 指定数据最大值(只有标记意义)或长度最大值
        + `min:int?`: 指定数据最小值(只有标记意义)
        + `precision:int?`: 指定数据精度(用于浮点类型的存储)
        + `scale:int?`: 指定数据精度(用于浮点类型的存储)
        + `enumName:boolean?`: 指定枚举类型采用枚举名称保存,默认false
        + `interceptHolder:Class<?>?`: 指定拦截处理器持有类型,默认为`void.class`
        + `interceptField:String?`: 拦截处理器静态字段名称,该字段应当满足`vat.api.store.Interceptor<T>`,即
          `T intercept(boolean read, T t)`函数式签名
        + 对于变长数据类型，如`String`,`byte[]`,`Buffer`,`Period`,`Duration`,`Class`等,必须使用`max`或`size`指定字段最大长度
        + 采用枚举名称保存的枚举值,必须使用`max`或`size`指定字段最大长度
5. 持久化数据会在当前包下生成 `Store`后缀的仓库访问工具类
6. 所有`*.Base`基础模型均包含以下基础字段,基础字段由Storage引擎自动处理,无需手动处理
    + `id:Long` 实体自增ID
    + `removed:boolean` 软删除标记,Storage引擎在查询时自动排除,无需手动声明排除
    + `version:int` 乐观锁,修改时Storage引擎自动修改,无需手动维护
    + `creator:long` 创建用户ID,由Storage引擎自动维护,创建时需要提供actor入参,如无明确actor,使用-1作为缺失值
    + `createdAt:Instant` 创建时间,由Storage引擎自动维护
    + `modifier:long` 最后更新用户ID,由Storage引擎自动维护,创建时需要提供actor入参,如无明确actor,使用-1作为缺失值
    + `modifiedAt:Instant` 最后更新时间,由Storage引擎自动维护
7. 可选继承`vat.api.trait.History` 实现变更历史记录,Storage引擎将自动将每次变更时的数据保存到`history`字段
    + 注意: 查询时默认不加载`history`字段, 如需加载需要使用带有`WithHistory`后缀的查询方法
8. 不常用扩展注解: 当集成`*.Base`时不需要使用以下注解
    + `@Audit.Creator`:标记身份类型的列为创建者审计字段, 类型必须是`@Identity`标记的Id类型
    + `@Audit.Created`: 标记时间戳紧凑类型列为创建时间审计字段,类型必须是Instant类型
    + `@Audit.Modifier`: 标记身份类型的列为修改者审计字段, 类型必须是`@Identity`标记的Id类型
    + `@Audit.Modified`: 标记时间戳紧凑类型列为修改时间审计字段,类型必须是Instant类型
    + `@Identity`: 标记时实体ID字段
    + `@OptimisticLock`: 标记为乐观锁字段,类型必须是`int`
    + `@SoftRemoved`: 标记为软删除标记,类型必须是`boolean`
    + `@Historic`: 标记字段为历史存储,类型必须是`JsonObject`, `*.Base`不包含本功能,需要额外继承`vat.api.trait.History`

特定模型约束

1. `vat.api.Actor`: 一般不需要手动实现该类型，而是使用`vat.foundation.users.api.Users.User`;
2. `vat.api.Ability`: 必须继承`vat.api.Ability.Base`;
3. `vat.api.Record`: 必须继承`vat.api.Record.Base`;

## 领域实现

在领域定义模块完成后基于生成的工厂代码,进行领域实现

### 生成类型说明

1. `XxxData`: 生成的基于JsonObject的基础领域对象实现
    + 通过`new XxxData(JsonObjectValue)` 来将已知的JsonObject转换为领域数据类型
    + 通过`XxxData.toJson()` 来将领域数据转换为JsonObject
    + 通过`XxxData.toJS()` 来将领域数据转换为JS兼容的JsonObject
    + 通过`new XxxData(JsonObjectValue,null)` 来将JS兼容的JsonObject转换为领域数据类型
    + `XxxObject`具备相同操作模式
    + 生成的实现类均包含各个属性的链式设置方法, `T xxx(P value)`, 不是setter模式的方法命名
2. `XxxStore`: 生成的实体仓库类型,应当通过`Domain.Context`定义的`@Storage`方法来访问,而非直接使用;
    + 常用一级访问方法: S 代表当前仓库操作实体,T代表定义的Entity
        + `xxx()`: 字段访问器, 获取对应字段的存储访问器,用于构造存储表达式
        + 仓库操作方法:
            + 通用命名规则: `put` 存储新的实体, `set` 更新实体属性, `one` 加载一个实体 `many|any` 加载若干实体
            + `Future<Void> remove(long actor,long id)`: 按ID移除一个实体(逻辑删除)
            + `Future<Void> remove(long actor,long id,int version)`: 按ID和版本号移除一个实体(逻辑删除)
            + `Future<Integer> removeAny(long actor, Function<S, Value.BooleanValue> cond)`: 按条件表达式移除若干实体(
              逻辑删除)
                + cond: 通过`XxxStore` 构造匹配表达式, 例如 `t->t.id().eq(1)`
                + 返回值: 影响的实体数量
            + `Future<Void> justPut(long actor, Function<S, Collection<StmtAssign>> set)`: 储存一个新的实体
                + set: 通过store实例构造column设置列表, 例如
                  `t->List.of(t.name().value(someNameValue),t.gender().value(1))`
                + 不允许操作基础字段: id,removed,version,creator,createdAt,modifier,modifiedAt,removed,history(如果有)
            + `Future<Void> justPut(long actor, JsonObject set)`: 储存一个JsonObject表达的实体数据(key为属性名)
            + `Future<Long> putGetIdentity(long actor, Function<S, Collection<StmtAssign>> set)`: 储存并获取id,使用方法同
              `Future<Void> justPut(long actor, Function<S, Collection<StmtAssign>> set)`
            + `Future<Long> putGetIdentity(long actor, JsonObject set)`: 储存并获取id,使用方法同
              `Future<Void> justPut(long actor, JsonObject set)`
            + `Future<T> put(long actor, JsonObject set)`: 储存并返回实体,使用方法同
              `Future<Void> justPut(long actor, JsonObject set)`
            + `Future<T> put(long actor, Function<S, Collection<StmtAssign>> set)`: 储存并返回实体,使用方法同
              `Future<Void> justPut(long actor, Function<S, Collection<StmtAssign>> set)`
3. `XxxDomain<T>`: 领域实现上下文抽象类, 实现应当继承该类
    + 构造模式: 合计构造模式有5种:
        + `public Xxx(Vertx vertx,@Nullable String address)`: 没有定义`@Enhance(endpoint=true)`, 同时没有任何配置依赖和
          `@Storage`仓库依赖
        + `public Xxx(Vertx vertx,@Nullable String address，JsonObject conf)`： 只定义了配置依赖，例如 `@Config` 和/或
          `@Errors`的上下文
        + `public Xxx(Vertx vertx,@Nullable String address，vat.api.implement.Web.Factory web, JsonObject conf)`： 定义了
          `@Enhance(endpoint=true)`的上下文,未定义`@Storage`仓库依赖
        + `public Xxx(Vertx vertx,@Nullable String address， Pool sql, Dialect dialect, JsonObject conf)`： 定义了
          `@Storage`仓库依赖的上下文,未定义`@Enhance(endpoint=true)`
        +
      `public Xxx(Vertx vertx,@Nullable String address，vat.api.implement.Web.Factory web, Pool sql, Dialect dialect, JsonObject conf)`：
      定义了`@Storage`仓库依赖以及`@Enhance(endpoint=true)`依赖的上下文
    + 实现类应当包含以下两个注解
        + `@AutoService(Activities.class)`: 启用SPI自动生成 (或手动编码SPI协议文件)
        + `@Activity(mode=Activity.Mode.COMPOENT,autp=true)`: 部署模式配置
    + 实现类必须具有公开无参数构造函数: 用于满足SPI协议, 其中父类的构造函数也应当调用对应无参数构造函数

## 常用工具说明



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
+ RemoveContract:Data.Request类型, 移除协议记录请求数据
    + actor:`Optional<Long>` 用户ID
    + user: `long` 领域外部管理的用户(校验使用)
    + card: `String` 领域外部管理的卡号(校验使用)
    + files:`List<Long>` 移除的协议文件清单ID
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
    + 添加新的协议, 需要添加调用审计
+ `Future<List<ContractInfo>> contracts(Contractor data)`
    + 查询存在的协议

注意: 实现采用Future链式风格,即整个过程由若干Future.map 或Future.flatMap 方法块构成
