package com.niki914.zafiro.api.telemetry

/**
 * 主进程里的记账端口。
 *
 * 业务代码走 [Metrics],不直接调用本接口。`:python` 进程和 Xposed 宿主不安装实现。
 */
interface Telemetry {
    /**
     * 把 [event] 的计数记到当天账页。
     *
     * [label] 为空表示这条计数没有标签,否则必须是 [event] 声明过的标签。
     * [n] 是次数。[custom] 的键是这条计数自己的扩展键。
     */
    fun record(
        event: EventDef,
        label: String? = null,
        n: Int = 1,
        custom: Map<CustomKeyDef, CustomValue> = emptyMap(),
    )

    /** 写入信封上的扩展字段。[value] 为空时清除该键。 */
    fun setEnvelopeCustom(key: CustomKeyDef, value: CustomValue?)

    /** 写入当天账页上的扩展字段。[value] 为空时清除该键。 */
    fun setDayCustom(key: CustomKeyDef, value: CustomValue?)
}

/**
 * 契约里的一条事件。模块外拿不到构造器,只能使用 [TelemetryContract.events] 给出的实例。
 *
 * [labels] 按声明顺序保存。空列表表示该事件没有标签。
 * [status] 取 `active` 或 `deprecated`。[replacedBy] 指向接替它的事件名。
 */
class EventDef internal constructor(
    val name: String,
    val description: String,
    labels: List<String>,
    val status: String,
    val addedIn: Int,
    val replacedBy: String? = null,
) {
    val labels: List<String> = labels.toList()
}

/**
 * 契约里的一个扩展键。模块外拿不到构造器,只能使用 [TelemetryContract.customKeys] 给出的实例。
 *
 * 比较使用实例身份:调用方传契约里的那个对象。
 * [type] 为 [Type.ENUM] 时,取值用 [CustomValue.Str],允许值在 [enumValues]。
 */
class CustomKeyDef internal constructor(
    val name: String,
    val scope: Scope,
    val type: Type,
    val role: Role?,
    val agg: Agg?,
    enumValues: List<String>,
    val maxChars: Int?,
    val min: Int?,
    val max: Int?,
    val addedIn: Int,
) {
    val enumValues: List<String> = enumValues.toList()

    /** 扩展口。信封、当天账页、单条计数。 */
    enum class Scope {
        ENVELOPE,
        DAY,
        COUNT,
    }

    enum class Type {
        STRING,
        INT,
        BOOL,
        ENUM,
    }

    /** 只用于 [Scope.COUNT]。维度拆开计数,度量按 [Agg] 合并。 */
    enum class Role {
        DIMENSION,
        MEASURE,
    }

    enum class Agg {
        SUM,
        MAX,
    }
}

/** 扩展字段的值。枚举值用 [Str]。 */
sealed interface CustomValue {
    data class Str(val value: String) : CustomValue
    data class Num(val value: Int) : CustomValue
    data class Bool(val value: Boolean) : CustomValue
}
