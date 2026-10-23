# Portylus v0.0.11 — identidad y preparación de Google Play

Fecha: 21/10/2026. versionName 0.0.11, versionCode 11.

## Cambios

Proyecto trasladado a `C:\0codex\Portylus`; nombre Gradle, nombre visible, recursos y
BrandKit renombrados. Se conserva el símbolo original P y su prompt literal de procedencia.
Se mantienen `es.gtavo.pdf`, las claves de anotaciones propias y los prefijos de recuperación
para conservar compatibilidad. Carpetas antiguas vacías; Windows mantiene abierta la raíz anterior.

Se incorporan privacidad, avisos de MuPDF/componentes y AGPL completa sin conexión; acceso
desde el explorador y el menú del lector. Correo de soporte: gtavo.apps@gmail.com.
`distribution.properties` centraliza los datos públicos. Se añade `preparePlayBundle`,
que valida datos y firma antes de generar el bundle de subida. CI también compila el AAB.

Fuentes oficiales de MuPDF 1.28.5, wrapper Android y procedencia archivados localmente;
se fija el AAR por SHA-256 contrastado con Artifex. El submódulo del wrapper coincide con
el commit del tag del motor. La AGPL elegida requiere publicar las fuentes antes de distribuir.

## Verificación

- APK debug, APK release y AAB release compilan; bundle release sin firma, verificado en ZIP.
- 28 pruebas JVM y 40 Android, sin fallos/errores/omisiones. Emulador Android 16/API 36.
- Lint: 0 errores, 7 recomendaciones UseKtx (6 previas y 1 en la pantalla legal).
- APK: firma debug, API mínima 33/objetivo 36, ARM64 y x86_64, alineación ZIP y segmentos
  ELF de 16 KiB verificados. Sin permisos de Internet ni almacenamiento global.
- Pantalla legal revisada visualmente en tablet 1280×800 y móvil 420×900. Privacidad,
  avisos de terceros y AGPL completa comprobados con el árbol de accesibilidad.
- Validación de publicación rechaza correctamente las URLs vacías; fallo esperado de
  `verifyPlayRelease`, separado de la compilación y pruebas que pasan.
- Las 10 APK anteriores conservan sus SHA-256. Copia previa de 83 archivos fuente íntegra
  en `artifacts/Portylus-before-rename-2026-10-21.zip`, con CRC ZIP y SHA-256.
- Archivo de fuentes de esta versión preparado por `scripts/export-source.py`, con fuentes
  completas del motor/componentes y wrapper; CRC y hashes verificados.

## Entregas y límites

- [APK de prueba](../artifacts/Portylus-v0.0.11.apk), firma debug estable.
- [AAB técnico SIN FIRMAR](../artifacts/Portylus-v0.0.11-unsigned.aab), no listo para subir.
- [Fuentes completas](../artifacts/Portylus-v0.0.11-source.zip), preparación local sin publicación.
- [Resultados](../artifacts/validation/v0.0.11-summary.json), logs v0.0.11-build-final.log,
  v0.0.11-android.log, v0.0.11-apk-verification.log y v0.0.11-publication-gate.log.
- [Pantalla legal tablet](../artifacts/screenshots/Portylus-v0.0.11-legal-tablet.png) y
  [móvil](../artifacts/screenshots/Portylus-v0.0.11-legal-mobile.png).

Faltan titular legal, URLs públicas de privacidad/fuentes, clave de subida, recursos finales
de tienda y trámites/pruebas de Play. No se ha publicado nada ni creado una clave de producción.
No se certifica stylus físico, funcionamiento real con páginas de 16 KB ni reconstrucción
idéntica del AAR nativo. Ver [guía de registro](PREPARACION_GOOGLE_PLAY.md) y
[cumplimiento MuPDF](CUMPLIMIENTO_MUPDF.md).
