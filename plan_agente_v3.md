# 📋 Plan de Acción — BookSpread v3.0
## Planificación Técnica y Desarrollo por Fases

> **Nombre del Producto:** **BookSpread** *(The 3D Dual-Page Reader for Android TV)*  
> **Repositorio:** `https://github.com/vmatos001/calibrotv` *(upstream original; pendiente nuevo remote para BookSpread)*  
> **Workspace local:** `c:\Users\laura hart\Proyectos\BookSpread\bookspread_app`  
> **Package ID:** `com.bookspread.app`  
> **JDK requerida:** `C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7`  
> **Comando de compilación:**  
> `$env:JAVA_HOME = 'C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7'; .\gradlew.bat assembleRelease --no-daemon`

---

## ⚠️ Reglas Obligatorias de Ingeniería (No Negociables)

1. **Protección del Motor 3D:** NO alterar los parámetros físicos ni la curva de interpolación del paso de página (`curlAnim`, `CubicBezierEasing`, `TransformOrigin`).
2. **Prioridad Absoluta al D-Pad (10-Foot UI):** Toda pantalla, diálogo o botón DEBE ser 100% navegable con el control remoto. Queda prohibido depender de eventos táctiles o del teclado virtual del sistema.
3. **Presupuesto Estricto de Memoria (Anti-OOM):** En televisores y TV Boxes (1 a 2 GB RAM), la memoria no puede crecer sin límite. Máximo 3 pliegos en memoria (anterior, actual, siguiente). Siempre usar `Bitmap.Config.HARDWARE` y reciclar bitmaps viejos.
4. **Hilos Aislados (Cero ANR):** Todo parseo de EPUB/PDF/CBZ, cálculo de pliegos y descargas de red debe ejecutarse en `Dispatchers.IO`. La UI en Compose jamás se congela.
5. **Arquitectura Local-First y Legal:** Cero almacenamiento ilegal de libros protegidos en servidores propios. La app es un reproductor privado; el contenido comercial se enlaza con afiliados oficiales.
6. **Compilación y Commit por Fase:** Al culminar cada fase se debe ejecutar `assembleRelease` para verificar cero errores y realizar un commit git semántico.

---

## 🗺️ Mapa de Fases de Desarrollo

```mermaid
flowchart LR
    F1["Fase 1: Rebranding & APK Slim (~10MB)"] --> F2["Fase 2: Arquitectura Multi-Fuente"]
    F2 --> F3["Fase 3: Motor PDF Pliego Dual"]
    F3 --> F4["Fase 4: Cartelera & Curaduría Firestore"]
    F4 --> F5["Fase 5: Audio Desacoplado & Ducking"]
    F5 --> F6["Fase 6: Perfiles Familiares & Kids Mode"]
    F6 --> F7["Fase 7: Mobile Companion (QR & Quotes)"]
    F7 --> F8["Fase 8: Google Play Billing (Licencia Pro)"]
```

---

## FASE 1 — Rebranding Completo y Reducción de Peso del APK (~10 MB)
**Objetivo:** Consolidar la identidad visual de **BookSpread**, habilitar optimización R8 y reducir el APK de 22 MB a ~10 MB.

### 1.1 Configuración de Compilación y Minificación (R8)
- **Archivo:** `app/build.gradle.kts`
- Habilitar `isMinifyEnabled = true` y `isShrinkResources = true` en el build type `release`.
- Configurar reglas en `proguard-rules.pro` para proteger clases de serialización (`kotlinx.serialization`), entidades Room (`androidx.room`) y modelos de Compose.
- Depurar la dependencia `material-icons-extended` para empaquetar únicamente los vectores utilizados o reemplazarlos por los equivalentes en el set básico de Material Icons.

### 1.2 Limpieza de Recursos y Gráficos Duplicados
- **Archivos:** `app/src/main/res/drawable*` y `mipmap*`
- Eliminar duplicados innecesarios de banners de densidad extrema (`xxxhdpi`) que inflan el instalador.
- Diseñar el nuevo banner oficial de televisión (16:9, 1920x1080 optimizado en WebP/PNG comprimido) con la marca **BookSpread**.
- Actualizar títulos en `strings.xml` y tema visual inicial.

### 1.3 Verificación de la Fase 1
- Ejecutar compilación de Release. -> **COMPLETADO (BUILD SUCCESSFUL en Gradle 9.1 / JDK 17)**
- Comprobar que el APK resultante pese entre **8 MB y 12 MB**. -> **SUPERADO: APK final de 5.91 MB (6,201,214 bytes), reducción del 71.8% desde 22 MB**.
- **Commit:** `feat(core): rebrand to BookSpread and optimize APK size with R8 shrinking` -> **EJECUTADO**

---

