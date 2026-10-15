# Spartan CE — Directrices y Contexto Técnico para Agentes IA

> **Este archivo (`AGENTS.md`) es detectado automáticamente por agentes y asistentes de código.**
> Contiene todas las reglas de diseño, arquitectura técnica, estado del proyecto y directrices críticas para continuar el desarrollo de **Spartan CE** sin perder contexto ni consumir tokens innecesarios.

---

## 🚨 Reglas Críticas e Inviolables

1. **Branding Oficial Exclusivo:**
   - El proyecto se llama única y exclusivamente **Spartan CE**.
   - **ESTRICTAMENTE PROHIBIDO** incluir referencias directas a *"Halo"*, *"MCC"*, *"Master Chief Collection"*, *"Halo Protocol"* o similares en textos visibles, subtítulos, diálogos, notificaciones o mensajes remotos.
2. **Estética de Diseño Táctica (Consola Militar Sobria):**
   - **Paleta Oficial:** Fondo grafito obsidiana profundo (`#0A0D14` / `#06090E`), tarjetas en pizarra mate oscura (`#101620` a `#182230`), bordes milimétricos en titanio sutil (`rgba(148, 163, 184, 0.25)`).
   - **Tipografía y Acentos:** Títulos en blanco puro sólido (`#FFFFFF`), texto secundario en blanco atenuado (`#B4C3D7`), alertas en ámbar táctico (`#F59E0B`), confirmaciones en esmeralda militar (`#34D399`).
   - **ESTRICTAMENTE PROHIBIDO:** Bordes fluorescentes cian o morados genéricos de IA, botones encerrados en cajas rígidas con esquinas brillantes, y texturas metálicas pesadas con biseles 3D falsos.
3. **Control de Versiones y Despliegue:**
   - **NO realizar `git push` a remoto ni modificar GitHub Releases a menos que el usuario lo solicite expresamente.**
   - Todos los cambios se prueban y compilan localmente primero en el teléfono conectado (`AYQTJV5B25G04364`).

---

## 🖥️ Arquitectura de la Interfaz Táctica (Hub)

* **Menú Lateral Vertical (Izquierda):**
  - Ubicado en un `ScrollView` táctico con scroll oculto para pantallas de cualquier aspecto (16:9 a 21:9).
  - Cada pestaña tiene un botón táctil con una barra indicadora vertical activa.
  - Pestañas actuales:
    1. `CAMPAÑA / JUGAR`
    2. `CONFIGURACIÓN GRÁFICA`
    3. `CONTROLES Y HUD`
    4. `SISTEMA Y ARCHIVOS`
    5. `AJUSTES Y RUTAS` (Pestaña 4 en código)
    6. `INFORMACIÓN Y ESTADO` (Pestaña 5 en código)
    7. `SALIR DEL JUEGO`
* **Contenedor Flotante (Derecha):**
  - Ocupa el **100% de la altura vertical de la pantalla**.
  - Sin recuadros, bordes gruesos ni marcos rígidos. Posee un fondo oscuro con velo translúcido (`rgba(6, 9, 14, 0.88)`).
  - Separado del menú izquierdo por una línea divisoria vertical ultrafina de 1dp en tono titanio.
  - Barras de desplazamiento tácticas automáticas (`configureTacticalScrollView`) que informan visualmente al usuario cuando existe más contenido hacia abajo.
* **Pie de Página Central:**
  - Eslogan centrado y licencia comunitaria: `"SPARTAN CE // MOTOR TÁCTICO RECOMPILADO PARA ANDROID"` - Licencia MIT / GPL.

---

## 🔔 Sistema de Notificaciones Persistente y Campana

* **Campana Vectorial en Header (`ic_tactical_bell.xml`):**
  - Totalmente **frameless**, sin recuadro de botón ni bordes que recorten el icono.
  - **Reposo / Al día:** Tintada en blanco atenuado táctico (`#B4C3D7` al 60% de opacidad).
  - **Avisos no leídos:** Tintada en blanco sólido puro (`#FFFFFF`) con un punto de alerta ámbar (`#F59E0B`) en la esquina superior derecha.
* **Modal "Avisos de Spartan CE":**
  - Botón de cierre **frameless (`✕`)** en `#94A3B8`, sin caja de botón.
  - Título `"AVISOS DE SPARTAN CE"` y subtítulo `"Canal de avisos y registros del proyecto Spartan CE"`.
  - **Identificador Único (`id`):** Cada notificación remota incluye un `id` único (ej. `notice_v110_tactical`).
  - **Persistencia en `SharedPreferences`:** Al marcar una notificación como leída o descartarla, su ID se guarda en el conjunto `dismissed_notices`. Al abrir la app o reiniciar, los avisos con ID guardado no vuelven a mostrarse como no leídos ni activan el punto ámbar.
  - Acciones: `"✓ MARCAR LEÍDA"`, `"✓ LEER TODAS"`, `"REINTENTAR SYNC"`, `"CERRAR"`.

