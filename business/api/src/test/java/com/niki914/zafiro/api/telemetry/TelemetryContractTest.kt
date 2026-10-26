// 保护：手写常量与 telemetry-contract/contract.yaml 分叉。只改契约或只改常量时,渠道、上限或配置默认值会按错误的数发出去。
package com.niki914.zafiro.api.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier
import java.util.Locale

class TelemetryContractTest {

    @Test
    fun `hand written constants match contract yaml`() {
        assertEquals("", structureGaps().joinToString("\n"))
        val expected = expectedTree()
        val actual = parseContractYaml(contractFile().readText())
        assertEquals("\n${render(expected)}\n---\n${render(actual)}", expected, actual)
    }

    private fun structureGaps(): List<String> {
        val gaps = mutableListOf<String>()
        fun fields(type: Class<*>, expected: Set<String>) {
            val actual = type.declaredFields
                .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) && '$' !in it.name }
                .map { it.name }
                .toSet()
            if (actual != expected) {
                gaps += "${type.simpleName} 字段 $actual,投影 $expected"
            }
        }
        fields(
            TelemetryContract.Channel::class.java,
            setOf("name", "kind", "firstChannelOnly"),
        )
        fields(
            TelemetryContract.ConfigKey::class.java,
            setOf("name", "type", "default", "min", "max"),
        )
        fields(
            TelemetryContract.ProfileField::class.java,
            setOf("name", "type", "nullable"),
        )
        fields(
            TelemetryContract.DensityBucket::class.java,
            setOf("name", "dpi"),
        )
        fields(EventDef::class.java, eventFields.toSet())
        fields(
            CustomKeyDef::class.java,
            customItemFields.toSet() + "scope",
        )
        gaps += constGaps(
            "渠道名",
            TelemetryContract.channels.map { it.name },
            TelemetryContract.ChannelName::class.java,
        )
        gaps += constGaps(
            "渠道种类",
            TelemetryContract.channels.map { it.kind },
            TelemetryContract.ChannelKind::class.java,
        )
        gaps += constGaps(
            "配置键",
            TelemetryContract.configKeys.map { it.name },
            TelemetryContract.ConfigName::class.java,
        )
        gaps += constGaps(
            "配置类型",
            TelemetryContract.configKeys.map { it.type },
            TelemetryContract.ConfigType::class.java,
        )
        gaps += constGaps(
            "画像类型",
            TelemetryContract.profileFields.map { it.type },
            TelemetryContract.FieldType::class.java,
        )
        gaps += constGaps(
            "构建类型",
            TelemetryContract.buildTypes,
            TelemetryContract.BuildType::class.java,
        )
        gaps += memberGaps()
        return gaps
    }

    private fun constGaps(label: String, used: List<String>, type: Class<*>): List<String> {
        val declared = type.fields
            .filter {
                Modifier.isStatic(it.modifiers) &&
                    it.type == String::class.java &&
                    '$' !in it.name
            }
            .map { it.get(null) as String }
        val gaps = mutableListOf<String>()
        if (declared.size != declared.toSet().size) gaps += "$label 常量值重复: $declared"
        val unused = declared.toSet() - used.toSet()
        val unknown = used.toSet() - declared.toSet()
        if (unused.isNotEmpty()) gaps += "$label 常量没有进清单: $unused"
        if (unknown.isNotEmpty()) gaps += "$label 清单有契约常量之外的值: $unknown"
        return gaps
    }

    private fun memberGaps(): List<String> {
        val gaps = mutableListOf<String>()
        val type = TelemetryContract::class.java
        val getters = type.declaredMethods
            .filter {
                !it.isSynthetic &&
                    it.parameterCount == 0 &&
                    it.name.startsWith("get") &&
                    it.name != "getClass"
            }
            .map { it.name }
            .sorted()
        val expectedGetters = listOf(
            "getBuildTypes",
            "getChannels",
            "getConfigKeys",
            "getCustomKeys",
            "getEvents",
            "getProfileFields",
            "getRamGbBuckets",
            "getScreenDensity",
            "getScreenWidthDp",
        )
        if (getters != expectedGetters) gaps += "TelemetryContract 访问器 $getters"
        val properties = getters.map { it.removePrefix("get").replaceFirstChar { c -> c.lowercase(Locale.ROOT) } }.toSet()
        val staticOnly = type.declaredFields
            .filter {
                Modifier.isStatic(it.modifiers) &&
                    it.name != "INSTANCE" &&
                    !it.isSynthetic &&
                    '$' !in it.name &&
                    it.name !in properties
            }
            .map { it.name }
            .sorted()
        val expectedStatic = listOf("CHANNEL_NAME_PATTERN", "CONTRACT_VERSION", "SCHEMA_VERSION")
        if (staticOnly != expectedStatic) gaps += "TelemetryContract 常量 $staticOnly"
        return gaps
    }

    private fun expectedTree(): Map<String, Any?> = linkedMapOf(
        "schema_version" to TelemetryContract.SCHEMA_VERSION,
        "contract_version" to TelemetryContract.CONTRACT_VERSION,
        "channel_name_pattern" to TelemetryContract.CHANNEL_NAME_PATTERN,
        "channels" to TelemetryContract.channels.map {
            project(it, listOf("name", "kind", "firstChannelOnly"))
        },
        "events" to TelemetryContract.events.map {
            project(it, eventFields)
        },
        "custom" to CustomKeyDef.Scope.entries.associate { scope ->
            scope.name.lowercase(Locale.ROOT) to TelemetryContract.customKeys
                .filter { it.scope == scope }
                .map { project(it, customItemFields) }
        },
        "config_keys" to TelemetryContract.configKeys.map {
            project(it, listOf("name", "type", "default", "min", "max"))
        },
        "limits" to limitsFromCode(),
        "profile_fields" to TelemetryContract.profileFields.map {
            project(it, listOf("name", "type", "nullable"))
        },
        "ram_gb_buckets" to TelemetryContract.ramGbBuckets,
        "screen_width_dp" to TelemetryContract.screenWidthDp,
        "screen_density" to TelemetryContract.screenDensity.map {
            project(it, listOf("name", "dpi"))
        },
        "build_types" to TelemetryContract.buildTypes,
    )

    private fun limitsFromCode(): Map<String, Int> =
        TelemetryContract.Limits::class.java.fields
            .filter {
                Modifier.isStatic(it.modifiers) &&
                    it.type == Int::class.javaPrimitiveType &&
                    '$' !in it.name
            }
            .associate { it.name.lowercase(Locale.ROOT) to it.getInt(null) }

    private fun project(target: Any, fieldNames: List<String>): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>()
        for (name in fieldNames) {
            val raw = read(target, name) ?: continue
            map[camelToSnake(name)] = normalize(raw)
        }
        return map
    }

    private fun read(target: Any, name: String): Any? {
        val suffix = name.replaceFirstChar { it.uppercase(Locale.ROOT) }
        val getter = target.javaClass.methods.firstOrNull {
            it.parameterCount == 0 && (it.name == "get$suffix" || it.name == "is$suffix")
        } ?: error("${target.javaClass.simpleName} 没有 $name 的访问器")
        return getter.invoke(target)
    }

    private fun normalize(raw: Any): Any = when (raw) {
        is TelemetryContract.ConfigDefault.OfBool -> raw.value
        is TelemetryContract.ConfigDefault.OfInt -> raw.value
        is Enum<*> -> raw.name.lowercase(Locale.ROOT)
        is List<*> -> raw.map { item -> if (item == null) null else normalize(item) }
        else -> raw
    }

    private fun camelToSnake(name: String): String =
        name.replace(Regex("([a-z0-9])([A-Z])")) {
            "${it.groupValues[1]}_${it.groupValues[2]}"
        }.lowercase(Locale.ROOT)

    private fun contractFile(): File {
        val relative = File("telemetry-contract", "contract.yaml")
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, relative.path)
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        error("找不到 telemetry-contract/contract.yaml")
    }

    private fun render(value: Any?): String =
        buildString { appendValue(value, 0) }.removePrefix("\n")

    private fun StringBuilder.appendValue(value: Any?, indent: Int) {
        when (value) {
            null -> append("null")
            is Map<*, *> -> appendMap(value, indent)
            is List<*> -> appendList(value, indent)
            is String, is Boolean, is Int -> append(value.toString())
            else -> error("无法渲染 ${value.javaClass.simpleName}")
        }
    }

    private fun StringBuilder.appendMap(value: Map<*, *>, indent: Int) {
        val keys = value.keys.map { it as String }.sorted()
        if (keys.isEmpty()) {
            append("{}")
            return
        }
        for (key in keys) {
            append("\n").append("  ".repeat(indent)).append(key).append(":")
            val child = value[key]
            if (child is Map<*, *> || (child is List<*> && child.any { it is Map<*, *> })) {
                appendValue(child, indent + 1)
            } else {
                append(" ")
                appendValue(child, indent + 1)
            }
        }
    }

    private fun StringBuilder.appendList(value: List<*>, indent: Int) {
        if (value.isEmpty()) {
            append("[]")
            return
        }
        if (value.any { it is Map<*, *> }) {
            for (item in value) {
                append("\n").append("  ".repeat(indent)).append("-")
                if (item is Map<*, *>) {
                    appendMap(item, indent + 1)
                } else {
                    append(" ")
                    appendValue(item, indent + 1)
                }
            }
        } else {
            append("[")
            value.forEachIndexed { index, item ->
                if (index > 0) append(", ")
                appendValue(item, indent)
            }
            append("]")
        }
    }
}

private val eventFields = listOf(
    "name",
    "description",
    "labels",
    "status",
    "addedIn",
    "replacedBy",
)

private val customItemFields = listOf(
    "name",
    "type",
    "role",
    "agg",
    "enumValues",
    "maxChars",
    "min",
    "max",
    "addedIn",
)

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
        if (!KEY.matches(key)) fail(line, "非法键 $key")
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
            INT.matches(raw) -> return raw.toIntOrNull() ?: fail(line, "整数超出范围 $raw")
            raw.any { it.isWhitespace() } -> fail(line, "裸字符串不能含空白")
            else -> return raw
        }
    }

    private fun fail(line: YamlLine?, message: String): Nothing {
        val where = line?.let { "第 ${it.number} 行" } ?: "文件末尾"
        error("$where: $message")
    }

    private companion object {
        val KEY: Regex = Regex("[A-Za-z0-9_]+")
        val INT: Regex = Regex("-?[0-9]+")
    }
}
