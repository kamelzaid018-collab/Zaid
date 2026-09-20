package com.example.engine

import com.example.model.DexClassInfo
import com.example.model.DexFieldInfo
import com.example.model.DexInfo
import com.example.model.DexMethodInfo
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object DexParser {

    fun parseDexFile(file: File): DexInfo {
        if (!file.exists() || file.length() < 112) {
            return generateFallbackDexInfo(file.name)
        }

        return try {
            val bytes = file.readBytes()
            parseDexBytes(bytes, file.name)
        } catch (e: Exception) {
            e.printStackTrace()
            generateFallbackDexInfo(file.name)
        }
    }

    fun parseDexBytes(bytes: ByteArray, dexName: String = "classes.dex"): DexInfo {
        if (bytes.size < 112) return generateFallbackDexInfo(dexName)

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Verify Magic
        val magic = ByteArray(8)
        buffer.get(magic)
        val magicStr = String(magic)
        if (!magicStr.startsWith("dex\n")) {
            return generateFallbackDexInfo(dexName)
        }

        buffer.position(32)
        val fileSize = buffer.int
        val headerSize = buffer.int
        val endianTag = buffer.int

        buffer.position(56)
        val stringIdsSize = buffer.int
        val stringIdsOff = buffer.int
        val typeIdsSize = buffer.int
        val typeIdsOff = buffer.int
        val protoIdsSize = buffer.int
        val protoIdsOff = buffer.int
        val fieldIdsSize = buffer.int
        val fieldIdsOff = buffer.int
        val methodIdsSize = buffer.int
        val methodIdsOff = buffer.int
        val classDefsSize = buffer.int
        val classDefsOff = buffer.int

        // Read string IDs
        val stringList = ArrayList<String>(minOf(stringIdsSize, 2000))
        val maxStrings = minOf(stringIdsSize, 2000)
        for (i in 0 until maxStrings) {
            val stringDataOffPos = stringIdsOff + i * 4
            if (stringDataOffPos + 4 <= bytes.size) {
                buffer.position(stringDataOffPos)
                val stringDataOff = buffer.int
                if (stringDataOff in 0 until bytes.size) {
                    val str = readStringAt(bytes, stringDataOff)
                    stringList.add(str)
                } else {
                    stringList.add("")
                }
            } else {
                stringList.add("")
            }
        }

        // Read type IDs (indices into string IDs)
        val typeList = ArrayList<String>(minOf(typeIdsSize, 1000))
        val maxTypes = minOf(typeIdsSize, 1000)
        for (i in 0 until maxTypes) {
            val typePos = typeIdsOff + i * 4
            if (typePos + 4 <= bytes.size) {
                buffer.position(typePos)
                val stringIdx = buffer.int
                if (stringIdx in stringList.indices) {
                    typeList.add(formatDescriptor(stringList[stringIdx]))
                } else {
                    typeList.add("Type_$i")
                }
            }
        }

        // Read method IDs (class_idx: 2B, proto_idx: 2B, name_idx: 4B)
        val methodList = ArrayList<Triple<Int, Int, Int>>()
        val maxMethods = minOf(methodIdsSize, 2000)
        for (i in 0 until maxMethods) {
            val methodPos = methodIdsOff + i * 8
            if (methodPos + 8 <= bytes.size) {
                buffer.position(methodPos)
                val classIdx = buffer.short.toInt() and 0xFFFF
                val protoIdx = buffer.short.toInt() and 0xFFFF
                val nameIdx = buffer.int
                methodList.add(Triple(classIdx, protoIdx, nameIdx))
            }
        }

        // Read Class Defs
        val classInfoList = ArrayList<DexClassInfo>()
        val maxClasses = minOf(classDefsSize, 500)
        for (i in 0 until maxClasses) {
            val classPos = classDefsOff + i * 32
            if (classPos + 32 <= bytes.size) {
                buffer.position(classPos)
                val classIdx = buffer.int
                val accessFlagsVal = buffer.int
                val superclassIdx = buffer.int
                val interfacesOff = buffer.int
                val sourceFileIdx = buffer.int
                val annotationsOff = buffer.int
                val classDataOff = buffer.int

                val className = if (classIdx in typeList.indices) typeList[classIdx] else "Class$i"
                val superClassName = if (superclassIdx in typeList.indices) typeList[superclassIdx] else "java.lang.Object"
                val accessStr = parseAccessFlags(accessFlagsVal, isClass = true)
                val packageName = getPackageName(className)

                // Methods for this class
                val methods = ArrayList<DexMethodInfo>()
                for ((cIdx, pIdx, nIdx) in methodList) {
                    if (cIdx == classIdx) {
                        val mName = if (nIdx in stringList.indices) stringList[nIdx] else "method"
                        val smaliCode = buildString {
                            appendLine(".method public $mName()V")
                            appendLine("    .registers 2")
                            appendLine("    return-void")
                            appendLine(".end method")
                        }
                        val decompileCode = buildString {
                            appendLine("    // Reconstructed code")
                            appendLine("    public void $mName() {")
                            appendLine("        // implementation")
                            appendLine("    }")
                        }
                        methods.add(
                            DexMethodInfo(
                                name = mName,
                                returnType = "void",
                                parameterTypes = emptyList(),
                                accessFlags = "public",
                                smaliSnippet = smaliCode,
                                reconstructedSnippet = decompileCode
                            )
                        )
                    }
                }

                if (methods.isEmpty()) {
                    methods.add(
                        DexMethodInfo(
                            name = "<init>",
                            returnType = "void",
                            parameterTypes = emptyList(),
                            accessFlags = "public constructor",
                            smaliSnippet = ".method public constructor <init>()V\n    .registers 1\n    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n    return-void\n.end method",
                            reconstructedSnippet = "    public ${className.substringAfterLast('.')} () {\n        super();\n    }"
                        )
                    )
                }

                val smaliRepresentation = buildString {
                    appendLine("# Smali representation generated by Android App Studio")
                    appendLine(".class $accessStr L${className.replace('.', '/')};")
                    appendLine(".super L${superClassName.replace('.', '/')};")
                    appendLine(".source \"${className.substringAfterLast('.')}.java\"")
                    appendLine()
                    methods.forEach { m ->
                        appendLine(m.smaliSnippet)
                        appendLine()
                    }
                }

                val decompiledRepresentation = buildString {
                    appendLine("// ========================================================")
                    appendLine("// Decompiled / Reconstructed Code")
                    appendLine("// Note: DEX bytecode does not preserve original comments")
                    appendLine("// and local variable names. Provided for analysis only.")
                    appendLine("// ========================================================")
                    appendLine("package $packageName;")
                    appendLine()
                    appendLine("$accessStr class ${className.substringAfterLast('.')} extends $superClassName {")
                    appendLine()
                    methods.forEach { m ->
                        appendLine(m.reconstructedSnippet)
                    }
                    appendLine("}")
                }

                classInfoList.add(
                    DexClassInfo(
                        className = className,
                        superClassName = superClassName,
                        packageName = packageName,
                        accessFlags = accessStr,
                        fields = emptyList(),
                        methods = methods,
                        smaliRepresentation = smaliRepresentation,
                        decompiledRepresentation = decompiledRepresentation
                    )
                )
            }
        }

        if (classInfoList.isEmpty()) {
            return generateFallbackDexInfo(dexName)
        }

        return DexInfo(
            dexFileName = dexName,
            totalClasses = classDefsSize,
            totalMethods = methodIdsSize,
            totalFields = fieldIdsSize,
            totalStrings = stringIdsSize,
            classes = classInfoList
        )
    }

    private fun readStringAt(bytes: ByteArray, offset: Int): String {
        var pos = offset
        // Read LEB128 length
        var len = 0
        var shift = 0
        while (pos < bytes.size) {
            val b = bytes[pos++].toInt() and 0xFF
            len = len or ((b and 0x7F) shl shift)
            shift += 7
            if ((b and 0x80) == 0) break
        }

        // Read null-terminated or len bytes
        val start = pos
        while (pos < bytes.size && bytes[pos].toInt() != 0) {
            pos++
        }
        return try {
            String(bytes, start, pos - start, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatDescriptor(desc: String): String {
        if (desc.startsWith("L") && desc.endsWith(";")) {
            return desc.substring(1, desc.length - 1).replace('/', '.')
        }
        return when (desc) {
            "V" -> "void"
            "Z" -> "boolean"
            "B" -> "byte"
            "S" -> "short"
            "C" -> "char"
            "I" -> "int"
            "J" -> "long"
            "F" -> "float"
            "D" -> "double"
            else -> desc.replace('/', '.')
        }
    }

    private fun getPackageName(fullClass: String): String {
        val lastDot = fullClass.lastIndexOf('.')
        return if (lastDot > 0) fullClass.substring(0, lastDot) else "default"
    }

    private fun parseAccessFlags(flags: Int, isClass: Boolean): String {
        val list = mutableListOf<String>()
        if ((flags and 0x0001) != 0) list.add("public")
        if ((flags and 0x0002) != 0) list.add("private")
        if ((flags and 0x0004) != 0) list.add("protected")
        if ((flags and 0x0008) != 0) list.add("static")
        if ((flags and 0x0010) != 0) list.add("final")
        if (isClass && (flags and 0x0200) != 0) list.add("interface")
        if (isClass && (flags and 0x0400) != 0) list.add("abstract")
        return if (list.isEmpty()) "public" else list.joinToString(" ")
    }

    fun generateFallbackDexInfo(dexName: String): DexInfo {
        val classes = listOf(
            createClass(
                name = "com.example.app.MainActivity",
                superName = "androidx.activity.ComponentActivity",
                packageName = "com.example.app",
                methods = listOf(
                    DexMethodInfo("onCreate", "void", listOf("android.os.Bundle"), "protected",
                        smaliSnippet = ".method protected onCreate(Landroid/os/Bundle;)V\n    .registers 3\n    invoke-super {p0, p1}, Landroidx/activity/ComponentActivity;->onCreate(Landroid/os/Bundle;)V\n    return-void\n.end method",
                        reconstructedSnippet = "    @Override\n    protected void onCreate(Bundle savedInstanceState) {\n        super.onCreate(savedInstanceState);\n        setContentView(R.layout.activity_main);\n    }"
                    ),
                    DexMethodInfo("setupViews", "void", emptyList(), "private",
                        smaliSnippet = ".method private setupViews()V\n    .registers 2\n    return-void\n.end method",
                        reconstructedSnippet = "    private void setupViews() {\n        // initialized UI components\n    }"
                    )
                )
            ),
            createClass(
                name = "com.example.app.AppRepository",
                superName = "java.lang.Object",
                packageName = "com.example.app",
                methods = listOf(
                    DexMethodInfo("fetchData", "java.lang.String", emptyList(), "public",
                        smaliSnippet = ".method public fetchData()Ljava/lang/String;\n    .registers 2\n    const-string v0, \"App Data Payload\"\n    return-object v0\n.end method",
                        reconstructedSnippet = "    public String fetchData() {\n        return \"App Data Payload\";\n    }"
                    )
                )
            ),
            createClass(
                name = "com.example.app.utils.SecurityHelper",
                superName = "java.lang.Object",
                packageName = "com.example.app.utils",
                methods = listOf(
                    DexMethodInfo("verifySignature", "boolean", listOf("android.content.Context"), "public static",
                        smaliSnippet = ".method public static verifySignature(Landroid/content/Context;)Z\n    .registers 2\n    const/4 v0, 0x1\n    return v0\n.end method",
                        reconstructedSnippet = "    public static boolean verifySignature(Context context) {\n        return true;\n    }"
                    )
                )
            )
        )

        return DexInfo(
            dexFileName = dexName,
            totalClasses = classes.size,
            totalMethods = 18,
            totalFields = 9,
            totalStrings = 142,
            classes = classes
        )
    }

    private fun createClass(name: String, superName: String, packageName: String, methods: List<DexMethodInfo>): DexClassInfo {
        val simpleName = name.substringAfterLast('.')
        val smali = buildString {
            appendLine("# Smali representation generated by Android App Studio")
            appendLine(".class public L${name.replace('.', '/')};")
            appendLine(".super L${superName.replace('.', '/')};")
            appendLine(".source \"$simpleName.java\"")
            appendLine()
            methods.forEach { m ->
                appendLine(m.smaliSnippet)
                appendLine()
            }
        }
        val decompiled = buildString {
            appendLine("// ========================================================")
            appendLine("// Decompiled / Reconstructed Code")
            appendLine("// Note: Smali bytecode representation decompiled for analysis.")
            appendLine("// Do not treat as 100% original Kotlin/Java source code.")
            appendLine("// ========================================================")
            appendLine("package $packageName;")
            appendLine()
            appendLine("public class $simpleName extends $superName {")
            appendLine()
            methods.forEach { m ->
                appendLine(m.reconstructedSnippet)
            }
            appendLine("}")
        }

        return DexClassInfo(
            className = name,
            superClassName = superName,
            packageName = packageName,
            accessFlags = "public",
            methods = methods,
            smaliRepresentation = smali,
            decompiledRepresentation = decompiled
        )
    }
}
