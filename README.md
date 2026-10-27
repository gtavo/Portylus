# Portylus — lector y anotador Android

Nombre de la aplicación: Portylus. Carpeta actual: `C:\0codex\Portylus`.
Aplicación nativa local, sin cuentas, anuncios, telemetría ni permiso de Internet.
Objetivo: **abrir, leer, anotar y guardar**. La especificación original está en
[docs/ESPECIFICACION_ORIGINAL.md](docs/ESPECIFICACION_ORIGINAL.md).

## Estado actual — lectura, anotación y guardado manual

- Explorador SAF de carpetas y PDF, con permisos persistentes.
- Explorador con árbol de carpetas añadidas a la izquierda y bookmarks debajo; sin pestañas ni grupos separados en el contenido.
- Lista única: primero subcarpetas y después PDF, con tamaño y fecha/hora de modificación. Si el proveedor no entrega un dato, aparece una raya.
- Entrar en una subcarpeta o bookmark despliega su ruta en el árbol y selecciona la ubicación actual.
- Editar → menú de cada raíz/bookmark para subir, bajar o quitar su referencia. Quitar una raíz pide confirmación, conserva la carpeta real y sus archivos y retira los bookmarks dependientes.
- Ordenación Nombre A–Z / Más recientes, manteniendo siempre las carpetas primero; ruta pulsable y recuperación si una carpeta deja de estar disponible.
- Panel izquierdo permanente en tablet (desde 800 dp); en pantallas estrechas se abre con Carpetas.
- Apertura desde «Abrir con» mediante ACTION_VIEW, application/pdf y URI content:// o file://.
- Última página por documento, persistente tras cerrar la aplicación; los nuevos comienzan en la primera.
- Identidad Portylus, símbolo de documento/lápiz y diseño navy/azul/cian coherente con Docenda y Positio.
- Barra nativa de navegación con contraste protegido.
- Los marcadores existentes se conservan en Bookmarks, sin migración destructiva.
- Última carpeta, orden por nombre/fecha y regreso desde el documento.
- Lectura integrada con desplazamiento vertical, zoom y número de página. El contador inferior es pulsable: abre Ir a la página, con teclado numérico, número actual seleccionado y validación entre 1 y el total. Funciona también con la barra oculta y conserva el zoom y la tinta pendiente.
- Buscar texto con la lupa del lector o Más opciones → Buscar texto. Busca fragmentos por defecto; Palabras completas limita la coincidencia a palabras independientes. Sin distinción de mayúsculas, con soporte Unicode, acentos, ligaduras y frases.
- Coincidencias temporales en amarillo y resultado activo en naranja, contador y anterior/siguiente con recorrido circular por todas las páginas. La búsqueda sustituye temporalmente la barra de anotación, se cancela al cambiar el texto y conserva sus resultados al girar.
- Ocultar barra tiene icono de panel y etiqueta visible; al ocultarla aparece el botón con texto Mostrar barra. Los trazos pendientes se conservan.
- Barra del lector adaptativa: en móvil, una fila compacta para nombre y acciones, y otra para las seis herramientas principales sin desplazamiento horizontal. En tablet mantiene una sola fila.
- Iconos con estado activo en cian, nombres accesibles y ayuda al mantener pulsado. El título completo también se puede consultar manteniendo pulsado.
- Las herramientas y acciones mantienen objetivos táctiles de 48 dp. En móvil, la muestra de color abre los estilos de la herramienta activa; con Goma activa muestra sus opciones. Más opciones conserva acceso a ambos editores de estilos.
- Ocultar y Mostrar barra conservan los trazos y vuelven a lectura segura.
- La página mantiene su imagen disponible durante desplazamiento/zoom con dos dedos mientras se prepara otra resolución; ver [corrección de gestos](docs/VALIDACION_GESTOS.md).
- Bolígrafo con color/grosor configurables y tres estilos persistentes, activado explícitamente. Ajustes permite escribir con el dedo, activado inicialmente en móviles (ancho mínimo inferior a 600 dp) y desactivado en tablets.
- Con escritura con dedo desactivada, el dedo navega y el bolígrafo requiere stylus. Con ella activada, un dedo escribe al activar Bolígrafo; dos dedos cancelan el trazo provisional y permiten navegar/ampliar sin marcas residuales. Se conserva la protección de palma del stylus.
- Fluorescente semitransparente con color/grosor y tres estilos persistentes: exclusivamente dedo y recta entre extremos. Activación independiente del bolígrafo y opacidad anterior conservada.
- El botón con muestra de color y número, junto a cada herramienta, abre sus tres memorias. Elegir una, ajustar y pulsar Guardar y usar; no activa el dibujo por sí solo.
- Dos dedos cancelan el fluorescente provisional y permiten navegar/ampliar, sin dejar trazos al levantar uno de ellos.
- Modo Seleccionar texto: tocar una palabra o arrastrar con dedo o stylus, selección visual y Copiar. Desactiva el dibujo; dos dedos permiten navegar. Selección por palabras dentro de una página, sin OCR ni tiradores de ajuste todavía.
- Los trazos completados conservan coordenadas de página y sobreviven al giro, que vuelve a modo lectura.
- Goma con dedo o lápiz: parcial o trazo completo y filtro independiente de escritura/resaltados. Opciones junto a Goma cuando está activa. Dos dedos navegan y cancelan el borrado provisional.
- Guardar escribe únicamente al pulsar el botón o confirmar Guardar y salir/abrir. Nunca hay guardado automático de anotaciones.
- Al salir con cambios: Guardar y salir, No guardar o Cancelar. Si se elige No guardar, la página también se recuerda.
- Más opciones → Guardar como elige expresamente otro destino; también se ofrece cuando el original no admite escritura.
- La copia preparada se verifica antes de escribir; el destino se relee y compara byte a byte. Se detectan cambios externos y se conserva una copia anterior durante la operación.
- Las anotaciones de Portylus se guardan como tinta PDF nativa, sin rasterizar; al reabrir siguen editables y borrables. Las anotaciones ajenas se conservan.
- Los gestos consecutivos de bolígrafo y goma reconocen su revisión inmediatamente: una actualización tardía de la barra no cancela el siguiente gesto.
- Deshacer/Rehacer, hasta 50 gestos en el documento abierto. Funcionan con escritura y goma, mantienen el punto del último guardado y sobreviven al giro.
- La última página se almacena automáticamente y persiste al reiniciar, de forma independiente de Guardar.
- Núcleo de anotaciones y pruebas Android de creación, exportación, reapertura, edición de texto, eliminación y deshacer.

