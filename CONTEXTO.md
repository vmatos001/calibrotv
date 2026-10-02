# 📖 CONTEXTO.md — CalibroTV (Lector 3D para Android TV & Google TV)

> **Instrucción para el Agente AI:** Lee este archivo al iniciar cualquier nueva conversación para entender el estado completo del proyecto, la arquitectura, la configuración de firma de APK, las versiones publicadas y las reglas de diseño para Android TV.

---

## 📅 Estado Actual del Proyecto (Actualizado: 01/10/2026)

- **Versión Activa:** `v3.2` (Plan Maestro de Arquitectura y Rediseño v3.0 culminado)
- **Repositorio:** `https://github.com/vmatos001/calibrotv`
- **Descarga Directa Downloader / TinyURL:** `https://tinyurl.com/29t27s6q`
- **Keystore de Firma:** `app/keystore/calibrotv.keystore` (Firma unificada permanente para Debug y Release con Huella SHA256: `52:E8:ED:A7:6F:9C:CA:51:F5:55:6E:48:47:7C:20:C3:8F:AE:6B:4F:6C:AE:C3:D4:84:FB:98:17:7F:7E:F8:0C`).
- **Artefacto Compilado:** `app/build/outputs/apk/release/app-release.apk` (~12.4 MB, R8 shrinker activado, 0 errores de compilación).

---

## ⚠️ Nota Importante sobre la Actualización (Error "Conflicto de nombre de paquete")

Si en la TV aparece el mensaje:
> **"app no instalada. Hay un conflicto de nombre con un paquete que ya existe."**

**Causa:** La versión previa (v2.1 o anterior) instalada en la TV fue firmada con una clave temporal de debug de Android Studio. A partir de v2.2 se estandarizó la firma oficial del proyecto usando `app/keystore/calibrotv.keystore`.

**Solución por única vez:**
1. Desinstalar la versión antigua de CalibroTV en la TV.
2. Instalar la nueva versión utilizando el código de Downloader `https://tinyurl.com/29t27s6q`.
3. Todas las actualizaciones conservan el mismo keystore oficial para actualizar sin desinstalar.

---

## 🏗️ Arquitectura y Componentes Principales (v3.2)

### 1. Sistema Multi-Source Provider & Repositorio
- `BookSourceProvider.kt`: Interfaz extensible para proveedores de libros.
- `LocalRoomProvider.kt`: Proveedor de base de datos local SQLite con Room.
- `DirectTransferProvider.kt`: Libros importados vía WiFi desde navegador móvil/PC.
- `OpdsProvider.kt`: Conexión con catálogos OPDS remotos (Calibre-Web).
- `CoverLoader.kt`: Carga downsampled segura en `RGB_565` para prevenir Out-Of-Memory en Smart TVs de bajos recursos (1GB/1.5GB RAM).
- `AppDatabase.kt` (Room v3): Soporte integrado para notas de lectura (`book_notes`).

### 2. Navegación TopBar de 5 Pestañas
- `TvTopBar.kt` y `Navigation.kt`: Barra superior cinematográfica con 5 secciones D-Pad:
  1. `[Inicio]`: Hero banner, libros recientes, cartelera dinámica y accesos directos.
  2. `[Biblioteca]`: Estantes de personajes (Sección A) y cuadrícula completa (Sección B).
  3. `[Tus Libros]`: Filtrado personal, favoritos y libros descargados localmente.
  4. `[Lector 3D]`: Reanuda inmediatamente la última lectura activa.
  5. `[Ajustes]`: Temas, audio ambiental, velocidad de paso de página, perfiles y OTA.

### 3. Motores de Lectura (EPUB 3D + PDF Dual Spread + Cómics)
- **Lector 3D Inmersivo (`ReaderScreen.kt`):**
  - Efecto de paso de página en 3D con pliegos dobles panorámicos 16:9.
  - **⚠️ REGLA INTOCABLE:** NUNCA modificar los parámetros de animación física 3D (`curlAnim`, `CubicBezierEasing`, `TransformOrigin`).
- **Lector PDF Nativo Panorámico (`PdfReaderScreen.kt` & `PdfParser.kt`):**
  - Renderizado nativo por pliegos dobles (página izquierda + página derecha).
  - Mutex thread-safe sobre `PdfRenderer` de Android.
  - Ventana deslizante de solo 3 pliegos en memoria para garantizar 0% de fallas OOM en TV.
  - Salto de página rápido, creación de notas y cuestionarios integrados.
- **Lector de Cómics (`ComicReaderScreen.kt`):**
  - Soporte de archivos `.cbz` y `.cbr`.

### 4. Cartelera Dinámica de Personajes y Afiliados (Curator)
- `CuratorRepository.kt` & `CuratorModels.kt`: 10 arquetipos literarios con medallas de dificultad (1 a 5).
- Libros de dominio público con descarga directa y libros comerciales recomendados con código QR de afiliado para compra instantánea en smartphone.
- `CuratorRow.kt` y `CuratedBookModal.kt`: Fila horizontal navegable con control remoto y modal interactivo.

