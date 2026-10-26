// 保护：TelemetryContract.kt 的常量若改名、改值，或新增 LIMIT_/CHANNEL_/CONFIG_KEY_ 但不在服务端 MAPPING 里，核对脚本会报错，两端上限和渠道对不上。
package com.niki914.zafiro.api.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class TelemetryContractTest {

    @Test
    fun `const vals match the server mapping`() {
        val source = readRepoFile(
            File("business/api/src/main/java/com/niki914/zafiro/api/telemetry/TelemetryContract.kt"),
        )
        val parsed = parseKotlin(source)
        val problems = compareKotlin(parsed).toMutableList()
        problems += yamlDrift(parseContractYaml(readRepoFile(File("telemetry-contract", "contract.yaml"))))
        parsed.consts.forEach { (name, value) ->
            if (value is RawLiteral) return@forEach
            val compiled = TelemetryContract::class.java.getField(name).get(null)
            if (compiled != value) problems += "$name: 源码 $value, 编译结果 $compiled"
        }
        assertEquals("", problems.joinToString("\n"))
    }
}

private val watchedPrefixes = listOf("LIMIT_", "CHANNEL_", "CONFIG_KEY_")

private val mapping: List<Pair<String, Any>> = listOf(
    "SCHEMA_VERSION" to 1,
    "CONTRACT_VERSION" to 1,
    "LIMIT_BODY_BYTES" to 32768,
    "LIMIT_DAYS_MIN" to 1,
    "LIMIT_DAYS_MAX" to 8,
    "LIMIT_COUNTS_PER_DAY" to 64,
    "LIMIT_FIRSTS_PER_DAY" to 64,
    "LIMIT_UNPLANNED_EVENTS_PER_DAY" to 8,
    "LIMIT_NAME_LENGTH" to 40,
    "LIMIT_COUNT_N_MIN" to 1,
    "LIMIT_COUNT_N_MAX" to 1000000,
    "LIMIT_CUSTOM_KEYS" to 16,
    "LIMIT_CUSTOM_UNDECLARED_KEYS" to 8,
    "LIMIT_CUSTOM_STRING_LENGTH" to 40,
    "LIMIT_BRAND_LENGTH" to 64,
    "LIMIT_MODEL_LENGTH" to 64,
    "LIMIT_INSTALLER_LENGTH" to 100,
    "LIMIT_TZ_OFFSET_MIN" to -720,
    "LIMIT_TZ_OFFSET_MAX" to 840,
    "LIMIT_METRIC_MIN" to 0,
    "LIMIT_METRIC_MAX" to 2147483647,
    "LIMIT_CHANNEL_NAME_MIN" to 1,
    "LIMIT_CHANNEL_NAME_MAX" to 32,
    "LIMIT_CHANNEL_NAME_PATTERN" to "[a-z0-9_]{1,32}",
    "LIMIT_LANG_LENGTH" to 16,
    "LIMIT_APP_LANG_LENGTH" to 16,
    "LIMIT_LOCALE_REGION_LENGTH" to 2,
    "LIMIT_DAY_KEY_LENGTH" to 36,
    "LIMIT_ACTIVITY_WINDOW_DAYS" to 8,
    "LIMIT_CLIENT_RETENTION_PAST_DAYS" to 7,
    "LIMIT_CLOCK_ROLLBACK_DAYS" to 8,
    "LIMIT_DELETED_DAY_MEMORY_DAYS" to 9,
    "LIMIT_REV_MIN" to 1,
    "LIMIT_DROPPED_DAYS_MIN" to 0,
    "LIMIT_COUNTS_TRUNCATED_MIN" to 0,
    "LIMIT_VERSION_CODE_MIN" to 1,
    "CHANNEL_GITHUB_NAME" to "github",
    "CHANNEL_GITHUB_KIND" to "official",
    "CHANNEL_XPOSED_NAME" to "xposed",
    "CHANNEL_XPOSED_KIND" to "repo",
    "CHANNEL_UNKNOWN_NAME" to "unknown",
    "CHANNEL_UNKNOWN_KIND" to "unknown",
    "CHANNEL_DEV_NAME" to "dev",
    "CHANNEL_DEV_KIND" to "dev",
    "CHANNEL_LEGACY_NAME" to "legacy",
    "CHANNEL_LEGACY_KIND" to "legacy",
    "CONFIG_KEY_TELEMETRY_ENABLED_TYPE" to "bool",
    "CONFIG_KEY_TELEMETRY_ENABLED_DEFAULT" to true,
    "CONFIG_KEY_REPORT_INTERVAL_HOURS_TYPE" to "int",
    "CONFIG_KEY_REPORT_INTERVAL_HOURS_DEFAULT" to 24,
    "CONFIG_KEY_REPORT_INTERVAL_HOURS_MIN" to 24,
    "CONFIG_KEY_REPORT_INTERVAL_HOURS_MAX" to 168,
    "CONFIG_KEY_CONFIG_TTL_HOURS_TYPE" to "int",
    "CONFIG_KEY_CONFIG_TTL_HOURS_DEFAULT" to 12,
    "CONFIG_KEY_CONFIG_TTL_HOURS_MIN" to 12,
    "CONFIG_KEY_CONFIG_TTL_HOURS_MAX" to 168,
    "RAM_GB_BUCKETS" to "2,3,4,6,8,12,16,24",
    "SCREEN_WIDTH_BUCKETS" to "320,360,400,480,600,720",
    "SCREEN_DENSITY_BUCKETS" to "mdpi:160,hdpi:240,xhdpi:320,xxhdpi:480,xxxhdpi:640",
    "BUILD_TYPES" to "release,debug",
    "PROFILE_FIELDS" to "api_level,brand,model,abi,ram_gb,screen_class,lang,app_lang,locale_region,tz_id,tz_offset_min,installer",
    "EVENT_COUNT" to 0,
    "CUSTOM_KEY_COUNT" to 0,
)