---

## ⚙️ Pestaña "Ajustes y Rutas" (Tab 4)

Permite personalizar las rutas remotas para actualizar avisos y configuraciones sin recompilar la APK:

1. **Rutas Sincronizadas:**
   - `updates.json`: Consulta de versión, changelog y array de avisos.
   - `motd.json`: Mensajes rotativos del header.
   - `commands.json`: Lista de trucos y comandos de consola.
   - `repository_url`: Enlace base al repositorio.
2. **Botón "PROBAR CONEXIÓN":**
   - Sin emojis (`⚡`). Estilo pizarra oscura mate con borde de titanio y padding horizontal exacto (`dp(14)` y `setIncludeFontPadding(false)`) para evitar cualquier recorte de texto en Android.
   - Verificación asíncrona HTTP GET ligera con respuesta en vivo: `✓ Conexión exitosa (200 OK)` o `✕ Error de conexión`.
3. **Gestión de Notificaciones Locales:**
   - Contador de avisos leídos en el dispositivo.
   - Botón `"RESTABLECER HISTORIAL DE AVISOS LEÍDOS"` para vaciar `dismissed_notices` y probar alertas.

---

## 📊 Pestaña "Información y Estado" (Tab 5)

* **Telemetría de Hardware en Vivo:**
  - RAM Libre vs. RAM Total (en MB y GB).
  - Resolución nativa física y Tasa de Refresco de pantalla en Hz (60 Hz, 90 Hz, 120 Hz, 144 Hz).
  - Nivel de batería (%) y estado de carga.
  - Almacenamiento libre en almacenamiento interno.
* **Selector de Idioma de Campañas:**
  - Detección automática de mapas en disco (`Español`, `Inglés`, etc.) permitiendo cambiar de idioma al vuelo.

---

## 🎮 Ajustes de Motor Gráfico y Controles (Tabs 2 y 3)

* **Gráficos:** FOV (60° a 110°), Iluminación por píxel (Per-Pixel Lighting), Suavizado de bordes (Anti-Aliasing), Sombras dinámicas.
* **Controles:** Botones vectoriales estilo Xbox ABXY, D-Pad reactivo que solo ilumina la dirección presionada, drag-to-aim simultáneo, háptica de disparo y sensibilidad de giroscopio.
* **Editor HUD:** Invocable desde la pestaña de controles; incluye atajo secreto configurable (combo `Y-Y-X-B-B-Y`) para alternar la consola táctica.

---

## 🛠️ Comandos de Desarrollo y Entorno Local

* **Entorno de Compilación:**
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
  $env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe
  ```
* **Compilación Debug:**
  ```powershell
  .\gradlew.bat assembleDebug
  ```
* **Compilación Release:**
  ```powershell
  .\gradlew.bat assembleRelease
  ```
* **Instalación y Ejecución en Teléfono (`AYQTJV5B25G04364`):**
  ```powershell
  & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s AYQTJV5B25G04364 install -r app\build\outputs\apk\release\app-release.apk
  & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s AYQTJV5B25G04364 shell am start -n com.kyrn.spartan/com.halo.decomp.LauncherActivity
  ```
* **Captura de Pantalla para Verificación:**
  ```powershell
  & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s AYQTJV5B25G04364 shell screencap -p /sdcard/screen.png
  & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s AYQTJV5B25G04364 pull /sdcard/screen.png "C:\Users\PC\.gemini\antigravity-cli\brain\<conv-id>\screen.png"
  ```
* **Subida a GitHub Release (Solo si el usuario lo ordena):**
  ```powershell
  gh release upload v1.1.0 Spartan_CE_v1.1.0.apk --clobber
  ```

---

## 🗺️ Hoja de Ruta Pendiente (Próximas Sesiones)

1. **Fase 5 — Módulo Comunitario y Comentarios:**
   - Integración futura con Cloudflare Free (Workers / D1 / KV).
   - Mantener consultas optimizadas dentro de los límites estrictos de la capa gratuita sin generar sobrecostes ni bloqueos.
2. **Fase 6 — Personalización Avanzada:**
   - Gestor dinámico de fondos escénicos y selector de perfiles tácticos.
