package com.example.gameassettool.formats

import android.content.ContentResolver
import android.net.Uri
import com.example.gameassettool.model.ModelData

data class FormatInfo(
    val id: String,
    val displayName: String,
    val extensions: Set<String>,
    val canImport: Boolean,
    val canExportDff: Boolean = false,
    val canExportTxd: Boolean = false
)

interface ModelFormatPlugin {
    val info: FormatInfo
    fun canRead(fileName: String): Boolean
    fun read(resolver: ContentResolver, uri: Uri, fileName: String): ModelData
}