private class RawLiteral(val raw: String)

private class ParsedKotlin(
    val consts: Map<String, Any?>,
    val duplicates: List<String>,
)

private fun compareKotlin(parsed: ParsedKotlin): List<String> {
    val diffs = mutableListOf<String>()
    val known = mapping.map { it.first }.toSet()
    for ((name, expected) in mapping) {
        val actual = parsed.consts[name]
        val same = actual !is RawLiteral && actual == expected
        if (actual == null || !same) {
            diffs += "$name: server=${formatValue(expected)} android=${formatParsed(actual)}"
        }
    }
    for (name in parsed.duplicates) {
        diffs += "$name: server=one definition android=duplicate"
    }
    for ((name, value) in parsed.consts) {
        if (watchedPrefixes.any { name.startsWith(it) } && name !in known) {
            diffs += "$name: server=not in mapping android=${formatParsed(value)}"
        }
    }
    return diffs
}

private fun formatValue(value: Any?): String = when (value) {
    is String -> "\"$value\""
    else -> value.toString()
}

private fun formatParsed(value: Any?): String = when (value) {
    null -> "missing"
    is RawLiteral -> value.raw
    else -> formatValue(value)
}

private fun yamlDrift(yaml: Map<String, Any?>): List<String> {
    val problems = mutableListOf<String>()
    fun eq(label: String, expected: Any?, actual: Any?) {
        if (expected != actual) problems += "$label: 契约 $actual, 名单 $expected"
    }
    val expected = mapping.toMap()
    eq("schema_version", expected["SCHEMA_VERSION"], yaml["schema_version"])
    eq("contract_version", expected["CONTRACT_VERSION"], yaml["contract_version"])
    eq("channel_name_pattern", expected["LIMIT_CHANNEL_NAME_PATTERN"], yaml["channel_name_pattern"])
    eq("event_count", expected["EVENT_COUNT"], (yaml["events"] as List<*>).size)
    val custom = yaml["custom"] as Map<*, *>
    val customCount = listOf("envelope", "day", "count").sumOf { (custom[it] as List<*>).size }
    eq("custom_key_count", expected["CUSTOM_KEY_COUNT"], customCount)

    val yamlChannels = (yaml["channels"] as List<*>).map { it as Map<*, *> }
    eq(
        "channels",
        listOf("GITHUB", "XPOSED", "DEV", "LEGACY", "UNKNOWN").map { id ->
            listOf(expected["CHANNEL_${id}_NAME"], expected["CHANNEL_${id}_KIND"], id == "LEGACY")
        },
        yamlChannels.map { listOf(it["name"], it["kind"], it["first_channel_only"]) },
    )
    eq(
        "channel list",
        yamlChannels.map { listOf(it["name"], it["kind"], it["first_channel_only"]) },
        TelemetryContract.channels.map { listOf(it.name, it.kind, it.firstChannelOnly) },
    )

    eq(
        "config",
        listOf(
            configRow("telemetry_enabled", "TELEMETRY_ENABLED", expected, withRange = false),
            configRow("report_interval_hours", "REPORT_INTERVAL_HOURS", expected, withRange = true),
            configRow("config_ttl_hours", "CONFIG_TTL_HOURS", expected, withRange = true),
        ),
        yaml["config_keys"],
    )

    val limits = (yaml["limits"] as Map<*, *>).mapKeys { it.key.toString() }
    val limitNames = mapping.map { it.first }.filter { it.startsWith("LIMIT_") && it != "LIMIT_CHANNEL_NAME_PATTERN" }
    val limitKeys = limitNames.map { it.removePrefix("LIMIT_").lowercase() }
    eq("limit keys", limitKeys, limits.keys.toList())
    limitNames.forEach { name ->
        eq(name, expected[name], limits[name.removePrefix("LIMIT_").lowercase()])
    }

    eq("ram_gb_buckets", expected["RAM_GB_BUCKETS"], (yaml["ram_gb_buckets"] as List<*>).joinToString(","))
    eq("screen_width_buckets", expected["SCREEN_WIDTH_BUCKETS"], (yaml["screen_width_buckets"] as List<*>).joinToString(","))
    val density = (yaml["screen_density"] as List<*>).joinToString(",") { item ->
        val bucket = item as Map<*, *>
        "${bucket["name"]}:${bucket["dpi"]}"
    }
    eq("screen_density", expected["SCREEN_DENSITY_BUCKETS"], density)
    eq("build_types", expected["BUILD_TYPES"], (yaml["build_types"] as List<*>).joinToString(","))
    val fields = (yaml["profile_fields"] as List<*>).map { it as Map<*, *> }
    eq("profile_fields", expected["PROFILE_FIELDS"], fields.joinToString(",") { it["name"].toString() })
    eq(
        "profile list",
        fields.map { listOf(it["name"], it["type"], it["nullable"]) },
        TelemetryContract.profileFields.map { listOf(it.name, it.type, it.nullable) },
    )
    return problems
}

