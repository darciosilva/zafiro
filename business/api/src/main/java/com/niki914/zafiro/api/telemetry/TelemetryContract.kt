package com.niki914.zafiro.api.telemetry

/**
 * 与 `telemetry-contract/contract.yaml` 对应的手写常量。
 *
 * 服务端核对只正则读取本文件里的大写蛇形常量，不读 yaml。
 * 上限以 LIMIT_ 开头，渠道名以 CHANNEL_ 开头，配置键名以 CONFIG_KEY_ 开头。
 * 值写字面量。档位用逗号字符串。同名只写一次。
 */
object TelemetryContract {
    const val SCHEMA_VERSION = 1
    const val CONTRACT_VERSION = 1

    const val EVENT_COUNT = 0
    const val CUSTOM_KEY_COUNT = 0

    const val CHANNEL_NAME_PATTERN = "[a-z0-9_]{1,32}"
    const val CHANNEL_GITHUB = "github"
    const val CHANNEL_XPOSED = "xposed"
    const val CHANNEL_DEV = "dev"
    const val CHANNEL_LEGACY = "legacy"
    const val CHANNEL_UNKNOWN = "unknown"

    const val CONFIG_KEY_TELEMETRY_ENABLED = "telemetry_enabled"
    const val CONFIG_DEFAULT_TELEMETRY_ENABLED = true
    const val CONFIG_KEY_REPORT_INTERVAL_HOURS = "report_interval_hours"
    const val LIMIT_REPORT_INTERVAL_HOURS_DEFAULT = 24
    const val LIMIT_REPORT_INTERVAL_HOURS_MIN = 24
    const val LIMIT_REPORT_INTERVAL_HOURS_MAX = 168
    const val CONFIG_KEY_CONFIG_TTL_HOURS = "config_ttl_hours"
    const val LIMIT_CONFIG_TTL_HOURS_DEFAULT = 12
    const val LIMIT_CONFIG_TTL_HOURS_MIN = 12
    const val LIMIT_CONFIG_TTL_HOURS_MAX = 168

    const val LIMIT_BODY_BYTES = 32768
    const val LIMIT_DAYS_MIN = 1
    const val LIMIT_DAYS_MAX = 8
    const val LIMIT_COUNTS_PER_PAGE = 64
    const val LIMIT_FIRSTS_PER_PAGE = 64
    const val LIMIT_UNPLANNED_EVENTS_PER_PAGE = 8
    const val LIMIT_EVENT_NAME_MAX_CHARS = 40
    const val LIMIT_LABEL_MAX_CHARS = 40
    const val LIMIT_COUNT_N_MIN = 1
    const val LIMIT_COUNT_N_MAX = 1000000
    const val LIMIT_CUSTOM_KEYS_MAX = 16
    const val LIMIT_UNDECLARED_CUSTOM_KEYS_MAX = 8
    const val LIMIT_CUSTOM_STRING_MAX_CHARS = 40
    const val LIMIT_BRAND_MAX_CHARS = 64
    const val LIMIT_MODEL_MAX_CHARS = 64
    const val LIMIT_INSTALLER_MAX_CHARS = 100
    const val LIMIT_TZ_OFFSET_MIN = -720
    const val LIMIT_TZ_OFFSET_MAX = 840
    const val LIMIT_MEASURE_MIN = 0
    const val LIMIT_MEASURE_MAX = 2147483647
    const val LIMIT_LANG_MAX_CHARS = 16
    const val LIMIT_DAY_KEY_LENGTH = 36
    const val LIMIT_ACTIVITY_WINDOW_PAST_DAYS = 8
    const val LIMIT_RETAIN_PAST_DAYS = 7
    const val LIMIT_DELETED_DAY_MEMORY_DAYS = 9
    const val LIMIT_RETRY_BACKOFF_HOURS = "1,2,4"

    const val RAM_GB_BUCKETS = "2,3,4,6,8,12,16,24"
    const val SCREEN_WIDTH_DP_BUCKETS = "320,360,400,480,600,720"
    const val SCREEN_DENSITY_BUCKETS = "mdpi:160,hdpi:240,xhdpi:320,xxhdpi:480,xxxhdpi:640"
    const val BUILD_TYPES = "release,debug"

    data class Channel(
        val name: String,
        val kind: String,
        val firstChannelOnly: Boolean,
    )

    val channels: List<Channel> = listOf(
        Channel(CHANNEL_GITHUB, "official", firstChannelOnly = false),
        Channel(CHANNEL_XPOSED, "repo", firstChannelOnly = false),
        Channel(CHANNEL_DEV, "dev", firstChannelOnly = false),
        Channel(CHANNEL_LEGACY, "legacy", firstChannelOnly = true),
        Channel(CHANNEL_UNKNOWN, "unknown", firstChannelOnly = false),
    )

    val events: List<EventDef> = emptyList()

    val customKeys: List<CustomKeyDef> = emptyList()

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
            name = CONFIG_KEY_TELEMETRY_ENABLED,
            type = "bool",
            default = ConfigDefault.OfBool(CONFIG_DEFAULT_TELEMETRY_ENABLED),
        ),
        ConfigKey(
            name = CONFIG_KEY_REPORT_INTERVAL_HOURS,
            type = "int",
            default = ConfigDefault.OfInt(LIMIT_REPORT_INTERVAL_HOURS_DEFAULT),
            min = LIMIT_REPORT_INTERVAL_HOURS_MIN,
            max = LIMIT_REPORT_INTERVAL_HOURS_MAX,
        ),
        ConfigKey(
            name = CONFIG_KEY_CONFIG_TTL_HOURS,
            type = "int",
            default = ConfigDefault.OfInt(LIMIT_CONFIG_TTL_HOURS_DEFAULT),
            min = LIMIT_CONFIG_TTL_HOURS_MIN,
            max = LIMIT_CONFIG_TTL_HOURS_MAX,
        ),
    )

    data class ProfileField(
        val name: String,
        val type: String,
        val nullable: Boolean,
    )

    val profileFields: List<ProfileField> = listOf(
        ProfileField("api_level", "int", nullable = true),
        ProfileField("lang", "string", nullable = true),
        ProfileField("brand", "string", nullable = false),
        ProfileField("model", "string", nullable = false),
        ProfileField("abi", "string", nullable = false),
        ProfileField("ram_gb", "int", nullable = false),
        ProfileField("screen_class", "string", nullable = false),
        ProfileField("locale_region", "string", nullable = true),
        ProfileField("tz_id", "string", nullable = false),
        ProfileField("tz_offset_min", "int", nullable = false),
    )

    val ramGbBuckets: List<Int> = RAM_GB_BUCKETS.split(",").map { it.toInt() }

    val screenWidthDp: List<Int> = SCREEN_WIDTH_DP_BUCKETS.split(",").map { it.toInt() }

    data class DensityBucket(
        val name: String,
        val dpi: Int,
    )

    val screenDensity: List<DensityBucket> = SCREEN_DENSITY_BUCKETS.split(",").map { bucket ->
        val (name, dpi) = bucket.split(":")
        DensityBucket(name, dpi.toInt())
    }

    val buildTypes: List<String> = BUILD_TYPES.split(",")
}
