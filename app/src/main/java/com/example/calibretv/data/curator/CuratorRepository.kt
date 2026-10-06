package com.example.calibretv.data.curator

import android.content.Context
import android.util.Log
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.epub.EpubParser
import com.example.calibretv.data.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repositorio de la cartelera dinámica y curaduría de BookSpread.
 * Provee carruseles guiados por arquetipos literarios, soporte para descargas
 * directas de dominio público y enlaces QR de afiliados.
 */
object CuratorRepository {

    private const val TAG = "CuratorRepository"

    private var cachedCmsSections: List<CuratorSection>? = null
    private var cachedHeroBanner: HeroBanner? = null
    private var cachedHomeData: HomeCarteleraData? = null

    /**
     * Retorna las secciones curadas para la pantalla principal.
     * Si el CMS está sincronizado, retorna el catálogo dinámico;
     * de lo contrario, opera 100% offline con el catálogo local integrado.
     */
    fun getCuratedSections(): List<CuratorSection> {
        val cms = cachedCmsSections
        if (!cms.isNullOrEmpty()) {
            return cms
        }
        return getOfflineCuratedSections()
    }

    private const val FIRESTORE_BASE = "https://firestore.googleapis.com/v1/projects/bookspread-app-2026/databases/(default)/documents"
    private const val FIRESTORE_HERO_ES = "$FIRESTORE_BASE/cartelera_hero/main_hero_es"
    private const val FIRESTORE_HERO_EN = "$FIRESTORE_BASE/cartelera_hero/main_hero_en"
    private const val FIRESTORE_OFFERS = "$FIRESTORE_BASE/cartelera_offers"
    private const val FIRESTORE_SHELVES = "$FIRESTORE_BASE/cartelera_shelves"
    private const val FIRESTORE_SOUNDS = "$FIRESTORE_BASE/cartelera_sounds"

    // API REST Alternativa (Hosting Web de BookSpread)
    private const val CMS_WEB_BASE = "https://bookspread-app-2026.web.app"
    private const val CMS_WEB_API_HOME = "$CMS_WEB_BASE/api/v1/cartelera/home"
    private const val CMS_WEB_API_SHELVES = "$CMS_WEB_BASE/api/v1/cartelera/shelves"

    private fun org.json.JSONObject.getFsString(field: String, default: String = ""): String {
        val f = optJSONObject(field) ?: return default
        return f.optString("stringValue", default)
    }

    private fun org.json.JSONObject.getFsInt(field: String, default: Int = 0): Int {
        val f = optJSONObject(field) ?: return default
        return f.optString("integerValue", default.toString()).toIntOrNull() ?: f.optInt("integerValue", default)
    }

    private fun org.json.JSONObject.getFsBoolean(field: String, default: Boolean = false): Boolean {
        val f = optJSONObject(field) ?: return default
        return f.optBoolean("booleanValue", default)
    }

