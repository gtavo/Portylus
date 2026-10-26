# Movilidad, idiomas y escritura con dedo

Revisión local de Portylus, 24/10/2026. Base 0.1.0 / código 14; sin publicación.

## Cambios

- Cabecera móvil de 56 dp, símbolo de 26 dp y sin subtítulo de marca.
- Lector en móvil: dos filas de 48 dp, acciones fijas y seis controles visibles
  (bolígrafo, fluorescente, selección, goma, estilos/opciones y ocultar).
  Tablet conserva una fila; la distribución también responde al tamaño de letra.
- Menos márgenes y títulos más contenidos en el explorador estrecho.
- Recursos de interfaz en español, inglés y francés, con formatos y descripciones
  de accesibilidad; fechas y grosores se adaptan al idioma.
- Idioma por aplicación con LocaleManager: Sistema, Español, English, Français.
- Preferencias locales de escritura con dedo y pantalla encendida. En móvil se
  permite dedo por defecto; cada documento comienza en lectura y exige activar
  Bolígrafo. Dos dedos cancelan el trazo provisional y navegan/amplían. Con el
  modo de dedo desactivado se conserva el flujo independiente de lápiz y resaltador.
- Cambiar idioma conserva el ViewModel, documento y trazos pendientes de guardar.
- Botón Ko-fi coral con símbolo oficial, navegador externo y errores controlados.
- No se cambian las políticas ni licencias legales: textos en su idioma original,
  con aviso localizado. No se añade traducción en línea, SDK ni permiso de Internet.

## Verificación

- assembleDebug, assembleRelease, testDebugUnitTest, lintDebug y APK de pruebas Android.
- Lint: 0 errores, 6 recomendaciones UseKtx preexistentes.
- 30 pruebas JVM correctas, 0 fallos/errores/omitidas.
- 41 pruebas Android correctas en la compilación final. Las 2 pruebas dirigidas
  de móvil 320 dp y la prueba de móvil 360 dp con letra al 130 % también pasan. Logs de
  artifacts/validation/mobile-settings. Se conserva el primer fallo detectado y
  los registros finales posteriores a su corrección.
- Móvil 320×800 dp: todos los controles visibles simultáneamente, con objetivos
  táctiles de 48 dp. Prueba de idiomas, estilos/goma, ocultar/mostrar y dedo.
- Móvil 360×800 dp con letra al 130 %: idiomas, preferencias y dedo comprobados.
- Tablet 1280×800 dp: regresión de lectura, gestos, lápiz, selección y guardado.
- Las pruebas de idioma cambian español → inglés → francés con tinta pendiente,
  verifican el mismo documento, sus trazos y que el PDF fuente no se ha escrito.
- Revisión visual de capturas del explorador, lector y ajustes en los tres idiomas.
- Logo de Ko-fi procedente del kit oficial; origen en third_party/kofi/README.md.

## Entrega local

APK: artifacts/Portylus-prueba-movil-2026-10-24.apk, firmada con la misma clave local
de subida que las entregas anteriores. La comprobación de firma/alineación y sus
hashes se guardan en artifacts/validation/mobile-settings.
Fuentes completas: artifacts/Portylus-prueba-movil-2026-10-24-source.zip, incluyendo
los archivos originales de MuPDF y su wrapper fijados por hash.

No se sobrescriben APK/AAB/ZIP oficiales de v0.1.0. Esta APK conserva 0.1.0 / 14
para prueba local; no es un AAB nuevo ni una actualización publicada en Play.
La validación sintética en emulador no certifica un teléfono ni un lápiz físico.
