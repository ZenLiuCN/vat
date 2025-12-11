type MetaData = {
    identity: string
    name: string
    description: string
}
type Type<T> = MetaData & T
type Integer = number
type Long = number | string | bigint

//region primitive numeric type
type NumberType = Type<{
    bits: Integer
    floatingPoint: boolean
}>
declare const BYTE: NumberType & {
    bits: 8,
    floatingPoint: false,
    name: "byte",
    identity: "byte",
    description: "",
}
declare const SHORT: NumberType & {
    bits: 16,
    floatingPoint: false,
    name: "short",
    identity: "short",
    description: "",
}
declare const INT: NumberType & {
    bits: 32,
    floatingPoint: false,
    name: "int",
    identity: "int",
    description: "",
}
declare const CHAR: NumberType & {
    bits: 32,
    floatingPoint: false,
    name: "char",
    identity: "char",
    description: "",
}
declare const LONG: NumberType & {
    bits: 64,
    floatingPoint: false,
    name: "long",
    identity: "long",
    description: "",
}
declare const FLOAT: NumberType & {
    bits: 32,
    floatingPoint: true,
    name: "float",
    identity: "float",
    description: "",
}
declare const DOUBLE: NumberType & {
    bits: 64,
    floatingPoint: true,
    name: "double",
    identity: "double",
    description: "",
}
//endregion
//region normal built in type
type NormalType = Type<{}>
declare const VOID: NormalType & {
    name: 'Void',
    identity: 'Void',
    description: 'nothing',
}
declare const ERROR: NormalType & {
    name: 'Error',
    identity: 'error',
    description: 'domain error',
}
declare const BOOLEAN: NormalType & {
    name: 'boolean',
    identity: 'boolean',
    description: 'boolean value',
}
declare const STRING: NormalType & {
    name: 'String',
    identity: 'String',
    description: 'String value',
}
declare const BINARY: NormalType & {
    name: 'Binary',
    identity: 'Binary',
    description: 'small binary data',
}
declare const BUFFER: NormalType & {
    name: 'Buffer',
    identity: 'Buffer',
    description: 'big binary data',
}
declare const JSON_OBJECT: NormalType & {
    name: 'JsonObject',
    identity: 'JsonObject',
    description: 'json style object',
}
declare const JSON_ARRAY: NormalType & {
    name: 'JsonArray',
    identity: 'JsonArray',
    description: 'json style array',
}
declare const UUID: NormalType & {
    name: 'UUID',
    identity: 'UUID',
    description: 'Universally unique identifier',
}
declare const TIME: NormalType & {
    name: 'Time',
    identity: 'Time',
    description: 'local time',
}
declare const DATE: NormalType & {
    name: 'Date',
    identity: 'Date',
    description: 'local date',
}
declare const DATETIME: NormalType & {
    name: 'DateTime',
    identity: 'DateTime',
    description: 'local date with time',
}
declare const TIME_TZ: NormalType & { name: 'TimeTZ', identity: 'TimeTZ', description: 'time with time-zone' }
declare const DATETIME_TZ: NormalType & {
    name: 'DateTimeTZ',
    identity: 'DateTimeTZ',
    description: 'datetime with time-zone'
}
declare const INSTANT: NormalType & { name: 'Instant', identity: 'Instant', description: 'Timestamp' }
declare const DECIMAL: NormalType & { name: 'Decimal', identity: 'Decimal', description: 'Tig decimal value' }
declare const NUMERIC: NormalType & {
    name: 'Numeric',
    identity: 'Numeric',
    description: 'Numeric value almost same as big decimal'
}
declare const DURATION: NormalType & {
    name: 'Duration',
    identity: 'Duration',
    description: 'Time interval of maximum of days'
}
declare const PERIOD: NormalType & {
    name: 'Period',
    identity: 'Period',
    description: 'Time interval of minimal of days'
}
declare const INTEGER_DATE: NormalType & {
    name: 'IDate',
    identity: 'IDate',
    description: 'Date present as literal date integer.Eg: 20250101'
}
declare const INTEGER_TIME: NormalType & {
    name: 'ITime',
    identity: 'ITime',
    description: 'Time present as literal time integer.Eg: 121201'
}
declare const LONG_DATETIME: NormalType & {
    name: 'IDateTime',
    identity: 'IDateTime',
    description: 'datetime present as literal datetime integer.Eg: 20240101120101'
}
//endregion
/**
 * Optional valued type
 */
