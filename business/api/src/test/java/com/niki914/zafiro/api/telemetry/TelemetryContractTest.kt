// 保护：手写常量与 telemetry-contract/contract.yaml 分叉，或常量写成表达式、同名写了两次。服务端只正则读 const val，这种改动会让两端上限或渠道对不上。
package com.niki914.zafiro.api.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class TelemetryContractTest {

    @Test
    fun `hand written const vals match contract yaml`() {
        val source = contractSource()
        assertEquals("", blockCommentsWithConst(source).joinToString("\n"))
        val consts = parseConstVals(source)
        assertEquals("", duplicateNames(source).joinToString("\n"))
        val yaml = parseContractYaml(readRepoFile(File("telemetry-contract", "contract.yaml")))
        val left = consts.toMutableMap()

        fun take(name: String): Any? {
            if (name !in left) return missing
            return left.remove(name)
        }

        val problems = mutableListOf<String>()
        fun eq(label: String, expected: Any?, actual: Any?) {
            if (expected != actual) problems += "$label: 契约 $expected, 常量 $actual"
        }

        eq("schema_version", yaml["schema_version"], take("SCHEMA_VERSION"))
        eq("contract_version", yaml["contract_version"], take("CONTRACT_VERSION"))
        eq("channel_name_pattern", yaml["channel_name_pattern"], take("CHANNEL_NAME_PATTERN"))
        eq("event_count", (yaml["events"] as List<*>).size, take("EVENT_COUNT"))
        val custom = yaml["custom"] as Map<*, *>
        val customCount = listOf("envelope", "day", "count").sumOf { (custom[it] as List<*>).size }
        eq("custom_key_count", customCount, take("CUSTOM_KEY_COUNT"))

        val yamlChannels = (yaml["channels"] as List<*>).map { it as Map<*, *> }
        yamlChannels.forEach { channel ->
            val wire = channel["name"] as String
            eq("channel $wire", wire, take("CHANNEL_${wire.uppercase()}"))
        }
        eq(
            "channels",
            yamlChannels.map { channel ->
                listOf(channel["name"], channel["kind"], channel["first_channel_only"])
            },
            TelemetryContract.channels.map { listOf(it.name, it.kind, it.firstChannelOnly) },
        )

        eq(
            "config",
            yaml["config_keys"],
            listOf(
                configRow(
                    take("CONFIG_KEY_TELEMETRY_ENABLED"),
                    "bool",
                    take("CONFIG_DEFAULT_TELEMETRY_ENABLED"),
                    null,
                    null,
                ),
                configRow(
                    take("CONFIG_KEY_REPORT_INTERVAL_HOURS"),
                    "int",
                    take("LIMIT_REPORT_INTERVAL_HOURS_DEFAULT"),
                    take("LIMIT_REPORT_INTERVAL_HOURS_MIN"),
                    take("LIMIT_REPORT_INTERVAL_HOURS_MAX"),
                ),
                configRow(
                    take("CONFIG_KEY_CONFIG_TTL_HOURS"),
                    "int",
                    take("LIMIT_CONFIG_TTL_HOURS_DEFAULT"),
                    take("LIMIT_CONFIG_TTL_HOURS_MIN"),
                    take("LIMIT_CONFIG_TTL_HOURS_MAX"),
                ),
            ),
        )

        val limits = yaml["limits"] as Map<*, *>
        limits.forEach { (key, value) ->
            eq("limit $key", value, take("LIMIT_${key.toString().uppercase()}"))
        }

        eq("ram_gb_buckets", joinInts(yaml["ram_gb_buckets"]), take("RAM_GB_BUCKETS"))
        eq("screen_width_dp", joinInts(yaml["screen_width_dp"]), take("SCREEN_WIDTH_DP_BUCKETS"))
        val density = (yaml["screen_density"] as List<*>).joinToString(",") { item ->
            val bucket = item as Map<*, *>
            "${bucket["name"]}:${bucket["dpi"]}"
        }
        eq("screen_density", density, take("SCREEN_DENSITY_BUCKETS"))
        eq("build_types", (yaml["build_types"] as List<*>).joinToString(","), take("BUILD_TYPES"))
        eq(
            "profile_fields",
            yaml["profile_fields"],
            TelemetryContract.profileFields.map {
                linkedMapOf("name" to it.name, "type" to it.type, "nullable" to it.nullable)
            },
        )

        consts.forEach { (name, value) ->
            val compiled = compiledConst(name)
            if (compiled != value) problems += "$name: 源码 $value, 编译结果 $compiled"
        }
        if (left.isNotEmpty()) problems += "常量没有契约对照: ${left.keys.sorted()}"
        assertEquals("", problems.joinToString("\n"))
    }

    private fun configRow(
        name: Any?,
        type: String,
        default: Any?,
        min: Any?,
        max: Any?,
    ): Map<String, Any?> {
        val row = linkedMapOf<String, Any?>("name" to name, "type" to type, "default" to default)
        if (min != null) row["min"] = min
        if (max != null) row["max"] = max
        return row
    }

    private fun joinInts(value: Any?): String = (value as List<*>).joinToString(",")

    private fun compiledConst(name: String): Any? =
        TelemetryContract::class.java.getField(name).get(null)

    private fun contractSource(): String =
        readRepoFile(
            File(
                "business/api/src/main/java/com/niki914/zafiro/api/telemetry/TelemetryContract.kt",
            ),
        )

    private fun readRepoFile(relative: File): String {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, relative.path)
            if (candidate.isFile) return candidate.readText()
            dir = dir.parentFile
        }
        error("找不到 ${relative.path}")
    }
}

private val missing = Any()

private val constLine = Regex("""^const val ([A-Z][A-Z0-9_]*) = (.+)$""")

private val literal = Regex("-?[0-9]+|true|false|\"[^\"]*\"")

private fun blockCommentsWithConst(source: String): List<String> =
    Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL).findAll(source)
        .filter { "const val" in it.value }
        .map { "块注释里写了常量" }
        .toList()

private fun parseConstVals(source: String): Map<String, Any?> {
    val visible = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL).replace(source, "")
    val values = linkedMapOf<String, Any?>()
    visible.lines().forEach { raw ->
        val line = raw.substringBefore("//").trim()
        val match = constLine.matchEntire(line) ?: return@forEach
        val name = match.groupValues[1]
        val token = match.groupValues[2].trim()
        if (!literal.matches(token)) error("$name 的值不是字面量: $token")
        values[name] = when {
            token == "true" -> true
            token == "false" -> false
            token.startsWith("\"") -> token.removeSurrounding("\"")
            else -> token.toInt()
        }
    }
    return values
}

private fun duplicateNames(source: String): List<String> {
    val visible = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL).replace(source, "")
    val seen = mutableSetOf<String>()
    val dup = mutableListOf<String>()
    visible.lines().forEach { raw ->
        val match = constLine.matchEntire(raw.substringBefore("//").trim()) ?: return@forEach
        val name = match.groupValues[1]
        if (!seen.add(name)) dup += "同名常量 $name"
    }
    return dup
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
