package com.example.calibretv.data.curator

import kotlinx.serialization.Serializable

/**
 * Arquetipos estéticos de curaduría de BookSpread.
 * Inspirados en la literatura clásica y figuras arquetípicas para evitar marcas registradas.
 */
enum class CuratorArchetype(
    val title: String,
    val subtitle: String,
    val iconName: String,
    val quote: String
) {
    PRODIGY(
        title = "La Estudiante Prodigio",
        subtitle = "Lecturas para expandir el intelecto y el pensamiento analítico",
        iconName = "School",
        quote = "«El conocimiento no se acumula, se comprende.»"
    ),
    DETECTIVE(
        title = "El Detective de Baker St.",
        subtitle = "Misterio, deducción y mentes brillantes frente al enigma",
        iconName = "Search",
        quote = "«Elimina lo imposible; lo que queda, por improbable que sea, debe ser la verdad.»"
    ),
    COSMIC(
        title = "El Crononauta Cósmico",
        subtitle = "Odiseas estelares, ciencia ficción y futuros distópicos",
        iconName = "Explore",
        quote = "«Miramos hacia las estrellas no por curiosidad, sino por destino.»"
    ),
    CLASSICS(
        title = "Tesoros Universales",
        subtitle = "Obras cumbre de la humanidad de descarga gratuita y libre acceso",
        iconName = "AutoStories",
        quote = "«Un libro clásico nunca termina de decir lo que tiene que decir.»"
    )
}

/**
 * Obra curada que se exhibe en los carruseles dinámicos de la cartelera.
 * Si isPublicDomain == true, se puede descargar gratis directamente a la TV.
 * Si isPublicDomain == false, muestra el código QR dinámico de compra en librería asociada.
 */
@Serializable
data class CuratedBook(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val summary: String,
    val category: String,
    val isPublicDomain: Boolean,
    val affiliateQrUrl: String? = null,
    val publicDownloadUrl: String? = null,
    val approximatePrice: String? = null,
    val rating: Float = 4.8f,
    val year: String? = null
)

/**
 * Sección de cartelera curada por un personaje o arquetipo literario.
 */
@Serializable
data class CuratorSection(
    val id: String,
    val archetype: CuratorArchetype,
    val name: String,
    val tagline: String,
    val books: List<CuratedBook>
)