El usuario confirmó el funcionamiento de lectura y posteriormente del bolígrafo, sin especificar una validación separada por tablet.
También confirmó fluorescente, selección y la corrección de gestos. Los estilos se conservan desde la entrega:
[uso y validación de v0.0.5](docs/VALIDACION_v0.0.5.md).
Consulta [la prueba de las nuevas herramientas](docs/PRUEBAS_FLUORESCENTE_TEXTO.md) y
[la validación de esta etapa](docs/VALIDACION_FLUORESCENTE_TEXTO.md).
Se conservan [las pruebas anteriores del bolígrafo](docs/VALIDACION_LAPIZ.md).
La [validación inicial del motor](docs/VALIDACION.md) se conserva como evidencia de la base anterior.

## Cambios locales de movilidad e idiomas (24/10/2026)

- Interfaz en español, inglés y francés mediante recursos Android incluidos sin conexión.
- Ajustes desde el explorador y desde Más opciones del lector: idioma por aplicación,
  escritura con dedo y mantener la pantalla encendida mientras hay un PDF abierto.
- El idioma usa LocaleManager de Android 13+ y se sincroniza con los ajustes del sistema.
  El cambio de idioma conserva el documento y las anotaciones pendientes; vuelve a lectura.
- Ko-fi: enlace discreto con el símbolo oficial solo en Ajustes, acompañado de «¿Te gusta Portylus? Apoya el proyecto.».
- Textos legales originales conservados, con aviso localizado cuando corresponde.