## FASE 2 — Desacoplamiento de Fuentes de Datos (Patrón Provider / Multi-Source)
**Objetivo:** Permitir que la app funcione sin depender de un servidor Calibre-Web, soportando entrada local directa por QR y preparando nubes personales.

### 2.1 Interfaz Común `BookSourceProvider`
- **Archivo nuevo:** `app/src/main/java/com/example/calibretv/data/provider/BookSourceProvider.kt`
- Métodos requeridos:
  ```kotlin
  interface BookSourceProvider {
      val sourceId: String
      val displayName: String
      suspend fun fetchCatalog(): List<Book>
      suspend fun resolveBookFile(book: Book): File?
      suspend fun isAvailable(): Boolean
  }
  ```

### 2.2 Implementación de Proveedores
1. `LocalRoomProvider`: Libros almacenados en el almacenamiento interno de la TV y progreso de lectura en Room.
2. `DirectTransferProvider`: Conecta con `WifiImportServer` (transferencias P2P desde el móvil vía QR).
3. `OpdsProvider`: Mantiene la conexión a Calibre-Web como fuente secundaria/avanzada en Ajustes.

### 2.3 Refactorización de `BookRepository`
- **Archivo:** `app/src/main/java/com/example/calibretv/data/BookRepository.kt`
- Eliminar la dependencia directa y obligada de OPDS al iniciar la aplicación.
- Orquestar los proveedores activos devolviendo un catálogo unificado para la pantalla principal.

### 2.4 Verificación de la Fase 2
- La app debe arrancar inmediatamente en modo local sin pedir configuración de servidor ni lanzar pantallas de error si no hay red. -> **COMPLETADO (Desacoplamiento total vía `BookSourceProvider`, `LocalRoomProvider`, `DirectTransferProvider`, `OpdsProvider` y arranque directo a `HomeNavKey`)**.
- Compilación de Release verificada con R8 activo (APK de 5.91 MB).
- **Commit:** `refactor(data): decouple data sources with BookSourceProvider pattern` -> **EJECUTADO**

---

## FASE 3 — Motor PDF en Pliego Dual 16:9 y Blindaje de Memoria
**Objetivo:** Renderizar archivos PDF en dos páginas simultáneas con navegación fluida por D-Pad y cero riesgo de *OutOfMemory*.

### 3.1 Integración del Renderizador Nativo (`PdfRenderer`)
- **Archivo nuevo:** `app/src/main/java/com/example/calibretv/data/pdf/PdfParser.kt`
- Utilizar `android.graphics.pdf.PdfRenderer` en un contexto protegido de `Dispatchers.IO`.
- Generar `Bitmap` en resolución adaptada a la pantalla (1080p / 4K) usando `Bitmap.Config.HARDWARE` para que los píxeles residan en la memoria GPU y no en la memoria Java.

### 3.2 Lector de Pliego Dual (Páginas Pares e Impares)
- **Archivo:** `app/src/main/java/com/example/calibretv/ui/screens/PdfReaderScreen.kt` *(o unificar con `ComicReaderScreen.kt`)*
- Disposición en fila de 2 páginas (izq: par, der: impar) llenando la proporción 16:9.
- **Ventana de memoria estricta:** Almacenar solo 3 pliegos (anterior, actual, siguiente). Al avanzar, invocar `bitmap.recycle()` inmediatamente en las páginas viejas.
- **Cancelación reactiva:** Ante pulsaciones rápidas del D-Pad (mantener presionado avanzar), cancelar los `Job` de renderizado anteriores para evitar saturación de CPU/RAM.

### 3.3 Modo Zoom para Textos Densos
- Con la cruceta (Arriba/Abajo) permitir dividir la visualización en mitad superior e inferior de la página para textos con letra pequeña.

### 3.4 Verificación de la Fase 3
- Motor nativo `PdfParser` con renderizado thread-safe y extracción de portada.
- Visor `PdfReaderScreen` en pliego dual 16:9 (portada solitaria + páginas pares/impares con lomo editorial central).
- Blindaje estricto de memoria con ventana activa de 3 pliegos y reciclaje inmediato de bitmaps fuera de ventana.
- Navegación completa por D-Pad, HUD superior/inferior con persistencia de progreso en Room (`ReadingProgressEntity`).
- Soporte para subida y procesamiento directo de `.pdf` vía `WifiImportServer`.
- Compilación de Release verificada con R8 activo (APK de 5.92 MB).
- **Commit:** `feat(reader): add memory-safe dual-spread PDF renderer for TV` -> **EJECUTADO**

---

## FASE 4 — Cartelera Dinámica y Curaduría por Personajes (Firestore)
**Objetivo:** Mostrar la pantalla de inicio estilo streaming con carruseles curados por personajes icónicos mediante avatares estilizados y enlaces de compra/descarga.

