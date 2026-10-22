package com.example.gameassettool.model

/**
 * Format-neutral mesh. Coordinates are stored as triangle-list positions.
 * Optional UVs/normals are reserved for future format modules.
 */
data class ModelData(
    val positions: FloatArray,
    val sourceFormat: String,
    val warnings: List<String> = emptyList()
) {
    val triangleCount: Int get() = positions.size / 9
}