private fun configRow(
    name: String,
    id: String,
    expected: Map<String, Any>,
    withRange: Boolean,
): Map<String, Any?> {
    val row = linkedMapOf<String, Any?>(
        "name" to name,
        "type" to expected.getValue("CONFIG_KEY_${id}_TYPE"),
        "default" to expected.getValue("CONFIG_KEY_${id}_DEFAULT"),
    )
    if (withRange) {
        row["min"] = expected.getValue("CONFIG_KEY_${id}_MIN")
        row["max"] = expected.getValue("CONFIG_KEY_${id}_MAX")
    }
    return row
}

private fun readRepoFile(relative: File): String {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
        val candidate = File(dir, relative.path)
        if (candidate.isFile) return candidate.readText()
        dir = dir.parentFile
    }
    error("找不到 ${relative.path}")
}

private val declaration =
    Regex("""^(?:public|internal|private)?\s*const\s+val\s+([A-Za-z_][A-Za-z0-9_]*)\b(.*)$""")

private fun parseKotlin(source: String): ParsedKotlin {
    val consts = linkedMapOf<String, Any?>()
    val duplicates = mutableListOf<String>()
    for (rawLine in stripKotlinComments(source).split("\n", "\r\n")) {
        val match = declaration.matchEntire(rawLine.trim()) ?: continue
        val name = match.groupValues[1]
        val rest = match.groupValues[2]
        val eq = rest.indexOf('=')
        if (eq < 0) continue
        if (name in consts) {
            duplicates += name
            continue
        }
        consts[name] = parseLiteral(rest.substring(eq + 1))
    }
    return ParsedKotlin(consts, duplicates)
}

