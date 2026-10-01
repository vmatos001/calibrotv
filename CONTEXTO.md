# 📖 CONTEXTO.md — CalibroTV (Lector 3D para Android TV & Google TV)

> **Instrucción para el Agente AI:** Lee este archivo al iniciar cualquier nueva conversación para entender el estado completo del proyecto, la arquitectura, la configuración de firma de APK, las versiones publicadas y las reglas de diseño para Android TV.

---

## 📅 Estado Actual del Proyecto (Actualizado: 01/10/2026)

- **Versión Activa:** `v3.1` (Build `versionCode = 15`, `versionName = "3.1"`)
- **Repositorio:** `https://github.com/vmatos001/calibrotv`
- **Descarga Directa Downloader / TinyURL:** `https://tinyurl.com/29t27s6q`
- **Keystore de Firma:** `app/keystore/calibrotv.keystore` (Firma unificada permanente para Debug y Release con Huella SHA256: `52:E8:ED:A7:6F:9C:CA:51:F5:55:6E:48:47:7C:20:C3:8F:AE:6B:4F:6C:AE:C3:D4:84:FB:98:17:7F:7E:F8:0C`).

---

## ⚠️ Nota Importante sobre la Actualización (Error "Conflicto de nombre de paquete")

Si en la TV aparece el mensaje:
> **"app no instalada. Hay un conflicto de nombre con un paquete que ya existe."**

**Causa:** La versión previa (v2.1 o anterior) instalada en la TV fue firmada con una clave temporal de debug de Android Studio. La v2.2 estandarizó la firma oficial del proyecto usando `app/keystore/calibrotv.keystore`.

**Solución por única vez:**
1. Desinstalar la versión antigua de CalibroTV en la TV.
2. Instalar **v2.2** utilizando el código de Downloader `https://tinyurl.com/29t27s6q`.
3. A partir de **v2.2 en adelante** (v2.3, v2.4, v2.5, v2.6, v2.9, v3.1, etc.), todas las actualizaciones automáticas OTA o vía Downloader se instalarán **sin desinstalar nada**, ya que mantendrán la misma firma oficial.

---

## 🏗️ Arquitectura y Componentes Principales

1. **Interfaz TV & Navegación D-Pad (16:9):**
   - Diseñado específicamente para televisión con enfoque en control remoto (Filtro cian/ámbar de alto contraste, halo de enfoque vibrante).
   - `LibraryGridScreen.kt`: Pantalla principal de biblioteca con Sección A (estanterías circulares estilo Netflix Kids) y Sección B (cuadrícula de libros).
   - `HomeScreen.kt`: Pantalla de inicio con libros recientes, favoritos y recomendaciones.
   - `SettingsScreen.kt`: Configuración de temas, fuentes, velocidad de hoja, sonidos ambientales y actualización OTA.

2. **Estanterías de Calibre-Web & Niveles de Dificultad:**
   - **Sección A:** Avatares circulares para Shelves de personajes (ej: Dr. House, Lisa Simpson, Matilda, Patrick Jane, Dune, etc.) con imágenes de estantes oficiales (`/personajes/api/shelves/<id>/image`) o portada de libro recortada en círculo y medallas circulares del 1 al 5 para Niveles de Dificultad.
   - **Sección B:** Cuadrícula vertical de libros. Al presionar una estantería de personaje, se muestran los libros de esa estantería **ordenados estrictamente del Nivel 1 al 5**.

3. **Lector 3D Inmersivo (Page Curl Engine):**
   - `ReaderScreen.kt`: Visualizador EPUB con efecto de paso de página en 3D (pliego a pliego panorámico 16:9).
   - `ComicReaderScreen.kt`: Lector de Cómics y Mangas (.cbz / .cbr).
   - **Regla Intocable:** NO modificar los parámetros físicos de la animación 3D (`curlAnim`, `CubicBezierEasing`, `TransformOrigin`) sin autorización.

4. **Motor TTS (Lectura en Voz Alta & HUD de Controles):**
   - `TtsController.kt`: Configurado con `AudioAttributes` (`USAGE_MEDIA`, `CONTENT_TYPE_SPEECH`), salida a `STREAM_MUSIC`, compatibilidad con Android 11+ / API 30+ y soporte para Tono/Pitch, Velocidad y Acento de Voz (España, México, Latino).

5. **Sonidos Ambientales y Foley:**
   - Ubicados en `app/src/main/res/raw/` en formato OGG Vorbis liviano (lluvia, chimenea, bosque, océano, café murmur) y efecto de paso de hoja suave (`page_turn_*.wav`).

6. **Sistema Global de Perfiles TV:**
   - `UserProfilesDialog.kt`: Permite crear y gestionar perfiles de usuario con selector de color interactivo mediante D-Pad y soporte completo para campos de texto.

---

## 📋 Historial de Versiones

### v3.1 (Versión Activa)
- **Arquitectura de Biblioteca v2.6 Restaurada:** Carga directa y fluida de catálogo y estantes desde la API REST (`/personajes/api/books` y `/personajes/api/shelves`), preservando las etiquetas de estantes de personaje y nivel en los libros.
- **Imágenes Oficiales de Estantes de la API REST:** Soporte completo para `has_image` e `image_url` (`/personajes/api/shelves/<id>/image`) mostrando las fotografías oficiales de personajes e insignias de nivel en la Sección A.
- **Optimización de Peso (v2.7):** Compilación minificada con R8 y ProGuard (APK Release reducido a 11.75 MB).
- **Filtro de Libros en Inglés (v2.8):** Clasificación dinámica de libros en inglés sin incluir estantes nulos.
- **Mejoras de Usabilidad y Lector 3D (v2.9):**
  - **Modal de Detalles:** Salida inmediata con 1 sola pulsación de Atrás (`detailsBook = null`, `onKeyEvent`).
  - **Botón Favoritos:** Texto limpio sin estrella duplicada (`En Favoritos` / `Añadir a Favoritos`).
  - **HUD Lector TTS:** Opciones de Acento de Voz (España 🇪🇸, México 🇲🇽, Latino 🌐), Tono/Pitch (Grave, Normal, Agudo) y Velocidad (0.75x a 1.5x).
  - **Fire TV Stick Banner:** Banner launcher en `mipmap-xhdpi/banner.png` y `mipmap-xxhdpi/banner.png`.

### v2.5
- **Foco de Navegación D-Pad:** Restauración precisa de foco al cerrar la ficha técnica de un libro; el cursor vuelve exactamente al libro seleccionado.
- **Sinopsis Real OPDS:** Carga directa de descripciones de libros desde el feed individual del servidor Calibre-Web y almacenamiento en Room.
- **Audio Ambiental Blindado:** Captura preventiva de errores en `MediaPlayer` para evitar cierres de la app.

### v2.4 y v2.3
- **Biblioteca estilo Netflix Kids:** Estanterías circulares de personajes y números 1-5.
- **Lectura TTS:** Corrección de compatibilidad en Android TV para lectura continua en voz alta.
- **Firma Única Keystore:** Configuración de `calibrotv.keystore` unificado.

---

## 🤖 Guía de Inicio para Agentes de IA

Al iniciar una nueva sesión sobre este proyecto:
1. Revisa `CONTEXTO.md` para refrescar el estado actual.
2. Si trabajas con el build Gradle, utiliza el comando:
   ```powershell
   $env:JAVA_HOME = 'C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7'; .\gradlew.bat assembleRelease --no-daemon
   ```
3. Todo APK producido debe generarse con el keystore ubicado en `app/keystore/calibrotv.keystore`.