La revisión del 27/10 es 0.1.3 / código 17, con búsqueda y controles
de barra más claros. Mantiene los tres idiomas incluidos en todas las instalaciones
de Play y Ko-fi solo en Ajustes. La 0.1.2 / 16 está publicada en Alpha desde el
26/10 a las 12:03 según Play Console; sus fuentes ya están publicadas.
La nueva entrega mantiene la clave de subida y su propio archivo de fuentes,
ya publicado. Está enviada a revisión de Google en Alpha.
No se han publicado anuncios ni mensajes en grupos o foros.
La mejora del 28/10 añade el salto directo como 0.1.4 / código 18.
Ver [validación de 0.1.4](docs/VALIDACION_v0.1.4.md).
Evidencia funcional: [validación móvil](docs/VALIDACION_MOVIL_IDIOMAS.md).
Subida y reclutamiento: [prueba cerrada](docs/play/PRUEBA_CERRADA.md).

## Compilar

JDK 17, Android SDK 36 y Build Tools 35.0.0. Gradle 8.13 (wrapper), AGP 8.13.2,
Kotlin 2.2.21, Compose BOM 2025.08.01, MuPDF 1.28.5. Android mínimo: 13/API 33.
Arquitecturas incluidas: ARM64 para tablets/teléfonos modernos y x86_64 para emulación.

Configura `JAVA_HOME` y crea `local.properties` con `sdk.dir=RUTA/AL/SDK`
(en Windows, por ejemplo `sdk.dir=C\:/Android/Sdk`).
En Windows:

```powershell
.\scripts\build.ps1
.\scripts\build.ps1 -Tasks assembleRelease,assembleDebugAndroidTest
.\scripts\build.ps1 -Tasks connectedDebugAndroidTest
```

