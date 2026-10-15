<p align="center">
  <img src="assets/icon.png" alt="Spartan CE Logo" width="128" height="128" style="border-radius: 24px;" />
</p>

<h1 align="center">Spartan CE</h1>

<p align="center">
  <b>Motor táctico, interfaz de consola moderna y capa de controles adaptativos para Android ARM64.</b><br>
  <i>A lightweight, tactical mobile engine hub, console-grade UI & adaptive touch controller layer.</i>
</p>

<p align="center">
  <a href="https://github.com/devlwte/spartance/releases/latest"><img src="https://img.shields.io/github/v/release/devlwte/spartance?color=00E5FF&label=Release&style=flat-square" alt="Latest Release" /></a>
  <img src="https://img.shields.io/badge/Version-v1.1.0-00E5FF?style=flat-square" alt="Version: v1.1.0" />
  <img src="https://img.shields.io/badge/Platform-Android%209.0%2B-blue?style=flat-square" alt="Platform: Android 9+" />
  <img src="https://img.shields.io/badge/Architecture-ARM64--v8a-emerald?style=flat-square" alt="Arch: ARM64" />
  <img src="https://img.shields.io/badge/Author-%40kyrnforge-cyan?style=flat-square" alt="Author: @kyrnforge" />
  <img src="https://img.shields.io/badge/License-GPL%20%2F%20MIT-lightgrey?style=flat-square" alt="License: Open Source" />
</p>

---