    private fun fetchJsonFromUrl(urlString: String): org.json.JSONObject? {
        return try {
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 7000
            conn.requestMethod = "GET"
            conn.connect()
            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                org.json.JSONObject(text)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchHeroFromFirestore(isSpanish: Boolean = true): HeroBanner? = withContext(Dispatchers.IO) {
        try {
            val url = if (isSpanish) FIRESTORE_HERO_ES else FIRESTORE_HERO_EN
            val json = fetchJsonFromUrl(url) ?: return@withContext null
            val fields = json.optJSONObject("fields") ?: return@withContext null
            HeroBanner(
                id = fields.getFsString("id", "main_hero"),
                title = fields.getFsString("title"),
                author = fields.getFsString("author"),
                tagline = fields.getFsString("tagline"),
                synopsis = fields.getFsString("synopsis"),
                backdropUrl = fields.getFsString("backdrop_url"),
                coverUrl = fields.getFsString("cover_url"),
                sampleEpubUrl = fields.getFsString("sample_epub_url").takeIf { it.isNotBlank() },
                affiliatePurchaseUrl = fields.getFsString("affiliate_purchase_url").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.d(TAG, "Hero banner fetch from Firestore failed: ${e.message}")
            null
        }
    }

    suspend fun fetchOffersFromFirestore(): List<BookOffer> = withContext(Dispatchers.IO) {
        try {
            val json = fetchJsonFromUrl(FIRESTORE_OFFERS) ?: return@withContext emptyList()
            val docs = json.optJSONArray("documents") ?: return@withContext emptyList()
            val list = mutableListOf<BookOffer>()
            for (i in 0 until docs.length()) {
                val d = docs.getJSONObject(i)
                val fields = d.optJSONObject("fields") ?: continue
                list.add(
                    BookOffer(
                        id = fields.getFsString("id", "offer_$i"),
                        title = fields.getFsString("title"),
                        author = fields.getFsString("author"),
                        coverUrl = fields.getFsString("cover_url"),
                        discountTag = fields.getFsString("discount_tag", "-30%"),
                        affiliateUrl = fields.getFsString("affiliate_url")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.d(TAG, "Offers fetch from Firestore failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchSoundsFromFirestore(): List<CloudAmbientSound> = withContext(Dispatchers.IO) {
        try {
            val json = fetchJsonFromUrl(FIRESTORE_SOUNDS) ?: return@withContext emptyList()
            val docs = json.optJSONArray("documents") ?: return@withContext emptyList()
            val list = mutableListOf<CloudAmbientSound>()
            for (i in 0 until docs.length()) {
                val d = docs.getJSONObject(i)
                val fields = d.optJSONObject("fields") ?: continue
                val rawUrl = fields.getFsString("stream_url")
                val fullStreamUrl = if (rawUrl.startsWith("/")) "$CMS_WEB_BASE$rawUrl" else rawUrl
                list.add(
                    CloudAmbientSound(
                        id = fields.getFsString("id", "sound_$i"),
                        name = fields.getFsString("name"),
                        streamUrl = fullStreamUrl,
                        icon = fields.getFsString("icon"),
                        isActive = fields.getFsBoolean("is_active", true),
                        type = fields.getFsString("type", "permanent")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.d(TAG, "Sounds fetch from Firestore failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchHomeCartelera(cmsUrl: String? = null, language: String = "es"): HomeCarteleraData = withContext(Dispatchers.IO) {
        try {
            val isSpanish = !language.equals("en", ignoreCase = true)
            val hero = fetchHeroFromFirestore(isSpanish = isSpanish)
            val offers = fetchOffersFromFirestore()
            if (hero != null || offers.isNotEmpty()) {
                val data = HomeCarteleraData(
                    heroBanner = hero ?: getOfflineHomeData(language).heroBanner,
                    offers = if (offers.isNotEmpty()) offers else getOfflineHomeData(language).offers
                )
                cachedHomeData = data
                return@withContext data
            }
            cachedHomeData ?: getOfflineHomeData(language)
        } catch (e: Exception) {
            Log.d(TAG, "fetchHomeCartelera failed: ${e.message}")
            cachedHomeData ?: getOfflineHomeData(language)
        }
    }

    fun getOfflineHomeData(language: String = "es"): HomeCarteleraData {
        val isEnglish = language.equals("en", ignoreCase = true)
        return HomeCarteleraData(
            heroBanner = if (isEnglish) {
                HeroBanner(
                    id = "main_hero_offline_en",
                    title = "The Three-Body Problem",
                    author = "Cixin Liu",
                    tagline = "The world-renowned sci-fi phenomenon that defies the laws of the cosmos",
                    synopsis = "Set against the backdrop of China's Cultural Revolution, a secret military project sends signals into space to establish contact with aliens, triggering an unprecedented cosmic crisis.",
                    backdropUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1920&auto=format&fit=crop",
                    coverUrl = "https://covers.openlibrary.org/b/isbn/9780765377067-L.jpg",
                    sampleEpubUrl = "https://standardebooks.org/ebooks/h-g-wells/the-war-of-the-worlds/downloads/h-g-wells_the-war-of-the-worlds.epub",
                    affiliatePurchaseUrl = "https://www.amazon.com/dp/0765382032"
                )
            } else {
                HeroBanner(
                    id = "main_hero_offline_es",
                    title = "El Problema de los Tres Cuerpos",
                    author = "Cixin Liu",
                    tagline = "El fenómeno mundial de la ciencia ficción que desafía las leyes del cosmos",
                    synopsis = "Durante la Revolución Cultural china, una señal militar secreta viaja al espacio exterior desatando una conspiración cuántica global sin precedentes.",
                    backdropUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1920&auto=format&fit=crop",
                    coverUrl = "https://covers.openlibrary.org/b/isbn/9788466659734-L.jpg",
                    sampleEpubUrl = "https://standardebooks.org/ebooks/h-g-wells/the-war-of-the-worlds/downloads/h-g-wells_the-war-of-the-worlds.epub",
                    affiliatePurchaseUrl = "https://www.amazon.es/dp/8466659730?tag=calibrotv-21"
                )
            },
            offers = listOf(
                BookOffer(
                    id = "off_1",
                    title = if (isEnglish) "Dune (60th Anniversary Edition)" else "Dune (Edición Especial 60º Aniversario)",
                    author = "Frank Herbert",
                    coverUrl = "https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1555447414i/44767458.jpg",
                    discountTag = "-45%",
                    affiliateUrl = if (isEnglish) "https://www.amazon.com/dp/0441172717" else "https://www.amazon.es/dp/8401025553?tag=calibrotv-21"
                ),
                BookOffer(
                    id = "off_2",
                    title = if (isEnglish) "Klara and the Sun" else "Klara y el Sol",
                    author = "Kazuo Ishiguro",
                    coverUrl = "https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1603206535i/54120408.jpg",
                    discountTag = "-30%",
                    affiliateUrl = if (isEnglish) "https://www.amazon.com/dp/0593318171" else "https://www.amazon.es/dp/8433999242?tag=calibrotv-21"
                )
            )
        )
    }

    fun findCuratedBook(bookId: String): CuratedBook? {
        return getCuratedSections().flatMap { it.books }.firstOrNull { it.id == bookId }
    }

    fun getAllCuratedBooks(): List<CuratedBook> {
        return getCuratedSections().flatMap { it.books }.distinctBy { it.id }
    }

    suspend fun fetchHeroBanner(cmsUrl: String? = null, language: String = "es"): HeroBanner? {
        return fetchHomeCartelera(cmsUrl, language).heroBanner
    }

    suspend fun syncWithCms(cmsUrl: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Intentar cargar estanterías de la nube desde Cloud Firestore
            val json = fetchJsonFromUrl(FIRESTORE_SHELVES)
            val docs = json?.optJSONArray("documents")
            if (docs != null && docs.length() > 0) {
                val sections = mutableListOf<CuratorSection>()
                for (i in 0 until docs.length()) {
                    val d = docs.getJSONObject(i)
                    val fields = d.optJSONObject("fields") ?: continue
                    val sId = fields.getFsString("id", "shelf_$i")
                    val name = fields.getFsString("name").ifBlank { fields.getFsString("character_name") }
                    val tagline = fields.getFsString("tagline").ifBlank { fields.getFsString("quote") }
                    val avatarUrl = fields.getFsString("avatar_url")
                    val quote = fields.getFsString("quote")
                    val archetypeStr = fields.getFsString("archetype")

                    val archetype = when {
                        archetypeStr.contains("prodigy", true) || sId.contains("prodigy") || name.contains("Lisa") || name.contains("Matilda") || name.contains("Hermione") -> CuratorArchetype.PRODIGY
                        archetypeStr.contains("detective", true) || sId.contains("detective") || name.contains("House") || name.contains("Patrick") || name.contains("Rust") -> CuratorArchetype.DETECTIVE
                        archetypeStr.contains("cosmic", true) || sId.contains("cosmic") || name.contains("Dune") || name.contains("Stark") -> CuratorArchetype.COSMIC
                        archetypeStr.contains("classic", true) || sId.contains("classic") || name.contains("Quijote") -> CuratorArchetype.CLASSICS
                        else -> CuratorArchetype.GENERAL
                    }

                    val booksList = mutableListOf<CuratedBook>()
                    val booksField = fields.optJSONObject("books")?.optJSONObject("arrayValue")?.optJSONArray("values")
                    if (booksField != null) {
                        for (j in 0 until booksField.length()) {
                            val bFields = booksField.getJSONObject(j).optJSONObject("mapValue")?.optJSONObject("fields") ?: continue
                            val storeLinksObj = bFields.optJSONObject("store_links")?.optJSONObject("mapValue")?.optJSONObject("fields")
                            val storesObj = storeLinksObj?.optJSONObject("stores")?.optJSONObject("mapValue")?.optJSONObject("fields")

                            val amazonUrl = storesObj?.optJSONObject("amazon")?.optJSONObject("mapValue")?.optJSONObject("fields")?.getFsString("url")
                                ?: storeLinksObj?.getFsString("amazon")
                                ?: bFields.getFsString("affiliate_url").takeIf { it.isNotBlank() }

                            val cdlUrl = storesObj?.optJSONObject("casadellibro")?.optJSONObject("mapValue")?.optJSONObject("fields")?.getFsString("url")
                                ?: storeLinksObj?.getFsString("casadellibro")

                            val gpUrl = storesObj?.optJSONObject("googleplay")?.optJSONObject("mapValue")?.optJSONObject("fields")?.getFsString("url")
                                ?: storeLinksObj?.getFsString("googleplay")

                            val parsedStoreLinks = if (amazonUrl != null || cdlUrl != null || gpUrl != null) {
                                BookStoreLinks(amazon = amazonUrl, casaDelLibro = cdlUrl, googlePlay = gpUrl)
                            } else null

                            booksList.add(
                                CuratedBook(
                                    id = bFields.getFsString("id", "book_${i}_$j"),
                                    title = bFields.getFsString("title"),
                                    author = bFields.getFsString("author"),
                                    coverUrl = bFields.getFsString("cover_url"),
                                    summary = bFields.getFsString("synopsis").ifBlank { bFields.getFsString("summary") },
                                    category = name,
                                    isPublicDomain = bFields.getFsBoolean("is_public_domain", false),
                                    affiliateQrUrl = bFields.getFsString("affiliate_url").takeIf { it.isNotBlank() },
                                    publicDownloadUrl = bFields.getFsString("download_url").takeIf { it.isNotBlank() },
                                    difficultyLevel = bFields.getFsInt("difficulty_level", 1),
                                    storeLinks = parsedStoreLinks
                                )
                            )
                        }
                    }

                    sections.add(
                        CuratorSection(
                            id = sId,
                            archetype = archetype,
                            name = name,
                            tagline = tagline,
                            books = booksList,
                            avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
                            quote = quote.takeIf { it.isNotBlank() }
                        )
                    )
                }
                if (sections.isNotEmpty()) {
                    cachedCmsSections = sections
                    Log.d(TAG, "Sincronizadas ${sections.size} estanterías desde Cloud Firestore")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            Log.d(TAG, "Cloud Firestore shelves sync error: ${e.message}")
            false
        }
    }

    fun getOfflineCuratedSections(): List<CuratorSection> {
        return listOf(
            CuratorSection(
                id = "curator_classics",
                archetype = CuratorArchetype.CLASSICS,
                name = CuratorArchetype.CLASSICS.title,
                tagline = CuratorArchetype.CLASSICS.subtitle,
                books = listOf(
                    CuratedBook(
                        id = "pub_principito",
                        title = "El Principito",
                        author = "Antoine de Saint-Exupéry",
                        coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80",
                        summary = "Un piloto perdido en el desierto del Sahara encuentra a un pequeño príncipe de otro planeta. Una fábula poética sobre el amor, la amistad y el sentido de la vida.",
                        category = "Clásico Universal",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/70114.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1943"
                    ),
                    CuratedBook(
                        id = "pub_orgullo",
                        title = "Orgullo y Prejuicio",
                        author = "Jane Austen",
                        coverUrl = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600&auto=format&fit=crop&q=80",
                        summary = "La brillante Elizabeth Bennet y el orgulloso Fitzwilliam Darcy deben superar sus prejuicios para descubrir la verdad de sus sentimientos en la Inglaterra georgiana.",
                        category = "Novela Clásica",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/1342.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1813"
                    ),
                    CuratedBook(
                        id = "pub_quijote",
                        title = "Don Quijote de la Mancha",
                        author = "Miguel de Cervantes",
                        coverUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80",
                        summary = "Las inmortales andanzas del hidalgo manchego y su fiel escudero Sancho Panza en pos de la justicia, la caballería andante y la libertad del espíritu.",
                        category = "Obra Cumbre",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/2000.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1605"
                    ),
                    CuratedBook(
                        id = "pub_metamorfosis",
                        title = "La Metamorfosis",
                        author = "Franz Kafka",
                        coverUrl = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop&q=80",
                        summary = "Una mañana, tras un sueño intranquilo, Gregorio Samsa se despierta convertido en un monstruoso insecto. El retrato más sobrecogedor de la alienación humana.",
                        category = "Ficción Existencial",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/5200.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1915"
                    )
                )
            ),
            CuratorSection(
                id = "curator_detective",
                archetype = CuratorArchetype.DETECTIVE,
                name = CuratorArchetype.DETECTIVE.title,
                tagline = CuratorArchetype.DETECTIVE.subtitle,
                books = listOf(
                    CuratedBook(
                        id = "pub_holmes_study",
                        title = "Estudio en Escarlata",
                        author = "Arthur Conan Doyle",
                        coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
                        summary = "La primera aventura donde se cruzan los destinos de Sherlock Holmes y el Dr. John Watson, desentrañando un misterioso crimen con la palabra RACHE escrita en sangre.",
                        category = "Misterio",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/244.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1887"
                    ),
                    CuratedBook(
                        id = "pub_holmes_baskerville",
                        title = "El Sabueso de los Baskerville",
                        author = "Arthur Conan Doyle",
                        coverUrl = "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=600&auto=format&fit=crop&q=80",
                        summary = "Una maldición familiar y una bestia demoníaca acechan en los sombríos páramos de Dartmoor. Una de las mayores obras maestras del suspense.",
                        category = "Suspense Gótico",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/2852.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1902"
                    ),
                    CuratedBook(
                        id = "com_orient_express",
                        title = "Asesinato en el Orient Express",
                        author = "Agatha Christie",
                        coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80",
                        summary = "El tren más lujoso del mundo queda atrapado por la nieve. Un pasajero yace apuñalado y Hércules Poirot debe descubrir al culpable antes de que vuelva a actuar.",
                        category = "Novela Policial",
                        isPublicDomain = false,
                        affiliateQrUrl = "https://www.amazon.es/dp/8467045437?tag=calibrotv-21",
                        approximatePrice = "10,95 €",
                        year = "1934",
                        difficultyLevel = 4,
                        storeLinks = BookStoreLinks(
                            amazon = "https://www.amazon.es/dp/8467045437?tag=calibrotv-21",
                            casaDelLibro = "https://www.casadellibro.com/libro-asesinato-en-el-orient-express/9788467045437/2585474",
                            googlePlay = "https://play.google.com/store/search?q=Asesinato+en+el+Orient+Express+Agatha+Christie&c=books"
                        )
                    )
                )
            ),
            CuratorSection(
                id = "curator_cosmic",
                archetype = CuratorArchetype.COSMIC,
                name = CuratorArchetype.COSMIC.title,
                tagline = CuratorArchetype.COSMIC.subtitle,
                books = listOf(
                    CuratedBook(
                        id = "pub_guerra_mundos",
                        title = "La Guerra de los Mundos",
                        author = "H. G. Wells",
                        coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80",
                        summary = "Cilindros de metal caen del cielo y trípodes gigantescos siembran el pánico con rayos calóricos y humo negro. La invasión marciana definitiva.",
                        category = "Ciencia Ficción",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/36.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1898",
                        difficultyLevel = 1
                    ),
                    CuratedBook(
                        id = "pub_viaje_luna",
                        title = "De la Tierra a la Luna",
                        author = "Julio Verne",
                        coverUrl = "https://images.unsplash.com/photo-1532693322450-2cb5c511067d?w=600&auto=format&fit=crop&q=80",
                        summary = "Tras la Guerra de Secesión, los artilleros del Gun-Club de Baltimore emprenden el desafío de disparar un proyectil tripulado hacia la Luna.",
                        category = "Aventura Científica",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/83.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "1865",
                        difficultyLevel = 1
                    ),
                    CuratedBook(
                        id = "com_dune",
                        title = "Dune",
                        author = "Frank Herbert",
                        coverUrl = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=600&auto=format&fit=crop&q=80",
                        summary = "En el planeta desértico Arrakis, la codicia por la especia melange desata una guerra dinástica que forjará el destino del joven Paul Atreides.",
                        category = "Ciencia Ficción Épica",
                        isPublicDomain = false,
                        affiliateQrUrl = "https://www.amazon.es/dp/8466353774?tag=calibrotv-21",
                        approximatePrice = "12,95 €",
                        year = "1965",
                        difficultyLevel = 5,
                        storeLinks = BookStoreLinks(
                            amazon = "https://www.amazon.es/dp/8466353774?tag=calibrotv-21",
                            casaDelLibro = "https://www.casadellibro.com/libro-dune/9788466353779/11690045",
                            googlePlay = "https://play.google.com/store/search?q=Dune+Frank+Herbert&c=books"
                        )
                    )
                )
            ),
            CuratorSection(
                id = "curator_prodigy",
                archetype = CuratorArchetype.PRODIGY,
                name = CuratorArchetype.PRODIGY.title,
                tagline = CuratorArchetype.PRODIGY.subtitle,
                books = listOf(
                    CuratedBook(
                        id = "pub_arte_guerra",
                        title = "El Arte de la Guerra",
                        author = "Sun Tzu",
                        coverUrl = "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=600&auto=format&fit=crop&q=80",
                        summary = "Tratado militar milenario sobre estrategia, liderazgo y psicología. El mayor arte consiste en someter al enemigo sin librar batalla.",
                        category = "Estrategia y Filosofía",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/132.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "Siglo V a.C.",
                        difficultyLevel = 1
                    ),
                    CuratedBook(
                        id = "pub_meditaciones",
                        title = "Meditaciones",
                        author = "Marco Aurelio",
                        coverUrl = "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=600&auto=format&fit=crop&q=80",
                        summary = "Pensamientos íntimos del emperador filósofo sobre la virtud estoica, la serenidad interior y el gobierno de uno mismo ante la adversidad.",
                        category = "Filosofía Estoica",
                        isPublicDomain = true,
                        publicDownloadUrl = "https://www.gutenberg.org/ebooks/2680.epub.images",
                        approximatePrice = "Gratis (Dominio Público)",
                        year = "180 d.C.",
                        difficultyLevel = 1
                    ),
                    CuratedBook(
                        id = "com_pensar_rapido",
                        title = "Pensar Rápido, Pensar Despacio",
                        author = "Daniel Kahneman",
                        coverUrl = "https://images.unsplash.com/photo-1499209974431-9dddcece7f88?w=600&auto=format&fit=crop&q=80",
                        summary = "Un recorrido magistral por la mente humana a través del Sistema 1 (rápido e intuitivo) y el Sistema 2 (lento y reflexivo) por el Premio Nobel de Economía.",
                        category = "Psicología y Decisión",
                        isPublicDomain = false,
                        affiliateQrUrl = "https://www.amazon.es/dp/8483068613?tag=calibrotv-21",
                        approximatePrice = "19,90 €",
                        year = "2011",
                        difficultyLevel = 4,
                        storeLinks = BookStoreLinks(
                            amazon = "https://www.amazon.es/dp/8483068613?tag=calibrotv-21",
                            casaDelLibro = "https://www.casadellibro.com/libro-pensar-rapido-pensar-despacio/9788499922072/1993433",
                            googlePlay = "https://play.google.com/store/search?q=Pensar+rapido+pensar+despacio+Daniel+Kahneman&c=books"
                        )
                    )
                )
            )
        )
    }

    /**
     * Comprueba si una obra curada ya se encuentra descargada físicamente en el almacenamiento local de la TV.
     */
    fun isBookDownloaded(bookId: String, repository: BookRepository): Boolean {
        // 1. Archivo descargado en filesDir
        val epubFile = java.io.File(repository.context.filesDir, "book_$bookId.epub")
        if (epubFile.exists() && epubFile.length() > 0) return true
        val pdfFile = java.io.File(repository.context.filesDir, "book_$bookId.pdf")
        if (pdfFile.exists() && pdfFile.length() > 0) return true
        val cbzFile = java.io.File(repository.context.filesDir, "book_$bookId.cbz")
        if (cbzFile.exists() && cbzFile.length() > 0) return true

        // 2. Si existe en la base de datos local y su ruta apunta a un archivo físico local existente
        val localBook = repository.getCachedBooks().find { it.id == bookId }
        if (localBook != null && !localBook.epubUrl.isNullOrBlank() && !localBook.epubUrl.startsWith("http://", ignoreCase = true) && !localBook.epubUrl.startsWith("https://", ignoreCase = true)) {
            val f = java.io.File(localBook.epubUrl.removePrefix("file://"))
            if (f.exists() && f.length() > 0) return true
        }

        // 3. Comprobar en cacheDir
        val cacheEpub = java.io.File(repository.context.cacheDir, "book_${bookId.hashCode()}.epub")
        if (cacheEpub.exists() && cacheEpub.length() > 0) return true

        return false
    }

    /**
     * Descarga un libro de dominio público directamente al almacenamiento local de la TV
     * y lo registra de inmediato en la base de datos Room.
     */
    suspend fun downloadPublicDomainBook(
        context: Context,
        book: CuratedBook,
        repository: BookRepository
    ): Result<Book> = withContext(Dispatchers.IO) {
        try {
            val destFile = File(context.filesDir, "book_${book.id}.epub")
            val tempFile = File(context.filesDir, "book_${book.id}.epub.tmp")

            val targetUrl = book.publicDownloadUrl?.ifBlank { null }
                ?: repository.getCachedBooks().find { it.id == book.id }?.epubUrl?.ifBlank { null }
                ?: return@withContext Result.failure(Exception("No hay enlace de descarga disponible para este libro"))

            var downloaded = false
            var currentUrl = targetUrl
            var redirects = 0
            val config = repository.getServerConfig()

            while (redirects < 6) {
                val urlObj = URL(currentUrl)
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 30000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 CalibreTV/1.0")
                conn.setRequestProperty("Accept", "application/epub+zip, application/octet-stream, */*")

                if (config.serverUrl.isNotBlank() && currentUrl.startsWith(config.serverUrl)) {
                    com.example.calibretv.data.image.CoverLoader.buildBasicAuth(config.username, config.password)?.let {
                        conn.setRequestProperty("Authorization", it)
                    }
                }

                conn.connect()
                val code = conn.responseCode

                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!loc.isNullOrBlank()) {
                        currentUrl = if (loc.startsWith("http://") || loc.startsWith("https://")) loc else URL(urlObj, loc).toString()
                        redirects++
                        continue
                    }
                }

                if (code in 200..299) {
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    conn.disconnect()

                    // Verificar que el archivo descargado sea un ZIP/EPUB genuino (empieza por 'PK') y tamaño > 1KB
                    if (tempFile.exists() && tempFile.length() > 1024) {
                        val header = ByteArray(2)
                        java.io.FileInputStream(tempFile).use { fis ->
                            fis.read(header)
                        }
                        if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) {
                            if (destFile.exists()) destFile.delete()
                            if (tempFile.renameTo(destFile)) {
                                downloaded = true
                            }
                        } else {
                            tempFile.delete()
                            Log.w(TAG, "El archivo descargado no es un EPUB válido (no coincide firma PK)")
                        }
                    } else {
                        tempFile.delete()
                    }
                } else {
                    conn.disconnect()
                }
                break
            }

            if (!downloaded) {
                if (tempFile.exists()) tempFile.delete()
                return@withContext Result.failure(Exception("No se pudo descargar el archivo EPUB. Comprueba tu conexión a internet."))
            }

            val newBook = Book(
                id = book.id,
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                epubUrl = destFile.absolutePath,
                summary = book.summary,
                category = book.category,
                tags = listOf("Dominio Público", "Curaduría", book.category)
            )

            val current = repository.getCachedBooks().toMutableList()
            val existingIdx = current.indexOfFirst { it.id == book.id }
            if (existingIdx >= 0) {
                current[existingIdx] = newBook
            } else {
                current.add(0, newBook)
            }
            repository.saveCachedBooks(current)
            Result.success(newBook)
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando libro de dominio público", e)
            Result.failure(e)
        }
    }
}
