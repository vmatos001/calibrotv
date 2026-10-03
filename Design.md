# 🎨 Design Specification: Light Theme (Editorial Bento & High-Contrast Day Mode)
**Documento:** `Design.md` — *Light Theme Edition*  
**Basado en:** Análisis visual de interfaz de referencia (Bento Dashboard Moderno)  
**Objetivo:** Definir el sistema de diseño completo para el modo claro (*Light Theme*) de la aplicación, optimizado para interfaces de televisión (*10-Foot UI*) y lectura diurna.

---

## 📸 Referencia Visual de Diseño

![Referencia Light Theme Bento](file:///C:/Users/laura%20hart/.gemini/antigravity/brain/aec2dfbd-f900-494f-a976-cfb6f5a23ae4/.user_uploaded/media_1791061402448.png)

---

## 1. 🧠 Filosofía y ADN del Diseño

La imagen de referencia combina **Soft-Neomorphism**, **Bento Grid minimalista** y **Editorial High-Contrast**. Rompe radicalmente con los temas claros tradicionales (que suelen cansar la vista con fondos `#FFFFFF` planos y sin jerarquía) mediante:

1. **Lienzo Cálido y Antirreflejo (*Paper Neutral Canvas*):**  
   El fondo no es blanco cegador, sino un tono grisáceo suave y orgánico (`#EBF0EB` / `#EAEFEA`), que en pantallas de TV de 55"+ previene el deslumbramiento y fatiga ocular.
2. **Jerarquía Bimodal (Dark Rail + Light Canvas):**  
   La barra de navegación lateral permanece en un tono carbón profundo casi negro (`#141619`), sirviendo como un ancla visual sólida, mientras que el área de contenido principal respira en tonos claros y aireados.
3. **Acento Dinámico "Cyber Lime" (`#E6FA53` / `#D4F639`):**  
   Un verde lima eléctrico de altísima energía utilizado estratégicamente para métricas destacadas, píldoras activas y focos interactivos. Aporta frescura tecnológica y personalidad única.
4. **Geometría Orgánica Suave (*Squircle & Pill Language*):**  
   Esquinas con radios muy generosos (24dp – 32dp), cápsulas píldora para estados e indicadores de progreso segmentados formados por barras redondeadas.

---

## 2. 🎨 Paleta Cromática y Tokens Semánticos

| Token Semántico | Valor Hex | Uso en UI | Contraste / Función |
| :--- | :---: | :--- | :--- |
| `CanvasBackground` | `#EAEFEA` | Fondo general de la aplicación | Suave, orgánico, antirreflejo para TV |
| `CardBackground` | `#FFFFFF` | Tarjetas Bento estándar, paneles | Máxima legibilidad y elevación limpia |
| `SidebarBackground`| `#141619` | Rail lateral persistente | Ancla oscura de alto contraste |
| `InkPrimary` | `#111317` | Texto principal, botones primarios | Negro tinta profundo y contundente |
| `InkSecondary` | `#636670` | Subtítulos, metadatos, iconos neutros | Gris editorial equilibrado |
| `InkMuted` | `#9DA1AA` | Textos secundarios, contornos tenues | Gris sutil para jerarquía terciaria |
| `AccentLime` | `#E6FA53` | Tarjetas hero, métricas vivas, badges | Acento vibrante de alto impacto |
| `AccentLimeText` | `#111317` | Texto sobre fondos `AccentLime` | 100% contraste legible |
| `PillInactive` | `#E2E7E2` | Cápsulas no seleccionadas, barras vacías | Neutro sutil |
| `FocusHaloAmber` | `#F59E0B` | Foco D-Pad secundario / lectura | Calidez para navegación TV |
| `FocusHaloLime` | `#D4F639` | Foco D-Pad primario en Light Mode | Resplandor nítido en control remoto |

---

## 3. 📐 Arquitectura de Componentes (Bento Grid)

### 3.1. Rail Lateral Oscuro (*Persistent Sidebar Rail*)
- **Estructura:** Columna flotante con esquinas redondeadas (`32dp`), fondo `#141619`.
- **Botón Superior:** Squircle suave con icono `+` de alto contraste.
- **Iconografía:** Líneas limpias de 1.5dp de grosor en `#9DA1AA`. Al enfocar: fondo de cápsula gris translúcido e icono blanco brillante `#FFFFFF`.
- **Avatar Inferior:** Retrato circular delimitado por un anillo limpio en la base del rail.

### 3.2. Encabezado Editorial y Acciones Rápidas
- **Titular:** Tipografía Display de trazo grueso combinada con badges decorativos incrustados en línea (ej. píldoras con iconos o estrellas dentro de la propia frase).
- **Barra de Píldoras Horizontales (Tabs / Filtros):**
  - **Activo:** Cápsula oscura (`#111317`) con texto blanco puro.
  - **Inactivo:** Cápsula blanca o translúcida (`#FFFFFF` con borde suave `#E2E7E2`) y texto `#636670`.
  - **Foco D-Pad:** Escala al 105% con borde iluminado en `AccentLime`.

### 3.3. Tarjetas de Métricas Bento (*Stat Cards*)
- **Card Blanca Clásica:**
  - Fondo blanco puro, esquina `28dp`.
  - Indicador de estado con micro-dona porcentual (ej. `82%`).
  - Número de métrica en tamaño gigante (display bold) seguido de la unidad en gris tenue.
  - **Barra de Progreso Segmentada:** Serie de cápsulas verticales (negras para las completadas, silueta punteada para las pendientes).
- **Card Acento Lima (*Spotlight Card*):**
  - Fondo completo en `AccentLime` (`#E6FA53`).
  - Textos y cápsulas en `InkPrimary` (`#111317`). Rompe la monotonía y resalta la acción más relevante.
- **Card Banner Hero Oscura (*Dark Hero Card*):**
  - Fondo carbón `#111317` con retrato 3D estilizado.
  - Botón CTA en forma de píldora blanca: *"Upgrade ▷"* o *"Continuar Lectura"*.

### 3.4. Gráfica de Progreso y Estadísticas (*Pill Chart*)
- Visualización de datos mediante barras verticales tipo cápsula dual:
  - **Segmento Superior:** Carbón profundo (`#111317`) con punto indicador blanco.
  - **Segmento Inferior:** Verde lima (`#E6FA53`) con punto indicador negro.
  - **Proyección futura:** Siluetas punteadas con punto flotante indicando metas de lectura.

### 3.5. Columna Lateral de Recursos / Biblioteca Rápida
- Fila superior de dos tarjetas cuadradas minimalistas (*Comunidad*, *Academia* / *Historial*, *Favoritos*).
- Lista vertical de módulos con flecha diagonal (`↗`) de acceso directo.

---

## 4. 📺 Adaptación a TV y Experiencia de 10 Pies (10-Foot UI)

1. **Prevención de Fatiga Luminosa en TV:**  
   - En una sala de estar con poca luz, un tema blanco puro al 100% ciega al usuario. Por ello, el fondo `CanvasBackground` se mantiene en tono papel mate (`#EAEFEA`), emulando la página de un libro de alta encuadernación.
2. **Sistema de Foco D-Pad Inconfundible:**  
   - Cuando el cursor del control remoto se posa sobre una tarjeta blanca:
     - Escala: `1.03x` suave con curva `FastOutSlowInEasing`.
     - Borde: Anillo de 3dp en color `InkPrimary` (`#111317`) con sombra difuminada sutil.
   - Cuando se posa sobre una tarjeta o píldora oscura:
     - Halo de resplandor `AccentLime` (`#E6FA53`).
3. **Escala Tipográfica para Lectura a 3 Metros:**  
   - Números de métricas: `40sp` a `48sp`.
   - Títulos de estantería: `24sp` (Bold).
   - Metadatos y autores: `14sp` – `16sp` (Regular).

---

## 5. 🛠️ Implementación en Jetpack Compose (Tokens de Código)

```kotlin
// BookSpread Light Theme Tokens
package com.example.calibretv.ui.theme

import androidx.compose.ui.graphics.Color

object LightThemeTokens {
    val CanvasBackground = Color(0xFFEAEFEA)      // Fondo suave papel mate
    val CardBackground = Color(0xFFFFFFFF)        // Blanco puro para tarjetas
    val CardBackgroundSoft = Color(0xFFF6F8F6)    // Blanco secundario
    val SidebarBackground = Color(0xFF141619)     // Rail carbón oscuro
    
    val InkPrimary = Color(0xFF111317)            // Texto principal / botones
    val InkSecondary = Color(0xFF636670)          // Textos secundarios
    val InkMuted = Color(0xFF9DA1AA)              // Bordes e iconos suaves
    
    val AccentLime = Color(0xFFE6FA53)            // Acento dinámico
    val AccentLimeDark = Color(0xFFC7DC37)        // Hover/Pressed lima
    val AccentLimeText = Color(0xFF111317)        // Texto sobre fondo lima
    
    val PillInactive = Color(0xFFE2E7E2)          // Cápsulas en reposo
    val FocusBorder = Color(0xFF111317)           // Borde de foco principal
    val FocusHaloLime = Color(0xFFE6FA53)         // Resplandor para elementos oscuros
}
```

---

## 6. 📚 Aplicación Directa en BookSpread / CalibroTV

1. **Pantalla Principal (Home Billboard):**  
   - Fondo general `CanvasBackground` (`#EAEFEA`).
   - El libro protagonista en 3D resalta con sombras suaves naturales sobre el fondo claro.
   - Carruseles de curaduría con tarjetas blancas redondeadas (`24dp`) y badges en `AccentLime`.
2. **Estadísticas de Lectura (Reading Stats Card):**  
   - Módulo Bento con páginas leídas hoy, racha de días consecutivos y barras segmentadas en píldoras.
3. **Modo Lectura Diurna (*Day Reading Mode*):**  
   - Pliego dual de libro emulando papel offset natural mate con tipografía serif oscura de alto contraste sin fatiga visual.

---
*Diseñado para BookSpread v3.0 — The 3D Dual-Page Reader for Android TV.*
