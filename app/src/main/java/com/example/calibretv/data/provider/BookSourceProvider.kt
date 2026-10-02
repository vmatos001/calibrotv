package com.example.calibretv.data.provider

import com.example.calibretv.data.model.Book
import java.io.File

/**
 * 📚 CalibroTV — Interfaz Común de Proveedores de Libros (Multi-Source Pattern)
 * Desacopla la fuente de datos (Almacenamiento Local, Transferencia WiFi QR, OPDS, Nubes).
 */
interface BookSourceProvider {
    val sourceId: String
    val displayName: String
    suspend fun fetchCatalog(): List<Book>
    suspend fun resolveBookFile(book: Book): File?
    suspend fun isAvailable(): Boolean
}
