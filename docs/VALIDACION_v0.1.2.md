# Portylus 0.1.2 — idiomas tras instalar desde Play y apoyo en Ajustes

26/10/2026. versionName 0.1.2, versionCode 16, applicationId com.gtavoapps.portylus.

## Correcciones

El usuario confirmó que logró actualizar desde Play y notificó que elegir otro
idioma no cambiaba la interfaz. El AAB 0.1.1 conservaba la división por idiomas
activada por defecto: una instalación que solicitaba solo español no incluía las
traducciones inglesas y francesas. La APK completa usada antes para probar sí
incluía los tres idiomas y no detectaba este defecto de distribución.

Se desactiva android.bundle.language.enableSplit. Español, inglés y francés
quedan en el APK base y el selector existente de LocaleManager funciona sin
descargar recursos adicionales. Se mantienen las divisiones por arquitectura y
densidad. Referencia oficial: https://developer.android.com/guide/app-bundle/configure-base#language_changes

Ko-fi desaparece de la cabecera y de los botones de Información. Solo Ajustes
contiene la llamada a apoyar: «¿Te gusta Portylus? Apoya el proyecto.», con texto
pequeño y enlace sin fondo naranja, acompañado del símbolo oficial a 20 dp.
El mensaje está traducido a inglés y francés; se conserva el área táctil de 48 dp.
Las explicaciones legales sobre aportaciones se mantienen sin modificar sus textos.

## Validación local

- Reproducción con el AAB 0.1.1: generado para dispositivo español, instalados
  base + es + mdpi + x86_64; prueba de recursos falla al pedir inglés:
  se esperaba «Settings» y se obtenía «Ajustes».
- Misma especificación con el AAB 0.1.2: solo base + mdpi + x86_64, sin APK
  separado por idioma. Las tres traducciones están instaladas en la base.
- APK del nuevo AAB firmados con clave debug solo para la instrumentación local;
  el AAB y la APK de entrega conservan la clave de subida existente.
- 2 pruebas de instalación desde AAB correctas, con Wi-Fi y datos del emulador
  desactivados: ES → EN → FR → ES → Sistema; persistencia tras volver a abrir,
  ausencia de Ko-fi en explorador e Información y presencia en Ajustes.
- 4 pruebas dirigidas en móvil 360×800 correctas: idiomas, ajustes, dibujo con
  dedo, conservación del documento y de tinta pendiente al recrear y barra visible.
- 43 pruebas Android completas en tablet 1280×800 correctas.
- 30 pruebas JVM correctas; lint sin errores y 6 avisos UseKtx previos.
- preparePlayBundle, assembleRelease/Debug, assembleDebugAndroidTest,
  testDebugUnitTest y lintDebug correctos. bundletool validate y firma AAB correctos;
  firma APK y alineación ZIP/ELF de 16 KiB verificadas. Sin permiso de Internet ni
  acceso global al almacenamiento.
- Nuevo LanguageDeliveryTest usa APIs públicas, por lo que puede probar los APK
  generados del AAB release sin depender de métodos internos del build debug.
- CI añade instalación desde bundleDebug para el dispositivo del emulador y la
  prueba de idiomas sin red, además de las pruebas anteriores. Esa ejecución
  remota no se ha realizado en esta sesión; validada localmente la ruta equivalente
  con el AAB release. Bundletool descargado con hash SHA-256 fijado.
- Recursos ES/EN/FR con las mismas claves. Capturas revisadas visualmente en
  artifacts/validation/v0.1.2/ (home-es, settings-en/fr y settings-support-es).

## Entrega y estado externo

Artifacts: Portylus-v0.1.2.aab, .apk y -source.zip, con SHA-256 separado.
Fuentes completas: aplicación, scripts, recursos, licencia, motor y wrapper MuPDF.
Se preservan todos los artefactos anteriores y una copia previa de los archivos
cambiados en artifacts/Portylus-before-v0.1.2-fix.zip.

La 0.1.2 está preparada localmente. No se ha subido a Play, publicado una release
GitHub ni hecho commit/push. El enlace de fuentes incorporado apunta a la futura
descarga de v0.1.2; publicar las fuentes al distribuir esta revisión.
La validación en emulador no equivale a comprobar el teléfono físico del usuario.