## 📋 Índice / Table of Contents
1. [Visión General del Proyecto](#-visión-general-del-proyecto)
2. [Arquitectura de Interfaz Táctica (Hub Moderno)](#-arquitectura-de-interfaz-táctica-hub-moderno)
3. [Sistema de Avisos y Notificaciones Remotas](#-sistema-de-avisos-y-notificaciones-remotas)
4. [Ajustes de Red y Rutas JSON Dinámicas](#-ajustes-de-red-y-rutas-json-dinámicas)
5. [Telemetría de Hardware y Selector de Idioma](#-telemetría-de-hardware-y-selector-de-idioma)
6. [Ajustes Gráficos y Sistema de Controles](#-ajustes-gráficos-y-sistema-de-controles)
7. [Reglas de Diseño e Identidad Visual](#-reglas-de-diseño-e-identidad-visual)
8. [Estructura del Repositorio](#-estructura-del-repositorio)
9. [Compilación, Despliegue y Publicación](#-compilación-despliegue-y-publicación)
10. [Hoja de Ruta y Próximas Fases](#-hoja-de-ruta-y-próximas-fases)
11. [Aviso Legal y Créditos](#-aviso-legal-y-créditos)

---

## 🌟 Visión General del Proyecto

**Spartan CE** nace para llevar la experiencia táctica clásica de escritorio a dispositivos móviles modernos sin sacrificar ergonomía, fluidez ni fidelidad visual.

Desarrollado desde cero por **@kyrnforge**, el proyecto actúa como un lanzador independiente, ejecutor nativo y puente de entrada táctil para Android (ARM64-v8a). Cuenta con un diseño inspirado en interfaces de consola de alta gama, telemetría de hardware en tiempo real, gestión remota de anuncios/rutas sin recompilar el APK y controles virtuales ergonómicos totalmente personalizables.

---

## 🖥️ Arquitectura de Interfaz Táctica (Hub Moderno)

La interfaz de Spartan CE v1.1.0 ha sido rediseñada para ofrecer una experiencia limpia, inmersiva y sin recuadros toscos:

```
+-----------------------------------------------------------------------------------------+
| [🔫] SPARTAN CE                          [🔔] Campana Vectorial       [● SPARTAN-117]   |
|      Engine Built by @kyrnforge_dev           (Muted / Alert Dot)      (Estado Motor)   |
+-------------------+---------------------------------------------------------------------+
| [Menú Vertical]   | [Contenedor Flotante 100% Pantalla Vertical]                        |
| • CAMPAÑA / JUGAR |   - Fondo difuminado leve (blur semitransparente oscuro)             |
| • CONFIG. GRÁFICA |   - Sin bordes rígidos ni cajas encerradas                          |
| • CONTROLES Y HUD |   - Separador vertical con línea de titanio sutil                    |
| • SISTEMA Y ARCH. |   - Barras de desplazamiento tácticas automáticas tipo consola      |
| • AJUSTES Y RUTAS |   - Contenido dinámico según la pestaña activa                      |
| • INFORMACIÓN     |                                                                     |
| • SALIR           |                                                                     |
+-------------------+---------------------------------------------------------------------+
|        SPARTAN CE // MOTOR TÁCTICO RECOMPILADO PARA ANDROID (Licencia MIT / GPL)        |
+-----------------------------------------------------------------------------------------+
```

### Principales Características del Hub:
* **Menú Lateral con Scroll Oculto:** Permite navegar entre pestañas en pantallas de cualquier tamaño y relación de aspecto (16:9, 18:9, 20:9, 21:9) sin desbordamiento ni cortes. Indicador vertical luminoso en la pestaña activa.
* **Contenedor Flotante de Pantalla Completa:** El área de contenido ocupa el 100% de la altura vertical de la pantalla, sin marcos gruesos. Posee un fondo oscuro con velo translúcido (`rgba(6, 9, 14, 0.88)`) que asegura legibilidad perfecta sobre el fondo escénico.
* **Separador de Titanio:** Línea vertical ultrafina de 1dp en tono acero/titanio que delimita elegantemente el menú del área de trabajo.
* **Barras de Scroll Tácticas Automáticas:** Implementadas con `configureTacticalScrollView`, visibles con deslizador sutil tipo HUD cuando hay contenido excedente para que el usuario sepa de forma natural que puede desplazarse hacia abajo.
* **Pie de Página Central Táctico:** Eslogan sobrio y créditos comunitarios centrados en la parte inferior sin invadir el espacio útil.

---

## 🔔 Sistema de Avisos y Notificaciones Remotas

El sistema de avisos de Spartan CE v1.1.0 garantiza comunicación directa con la comunidad sin generar spam al usuario:

### 1. Campana Vectorial sin Recuadro (`ic_tactical_bell.xml`)
* Ubicada en la barra superior izquierda del badge de Spartan-117.
* **Totalmente frameless:** No utiliza botones cuadrados ni fondos que recorten el icono.
* **Estados de la Campana:**
  * **Sin avisos pendientes (Al día):** Icono vectorial blanco atenuado (`#B4C3D7` al 60% de opacidad).
  * **Con avisos nuevos (Pendientes):** Icono vectorial en blanco puro sólido (`#FFFFFF`) acompañado de un punto de alerta ámbar táctico (`#F59E0B`) con borde oscuro.

### 2. Modal de Avisos ("Avisos de Spartan CE")
* **Botón de Cierre Frameless (`✕`):** Touch target limpio en `#94A3B8`, sin recuadro de botón.
* **Control por ID Único Persistente:** Cada aviso remoto posee un campo `id` único (por ejemplo, `notice_v110_tactical`). Al marcar una notificación como leída o descartarla, su ID se almacena localmente en `SharedPreferences` (`dismissed_notices`).
* **Prevención de Repetición:** Las notificaciones con ID registrado jamás vuelven a mostrarse como no leídas ni activan el punto ámbar al abrir el juego.
* **Filtrado en Dos Secciones:**
  1. *Avisos Pendientes:* Con badge táctico ámbar `"NUEVO"` y botón `"✓ MARCAR LEÍDA"`.
  2. *Historial de Avisos Leídos:* Tarjetas en tono pizarra atenuado con etiqueta `"LEÍDO"`.
* **Acciones Rápidas:**
  * `"✓ LEER TODAS"`: Descarta todos los avisos pendientes en un solo toque.
  * `"REINTENTAR SYNC"`: Vuelve a consultar los repositorios remotos.
  * `"CERRAR"`: Cierra el modal de forma instantánea.

---

## ⚙️ Ajustes de Red y Rutas JSON Dinámicas

En la pestaña **"AJUSTES Y RUTAS"**, el usuario puede personalizar las direcciones remotas para consultar actualizaciones y anuncios sin necesidad de recompilar ni reinstalar la aplicación:

| Parámetro | Valor por Defecto | Propósito |
| :--- | :--- | :--- |
| **URL de Actualizaciones** | `.../main/updates.json` | Consulta de versiones, parches y lista de notificaciones remotas. |
| **URL de Mensajes (MOTD)** | `.../main/motd.json` | Mensajes dinámicos rotativos en la cabecera del Hub. |
| **URL de Comandos** | `.../main/commands.json` | Diccionario de comandos de consola táctica y trucos. |
| **URL del Repositorio** | `https://github.com/devlwte/spartance` | Enlace base a la comunidad y código fuente. |

### Botón "PROBAR CONEXIÓN":
* Diseño de pizarra oscura mate con borde de titanio sutil, sin emojis (`⚡`) y sin recortes de texto.
* Ejecuta una verificación HTTP asíncrona (`GET` ligero con timeout de 3.5s) que valida conectividad en tiempo real y muestra un indicador visual:
  * `✓ Conexión exitosa (200 OK)` en verde esmeralda táctico.
  * `✕ Error de red / Timeout` en tono carmesí táctico.

### Gestión de Avisos Leídos:
* Contador dinámico de avisos marcados como leídos en el dispositivo actual.
* Botón **"RESTABLECER HISTORIAL DE AVISOS LEÍDOS"** para depurar o volver a recibir avisos anteriores.

---

## 📊 Telemetría de Hardware y Selector de Idioma

En la pestaña **"INFORMACIÓN Y ESTADO"**, la aplicación expone métricas críticas en tiempo real para optimizar el rendimiento en cualquier terminal:

* **Memoria RAM:** Lectura precisa de RAM libre vs. RAM total del sistema en Megabytes y Gigabytes.
* **Pantalla y Refresco:** Resolución física nativa exacta y tasa de refresco del panel en tiempo real (60 Hz, 90 Hz, 120 Hz, 144 Hz).
* **Batería:** Porcentaje de carga actual y estado (Cargando / Batería).
* **Almacenamiento:** Espacio libre en almacenamiento interno para mapas y texturas.
* **Selector de Idioma Oficial de Mapas:** Detecta automáticamente paquetes de idiomas disponibles en la imagen del juego (`Español`, `Inglés`, etc.) y aplica el idioma seleccionado sin manipular archivos manualmente.

---

## 🎮 Ajustes Gráficos y Sistema de Controles

### Ajustes del Motor Gráfico:
* **Campo de Visión (FOV):** Ajuste continuo de 60° a 110°.
* **Iluminación por Píxel (Per-Pixel Lighting):** Alternador para sombreado avanzado o modo clásico de alto rendimiento.
* **Suavizado de Bordes (Anti-Aliasing):** Eliminación de dientes de sierra en geometrías 3D.
* **Sombras Dinámicas:** Proyección de sombras en tiempo real sobre superficies.

### Sistema de Controles Adaptativos:
* **Botones Estilo Xbox Matte:** Botones A, B, X, Y con gráficos vectoriales nítidos de alto contraste.
* **D-Pad Contextual Dinámico:** Cruz direccional unificada que ilumina únicamente la flecha presionada.
* **Disparo y Apuntado Simultáneo (Drag-to-Aim):** Permite barrer y rastrear objetivos mientras se mantiene presionado el gatillo virtual.
* **Vibración Háptica:** Pulso de respuesta física al accionar armas.
* **Sensibilidad de Giroscopio:** Calibración milimétrica para apuntado fino por inclinación del dispositivo.
* **Editor HUD en Tiempo Real:** Reposicionamiento, escalado y opacidad de cada botón en pantalla, con sistema de protección de márgenes para esquinas curvas y notch.
* **Acceso a Consola Táctica:** Atajo secreto configurable (combo `Y-Y-X-B-B-Y`) para alternar la consola sin estorbar los controles.

---

## 🎨 Reglas de Diseño e Identidad Visual

Para mantener la estética profesional y coherente en cualquier desarrollo futuro:

1. **Branding Oficial: "Spartan CE" únicamente.**  
   * **PROHIBIDO** incluir textos o menciones directas como *"Halo"*, *"MCC"*, *"Master Chief Collection"* o *"Halo Protocol"* en interfaces, avisos o subtítulos visibles.
2. **Paleta de Colores Táctica:**
   * **Fondo Principal:** Grafito obsidiana profundo (`#0A0D14` / `#06090E`).
   * **Paneles y Tarjetas:** Pizarra mate oscura (`#101620` a `#182230`).
   * **Bordes y Divisores:** Titanio / platino milimétrico con opacidades sutiles (`rgba(148, 163, 184, 0.25)`).
   * **Acentos:** Blanco puro (`#FFFFFF`) para títulos principales, blanco atenuado (`#B4C3D7`) para texto secundario, ámbar táctico (`#F59E0B`) para alertas y esmeralda (`#34D399`) para confirmaciones de estado.
   * **PROHIBIDO:** Usar bordes fluorescentes cian o morados genéricos de IA, o texturas metálicas pesadas con biseles 3D artificiales.

---

## 📂 Estructura del Repositorio

```
spartance-repo/
├── .github/                      # Workflows y configuración de CI/CD
├── app/
│   ├── build.gradle              # Configuración de compilación Android (targetSdk 34, ARM64)
│   └── src/main/
│       ├── assets/
│       │   ├── motd.json         # Copia local/offline de mensajes dinámicos
│       │   └── updates.json      # Copia local/offline de avisos y configuración
│       ├── java/com/halo/decomp/
│       │   ├── LauncherActivity.java   # Actividad principal, Hub táctico, Dialogs y Telemetría
│       │   ├── MotdManager.java        # Gestor de sincronización de mensajes y avisos
│       │   └── ...                     # Controladores de entrada, puente JNI y HUD
│       └── res/
│           ├── drawable/
│           │   ├── bg_mcc_tactical.jpg # Fondo escénico atmosférico
│           │   ├── ic_tactical_bell.xml# Icono vectorial limpio de campana
│           │   └── ...                 # Vector drawables de botones y controles
├── assets/                       # Iconos y logotipos del proyecto
├── commands.json                 # Diccionario oficial de comandos y trucos
├── motd.json                     # Archivo remoto de mensajes rotativos
├── updates.json                  # Archivo remoto de avisos y versiones
├── Spartan_CE_v1.1.0.apk         # APK binario firmado listo para instalar
└── README.md                     # Documentación oficial del proyecto
```

---

## 🚀 Compilación, Despliegue y Publicación

### 1. Requisitos de Entorno
* **Java:** JDK 17 o superior (compatible con Android Studio JBR).
* **Android SDK:** Compile SDK 34, NDK con soporte ARM64 (`arm64-v8a`).
* **Dispositivo de Pruebas:** Terminal Android físico o emulador ARM64 (Android 9.0+).

### 2. Comandos de Compilación
* **Compilación Debug (Pruebas Locales):**
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
  .\gradlew.bat assembleDebug
  ```
* **Compilación Release (Producción Optimizada):**
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
  .\gradlew.bat assembleRelease
  ```

### 3. Instalación en Dispositivo Físico
```powershell
adb install -r app/build/outputs/apk/release/app-release.apk
# Lanzar la actividad principal:
adb shell am start -n com.kyrn.spartan/com.halo.decomp.LauncherActivity
```

### 4. Publicación en GitHub Releases
Para actualizar el binario en la release oficial de GitHub utilizando la herramienta CLI `gh`:
```powershell
gh release upload v1.1.0 Spartan_CE_v1.1.0.apk --clobber
```

---

## 🗺️ Hoja de Ruta y Próximas Fases

* [x] **Fase 1:** Rediseño del Hub táctico con menú lateral vertical y contenedor flotante difuminado.
* [x] **Fase 2:** Telemetría de hardware en tiempo real, selector de idioma y eliminación de botones intrusivos.
* [x] **Fase 3:** Sistema de rutas JSON dinámicas (`updates.json`, `motd.json`, `commands.json`) con prueba de conexión.
* [x] **Fase 4:** Centro de avisos con campana vectorial interactiva, gestión de IDs únicos y persistencia local de lectura.
* [ ] **Fase 5 (Futura):** Módulo de comentarios comunitarios integrado con servicios de Cloudflare Free (Workers / D1 / KV) respetando los límites de la capa gratuita sin generar costos ni bloqueos de cuota.
* [ ] **Fase 6 (Futura):** Gestor avanzado de mods y paquetes de texturas personalizados descargables directamente desde el Hub.

---

## 🔒 Aviso Legal y Créditos

> **Aviso de Propiedad Intelectual:**  
> Spartan CE es una herramienta independiente y capa de entrada táctil desarrollada estrictamente con fines educativos, de investigación técnica y comodidad de juego para dispositivos móviles.  
> 
> Este repositorio **NO contiene, distribuye ni aloja archivos propietarios, ejecutables comerciales con derechos de autor, texturas originales ni archivos de audio**. El usuario debe suministrar su propio archivo de juego adquirido legítimamente. Todas las marcas registradas pertenecen a sus respectivos propietarios.

* **Arquitectura, Diseño y Desarrollo:** **[@kyrnforge](https://github.com/devlwte)**
* Licencia: Código abierto bajo términos MIT / GPL.
