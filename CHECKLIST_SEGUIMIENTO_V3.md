# 📋 Lista de Verificación de Implementación — BookSpread v3.0
**Fuente:** [plan_agente_v3.md](file:///c:/Users/laura%20hart/Proyectos/BookSpread/bookspread_app/plan_agente_v3.md)  
**Fecha de Actualización:** 3 de Octubre de 2026  
**Estado Global:** 7 de 8 Fases Completadas (87.5% de avance)

---

## 📊 Tablero de Estado General

| Fase | Descripción | Estado | Commit / Hito |
| :--- | :--- | :---: | :--- |
| **Fase 1** | Rebranding a BookSpread & APK Slim (~10MB) | ✅ Completado | `e5f1042` (APK: 5.91 MB) |
| **Fase 2** | Arquitectura Multi-Fuente (`BookSourceProvider`) | ✅ Completado | `c72029c` |
| **Fase 3** | Motor PDF Pliego Dual 16:9 & Memoria Anti-OOM | ✅ Completado | `35045bc` (APK: 5.92 MB) |
| **Fase 4** | Cartelera Dinámica & Curaduría Firestore con QR | ✅ Completado | `8d66567` (APK: 6.14 MB) |
| **Fase 5** | Audio Desacoplado & Ducking Inteligente con TTS | ✅ Completado | `c4a0408` (APK: 6.14 MB) |
| **Fase 6** | Perfiles Familiares, D-Pad PIN & Modo Kids | ✅ Completado | `df0091e` |
| **Fase 7** | Mobile Companion (Notas vía QR & Quote Cards) | ✅ Completado | `d06dbe3` |
| **Fase 8** | Google Play Billing (Licencia Pro Vitalicia) | ⏳ **Pendiente** | En cola de desarrollo |

---

## 🛡️ Reglas de Ingeniería No Negociables (Checklist de Cumplimiento)

- [x] **1. Protección del Motor 3D:** Parámetros físicos y curvas de interpolación (`curlAnim`, `CubicBezierEasing`, `TransformOrigin`) preservados sin alteraciones de fluidez.
- [x] **2. Prioridad Absoluta al D-Pad (10-Foot UI):** 100% de navegación mediante control remoto en pantallas, diálogos y modales (sin dependencia táctil ni teclado virtual del sistema).
- [x] **3. Presupuesto Estricto de Memoria (Anti-OOM):** Ventana de máximo 3 pliegos en memoria (anterior, actual, siguiente), uso de `Bitmap.Config.HARDWARE` y reciclaje inmediato con `bitmap.recycle()`.
- [x] **4. Hilos Aislados (Cero ANR):** Lectura, parseo de documentos y descargas ejecutados en `Dispatchers.IO`. Cero bloqueos en hilo principal de Compose.
- [x] **5. Arquitectura Local-First y Legal:** La app funciona autónomamente sin servidor externo forzado; obras comerciales enlazadas con tags de afiliado.
- [ ] **6. Compilación y Commit por Fase:** Verificación en Release tras cada fase con `$env:JAVA_HOME = '...'; .\gradlew.bat assembleRelease --no-daemon`.

---

## 🔍 Detalle por Fases y Tareas de Verificación

---

### FASE 1 — Rebranding Completo y Reducción de Peso del APK (~10 MB)
**Objetivo:** Consolidar la identidad **BookSpread**, habilitar optimización R8 y reducir el APK de 22 MB a ~10 MB.

- [x] **1.1 Configuración de Compilación y Minificación (R8)**
  - [x] Habilitar `isMinifyEnabled = true` e `isShrinkResources = true` en `buildType "release"`.
  - [x] Reglas ProGuard añadidas para `kotlinx.serialization`, `androidx.room` y Compose runtime.
  - [x] Limpieza de dependencias innecesarias de `material-icons-extended`.
- [x] **1.2 Limpieza de Recursos y Gráficos Duplicados**
  - [x] Eliminación de recursos pesados y duplicados de densidades extremas (`xxxhdpi`).
  - [x] Actualización de strings oficiales a BookSpread en `strings.xml`.
  - [x] Banner 16:9 optimizado para Android TV.
- [x] **1.3 Criterios de Aceptación / QA**
  - [x] Compilación exitosa en `assembleRelease`.
  - [x] Tamaño del APK: **5.91 MB** (Objetivo: < 10 MB — superado con reducción del 71.8%).
  - [x] Commit semántico realizado: `feat(core): rebrand to BookSpread and optimize APK size with R8 shrinking`.

---

### FASE 2 — Desacoplamiento de Fuentes de Datos (Patrón Provider)
**Objetivo:** Permitir funcionamiento local independiente de Calibre-Web, con soporte para transferencia por QR y fuentes modulares.

- [x] **2.1 Interfaz Común `BookSourceProvider`**
  - [x] Definición de contrato `BookSourceProvider` (`fetchCatalog`, `resolveBookFile`, `isAvailable`).
- [x] **2.2 Implementación de Proveedores de Libros**
  - [x] `LocalRoomProvider`: Catálogo local en Room y persistencia de progreso.
  - [x] `DirectTransferProvider`: Transferencias P2P por red local con `WifiImportServer`.
  - [x] `OpdsProvider`: Mantenido como fuente secundaria opcional.
- [x] **2.3 Refactorización de `BookRepository`**
  - [x] Inicio de la app directo en local sin bloquearse ante falta de red.
  - [x] Orquestación reactiva de múltiples fuentes hacia la UI.
- [x] **2.4 Criterios de Aceptación / QA**
  - [x] Arranque offline instantáneo en `HomeScreen`.
  - [x] Compilación Release limpia (APK 5.91 MB).
  - [x] Commit semántico realizado: `refactor(data): decouple data sources with BookSourceProvider pattern`.

---

### FASE 3 — Motor PDF en Pliego Dual 16:9 y Blindaje de Memoria
**Objetivo:** Renderizar archivos PDF a dos páginas simultáneas con D-Pad y cero riesgo de *OutOfMemory*.

- [x] **3.1 Integración del Renderizador Nativo (`PdfRenderer`)**
  - [x] Módulo `PdfParser` con renderizado thread-safe en `Dispatchers.IO`.
  - [x] Uso de `Bitmap.Config.HARDWARE` para descarga de memoria hacia la GPU.
- [x] **3.2 Lector de Pliego Dual (Páginas Pares e Impares)**
  - [x] Pantalla `PdfReaderScreen` con disposición 16:9 (par izquierda, impar derecha).
  - [x] Portada individual y lomo central estilizado.
  - [x] Ventana deslizante de 3 pliegos con reciclaje de bitmaps no visibles.
  - [x] Cancelación reactiva de corrutinas ante scroll rápido con D-Pad.
- [x] **3.3 Modo Zoom y Controles**
  - [x] Segmentación vertical para textos densos con flechas Arriba/Abajo.
  - [x] HUD superior/inferior con selector de página y guardado en Room.
- [x] **3.4 Criterios de Aceptación / QA**
  - [x] Prueba de estrés: paso rápido continuo de 50 páginas sin micro-congelamientos ni fugas de RAM.
  - [x] Compilación Release verificada (APK 5.92 MB).
  - [x] Commit semántico realizado: `feat(reader): add memory-safe dual-spread PDF renderer for TV`.

---

### FASE 4 — Cartelera Dinámica y Curaduría por Personajes (Firestore)
**Objetivo:** Pantalla de inicio estilo streaming con carruseles curados por arquetipos literarios y códigos QR de compra/descarga.

- [x] **4.1 Modelado del Catálogo Curado**
  - [x] Modelos de datos `CuratorModels.kt` (arquetipos literarios, badges, libros curados).
  - [x] Repositorio offline-first `CuratorRepository.kt` con libros de dominio público integrados.
- [x] **4.2 Pantalla de Inicio y Carruseles (`HomeScreen.kt`)**
  - [x] Componente `CuratorRow.kt` con foco de halo ámbar y avatares vectoriales.
  - [x] Acción dual: descarga automática de dominio público y modal QR para libros comerciales.
- [x] **4.3 Modal de Detalle con QR Dinámico**
  - [x] Generador de QR ISO/IEC (`QrCodeGenerator.kt` con ZXing).
  - [x] Modal `CuratedBookModal.kt` con sinopsis, etiquetas de dificultad y QR para móvil.
- [x] **4.4 Criterios de Aceptación / QA**
  - [x] Descarga funcional de obras libres en segundo plano.
  - [x] Escaneo de QR de afiliado funcional en móvil.
  - [x] Compilación Release verificada (APK 6.14 MB).
  - [x] Commit semántico realizado: `feat(billboard): integrate dynamic character curated shelves with affiliate QR`.

---

### FASE 5 — Servicio de Audio Desacoplado y Mezcla Inteligente (Ducking)
**Objetivo:** Reproducción de sonidos ambientales (lluvia, chimenea) con Audio Focus y atenuación automática con TTS.

- [x] **5.1 Extracción de Audio a Servicio Independiente**
  - [x] `AudioPlaybackService` ejecutado en segundo plano / foreground.
  - [x] Gestión oficial de `AudioFocusRequest` de Android.
- [x] **5.2 Ducking Inteligente con TTS**
  - [x] Interacción suave en `AmbientSoundManager`: atenuación al 20% en 300 ms al hablar el TTS.
  - [x] Recuperación al 100% al pausar o terminar la lectura.
- [x] **5.3 Controles de Audio en Lector**
  - [x] HUD con selector de ambiente sonoro y volumen integrado tanto en EPUB como en PDF.
  - [x] Temporizador de apagado (*Sleep Timer*) con rampa de fade-out.
- [x] **5.4 Criterios de Aceptación / QA**
  - [x] No hay cortes ni crasheos de `MediaPlayer` al cambiar de pliego.
  - [x] Compilación Release verificada (APK 6.14 MB).
  - [x] Commit semántico realizado: `feat(audio): decouple ambient sound service with intelligent TTS ducking`.

---

### FASE 6 — Sistema de Perfiles Familiares, Modo Kids y Mini-Quizzes
**Objetivo:** Experiencia multiusuario con PIN D-Pad para adultos, bloqueo parental y preguntas de comprensión lectora.

- [x] **6.1 Pantalla y Diálogo de Selección de Perfil**
  - [x] Fila horizontal de avatares grandes con escalado animado en foco D-Pad.
  - [x] **Pad Numérico Visual (3x4):** Ingreso de PIN de 4 dígitos usando exclusivamente el control remoto.
- [x] **6.2 Aislamiento de Datos por Perfil**
  - [x] Entidades Room vinculadas con `profileId` (`ReadingSettings`, historial, favoritos).
- [x] **6.3 Modo Infantil (*Kids Mode*) por Lista Blanca**
  - [x] Filtrado de catálogo para perfiles infantiles: solo obras y estanterías aptas.
  - [x] Protección de salida con PIN adulto.
- [x] **6.4 Módulo de Preguntas de Comprensión (Mini-Quizzes)**
  - [x] Preguntas interactivas A/B/C al completar capítulos en perfil infantil.
- [x] **6.5 Criterios de Aceptación / QA**
  - [x] Cambio de perfil fluido recordando tipografía, tema y posición de lectura por usuario.
  - [x] Commit semántico realizado: `feat(profiles): add family profiles with D-Pad PIN, Kids Mode, and retention quizzes`.

---

### FASE 7 — Mobile Companion (Notas, Reseñas y Quote Cards)
**Objetivo:** Uso del smartphone como teclado auxiliar y generador de tarjetas de citas virales vía QR local.

- [x] **7.1 Asistente Web Local para Notas y Reseñas**
  - [x] Endpoint HTTP local en la TV para entrada de texto (`/note?bookId=...`).
  - [x] Formulario móvil responsivo para escribir cómodamente desde el teléfono.
  - [x] Sincronización inmediata de notas hacia la TV y guardado en Room.
- [x] **7.2 Generador de Tarjetas de Citas Estéticas (*Quote Cards*)**
  - [x] Renderizado de tarjeta editorial de alta resolución con texto seleccionado y autor.
  - [x] Código QR para descarga directa en el móvil (formato para Instagram / WhatsApp).
- [x] **7.3 Criterios de Aceptación / QA**
  - [x] Prueba de conexión P2P en red local: captura de nota y descarga de tarjeta completadas.
  - [x] Commit semántico realizado: `feat(companion): add mobile notes assistant, reviews and viral quote cards via QR`.

---

### FASE 8 — Monetización con Google Play Billing (Licencia Pro Vitalicia)
**Objetivo:** Pasarela de compra in-app para desbloquear la versión Pro vitalicia de forma 100% legal y offline-friendly.

- [ ] **8.1 Integración de Google Play Billing**
  - [ ] Añadir dependencia `com.android.billingclient:billing-ktx:7.0.0+` en `build.gradle.kts`.
  - [ ] Configurar SKU no consumible: `bookspread_pro_lifetime`.
- [ ] **8.2 Separación Lógica Free vs. Pro**
  - [ ] Bloqueo condicional en UI para funciones Pro:
    - Física 3D cinemática avanzada (lomo 3D y curvatura configurable).
    - Ambientes de sonido ilimitados.
    - Perfiles familiares ilimitados (>2 perfiles).
    - Almacenamiento local ilimitado (>3 libros importados).
- [ ] **8.3 Persistencia Cifrada de Licencia (`LicenseManager`)**
  - [ ] Implementar `LicenseManager` con `EncryptedSharedPreferences`.
  - [ ] Validación de compra con tolerancia offline permanente tras la primera activación.
- [ ] **8.4 Pantalla de Venta Pro para TV (Paywall D-Pad Friendly)**
  - [ ] Pantalla o diálogo con beneficios Pro, precio regionalizado y botón de compra compatible con Android TV.
- [ ] **8.5 Criterios de Aceptación / QA**
  - [ ] Flujo de compra verificado con cuenta de prueba en Google Play Console.
  - [ ] Funcionamiento offline tras comprar la licencia (sin conexión a internet).
  - [ ] Commit semántico pendiente: `feat(billing): integrate Google Play Billing for lifetime Pro license`.

---

## 🚀 Guía de Comandos para Verificación Continua

### Compilación Release
```powershell
$env:JAVA_HOME = 'C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7'
.\gradlew.bat assembleRelease --no-daemon
```

### Verificación de Peso de APK
```powershell
Get-ChildItem -Path "app\build\outputs\apk\release\*.apk" | Select-Object Name, @{Name="Tamanio_MB"; Expression={[math]::Round($_.Length / 1MB, 2)}}
```

### Inspección de Logs en TV / Emulador
```powershell
adb logcat -s "BookSpread" "PdfReader" "AmbientSound" "CuratorRepo"
```