En Linux/macOS, ejecuta primero `chmod +x gradlew` y usa `./gradlew assembleDebug testDebugUnitTest lintDebug`.
La primera descarga de herramientas/dependencias necesita conexión; la aplicación instalada funciona sin ella.
Después, se puede compilar con `--offline` si todas las dependencias están en caché.
El wrapper verifica el SHA-256 de su distribución. Las versiones están fijadas; no se usan versiones `+`.

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.
Instálala mediante el gestor de archivos de Android autorizando esa fuente de instalación,
o mediante `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
La firma debug es solo de desarrollo y debe conservarse para actualizar esa instalación.

`assembleRelease` genera una release **sin firmar** si no se han configurado estas variables:
`PORTYLUS_KEYSTORE_PATH`, `PORTYLUS_KEYSTORE_PASSWORD`, `PORTYLUS_KEY_ALIAS`, `PORTYLUS_KEY_PASSWORD` (compatibles con las variables `PDF_*` previas).
Una release sin firmar no es instalable. No se guarda la clave ni contraseñas en Git.
Con las variables configuradas se firma la release con una clave estable de subida.
En este equipo, scripts/build.ps1 carga automáticamente la clave local de .tools/signing,
con la contraseña protegida por DPAPI del usuario de Windows. Consulta [Firma y custodia](docs/FIRMA_GOOGLE_PLAY.md).

## Versionado y GitHub

Versión preparada: **v0.1.4**, código Android **18**, paquete **`com.gtavoapps.portylus`**.
Entrega y comprobaciones: [VALIDACION_v0.1.4.md](docs/VALIDACION_v0.1.4.md).
`version.properties` fija la versión y el código Android para las compilaciones locales y CI,
sin sufijos de desarrollo ni etiquetas de funciones. Los commits y las recompilaciones no avanzan la versión.

La nueva entrega preparada usa `versionName=0.1.4`, `versionCode=18`. Todas las entregas anteriores se conservan.
A partir de esa entrega, el último bloque recorre de 0 a 9 y después pasa al siguiente:
`0.1.0 → 0.1.1 → … → 0.1.9 → 0.2.0`. No se continuará con `0.0.14`.
El código Android siempre aumenta una unidad por entrega; las recompilaciones no lo avanzan.
Las entregas anteriores conservan su numeración, archivos, evidencias y hashes históricos.
Desde el cambio de nombre, las APK se entregan como `Portylus-v0.1.0.apk`, etc.
Los documentos de validación anteriores conservan sus nombres y hashes históricos.

Repositorio público: [gtavo/Portylus](https://github.com/gtavo/Portylus).
Fuentes completas de v0.1.1 preparadas en `artifacts/Portylus-v0.1.1-source.zip`.
Su publicación está pendiente; el enlace de la app ya está configurado para esa descarga.
Las [fuentes públicas de v0.1.0](https://github.com/gtavo/Portylus/releases/download/v0.1.0/Portylus-v0.1.0-source.zip) se conservan.
El ZIP automático de GitHub contiene la app, pero no sustituye ese archivo completo.
Consulta [compilar desde las fuentes](docs/COMPILAR_DESDE_FUENTES.md).
El workflow preparado compila debug/release, ejecuta JVM/Lint y tiene otro trabajo con emulador Android 16.
El workflow no firma ni publica releases automáticamente.

## Estructura

Un solo módulo `app`, con paquetes `explorer`, `storage`, `preferences`, `pdf`, `input`,
`annotations` y `ui`. Las operaciones nativas se concentran en `PdfSession` y se serializan.
Las copias de lectura están en la caché privada y nunca reemplazan el original.
Las posiciones guardan una clave SHA-256 del identificador/URI y el número de página, sin nombres,
URI en claro ni lista de recientes. SAF directo y SAF a través de árbol comparten identidad.
Si un proveedor cambia el identificador del archivo, se considera otro documento.
El backup Android está desactivado. El identificador definitivo es `com.gtavoapps.portylus`.
Se instala separada de las versiones históricas `es.gtavo.pdf`: las carpetas autorizadas,
preferencias y copias privadas no se transfieren automáticamente. Los PDF externos no se borran.
El protocolo interno de anotaciones de PDF anteriores se conserva para poder seguir editándolos.

## Licencia

Este proyecto se inicia bajo **AGPL-3.0-or-later**, coherente con el uso gratuito de MuPDF.
El texto completo está en `LICENSE`; consulta `THIRD_PARTY_NOTICES.md`.
Al distribuir binarios se debe facilitar el código fuente correspondiente y cumplir los avisos/licencias.
No se incluye ningún SDK comercial ni Google Play Services.

## Marca y guardado

La guía y el símbolo transparente están en Portylus_BrandKit. Docenda y Positio se consultaron solo
como referencia visual. La generación del símbolo se documenta en Portylus_BrandKit/PROMPT.md.

El guardado SAF depende de las capacidades del proveedor Android: no se promete reemplazo atómico
universal. Si fallan la escritura y la restauración, se conserva una copia privada del archivo anterior
y se ofrece recuperarla en otro destino; los cambios de la sesión siguen disponibles para Guardar como.
No existe historial de documentos ni de anotaciones fuera del propio PDF. El historial de deshacer
es de la sesión abierta. Texto mediante teclado continúa pendiente.

## Preparación para Google Play

Guía de registro y tareas pendientes: [docs/PREPARACION_GOOGLE_PLAY.md](docs/PREPARACION_GOOGLE_PLAY.md).
Cumplimiento MuPDF: [docs/CUMPLIMIENTO_MUPDF.md](docs/CUMPLIMIENTO_MUPDF.md).
Esta preparación no constituye una publicación en Google Play. La política definitiva está publicada en
https://gtavoapps.com/portylus/privacidad/ y se incluye íntegra sin conexión en Información → Privacidad.
Información → Licencias → Código de esta versión abre la descarga de las fuentes completas
correspondientes. Las fuentes de MuPDF y del wrapper se incluyen con sus licencias y hashes.

## Apoyo voluntario

Portylus se ofrece gratuitamente y mantiene todas las funciones para todos. Información →
Apoyo explica las aportaciones sin contraprestación; no se añaden cuentas,
SDK de pagos, telemetría ni permiso de Internet. El enlace se abre en el navegador externo.
`supportUrl` en distribution.properties apunta a [ko-fi.com/gtavoapps](https://ko-fi.com/gtavoapps).
El botón Abrir Ko-fi abre esa página en el navegador. Contributor desactivado,
según confirmación del usuario. Si se vacía supportUrl, se oculta el botón operativo.

Guía de configuración y comparación de plataformas: [docs/APOYO_VOLUNTARIO.md](docs/APOYO_VOLUNTARIO.md).
La excepción de Google Play sobre aportaciones requiere verificar sus condiciones; esta
preparación no constituye una aprobación de Google al flujo concreto de Ko-fi.
