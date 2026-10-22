package com.example.gameassettool.formats.obj

import android.content.ContentResolver
import android.net.Uri
import com.example.gameassettool.formats.FormatInfo
import com.example.gameassettool.formats.ModelFormatPlugin
import com.example.gameassettool.model.ModelData
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs

/**
 * First real format module: Wavefront OBJ geometry.
 * Reads vertices and polygon faces; triangulates n-gons using a fan.
 * UVs/normals/material libraries are reported as not yet rendered.
 */
class ObjFormatPlugin : ModelFormatPlugin {
    override val info = FormatInfo(
        id = "wavefront-obj",
        displayName = "Wavefront OBJ",
        extensions = setOf("obj"),
        canImport = true
    )

    override fun canRead(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").equals("obj", ignoreCase = true)

    override fun read(resolver: ContentResolver, uri: Uri, fileName: String): ModelData {
        val stream = resolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Android tidak dapat membuka file.")
        val vertices = ArrayList<FloatArray>()
        val triangles = ArrayList<Float>()
        var sawUv = false
        var sawNormals = false
        var sawMaterials = false
        BufferedReader(InputStreamReader(stream)).useLines { lines ->
            lines.forEachIndexed { index, raw ->
                val line = raw.substringBefore('#').trim()
                if (line.isEmpty()) return@forEachIndexed
                val parts = line.split(Regex("\\s+"))
                when (parts[0]) {
                    "v" -> {
                        if (parts.size < 4) throw IllegalArgumentException("Vertex tidak lengkap di baris ${index + 1}.")
                        val x = parts[1].toFloatOrNull()
                        val y = parts[2].toFloatOrNull()
                        val z = parts[3].toFloatOrNull()
                        if (x == null || y == null || z == null) {
                            throw IllegalArgumentException("Koordinat vertex tidak valid di baris ${index + 1}.")
                        }
                        vertices.add(floatArrayOf(x, y, z))
                    }
                    "vt" -> sawUv = true
                    "vn" -> sawNormals = true
                    "mtllib", "usemtl" -> sawMaterials = true
                    "f" -> {
                        if (parts.size < 4) throw IllegalArgumentException("Face kurang dari 3 vertex di baris ${index + 1}.")
                        val ids = parts.drop(1).map { token ->
                            token.substringBefore('/').toIntOrNull()
                                ?: throw IllegalArgumentException("Indeks face tidak valid di baris ${index + 1}.")
                        }
                        val resolved = ids.map { id ->
                            val idx = if (id < 0) vertices.size + id else id - 1
                            if (idx !in vertices.indices) {
                                throw IllegalArgumentException("Indeks vertex di luar batas di baris ${index + 1}.")
                            }
                            idx
                        }
                        for (i in 1 until resolved.size - 1) {
                            listOf(resolved[0], resolved[i], resolved[i + 1]).forEach { vi ->
                                triangles.addAll(vertices[vi].toList())
                            }
                        }
                    }
                }
            }
        }
        if (triangles.isEmpty()) throw IllegalArgumentException("File OBJ tidak memiliki face yang dapat ditampilkan.")
        val result = triangles.toFloatArray()
        var maxAbs = 0f
        result.forEach { maxAbs = maxOf(maxAbs, abs(it)) }
        if (maxAbs > 0f) {
            val scale = 1.5f / maxAbs
            for (i in result.indices) result[i] *= scale
        }
        val warnings = buildList {
            if (sawUv) add("UV ditemukan, tetapi belum dipakai untuk merender tekstur.")
            if (sawNormals) add("Normal ditemukan, tetapi pencahayaan normal belum diterapkan.")
            if (sawMaterials) add("Material/MTL terdeteksi, tetapi belum diterapkan.")
        }
        return ModelData(result, "OBJ", warnings)
    }
}