private fun parseLiteral(raw: String): Any? {
    val text = raw.trim().removeSuffix(",").trim()
    when (text) {
        "true" -> return true
        "false" -> return false
    }
    if (text.length >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
        return text.substring(1, text.length - 1)
    }
    val withoutLong = if (text.endsWith("L") || text.endsWith("l")) text.dropLast(1) else text
    val normalized = withoutLong.replace("_", "")
    if (Regex("-?\\d+").matches(normalized)) return normalized.toInt()
    return RawLiteral(text)
}

private fun stripKotlinComments(source: String): String {
    val out = StringBuilder()
    var i = 0
    fun at(offset: Int): Char = source.getOrElse(offset) { '\u0000' }
    while (i < source.length) {
        val ch = source[i]
        val next = at(i + 1)
        if (ch == '"' && next == '"' && at(i + 2) == '"') {
            out.append("\"\"\"")
            i += 3
            while (i < source.length) {
                if (at(i) == '"' && at(i + 1) == '"' && at(i + 2) == '"') {
                    out.append("\"\"\"")
                    i += 3
                    break
                }
                out.append(source[i])
                i += 1
            }
            continue
        }
        if (ch == '"' || ch == '\'') {
            out.append(ch)
            i += 1
            while (i < source.length) {
                val s = source[i]
                if (s == '\\') {
                    out.append(s)
                    if (i + 1 < source.length) out.append(source[i + 1])
                    i += 2
                    continue
                }
                out.append(s)
                i += 1
                if (s == ch) break
            }
            continue
        }
        if (ch == '/' && next == '/') {
            i += 2
            while (i < source.length && source[i] != '\n') i += 1
            continue
        }
        if (ch == '/' && next == '*') {
            i += 2
            while (i < source.length && !(source[i] == '*' && at(i + 1) == '/')) {
                if (source[i] == '\n') out.append('\n')
                i += 1
            }
            i += 2
            continue
        }
        out.append(ch)
        i += 1
    }
    return out.toString()
}

private fun parseContractYaml(text: String): Map<String, Any?> {
    val lines = mutableListOf<YamlLine>()
    text.replace("\r\n", "\n").replace('\r', '\n').lines().forEachIndexed { zero, raw ->
        val stripped = stripYamlComment(raw)
        if (stripped.isBlank()) return@forEachIndexed
        if (stripped.any { it == '\t' }) error("第 ${zero + 1} 行: 用空格缩进")
        val indent = stripped.indexOfFirst { it != ' ' }
        if (indent % 2 != 0) error("第 ${zero + 1} 行: 缩进必须是 2 的倍数")
        lines += YamlLine(zero + 1, indent, stripped.substring(indent).trimEnd())
    }
    return ContractYamlParser(lines).parse()
}

private fun stripYamlComment(raw: String): String {
    var quoted = false
    for (i in raw.indices) {
        val c = raw[i]
        if (c == '"') quoted = !quoted
        if (c == '#' && !quoted && (i == 0 || raw[i - 1] == ' ')) return raw.substring(0, i)
    }
    return raw
}

private data class YamlLine(val number: Int, val indent: Int, val text: String)

private class ContractYamlParser(private val lines: List<YamlLine>) {
    private var index = 0

    fun parse(): Map<String, Any?> {
        val document = parseMap(0)
        if (index != lines.size) fail(lines[index], "缩进无法接上")
        return document
    }