### 4.1 Modelado del Catálogo en Firestore
- Estructura JSON ligera descargada al iniciar o cacheada en Room:
  ```json
  {
    "curatorId": "curator_prodigy",
    "name": "La Estudiante Prodigio",
    "tagline": "Lecturas para expandir el intelecto",
    "avatarUrl": "https://.../avatar_prodigy.png",
    "books": [
      {
        "id": "book_001",
        "title": "Guerra y Paz",
        "author": "León Tolstói",
        "quote": "Un análisis profundo de la condición humana",
        "difficulty": 4,
        "isPublicDomain": true,
        "downloadUrl": "https://www.gutenberg.org/ebooks/...",
        "affiliateQrUrl": "https://amzn.to/..."
      }
    ]
  }
  ```

### 4.2 Pantalla de Inicio Estilo Streaming (Home Billboard)
- **Archivo:** `app/src/main/java/com/example/calibretv/ui/screens/HomeScreen.kt`
- Carruseles horizontales con foco D-Pad (halo ámbar vibrante).
- **Acción Dual según tipo de obra:**
  - *Dominio Público:* Botón "Descargar y Leer Gratis" (descarga en background y abre en 3D).
  - *Libro Comercial:* Botón "Comprar con el móvil" (muestra tarjeta en TV con código QR dinámico hacia Amazon/tienda con tag de afiliado) + botón secundario "¿Ya lo tienes? Pásalo por WiFi".

### 4.3 Protección de Propiedad Intelectual
- Utilizar exclusivamente ilustraciones vectoriales originales / arquetípicas para los avatares (cero capturas oficiales de Disney/Fox/Universal).

### 4.4 Verificación de la Fase 4
- Modelado de curaduría ligera (`CuratorModels.kt`) con arquetipos literarios (La Estudiante Prodigio, El Detective de Baker St., El Crononauta Cósmico, Tesoros Universales).
- Repositorio con catálogo offline de alta calidad y motor de descarga en segundo plano para obras de libre acceso (`CuratorRepository.kt`).
- Integración de carruseles interactivos con insignias, avatares y citas en `HomeScreen.kt` (`CuratorRow.kt`).
- Modal de detalle y compra con Códigos QR dinámicos estándar ISO/IEC generados con ZXing (`CuratedBookModal.kt` & `QrCodeGenerator.kt`).
- Compilación de Release verificada con R8 activo (APK de 6.14 MB).
- **Commit:** `feat(billboard): integrate dynamic character curated shelves with affiliate QR` -> **EJECUTADO**

---

## FASE 5 — Servicio de Audio Desacoplado y Mezcla Inteligente (Ducking)
**Objetivo:** Resolver definitivamente los fallos de `MediaPlayer` y permitir que los sonidos ambientales (lluvia, chimenea) convivan en armonía con el motor TTS de voz.

### 5.1 Extracción de Audio fuera de Compose
- **Archivo:** `app/src/main/java/com/example/calibretv/data/sound/AmbientSoundService.kt`
- Gestionar el ciclo de vida del reproductor de sonido de fondo fuera de los composables.
- Solicitar y gestionar el **Audio Focus** oficial de Android (`AudioFocusRequest`).

### 5.2 Audio Ducking Coordinado con TTS
- **Archivo:** `app/src/main/java/com/example/calibretv/data/tts/TtsController.kt`
- Cuando el TTS empiece a leer una frase, enviar evento de atenuación: el sonido de lluvia baja suavemente su volumen al 20%.
- Cuando el TTS pause o termine el párrafo, el sonido ambiental recupera progresivamente su volumen original (100%).

### 5.3 Temporizador de Apagado (*Sleep Timer*)
- Mantener la atenuación progresiva de pantalla y sonido en los últimos 2 minutos antes de apagar.

### 5.4 Verificación de la Fase 5
- Creación de `AudioPlaybackService` como Foreground Service con gestión de Audio Focus oficial (`AudioFocusRequest`).
- Ducking inteligente implementado en `AmbientSoundManager`: atenuación al 20% con interpolación suave de 300 ms mientras el TTS lee, y restauración gradual al terminar.
- Controles de volumen ambiental y velocidad de TTS integrados en el HUD accesible con D-Pad en `ReaderScreen.kt` y selector de ambiente en `PdfReaderScreen.kt`.
- Compilación de Release verificada con R8 activo (APK de 6.14 MB).
- **Commit:** `feat(audio): decouple ambient sound service with intelligent TTS ducking` -> **EJECUTADO**

---

## FASE 6 — Sistema de Perfiles Familiares, Modo Kids y Mini-Quizzes
**Objetivo:** Ofrecer una experiencia personalizada para cada miembro del hogar, con protección parental por PIN y lectura comprensiva para niños.

