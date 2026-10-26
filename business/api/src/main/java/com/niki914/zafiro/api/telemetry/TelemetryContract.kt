package com.niki914.zafiro.api.telemetry

/**
 * 与 `telemetry-contract/contract.yaml` 对应的手写常量。
 *
 * 服务端核对只读本文件里的常量声明。名字和值与 `scripts/check-contract.ts` 的 MAPPING 一致。
 * 以 LIMIT_、CHANNEL_、CONFIG_KEY_ 开头的名字必须在那张名单里。
 */
object TelemetryContract {
    const val SCHEMA_VERSION = 1
    const val CONTRACT_VERSION = 1

    const val LIMIT_BODY_BYTES = 32768
    const val LIMIT_DAYS_MIN = 1
    const val LIMIT_DAYS_MAX = 8
    const val LIMIT_COUNTS_PER_DAY = 64
    const val LIMIT_FIRSTS_PER_DAY = 64
    const val LIMIT_UNPLANNED_EVENTS_PER_DAY = 8
    const val LIMIT_NAME_LENGTH = 40
    const val LIMIT_COUNT_N_MIN = 1
    const val LIMIT_COUNT_N_MAX = 1000000
    const val LIMIT_CUSTOM_KEYS = 16
    const val LIMIT_CUSTOM_UNDECLARED_KEYS = 8
    const val LIMIT_CUSTOM_STRING_LENGTH = 40
    const val LIMIT_BRAND_LENGTH = 64
    const val LIMIT_MODEL_LENGTH = 64
    const val LIMIT_INSTALLER_LENGTH = 100
    const val LIMIT_TZ_OFFSET_MIN = -720
    const val LIMIT_TZ_OFFSET_MAX = 840
    const val LIMIT_METRIC_MIN = 0
    const val LIMIT_METRIC_MAX = 2147483647
    const val LIMIT_CHANNEL_NAME_MIN = 1
    const val LIMIT_CHANNEL_NAME_MAX = 32
    const val LIMIT_CHANNEL_NAME_PATTERN = "[a-z0-9_]{1,32}"
    const val LIMIT_LANG_LENGTH = 16
    const val LIMIT_APP_LANG_LENGTH = 16
    const val LIMIT_LOCALE_REGION_LENGTH = 2
    const val LIMIT_DAY_KEY_LENGTH = 36
    const val LIMIT_ACTIVITY_WINDOW_DAYS = 8
    const val LIMIT_CLIENT_RETENTION_PAST_DAYS = 7
    const val LIMIT_CLOCK_ROLLBACK_DAYS = 8
    const val LIMIT_DELETED_DAY_MEMORY_DAYS = 9
    const val LIMIT_REV_MIN = 1
    const val LIMIT_DROPPED_DAYS_MIN = 0
    const val LIMIT_COUNTS_TRUNCATED_MIN = 0
    const val LIMIT_VERSION_CODE_MIN = 1

    const val CHANNEL_GITHUB_NAME = "github"
    const val CHANNEL_GITHUB_KIND = "official"
    const val CHANNEL_XPOSED_NAME = "xposed"
    const val CHANNEL_XPOSED_KIND = "repo"
    const val CHANNEL_UNKNOWN_NAME = "unknown"
    const val CHANNEL_UNKNOWN_KIND = "unknown"
    const val CHANNEL_DEV_NAME = "dev"
    const val CHANNEL_DEV_KIND = "dev"
    const val CHANNEL_LEGACY_NAME = "legacy"
    const val CHANNEL_LEGACY_KIND = "legacy"

    const val CONFIG_KEY_TELEMETRY_ENABLED_TYPE = "bool"
    const val CONFIG_KEY_TELEMETRY_ENABLED_DEFAULT = true
    const val CONFIG_KEY_REPORT_INTERVAL_HOURS_TYPE = "int"
    const val CONFIG_KEY_REPORT_INTERVAL_HOURS_DEFAULT = 24
    const val CONFIG_KEY_REPORT_INTERVAL_HOURS_MIN = 24
    const val CONFIG_KEY_REPORT_INTERVAL_HOURS_MAX = 168
    const val CONFIG_KEY_CONFIG_TTL_HOURS_TYPE = "int"
    const val CONFIG_KEY_CONFIG_TTL_HOURS_DEFAULT = 12
    const val CONFIG_KEY_CONFIG_TTL_HOURS_MIN = 12
    const val CONFIG_KEY_CONFIG_TTL_HOURS_MAX = 168

