package com.example.calibretv.data.curator

import kotlinx.serialization.Serializable

/**
 * Arquetipos estéticos de curaduría de BookSpread / CalibroTV.
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
    ),
    GENERAL(
        title = "Curaduría Especial",
        subtitle = "Selección literaria destacada",
        iconName = "Book",
        quote = "«Un buen libro es un amigo que nunca da la espalda.»"
    )
}

/**
 * Tiendas oficiales para conversión comercial en TV.
 */
enum class CommercialStoreType(
    val storeName: String,
    val iconEmoji: String,
    val brandHex: Long
) {
    AMAZON("Amazon (Físico / Kindle)", "🟠", 0xFFFF9900),
    CASA_DEL_LIBRO("Casa del Libro (España / LATAM)", "🟢", 0xFF22C55E),
    GOOGLE_PLAY("Google Play Books (Móvil / Tablet)", "🔵", 0xFF3B82F6)
}

/**
 * Enlaces específicos a tiendas comerciales (parsed desde Cloud Firestore o CMS).
 */
@Serializable
data class BookStoreLinks(
    val amazon: String? = null,
    val casaDelLibro: String? = null,
    val googlePlay: String? = null
)

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
    val year: String? = null,
    val difficultyLevel: Int = 1,
    val storeLinks: BookStoreLinks? = null
) {
    /**
     * Retorna la URL oficial para la tienda seleccionada con fallback inteligente a búsqueda directa.
     */
    fun getStoreUrl(store: CommercialStoreType): String {
        val query = try {
            java.net.URLEncoder.encode("$title $author", "UTF-8")
        } catch (_: Exception) {
            title.replace(" ", "+")
        }

        return when (store) {
            CommercialStoreType.AMAZON -> {
                storeLinks?.amazon?.takeIf { it.isNotBlank() }
                    ?: affiliateQrUrl?.takeIf { it.isNotBlank() }
                    ?: "https://www.amazon.es/s?k=$query&tag=calibrotv-21"
            }
            CommercialStoreType.CASA_DEL_LIBRO -> {
                storeLinks?.casaDelLibro?.takeIf { it.isNotBlank() }
                    ?: "https://www.casadellibro.com/libros?q=$query"
            }
            CommercialStoreType.GOOGLE_PLAY -> {
                storeLinks?.googlePlay?.takeIf { it.isNotBlank() }
                    ?: "https://play.google.com/store/search?q=$query&c=books"
            }
        }
    }
}

/**
 * Sección de cartelera curada por un personaje o arquetipo literario.
 */
@Serializable
data class CuratorSection(
    val id: String,
    val archetype: CuratorArchetype,
    val name: String,
    val tagline: String,
    val books: List<CuratedBook>,
    val avatarUrl: String? = null,
    val quote: String? = null
)

/**
 * Hero Banner principal (Bestseller del Mes en 16:9).
 */
@Serializable
data class HeroBanner(
    val id: String,
    val title: String,
    val author: String,
    val tagline: String,
    val synopsis: String,
    val backdropUrl: String,
    val coverUrl: String,
    val sampleEpubUrl: String? = null,
    val affiliatePurchaseUrl: String? = null
)

/**
 * Oferta comercial destacada en el carrusel de la Home.
 */
@Serializable
data class BookOffer(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val discountTag: String,
    val affiliateUrl: String
)

@Serializable
data class HomeCarteleraData(
    val heroBanner: HeroBanner? = null,
    val offers: List<BookOffer> = emptyList()
)

/**
 * Paisaje sonoro ambiental desde Cloud Firestore (cartelera_sounds).
 */
@Serializable
data class CloudAmbientSound(
    val id: String,
    val name: String,
    val streamUrl: String,
    val icon: String = "",
    val isActive: Boolean = true,
    val type: String = "permanent"
)