    private fun parseMap(indent: Int): LinkedHashMap<String, Any?> {
        if (index >= lines.size || lines[index].indent != indent || lines[index].text.startsWith("- ")) {
            fail(lines.getOrNull(index), "这里应该是一组键")
        }
        val map = linkedMapOf<String, Any?>()
        while (index < lines.size && lines[index].indent == indent && !lines[index].text.startsWith("- ")) {
            val line = lines[index]
            val (key, raw) = splitKey(line, line.text)
            if (key in map) fail(line, "重复键 $key")
            index += 1
            map[key] = if (raw == null) parseNested(indent) else parseScalar(line, raw)
        }
        return map
    }

    private fun parseNested(parentIndent: Int): Any {
        if (index >= lines.size || lines[index].indent <= parentIndent) {
            fail(lines.getOrNull(index), "缺少嵌套内容")
        }
        val line = lines[index]
        return if (line.text.startsWith("- ")) parseList(line.indent) else parseMap(line.indent)
    }

    private fun parseList(indent: Int): List<Any?> {
        val items = mutableListOf<Any?>()
        while (index < lines.size && lines[index].indent == indent && lines[index].text.startsWith("- ")) {
            val line = lines[index]
            val body = line.text.removePrefix("- ").trim()
            if (body.isEmpty()) fail(line, "空列表项")
            index += 1
            items += if (bodyLooksLikeKey(body)) parseItemMap(line, body, indent) else parseScalar(line, body)
        }
        if (items.isEmpty()) fail(lines.getOrNull(index), "空列表")
        return items
    }

    private fun parseItemMap(firstLine: YamlLine, firstBody: String, dashIndent: Int): Map<String, Any?> {
        val contentIndent = dashIndent + 2
        val map = linkedMapOf<String, Any?>()
        val (firstKey, firstRaw) = splitKey(firstLine, firstBody)
        map[firstKey] = if (firstRaw == null) parseNested(contentIndent) else parseScalar(firstLine, firstRaw)
        while (
            index < lines.size &&
            lines[index].indent == contentIndent &&
            !lines[index].text.startsWith("- ")
        ) {
            val line = lines[index]
            val (key, raw) = splitKey(line, line.text)
            if (key in map) fail(line, "重复键 $key")
            index += 1
            map[key] = if (raw == null) parseNested(contentIndent) else parseScalar(line, raw)
        }
        return map
    }

    private fun splitKey(line: YamlLine, text: String): Pair<String, String?> {
        val colon = text.indexOf(':')
        if (colon <= 0) fail(line, "缺少键: $text")
        val key = text.substring(0, colon).trim()
        if (!Regex("[A-Za-z0-9_]+").matches(key)) fail(line, "非法键 $key")
        val rest = text.substring(colon + 1).trim()
        return key to rest.ifEmpty { null }
    }

    private fun bodyLooksLikeKey(body: String): Boolean =
        !body.startsWith("\"") && body.indexOf(':') > 0

    private fun parseScalar(line: YamlLine, raw: String): Any? {
        when {
            raw == "[]" -> return emptyList<Any>()
            raw == "null" -> return null
            raw == "true" -> return true
            raw == "false" -> return false
            raw.startsWith("\"") -> {
                if (raw.length < 2 || !raw.endsWith("\"")) fail(line, "引号未闭合")
                return raw.substring(1, raw.length - 1)
            }
            raw.startsWith("[") -> {
                if (!raw.endsWith("]")) fail(line, "列表未闭合")
                val inner = raw.substring(1, raw.length - 1).trim()
                if (inner.isEmpty()) return emptyList<Any>()
                if (inner.contains("[") || inner.contains("]")) fail(line, "不支持嵌套的单行列表")
                return inner.split(",").map { parseScalar(line, it.trim()) }
            }
            Regex("-?[0-9]+").matches(raw) -> return raw.toIntOrNull() ?: fail(line, "整数超出范围 $raw")
            raw.any { it.isWhitespace() } -> fail(line, "裸字符串不能含空白")
            else -> return raw
        }
    }

    private fun fail(line: YamlLine?, message: String): Nothing {
        val where = line?.let { "第 ${it.number} 行" } ?: "文件末尾"
        error("$where: $message")
    }
}
