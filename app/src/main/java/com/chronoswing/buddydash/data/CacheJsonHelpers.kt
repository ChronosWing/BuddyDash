package com.chronoswing.buddydash.data

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.putOptString(key: String, value: String?): JSONObject {
    if (value != null) put(key, value)
    return this
}

internal fun JSONObject.putOptObject(key: String, value: JSONObject?): JSONObject {
    if (value != null) put(key, value)
    return this
}

internal fun JSONObject.putOptDouble(key: String, value: Double?): JSONObject {
    if (value != null) put(key, value)
    return this
}

internal fun JSONObject.putOptInt(key: String, value: Int?): JSONObject {
    if (value != null) put(key, value)
    return this
}

internal fun JSONObject.putOptBoolean(key: String, value: Boolean?): JSONObject {
    if (value != null) put(key, value)
    return this
}

internal fun JSONObject.putOptLong(key: String, value: Long?): JSONObject {
    if (value != null) put(key, value)
    return this
}

internal fun JSONObject.optNullableInt(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

internal fun JSONObject.optNullableDouble(key: String): Double? =
    if (has(key) && !isNull(key)) {
        val v = optDouble(key)
        if (v.isNaN()) null else v
    } else {
        null
    }

internal fun JSONObject.optNullableBoolean(key: String): Boolean? =
    if (has(key) && !isNull(key)) optBoolean(key) else null

internal fun JSONObject.optNullableLong(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null

internal fun encodeStringList(values: List<String>): JSONArray =
    JSONArray().apply { values.forEach { put(it) } }

internal fun decodeStringList(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return buildList {
        for (i in 0 until array.length()) {
            array.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }
}
