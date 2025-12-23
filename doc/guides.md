# Development Guide

## Overview

**VAT (Vert.x Application Toolkit)** is an asynchronous, reactive, and progressive application toolkit based on Vert.x,
featuring:

1. **Reactive Non-blocking Model**: Fully asynchronous non-blocking operations via the `Future`/`Promise` pattern.
2. **Domain-Driven Design (DDD)**: Model-based constraint design rooted in core DDD concepts.
3. **Metadata Support**: Compile-time code and metadata generation based on annotation processing.
4. **Progressive Application**: Based on the Vert.x event-driven model, allowing for on-demand monolithic or
   microservice cluster deployment to optimize runtime costs based on business load.
5. **Minimum JDK 21 Support**.

## Core Concepts

1. `vat.api.Data`: The base domain data object, including domain events (`vat.api.Event`) and persistent entities (
   `vat.api.Entity`) subtypes.
2. `vat.api.Actor`: An abstract user model representing a system user; typically, the implementation
   `vat.foundation.users.api.Users.User` should be used directly.
3. `vat.api.Ability`: An abstract user role model, typically used to manage permissions for a system user within a
   specific domain.
4. `vat.api.Record`: A domain record model, typically used to express domain entity data that needs to be persisted.
5. `vat.api.Event`: A domain event model, typically used for cross-domain event communication.
6. `vat.api.Activities`: A domain activity model; the basic behavioral model of a domain (equivalent to "Domain
   Services" in other implementations). Interaction between different domains should primarily occur through domain
   activities.
7. `vat.api.Domain.Context`: A domain context model, expressing the context information required for a domain activity
   to run, such as dependencies on other domains, configuration information, persistence libraries, etc.

## Project Structure

A VAT-based project can be a multi-module application service project or a single-domain library module project.

* **Multi-module Maven project composed of multiple domains and business application entry points.** Structure example:

```text
Project Name/
├── pom.xml                     # Root POM, should inherit io.github.zenliucn.vertx:vat to apply vat dependency management and auto-processing
├── shops/                      # Shop Domain Module
│   ├── shops-api/              # Shop domain definition
│   └── shops-domain/           # Shop domain implementation
├── products/                   # Product Domain Module
│   ├── products-api/           # Product domain definition
│   └── products-domain/        # Product domain implementation
├── orders/                     # Order Domain Module
│   ├── orders-api/             # Order domain definition
│   └── orders-domain/          # Order domain implementation
├── customers/                  # Customer Account Domain Module: Customer features via basic domain composition
│   ├── customers-api/          # Customer domain definition
│   └── customers-domain/       # Customer domain implementation
├── merchants/                  # Merchant Domain: Merchant features via basic domain composition
│   ├── merchants-api/          # Merchant domain definition
│   └── merchants-domain/       # Merchant domain implementation
├── merchant-node/              # Merchant Entry Node: Deployable web service node
├── customer-node/              # Customer Entry Node: Deployable web service node
└── compose-node/               # Combined Entry Node: Node containing both merchant and customer entries for non-distributed deployment

```

* **Single Domain Module organization.** Structure example:

```text
Domain Name/
├── pom.xml                     # Root POM, should inherit io.github.zenliucn.vertx:vat to apply vat dependency management and auto-processing
├── Domain-api/                 # Domain definition
└── Domain-domain/              # Domain implementation

```

### Domain Module Structure Example

```text
customers/
├── pom.xml
├── customers-api/                                  # Domain Definition Module
│   ├── .enhance                                    # Code generation control file (Required)
│   └── src/main/java/some/package/api              # Domain definition package (one domain per package)
│       └── Customers.java                          # Domain definition file
│           └── `Customers extends Activities`      # Top-level definition: Generally Activities
│               ├── `Customer extends Ability.Base` # Customer Ability definition (optional)
│               ├── `Account extends Record.Base`   # Account Record definition
│               └── `Context extends Customers, Domain.Context` # Domain Context definition
├── customers-domain/                               # Domain Implementation
    └── src/main/java/some/package/domain           # Domain implementation package
        └── CustomersImpl.java                      # Domain implementation file
            └── `CustomersImpl extends CustomersDomain<CustomersImpl>` # Domain implementation

```

### Startup Node Structure Example

The startup node minimally requires dependencies `io.github.zenliucn.vertx:core` and
`io.github.zenliucn.vertx:runtime-node`.

```text
customers-node/
├── src/main/resources
│   └── logback.xml # If using logback logger
└── pom.xml
    └── `<properties>
                <module.name>customers.node</module.name>
                <node.shade>true</node.shade>
         </properties>` # Specify module name and packaging mode

```

### `io.github.zenliucn.vertx:vat` Parent POM Key Notes

1. Contains specific dependency management and maven configuration information; no extra definitions needed in
   implementation.
2. Dynamic features are controlled via submodule `<properties/>`.
3. Standard startup information is already included in the configuration; no special configuration needed unless
   required.
4. **Key Configuration Properties**:
    * `module.name:String`: Configures the automatic JPMS name for this module.
    * `node.shade:boolean`: Configures if this module needs to be packed as a runnable fat-jar (default: `false`).
    * `node.lib:boolean`: Configures if this module needs to be packed as a runnable libs deployment package (default:
      `false`).

## Model Definition

### Serializable Types

Serializable types refer to data types convertible to JSON and Buffer structures. Currently includes:

1. Data types inheriting `vat.api.Data` (includes manually implemented and automatically generated types).
2. JVM primitives and wrappers: `int`, `long`, `float`, `double`, `boolean`, etc.
3. **Basic Types**:
    * Strings: `String`
    * Math types: `BigDecimal`
    * Date/Time types: `Instant`, `LocalDateTime`, `LocalDate`, `LocalTime`, `OffsetDateTime`, `OffsetTime`, `Duration`,
      `Period`
    * JSON types: `JsonObject`, `JsonArray`
    * Binary types: `Buffer`, `byte[]`
    * Special types: `Class<?>`, `Enum<?>`, `UUID`.

4. **Container Types** (containing only serializable types): `Map`, `List`, `Optional`.

### Persistent Types

1. JVM primitives and wrappers: `int`, `long`, `float`, `double`, `boolean`, etc.
2. **Basic Types**:
    * Strings: `String`
    * Math types: `BigDecimal`
    * Date/Time types: `Instant`, `LocalDateTime`, `LocalDate`, `LocalTime`, `OffsetDateTime`, `OffsetTime`, `Duration`,
      `Period`
    * JSON types: `JsonObject`, `JsonArray`
    * Binary types: `Buffer`, `byte[]`
    * Special types: `Class<?>`, `Enum<?>`, `UUID`.

3. **Container Types** (only persistent types): `Optional`.

### Domain Activities: Activities

1. Must inherit `vat.api.Activities`.
2. Must be an **interface**.
3. Must use `@vat.api.meta.Enhance` on the type to trigger code generation.
4. **Metadata Definition**:
    * `@Describe`: Metadata description (optional, for method or type).
        * `value:String`: Name.
        * `desc:String?`: Detailed description.

5. **Domain Activity Methods**: All activity methods must satisfy:

* Public, non-`default` methods; method names must be unique.
* Return value must be `io.vertx.core.Future`, and its type parameter must be a Serializable Type.
* **Single parameter** or **no parameters**; parameter must be a Serializable Type.
* **Supported Extension Annotations**:
    * **`@Access`**: Marks Activities methods as standard storage access methods.
        * `entity`: Entity class (required when method return value is not the entity itself).
        * `value`: Copy strategy name (used when input parameter needs to be a Request entity).
        * **Supported Modes**:
            * `identity` mode: Get entity by ID. Method signature: `Future<Optional<E>> identityXXX(long entityId)`.
            * `authorize` mode: Get related Ability defined in this domain by User ID. Method signature:
              `Future<Optional<A>> authorizeXXX(long userId)`.
            * `create` mode: Create new entity data. Method signature:
              `Future<E|Void> createXXX(CreateData createData)`, where
              `CreateData` must implement `vat.api.trait.Accessor.Creator`.
            * `remove` mode: Remove entity by ID and version. Method signature:
              `Future<Void> removeXXX(RemoveData removeData)`,
              where `RemoveData` must implement `vat.api.trait.Accessor.Remover`.
            * `update` mode: Update entity by ID and version. Method signature:
              `Future<Void> updateXXX(ModifiedData modifiedData)`,
              where `ModifiedData` must implement `vat.api.trait.Accessor.Modificator`.
    * **`@Authorize`**: Marks Activities methods as requiring authorization verification.
        * `ability`: Ability class type to verify (provide when it belongs to an external domain or cannot be retrieved
          from
          context).
        * `authorize`: Authorization method full name (provide when not using standard naming; defaults to
          `authorizeAbilityName`).
        * `allowSystem`: Whether to allow system calls (if no actor is provided, skip permission check).
        * `holder`: Static field holder class (defaults to this domain's context interface class).
        * `value`: Authorization method static field name. Field must match signature
          `BiPredicate<? extends Data.Request<?>, @Nullable Ability>`.
        * `badRequest`: Static field for bad request error. Type must be `BiFunction<InputType, Ability?, DomainError>`
          or
          `Provider<DomainError>`.
        * `forbidden`: Static field for forbidden error. Type must be `BiFunction<InputType, Ability?, DomainError>` or
          `Provider<DomainError>`.
    * **`@Auditing`**: Marks current Activities operation for auditing.
        * `mode`: Mode, defaults to `vat.api.meta.Auditing.Mode.INVOKE`.
            * `vat.api.meta.Auditing.Mode.INVOKE`: Call audit (sends request and result to `vat.foundation.audits`
              domain).
            * `vat.api.meta.Auditing.Mode.REQUEST`: Request audit (sends request info to `vat.foundation.audits`).
            * `vat.api.meta.Auditing.Mode.RESPONSE`: Response audit (sends response info to `vat.foundation.audits`).
            * `vat.api.meta.Auditing.Mode.FAILURE`: Failure audit (sends error record with request info to
              `vat.foundation.audits`).
        * `topic`: Topic, defaults to complete method Identifier.
        * *Note*: When auditing is added, actual logic requires implementing a `doXxxx` method; audit functionality is
          automatically included in the generated original method logic.

### Domain Context: Domain.Context

Domain context defines domain runtime dependencies:

1. **Type Definition**:
    * Must be a public interface. First inherit the corresponding Domain Activity definition interface, then inherit
      `Domain.Context`.
    * Must use `@vat.api.meta.Enhance` on the type to trigger code generation (generated helper type named
      `DomainActivityDomain`).
2. **Metadata Definition**:
    * `@Describe`: Metadata description (optional, for method or type).
        * `value:String`: Name.
        * `desc:String?`: Detailed description.
3. **Configuration Info**:
    * Default method annotated with `@vat.api.meta.Config`. Used to read specified value from config; if config value
      implies non-existence, invokes default method.
    * Method must be a default method: `default`.
    * Method must have no parameters.
    * Method must return one of the following:
        * Primitives like `int`, `boolean`.
        * `String`, `JsonArray`, `JsonObject`.
        * `BigDecimal`.
        * `Buffer`.
        * `Instant`.
    * Return value can be wrapped with `Optional`.
    * Implementation should use the generated method directly.

4. **Error Factory**:
    * Default method annotated with `@vat.api.meta.Errors`. Used to read error message config; if config implies
      non-existence, invokes default method.
    * Method must be a default method: `default`.
    * Method can have no parameters or parameters for formatting error messages.
    * Method must return `vat.api.DomainError`.
    * Implementation should use the generated method directly.

5. **Event Subscription**:
    * Default method annotated with `@vat.api.meta.Subscribe`. Used to subscribe to events from this or other domains.
        * `value:String?`: Defines event address or configuration JsonPointer (starts with `/`); defaults to complete
          event
          Identifier.
    * Method must be a default method: `default`.
    * Method must contain exactly one parameter of type `EventType`.
    * Method must return `void`.
    * Domain implementation should implement the logic for this method.
6. **Event Publication**:
    * Default method annotated with `@vat.api.meta.Publish`. Used to publish events from this or other domains.
        * `value:String?`: Defines event address or configuration JsonPointer (starts with `/`); defaults to complete
          event
          Identifier.
    * Method must be a default method: `default`.
    * Method must contain exactly one parameter of type `Consumer<EventType>`.
    * Method must return `void`.
    * Domain implementation should use the generated `XXXXPublish` method for event construction. The generated function
      parameter is `Consumer<EventTypeData>` (replacing EventType with generated Data type).

7. **Storage Factory**:
    * Default method annotated with `@vat.api.meta.Storage`. Used to construct storage repositories.
        * `value:String?`: Defines used database Schema or configuration JsonPointer (starts with `/`); default requires
          no
          Schema.
    * Method must be a default method: `default`.
    * Method can have no parameters or include a nullable `SqlConnection` parameter. If included, the method supports
      transaction operations.
    * *Suggestion*: Always include the `SqlConnection` parameter.
    * Method must return `vat.api.Store<Entity Subtype Defined in Domain>`.
    * Method body should throw error directly (suggest `IllegalStateException`); normal scenarios won't trigger this.
    * Implementation should use the generated method directly.
    * Generated method replaces return value with corresponding generated `xxxStore` implementation type.

8. **Domain Dependencies**:
    * Default method annotated with `@vat.api.meta.Uses`. Used to get proxies for other domain activities.
        * `value:String?`: Defines domain service address or configuration JsonPointer (starts with `/`); defaults to
          default
          address.
    * Method must be a default method: `default`.
    * Method must have no parameters.
    * Method must return a Domain Activity that is **not** the current domain activity.
    * Method body should throw error directly (suggest `IllegalStateException`); normal scenarios won't trigger this.
    * Implementation should use the generated method directly.

### Domain Data: Data (Non-model subtypes usually used for DTO)

**Public Constraints**

1. **Type Marking**:
    * `@Enhance`: Triggers code generation (Non-manual implementations must mark this).
        * `record:boolean?`: Generate record implementation (default: true).
        * `pojo:boolean?`: Generate POJO implementation also.
        * `internal:boolean?`: Internal usage marker.
    * `@Describe`: Metadata description (optional).
        * `value:String`: Name.
        * `desc:String?`: Detailed description.

2. **Property Definition**: Defined via JVM record-style getter methods.
    * Method name does not handle `get` or `is` prefixes.
    * Return value is a Serializable Type and not `void`/`Void`.
    * Method has no parameters.
    * Method is not `default`:
        * *Exception 1*: `@vat.api.meta.Computed` annotated calculation methods must be `default`.
        * *Exception 2*: Immediate calculation methods should be `default`; these values are not saved in the serialized
          object.
    * **Supported Annotation Processors**:
    * `@Describe`: Metadata description (optional).
        * `value:String`: Name.
        * `desc:String?`: Detailed description.

    * `@vat.api.meta.Alias`: Defines key alias in JSON. Reading supports alias; writing only writes alias. Default uses
      getter name (no need for `@Alias` to repeat it).
        * `value:String`: Alias.
        * `strict:boolean?`: Strict mode? If true, only reads the key specified by alias.
    * `@vat.api.meta.Computed`: Defines computed property. Method must be `default`. Return value is the default value.
      When
      outputting to JsonObject, if key doesn't exist, outputs default value.
    * `@vat.api.meta.Validate`: Defines property validator.
        * `construct:boolean?`: Validate on construction? Default false.
        * `holder:Class<?>?`: Validator static field holder class. Default `vat.api.implement.Validators`.
        * `value:String`: Static field name. Field must be `Consumer<T>` or primitive Consumer.
        * Can be defined repeatedly; executes in definition order.
    * `@vat.api.meta.Intercept`: Defines converter.
        * `construct:boolean?`: Convert on construction? Default false (only converts on set).
        * `holder:Class<?>?`: Interceptor static field holder class. Default `vat.api.implement.Interceptors`.
        * `value:String`: Static field name. Field must be `UnaryOperator<T>` or primitive UnaryOperator.
        * Can be defined repeatedly; executes in definition order.
    * `@vat.api.meta.Virtual`: Defines a virtual field actually contained within another JsonObject property.
        * `value:String`: Name of the holding JsonObject property.
        * `key:String?`: Stored key name. Defaults to getter method name.
        * `@vat.api.meta.EnumName`: Specifies using Enum name as value (otherwise uses index). Annotated property must
          be Enum
          type.

3. **Non-manual Implementation Types**:
    * Must use `@vat.api.meta.Enhance` annotation to trigger code generation. **Note: Must be marked.**
    * To generate binary serialization support, inherit `vat.api.Data.Binary` instead of `vat.api.Data`.
    * To generate extra POJO class, use `@vat.api.meta.Enhance(pojo=true)`.
    * To generate non-record class, use `@vat.api.meta.Enhance(record=false)`.
    * Default generated data type automatically adds suffix `Data` and resides in current package.
    * Default generated POJO data type automatically adds suffix `Object` and resides in current package.
    * Codecs used by data are stored as static fields in `pkg.name.Codecs` class in current package.

4. **Manual Implementation Types**: Must implement public special constructors:
    * `public SomeData(JsonObject v)`: For JSON deserialization.
    * `public SomeData(JsonObject v, Void ignore)`: For JS-compatible JSON deserialization.
    * `public SomeData(Buf v)`: For binary data deserialization (requires type to implement `vat.api.Data.Binary`).

#### Non-Persistent Data

1. `vat.api.Event`: Domain Event. Must satisfy public constraints and one of:
    * Contains a `vat.api.meta.EventKind` annotated integer (int, long) or Enum property as event type marker.
    * Or lacks `vat.api.meta.EventKind` annotation but has a property named `kind`.

2. `vat.api.Data`: Domain Data Object. Only needs to satisfy public constraints.

#### Persistent Data: Includes Actor, Ability, Record Models

**Public Constraints**: In addition to Domain Data public constraints:

1. Field types can only be **Persistent Types**.
2. Other Serializable Types can only be defined as virtual fields via `@vat.api.meta.Virtual`.
3. Type must use `@vat.api.meta.Table` to specify table name.
4. **Special Getter Annotations**:
    * `@vat.api.meta.Column`: Specify column constraints (optional).
        * `value:String?`: Specify column name. Default automatically converts to `snake_case`.
        * `indexed:String[]?`: Specify list of **fields participating in index**. Use column names, not property names.
          If one field is in multiple indexes, define separately on different fields.
        * `unique:String[]?`: Specify list of **fields participating in unique index**. Use column names. Cannot be used
          with `indexed` simultaneously.
        * `size:int?`: Specify data size.
        * `max:int?`: Specify data max value (marker only) or max length.
        * `min:int?`: Specify data min value (marker only).
        * `precision:int?`: Specify data precision (for floating point storage).
        * `scale:int?`: Specify data scale (for floating point storage).
        * `enumName:boolean?`: Specify Enum type saved as name. Default false.
        * `interceptHolder:Class<?>?`: Specify interceptor holder type. Default `void.class`.
        * `interceptField:String?`: Interceptor static field name. Field must satisfy `vat.api.store.Interceptor<T>`,
          i.e., `T intercept(boolean read, T t)` functional signature.
        * **Note**: For variable length types like `String`, `byte[]`, `Buffer`, `Period`, `Duration`, `Class`, etc.,
          strictly must use `max` or `size` to specify max length.
        * **Note**: Enum values saved by name must use `max` or `size` to specify max length.
5. Persistent data generates `Store` suffixed repository access utility classes in current package.
6. All `*.Base` foundational models include these base fields (handled automatically by Storage engine, no manual
   handling needed):
    * `id:Long`: Entity auto-increment ID.
    * `removed:boolean`: Soft delete marker. Storage engine automatically excludes on query.
    * `version:int`: Optimistic lock. Storage engine automatically modifies on update.
    * `creator:long`: Creator User ID. Storage engine automatically maintains. Requires actor input on create; if no
      specific actor, uses -1.
    * `createdAt:Instant`: Creation time. Storage engine automatically maintains.
    * `modifier:long`: Last modifier User ID. Storage engine automatically maintains. Requires actor input; if no
      specific actor, uses -1.
    * `modifiedAt:Instant`: Last modified time. Storage engine automatically maintains.
7. Optionally inherit `vat.api.trait.History` to implement change history. Storage engine automatically saves data at
   time of change to `history` field.
    * *Note*: Queries default to **not** loading `history` field. To load, use query methods with `WithHistory` suffix.
8. **Uncommon Extension Annotations**: Not needed when inheriting `*.Base`.
    * `@Audit.Creator`: Marks identity type column as creator audit field. Type must be `@Identity` marked Id type.
    * `@Audit.Created`: Marks timestamp compact type column as creation time audit field. Type must be Instant.
    * `@Audit.Modifier`: Marks identity type column as modifier audit field. Type must be `@Identity` marked Id type.
    * `@Audit.Modified`: Marks timestamp compact type column as modification time audit field. Type must be Instant.
    * `@Identity`: Marks entity ID field.
    * `@OptimisticLock`: Marks optimistic lock field. Type must be `int`.
    * `@SoftRemoved`: Marks soft delete marker. Type must be `boolean`.
    * `@Historic`: Marks field for history storage. Type must be `JsonObject`. `*.Base` does not include this; requires
      inheriting `vat.api.trait.History`.

**Specific Model Constraints**

1. `vat.api.Actor`: Generally do not manually implement this; use `vat.foundation.users.api.Users.User`.
2. `vat.api.Ability`: Must inherit `vat.api.Ability.Base`.
3. `vat.api.Record`: Must inherit `vat.api.Record.Base`.

## Domain Implementation

Perform domain implementation based on generated factory code after Domain Definition Module is complete.

### Generated Types Explanation

1. **`XxxData`**: Generated JsonObject-based basic domain object implementation.
    * Use `new XxxData(JsonObjectValue)` to convert known JsonObject to domain data type.
    * Use `XxxData.toJson()` to convert domain data to JsonObject.
    * Use `XxxData.toJS()` to convert domain data to JS-compatible JsonObject.
    * Use `new XxxData(JsonObjectValue, null)` to convert JS-compatible JsonObject to domain data type.
    * `XxxObject` has same operation modes.
    * Generated implementation classes include chained setting methods for properties: `T xxx(P value)` (not setter pattern naming).
2. **`XxxStore`**: Generated entity repository type. Should be accessed via `@Storage` method defined in
   `Domain.Context`, not used directly.
    * **Common Level-1 Access Methods**: (S represents current repository operation entity, T represents defined
      Entity).
        * `xxx()`: Field accessor. Gets storage accessor for corresponding field, used to construct storage expressions.
    * **Repository Operation Methods**:
    * Naming convention: `put` store new, `set` update property, `one` load one, `many|any` load multiple.
        * `Future<Void> remove(long actor, long id)`: Remove entity by ID (logical delete).
        * `Future<Void> remove(long actor, long id, int version)`: Remove entity by ID and version (logical delete).
        * `Future<Integer> removeAny(long actor, Function<S, Value.BooleanValue> cond)`: Remove entities by condition (
          logical delete).
            * `cond`: Construct matching expression via `XxxStore` instance. E.g., `t -> t.id().eq(1)`.
            * Return: Number of entities affected.
    * `Future<Void> justPut(long actor, Function<S, Collection<StmtAssign>> set)`: Store a new entity.
        * `set`: Construct column setting list via store instance. E.g.,
          `t -> List.of(t.name().value(someNameValue), t.gender().value(1))`.
        * **Not allowed to operate on base fields**: id, removed, version, creator, createdAt, modifier, modifiedAt,
          history (if exists).
    * `Future<Void> justPut(long actor, JsonObject set)`: Store entity data expressed as JsonObject (key is property
      name).
    * `Future<Long> putGetIdentity(long actor, Function<S, Collection<StmtAssign>> set)`: Store and get ID. Usage same
      as `justPut`.
    * `Future<Long> putGetIdentity(long actor, JsonObject set)`: Store and get ID. Usage same as `justPut`.
    * `Future<T> put(long actor, JsonObject set)`: Store and return entity. Usage same as `justPut`.
    * `Future<T> put(long actor, Function<S, Collection<StmtAssign>> set)`: Store and return entity. Usage same as
      `justPut`.
3. **`XxxDomain<T>`**: Domain implementation context abstract class. Implementation should inherit this class.
    * **Construction Patterns**: Total 5 patterns:
        * `public Xxx(Vertx vertx, @Nullable String address)`: No `@Enhance(endpoint=true)` defined, and no
          configuration dependencies or `@Storage` dependencies.
        * `public Xxx(Vertx vertx, @Nullable String address, JsonObject conf)`: Only defined configuration
          dependencies (e.g., `@Config` and/or `@Errors`).
        * `public Xxx(Vertx vertx, @Nullable String address, vat.api.implement.Web.Factory web, JsonObject conf)`:
          Defined `@Enhance(endpoint=true)` context, but no `@Storage` dependency.
        * `public Xxx(Vertx vertx, @Nullable String address, Pool sql, Dialect dialect, JsonObject conf)`: Defined
          `@Storage` dependency, but no `@Enhance(endpoint=true)`.
        *
      `public Xxx(Vertx vertx, @Nullable String address, vat.api.implement.Web.Factory web, Pool sql, Dialect dialect, JsonObject conf)`:
      Defined both `@Storage` dependency and `@Enhance(endpoint=true)`.
    * **Implementation Class Requirements**:
        * `@AutoService(Activities.class)`: Enable SPI auto-generation (or manually code SPI protocol file).
        * `@Activity(mode=Activity.Mode.COMPOENT, autp=true)`: Deployment mode configuration.
    * Implementation class must have **public no-argument constructor**: Used to satisfy SPI protocol. The super
      constructor call inside must also call the corresponding no-argument constructor.
