package com.niki914.zafiro.api.telemetry

/**
 * 与 `telemetry-contract/contract.yaml` 对应的手写常量。
 *
 * 数值用十进制字面量,字符串写在常量上,清单引用这些常量。
 * 服务端核对脚本直接读本文件。
 */
object TelemetryContract {
    const val SCHEMA_VERSION: Int = 1
    const val CONTRACT_VERSION: Int = 1

    /** 整个渠道名匹配这一段。 */
    const val CHANNEL_NAME_PATTERN: String = "[a-z0-9_]{1,32}"

    object ChannelName {
        const val GITHUB: String = "github"
        const val XPOSED: String = "xposed"
        const val DEV: String = "dev"
        const val LEGACY: String = "legacy"
        const val UNKNOWN: String = "unknown"
    }

    object ChannelKind {
        const val OFFICIAL: String = "official"
        const val REPO: String = "repo"
        const val DEV: String = "dev"
        const val LEGACY: String = "legacy"
        const val UNKNOWN: String = "unknown"
    }

    data class Channel(
        val name: String,
        val kind: String,
        val firstChannelOnly: Boolean,
    )

    val channels: List<Channel> = listOf(
        Channel(ChannelName.GITHUB, ChannelKind.OFFICIAL, firstChannelOnly = false),
        Channel(ChannelName.XPOSED, ChannelKind.REPO, firstChannelOnly = false),
        Channel(ChannelName.DEV, ChannelKind.DEV, firstChannelOnly = false),
        Channel(ChannelName.LEGACY, ChannelKind.LEGACY, firstChannelOnly = true),
        Channel(ChannelName.UNKNOWN, ChannelKind.UNKNOWN, firstChannelOnly = false),
    )

    /** 本期没有事件。 */
    val events: List<EventDef> = emptyList()

    /** 本期没有扩展键。项上的 scope 决定它属于哪一组。 */
    val customKeys: List<CustomKeyDef> = emptyList()

    object ConfigName {
        const val TELEMETRY_ENABLED: String = "telemetry_enabled"
        const val REPORT_INTERVAL_HOURS: String = "report_interval_hours"
        const val CONFIG_TTL_HOURS: String = "config_ttl_hours"
    }

    object ConfigType {
        const val BOOL: String = "bool"
        const val INT: String = "int"
    }

    sealed interface ConfigDefault {
        data class OfBool(val value: Boolean) : ConfigDefault
        data class OfInt(val value: Int) : ConfigDefault
    }

    data class ConfigKey(
        val name: String,
        val type: String,
        val default: ConfigDefault,
        val min: Int? = null,
        val max: Int? = null,
    )

    val configKeys: List<ConfigKey> = listOf(
        ConfigKey(
            name = ConfigName.TELEMETRY_ENABLED,
            type = ConfigType.BOOL,
            default = ConfigDefault.OfBool(value = true),
        ),
        ConfigKey(
            name = ConfigName.REPORT_INTERVAL_HOURS,
            type = ConfigType.INT,
            default = ConfigDefault.OfInt(value = 24),
            min = 24,
            max = 168,
        ),
        ConfigKey(
            name = ConfigName.CONFIG_TTL_HOURS,
            type = ConfigType.INT,
            default = ConfigDefault.OfInt(value = 12),
            min = 12,
            max = 168,
        ),
    )

    object Limits {
        const val BODY_BYTES: Int = 32768
        const val DAYS_MIN: Int = 1
        const val DAYS_MAX: Int = 8
        const val COUNTS_PER_PAGE: Int = 64
        const val FIRSTS_PER_PAGE: Int = 64
        const val UNPLANNED_EVENTS_PER_PAGE: Int = 8
        const val EVENT_NAME_MAX_CHARS: Int = 40
        const val LABEL_MAX_CHARS: Int = 40
        const val COUNT_N_MAX: Int = 1000000
        const val CUSTOM_KEYS_MAX: Int = 16
        const val UNDECLARED_CUSTOM_KEYS_MAX: Int = 8
        const val CUSTOM_STRING_MAX_CHARS: Int = 40
        const val BRAND_MAX_CHARS: Int = 64
        const val MODEL_MAX_CHARS: Int = 64
        const val INSTALLER_MAX_CHARS: Int = 100
        const val TZ_OFFSET_MIN: Int = -720
        const val TZ_OFFSET_MAX: Int = 840
        const val COUNT_N_MIN: Int = 1
        const val MEASURE_MIN: Int = 0
        const val MEASURE_MAX: Int = 2147483647
        const val LANG_MAX_CHARS: Int = 16
        const val DAY_KEY_LENGTH: Int = 36
        const val ACTIVITY_WINDOW_PAST_DAYS: Int = 8
    }

    object FieldType {
        const val INT: String = "int"
        const val STRING: String = "string"
    }

    data class ProfileField(
        val name: String,
        val type: String,
        val nullable: Boolean,
    )

    val profileFields: List<ProfileField> = listOf(
        ProfileField("api_level", FieldType.INT, nullable = true),
        ProfileField("lang", FieldType.STRING, nullable = true),
        ProfileField("brand", FieldType.STRING, nullable = false),
        ProfileField("model", FieldType.STRING, nullable = false),
        ProfileField("abi", FieldType.STRING, nullable = false),
        ProfileField("ram_gb", FieldType.INT, nullable = false),
        ProfileField("screen_class", FieldType.STRING, nullable = false),
        ProfileField("locale_region", FieldType.STRING, nullable = true),
        ProfileField("tz_id", FieldType.STRING, nullable = false),
        ProfileField("tz_offset_min", FieldType.INT, nullable = false),
    )

    val ramGbBuckets: List<Int> = listOf(2, 3, 4, 6, 8, 12, 16, 24)

    val screenWidthDp: List<Int> = listOf(320, 360, 400, 480, 600, 720)

    data class DensityBucket(
        val name: String,
        val dpi: Int,
    )

    val screenDensity: List<DensityBucket> = listOf(
        DensityBucket(name = "mdpi", dpi = 160),
        DensityBucket(name = "hdpi", dpi = 240),
        DensityBucket(name = "xhdpi", dpi = 320),
        DensityBucket(name = "xxhdpi", dpi = 480),
        DensityBucket(name = "xxxhdpi", dpi = 640),
    )

    object BuildType {
        const val RELEASE: String = "release"
        const val DEBUG: String = "debug"
    }

    val buildTypes: List<String> = listOf(BuildType.RELEASE, BuildType.DEBUG)
}
