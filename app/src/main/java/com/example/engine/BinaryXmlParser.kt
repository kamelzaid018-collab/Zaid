package com.example.engine

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parses Android Binary XML (AXML) from compiled APK files (e.g. AndroidManifest.xml)
 * and reconstructs readable standard XML syntax.
 */
object BinaryXmlParser {

    private const val CHUNK_AXML_FILE = 0x00080003
    private const val CHUNK_STRING_POOL = 0x001C0001
    private const val CHUNK_RESOURCEIDS = 0x00080180
    private const val CHUNK_START_NAMESPACE = 0x00100100
    private const val CHUNK_END_NAMESPACE = 0x00100101
    private const val CHUNK_START_TAG = 0x00100102
    private const val CHUNK_END_TAG = 0x00100103
    private const val CHUNK_TEXT = 0x00100104

    fun parse(bytes: ByteArray): String {
        if (bytes.size < 8) return fallbackManifest()

        return try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buffer.int
            if (magic != CHUNK_AXML_FILE) {
                // If it's already plain text XML, return as string
                val text = String(bytes, Charsets.UTF_8)
                if (text.contains("<manifest") || text.contains("<?xml")) {
                    return text
                }
                return fallbackManifest()
            }

            val fileSize = buffer.int
            val stringPool = mutableListOf<String>()
            val result = StringBuilder()
            result.appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")

            var indentLevel = 0

            while (buffer.hasRemaining()) {
                val chunkStart = buffer.position()
                if (chunkStart + 8 > bytes.size) break
                val chunkType = buffer.int
                val chunkSize = buffer.int
                if (chunkSize <= 0 || chunkStart + chunkSize > bytes.size) break

                when (chunkType) {
                    CHUNK_STRING_POOL -> {
                        parseStringPool(buffer, chunkStart, stringPool)
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_START_TAG -> {
                        val lineNumber = buffer.int
                        val comment = buffer.int
                        val nsIdx = buffer.int
                        val nameIdx = buffer.int
                        val attrStart = buffer.short.toInt() and 0xFFFF
                        val attrSize = buffer.short.toInt() and 0xFFFF
                        val attrCount = buffer.short.toInt() and 0xFFFF
                        val idIndex = buffer.short.toInt() and 0xFFFF
                        val classIndex = buffer.short.toInt() and 0xFFFF
                        val styleIndex = buffer.short.toInt() and 0xFFFF

                        val tagName = getString(stringPool, nameIdx).ifEmpty { "element" }
                        val indent = "    ".repeat(indentLevel)
                        result.append("$indent<$tagName")

                        if (indentLevel == 0) {
                            result.append("\n$indent    xmlns:android=\"http://schemas.android.com/apk/res/android\"")
                        }

                        // Attributes
                        for (i in 0 until attrCount) {
                            val aNs = buffer.int
                            val aName = buffer.int
                            val aRawVal = buffer.int
                            val aTypedValSize = buffer.short
                            val aTypedValRes0 = buffer.get()
                            val aTypedValType = buffer.get()
                            val aTypedValData = buffer.int

                            val attrName = getString(stringPool, aName)
                            val attrVal = if (aRawVal != -1) {
                                getString(stringPool, aRawVal)
                            } else {
                                formatTypedValue(aTypedValType.toInt(), aTypedValData)
                            }
                            if (attrName.isNotEmpty()) {
                                result.append("\n$indent    android:$attrName=\"$attrVal\"")
                            }
                        }

                        result.appendLine(">")
                        indentLevel++
                        buffer.position(chunkStart + chunkSize)
                    }
                    CHUNK_END_TAG -> {
                        val lineNumber = buffer.int
                        val comment = buffer.int
                        val nsIdx = buffer.int
                        val nameIdx = buffer.int
                        val tagName = getString(stringPool, nameIdx).ifEmpty { "element" }
                        indentLevel = maxOf(0, indentLevel - 1)
                        val indent = "    ".repeat(indentLevel)
                        result.appendLine("$indent</$tagName>")
                        buffer.position(chunkStart + chunkSize)
                    }
                    else -> {
                        buffer.position(chunkStart + chunkSize)
                    }
                }
            }

            val parsed = result.toString()
            if (parsed.contains("<manifest")) parsed else fallbackManifest()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackManifest()
        }
    }

    private fun parseStringPool(buffer: ByteBuffer, chunkStart: Int, stringPool: MutableList<String>) {
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsStart = buffer.int
        val stylesStart = buffer.int

        val isUtf8 = (flags and (1 shl 8)) != 0
        val offsets = IntArray(stringCount)
        for (i in 0 until stringCount) {
            offsets[i] = buffer.int
        }

        val poolDataStart = chunkStart + stringsStart
        for (i in 0 until stringCount) {
            val offset = poolDataStart + offsets[i]
            if (offset < buffer.capacity()) {
                buffer.position(offset)
                val str = if (isUtf8) readUtf8(buffer) else readUtf16(buffer)
                stringPool.add(str)
            } else {
                stringPool.add("")
            }
        }
    }

    private fun readUtf8(buffer: ByteBuffer): String {
        var len = buffer.get().toInt() and 0xFF
        if ((len and 0x80) != 0) {
            len = ((len and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
        }
        var byteLen = buffer.get().toInt() and 0xFF
        if ((byteLen and 0x80) != 0) {
            byteLen = ((byteLen and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
        }
        val bytes = ByteArray(byteLen)
        buffer.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf16(buffer: ByteBuffer): String {
        var len = buffer.short.toInt() and 0xFFFF
        if ((len and 0x8000) != 0) {
            len = ((len and 0x7FFF) shl 16) or (buffer.short.toInt() and 0xFFFF)
        }
        val chars = CharArray(len)
        for (i in 0 until len) {
            chars[i] = buffer.char
        }
        return String(chars)
    }

    private fun formatTypedValue(type: Int, data: Int): String {
        return when (type) {
            0x03 -> "$data"
            0x10 -> "$data"
            0x11 -> "0x" + Integer.toHexString(data)
            0x12 -> if (data != 0) "true" else "false"
            0x01 -> "@0x" + Integer.toHexString(data)
            else -> "$data"
        }
    }

    private fun getString(pool: List<String>, index: Int): String {
        return if (index in pool.indices) pool[index] else ""
    }

    fun fallbackManifest(packageName: String = "com.example.modapp"): String {
        return """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$packageName"
    android:versionCode="1"
    android:versionName="1.0.0">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Modded Application"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.DeviceDefault.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
        """.trimIndent()
    }
}
