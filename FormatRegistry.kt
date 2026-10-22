package com.example.gameassettool.formats

import com.example.gameassettool.formats.obj.ObjFormatPlugin

/**
 * Central registry. Each game/format gets an independent plugin so one parser
 * cannot accidentally interpret another game's similarly named extension.
 */
class FormatRegistry {
    private val plugins: List<ModelFormatPlugin> = listOf(
        ObjFormatPlugin()
        // Planned modules: GltfFormatPlugin, StlFormatPlugin,
        // RenderWareDffPlugin, GtaVYdrPlugin, GtaVYftPlugin, GtaVYddPlugin.
    )

    fun supportedFormats(): List<FormatInfo> = plugins.map { it.info }

    fun findReader(fileName: String): ModelFormatPlugin? =
        plugins.firstOrNull { it.canRead(fileName) }

    fun supportedExtensions(): Set<String> =
        plugins.flatMap { it.info.extensions }.toSet()
}