    const val RAM_GB_BUCKETS = "2,3,4,6,8,12,16,24"
    const val SCREEN_WIDTH_BUCKETS = "320,360,400,480,600,720"
    const val SCREEN_DENSITY_BUCKETS = "mdpi:160,hdpi:240,xhdpi:320,xxhdpi:480,xxxhdpi:640"
    const val BUILD_TYPES = "release,debug"
    const val PROFILE_FIELDS = "api_level,brand,model,abi,ram_gb,screen_class,lang,app_lang,locale_region,tz_id,tz_offset_min,installer"
    const val EVENT_COUNT = 0
    const val CUSTOM_KEY_COUNT = 0

    data class Channel(
        val name: String,
        val kind: String,
        val firstChannelOnly: Boolean,
    )

    val channels: List<Channel> = listOf(
        Channel(CHANNEL_GITHUB_NAME, CHANNEL_GITHUB_KIND, firstChannelOnly = false),
        Channel(CHANNEL_XPOSED_NAME, CHANNEL_XPOSED_KIND, firstChannelOnly = false),
        Channel(CHANNEL_DEV_NAME, CHANNEL_DEV_KIND, firstChannelOnly = false),
        Channel(CHANNEL_LEGACY_NAME, CHANNEL_LEGACY_KIND, firstChannelOnly = true),
        Channel(CHANNEL_UNKNOWN_NAME, CHANNEL_UNKNOWN_KIND, firstChannelOnly = false),
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
            name = "telemetry_enabled",
            type = CONFIG_KEY_TELEMETRY_ENABLED_TYPE,
            default = ConfigDefault.OfBool(CONFIG_KEY_TELEMETRY_ENABLED_DEFAULT),
        ),
        ConfigKey(
            name = "report_interval_hours",
            type = CONFIG_KEY_REPORT_INTERVAL_HOURS_TYPE,
            default = ConfigDefault.OfInt(CONFIG_KEY_REPORT_INTERVAL_HOURS_DEFAULT),
            min = CONFIG_KEY_REPORT_INTERVAL_HOURS_MIN,
            max = CONFIG_KEY_REPORT_INTERVAL_HOURS_MAX,
        ),
        ConfigKey(
            name = "config_ttl_hours",
            type = CONFIG_KEY_CONFIG_TTL_HOURS_TYPE,
            default = ConfigDefault.OfInt(CONFIG_KEY_CONFIG_TTL_HOURS_DEFAULT),
            min = CONFIG_KEY_CONFIG_TTL_HOURS_MIN,
            max = CONFIG_KEY_CONFIG_TTL_HOURS_MAX,
        ),
    )

    data class ProfileField(
        val name: String,
        val type: String,
        val nullable: Boolean,
    )

    val profileFields: List<ProfileField> = listOf(
        ProfileField("api_level", "int", nullable = true),
        ProfileField("brand", "string", nullable = false),
        ProfileField("model", "string", nullable = false),
        ProfileField("abi", "string", nullable = false),
        ProfileField("ram_gb", "int", nullable = false),
        ProfileField("screen_class", "string", nullable = false),
        ProfileField("lang", "string", nullable = true),
        ProfileField("app_lang", "string", nullable = true),
        ProfileField("locale_region", "string", nullable = true),
        ProfileField("tz_id", "string", nullable = false),
        ProfileField("tz_offset_min", "int", nullable = false),
        ProfileField("installer", "string", nullable = true),
    )

    val ramGbBuckets: List<Int> = RAM_GB_BUCKETS.split(",").map { it.toInt() }

    val screenWidthDp: List<Int> = SCREEN_WIDTH_BUCKETS.split(",").map { it.toInt() }

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