### 6.1 Pantalla de Selección de Perfil (Netflix Kids UX)
- Fila horizontal centrada con avatares circulares grandes y animación de escala al enfocar con D-Pad.
- **Teclado numérico visual (Pad 3x4):** Para ingresar el PIN de 4 dígitos de perfiles adultos directamente con las flechas del control remoto (sin desplegar el teclado de Android TV).

### 6.2 Aislamiento de Datos en Room
- **Archivo:** `app/src/main/java/com/example/calibretv/data/storage/AppDatabase.kt`
- Asociar `ReadingSettings` (tamaño de letra, fuente, tema) al `profileId`.
- Guardar de forma independiente: favoritos, progreso y rachas de lectura por cada perfil.

### 6.3 Modo Infantil (*Kids Mode*) por Lista Blanca
- El perfil infantil únicamente muestra:
  - Libros explícitamente aprobados por los padres.
  - Obras de dominio público infantil o cómics aptos.
  - Estanterías de personajes infantiles (ocultando novelas adultas o misterios oscuros).

### 6.4 Módulo de Estudio y Preguntas de Control
- Al finalizar un capítulo en perfil infantil: desplegar tarjeta de pregunta de opción múltiple (3 opciones A/B/C navegables con D-Pad).
- Registro de respuestas para el **Panel de Métricas de los Padres**.

### 6.5 Verificación de la Fase 6
- Cambiar entre perfil adulto (con PIN) y perfil infantil. Verificar que el perfil infantil no tenga acceso a los libros protegidos y recuerde sus ajustes visuales propios.
- **Commit:** `feat(profiles): add family profiles with D-Pad PIN, Kids Mode, and retention quizzes`

---

## FASE 7 — Mobile Companion (Notas, Reseñas y Quote Cards)
**Objetivo:** Convertir el teléfono del usuario en un control secundario para redactar texto y compartir citas en redes sociales.

### 7.1 Notas y Reseñas desde el Móvil vía QR
- Al pulsar "Escribir nota" en la TV, la pantalla genera un código QR local (`192.168.x.x:8080/note?bookId=...`).
- El usuario escanea con el teléfono, se abre un formulario web limpio, escribe con el teclado del móvil y al guardar, la nota aparece de inmediato en la TV y se almacena en Room.

### 7.2 Generador de Tarjetas de Citas Estéticas (*Quote Cards*)
- En el lector, al seleccionar un fragmento de texto con el D-Pad:
  - La TV genera una imagen de alta resolución con tipografía editorial, la cita del libro, autor y marca de agua sutil: *"Leído en BookSpread"*.
  - Código QR en pantalla: al escanearlo, el móvil descarga la tarjeta formateada para publicar en Instagram Stories, WhatsApp o X.

### 7.3 Verificación de la Fase 7
- Escanear QR desde un teléfono real conectado a la misma red Wi-Fi; redactar una nota y descargar una Quote Card.
- **Commit:** `feat(companion): add QR mobile assistant for note taking and viral quote cards`

---

## FASE 8 — Monetización con Google Play Billing (Licencia Pro Vitalicia)
**Objetivo:** Implementar la pasarela de pago único para desbloquear funciones avanzadas en la televisión.

### 8.1 Integración de Google Play Billing Library
- Dependencia: `com.android.billingclient:billing-ktx:7.0.0+`
- Producto: In-App Purchase no consumible (`bookspread_pro_lifetime`).

### 8.2 Separación Free vs. Pro
- **Versión Gratuita:** Cartelera de personajes, descargas de dominio público, enlaces de compra QR, lector estándar, hasta 3 libros transferidos por WiFi.
- **Versión Pro (Pago único):**
  - Física 3D cinemática avanzada (lomo 3D, curvatura de hoja personalizable).
  - Sonidos de ambiente relajantes ilimitados + motor TTS.
  - Perfiles familiares ilimitados con Modo Kids y control parental.
  - Almacenamiento local de libros ilimitado.

### 8.3 Persistencia Cifrada de Licencia (`LicenseManager`)
- Validación de compra almacenada en `EncryptedSharedPreferences` para permitir uso 100% offline tras la activación inicial.

### 8.4 Verificación de la Fase 8
- Probar el flujo de compra utilizando las cuentas de prueba de Google Play Console con tarjetas de test.
- **Commit:** `feat(billing): integrate Google Play Billing for lifetime Pro license`

---

## 🏁 Criterio de Éxito de BookSpread v3.0

1. **Rendimiento:** 60 FPS estables con D-Pad en Android TV y TV Boxes económicas.
2. **Peso del APK:** Instalador final de **10 MB a 12 MB** (descarga rápida en 3 segundos).
3. **Estabilidad:** 0 cierres inesperados (*cero OOM*) al leer EPUBs, PDFs pesados o Cómics.
4. **Legalidad:** 100% cumplimiento de normativas de Google Play, COPPA (menores), accesibilidad WCAG y transparencia en enlaces de afiliados.