type OptionalType<T extends Type<any>> = {
    optional: true,
    type: T
}
/**
 * Generic type
 */
type GenericType<T> = Type<T>
/**
 * repeated value type
 */
type RepeatType<E extends Type<any>> = GenericType<{ element: E, nullable: boolean }>
/**
 * repeated list or set type with dynamic length.
 */
type ListType<E extends Type<any>> = RepeatType<E> & { unique: boolean }
/**
 * Fixed length array type
 */
type ArrayType<E extends Type<any>> = RepeatType<E>
/**
 * Key value projection type. Eg: Map
 */
type ProjectionType<KEY extends Type<any>, VALUE extends Type<any>> = GenericType<{
    key: KEY,
    value: VALUE
}>
/**
 * Referenced value type which provided by provider
 */
type ReferenceType = Type<{ provider: string }>
/**
 * Enumeration entry data
 */
type EnumerationEntry = MetaData & { ordinal: Long, text: string }
/**
 * Enumeration type
 */
type EnumerationType = Type<{ candidates: EnumerationEntry[] }>
/**
 * A function to process or validate with data objects.
 */
type Functor = MetaData & { construct: boolean }
/**
 * A data property
 */
type Property<P extends Type<any>> = MetaData & {
    interceptors: Functor[]
    validators: Functor[]
    mappings: string
    product: P
    optional: boolean
}
/**
 * An entity property
 */
type Column<P extends Type<any>> = MetaData & {
    column: string
    optional: boolean
    unique: string[]
    index: string[]
    size: Integer
    max: Integer
    min: Integer
    precision: Integer
    scale: Integer
    enumName: boolean
    interceptor: Functor
    product: P
}

type Properties = { properties: Property<Type<any>>[], binary: boolean }

type Entity = MetaData & {
    columns: Column<Type<any>>[]
    /**
     * storage table name
     */
    table: string
    /**
     * identity function provider
     */
    identify: string
    /**
     * support binary encoding
     */
    binary: boolean
}
/**
 * Actor represent a User
 */
type ActorType = Entity & { role: 'actor' }
/**
 * Ability that referenced to an Actor
 */
type AbilityType = Entity & { role: 'ability' }
/**
 * Record normally a data record
 */
type RecordType = Entity & { role: 'record' }
/**
 * Data object without persistent storage.
 */
type ObjectType = Properties & MetaData & { role: 'data' }
/**
 * Event specif kind
 */
type EventKind = MetaData & { ordinal: Long, text: string }

type EventType = Properties & MetaData & { role: 'event', kinds: EventKind[] }

type ActionType<I extends Type<any>, O extends Type<any>> = MetaData & { input: I, output: O }

type ConfigEntry = MetaData & { path: string }
/**
 * config for error information
 */
type ErrorEntry = ConfigEntry & {
    codePath: string
    userPath: string
    modePath: string
    systemPath: string
    parameters: Type<any>[]
}
/**
 * config value
 */
type ValueEntry = ConfigEntry & { type: Type<any> }

type Config = MetaData & {
    properties: (ValueEntry | ErrorEntry)[]
}
type Uses = MetaData & {
    address?: string
    configPath?: string
    type: ReferenceType
}
type Publish = MetaData & {
    address?: string
    configPath?: string
    type: ReferenceType
}
type Subscribe = MetaData & {
    address?: string
    configPath?: string
    type: ReferenceType
}
type Domain = MetaData & {
    actors?: ActorType[]
    abilities?: AbilityType[]
    records?: RecordType[]
    events?: EventType[]
    data?: ObjectType[]
    actions?: ActionType<any, any>[]
    publish?: Publish[]
    subscribe?: Subscribe[]
    uses?: Uses[]
    config?: Config
}