### 5. Control Parental y Modo Infantil (Kids Mode)
- Perfiles de usuario diferenciados con modo infantil (`isKidsMode`).
- `PinPadDialog.kt`: Teclado numérico visual 3x4 optimizado para D-Pad con confirmación por PIN de 4 dígitos.
- Filtrado estricto de catálogo y estantes en modo niños.
- Sistema de estrellas de lectura (`starsCount`).

### 6. Sistema Social & Companion WiFi
- `WifiImportServer.kt`: Servidor HTTP nano embebido en TV con:
  - Subida directa de archivos EPUB, PDF y CBZ desde el navegador del teléfono/PC.
  - Endpoint `/note` para tomar notas con teclado de smartphone y verlas reflejadas en la TV.
  - Endpoint `/quote_card.png` para descargar tarjetas de citas generadas en alta resolución.
- `QuoteCardGenerator.kt`: Generador en Canvas de fichas editoriales de citas en 1080x1920 para compartir.
- `QuizDialog.kt`: Cuestionarios interactivos de comprensión lectora.

### 7. Audio Ducking y Foley Ambiental
- `AmbientSoundManager.kt` y `AudioPlaybackService.kt`: Servicio en primer plano para audio ambiental.
- **Audio Ducking:** Cuando el motor TTS comienza a leer en voz alta, el volumen ambiental se atenúa automáticamente al 20%, recuperando el 100% de manera suave cuando la lectura se detiene.

### 8. Paleta Editorial "Noble Ink & Gold"
- `Color.kt`: Nueva paleta premium basada en Deep Midnight Navy, Gold Ochre, Parchment Paper, Charcoal Ink y Sand Cream con retrocompatibilidad para los temas clásicos.

---

## 📋 Historial de Versiones

### v3.2 (Versión Actual - Plan Maestro Completado)
- **Fase 1:** Arquitectura Multi-Source Provider (`BookSourceProvider`), Room v3 con notas, `CoverLoader` RGB_565 anti-OOM, perfiles Kids con PIN.
- **Fase 2:** Lector PDF nativo panorámico 16:9 (`PdfReaderScreen`), ventana deslizante de memoria (3 pliegos) y mutex seguro en `PdfRenderer`.
- **Fase 3:** `WifiImportServer` enriquecido (EPUB, PDF, CBZ), notas QR y endpoint móvil, `QuoteCardGenerator` (1080x1920) y `QuizDialog`.
- **Fase 4:** `AudioPlaybackService`, Audio Ducking automático en `AmbientSoundManager` ante lectura TTS, paleta editorial "Noble Ink & Gold".
- **Fase 5:** Cartelera dinámica de personajes (`CuratorRepository`), soporte de libros comerciales con QR de afiliado, TopBar de 5 pestañas (`[Inicio]`, `[Biblioteca]`, `[Tus Libros]`, `[Lector 3D]`, `[Ajustes]`), diálogo de PIN 3x4 para modo niños.
- **Fase 6:** Minificación R8 verificada, APK release firmado unificado (12.4 MB) y documentación completa.

### v3.1
- **Arquitectura de Biblioteca v2.6 Restaurada:** Carga directa y fluida de catálogo y estantes desde la API REST (`/personajes/api/books` y `/personajes/api/shelves`), preservando las etiquetas de estantes de personaje y nivel en los libros.
- **Imágenes Oficiales de Estantes de la API REST:** Soporte completo para `has_image` e `image_url` (`/personajes/api/shelves/<id>/image`).
- **Optimización de Peso (v2.7):** Compilación minificada con R8 y ProGuard.
- **Filtro de Libros en Inglés (v2.8):** Clasificación dinámica de libros en inglés sin incluir estantes nulos.
- **Mejoras de Usabilidad y Lector 3D (v2.9):**
  - Modal de Detalles: Salida inmediata con 1 pulsación de Atrás.
  - HUD Lector TTS: Acento de Voz (España 🇪🇸, México 🇲🇽, Latino 🌐), Tono y Velocidad.
  - Fire TV Stick Banner: Launcher banner adaptado en `mipmap-xhdpi` y `mipmap-xxhdpi`.

---

## 🤖 Guía de Inicio para Agentes de IA

Al iniciar una nueva sesión sobre este proyecto:
1. Revisa `CONTEXTO.md` para refrescar el estado actual.
2. Si trabajas con el build Gradle, utiliza el comando:
   ```powershell
   $env:JAVA_HOME = 'C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7'; .\gradlew.bat assembleRelease --no-daemon
   ```
3. Todo APK producido debe generarse con el keystore ubicado en `app/keystore/calibrotv.keystore`.
4. **Respetar la regla de oro:** La física de paso de página 3D de `ReaderScreen.kt` es intocable.
