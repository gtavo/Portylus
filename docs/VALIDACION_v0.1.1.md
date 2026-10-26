# Portylus 0.1.1 — móvil, idiomas y prueba cerrada

Preparación: 25/10/2026. versionName 0.1.1, versionCode 15,
applicationId com.gtavoapps.portylus, Android 13+ / API 33, objetivo API 36.

## Funciones

Integra los cambios de movilidad del 24/10: barra móvil con seis herramientas
visibles, cabecera reducida, idiomas ES/EN/FR, ajustes persistentes de escritura
con dedo y pantalla encendida, y botón Ko-fi con símbolo oficial. Las políticas
legales originales se conservan. Ver VALIDACION_MOVIL_IDIOMAS.md para el detalle.

## Preparación de distribución

- Copia anterior: artifacts/Portylus-before-v0.1.1.zip, CRC y SHA-256 comprobados.
- Incrementada versión/código; fuentes configuradas para su descarga exacta v0.1.1.
- Firma local de subida existente, sin regenerar claves ni incluirlas en fuentes.
- AAB firmado y validado con bundletool, manifiesto 0.1.1/15, API 33–36 y dos ABIs.
- APK: firma y alineación 16 KiB comprobadas, sin INTERNET ni acceso global a archivos.
- preparePlayBundle, assembleRelease/Debug, testDebugUnitTest, lintDebug y test APK correctos.
- 30 pruebas JVM correctas y Lint sin errores (6 recomendaciones UseKtx preexistentes).
- 41 pruebas Android correctas en tablet. Runner de pruebas fija ES para que las
  etiquetas históricas sean estables en emuladores ingleses; la prueba de idiomas
  sigue comprobando ES → EN → FR con tinta sin guardar y sin alterar el PDF fuente.
- 2 pruebas dirigidas de móvil correctas en 360×800 dp con el runner nuevo.
- CI repite la prueba dirigida de interfaz en viewport móvil después de tablet.
- Notas de versión y fichas de tienda en ES, EN y FR. Textos para reclutar testers.

## Estado externo

Google Play Console confirma la 0.1.0/14 publicada en prueba interna y el canal
cerrado Alpha. 0.1.1/15 se ha subido al borrador de Alpha con notas en tres idiomas.
Un borrador no equivale a una versión publicada ni a una instalación por un tester.
No se ha anunciado la prueba en ningún foro ni enviado mensajes a terceros.

Publicar las fuentes de v0.1.1 antes de distribuir la app. Las fuentes completas
incluyen el motor y el wrapper MuPDF originales verificados por sus hashes. Los
archivos históricos v0.1.0 y la APK candidata del 24/10 se conservan.

La validación local en emulador no certifica un teléfono ni stylus físico. La
aceptación en Play y el acceso a producción dependen de su revisión y del test real.

Estado externo actualizado y pendientes: [ESTADO_v0.1.1.md](play/ESTADO_v0.1.1.md).
