# PDFiz v0.0.7 — 19/10/2026

Entrega: artifacts/PDFiz-v0.0.7.apk. versionName 0.0.7, versionCode 7.
APK debug instalable y firmada como las anteriores. El paquete sigue siendo es.gtavo.pdf.
SHA-256: 2351CBB3E8CCD3E703C77540067876C2E240CB8A5584617C5A7598402396BFCD

## Entregado

- Logo P/documento/lápiz, libre y transparente; integrado en la cabecera y en el icono adaptativo
  y monocromo Android. Kit, guía, tokens y prompt en PDFiz_BrandKit.
- Diseño navy, azul y cian basado en las guías actuales de Docenda y Positio, sin modificar esos proyectos.
- Guardar explícito sobre el documento abierto; Más opciones → Guardar como para otro destino.
- Aviso al salir: Guardar y salir, No guardar o Cancelar. La apertura externa con cambios tiene
  la misma protección. Nunca se guardan anotaciones por cierre, giro, navegación o deshacer.
- La página se recuerda automáticamente aunque se elija No guardar. Sigue persistiendo al reiniciar.
- Deshacer y Rehacer para trazos y goma: 50 operaciones en la sesión, punto de guardado independiente.
- Tinta PDF nativa editable al reabrir, sin duplicar la capa ni rasterizar páginas. Las anotaciones
  ajenas y las notas de texto preexistentes se conservan.

## Verificación

- JVM: 28 pruebas, 0 fallos.
- Android 16/API 36, x86_64: 35 pruebas, 0 fallos y 0 omitidas.
- Cinco pruebas nuevas de lector real: guardar/deshacer/rehacer/reabrir/volver a guardar sin duplicados;
  No guardar conservando página; Guardar y salir; fuente content de solo lectura y Guardar como;
  tinta en página girada y recortada.
- Fallos inyectados: conflicto externo, escritura parcial, proveedor sin truncado real, permiso denegado
  y fallo simultáneo de escritura/restauración con conservación de copia privada.
- Debug y release compiladas. Lint: 0 errores, 6 avisos de recomendaciones de Gradle/KTX.
- Firma y alineación ZIP/ELF de 16 KiB correctas; ARM64 y x86_64. Sin Internet ni acceso global a archivos.
- La suite Android completa pasó antes de expresar de forma equivalente las constantes URI para Lint;
  la APK final se recompiló, pasó JVM/Lint y se comprobó adicionalmente mediante SAF real.
- Inspección visual a 1280 × 800 y 420 × 900: logo, explorador, barra de lectura, controles de edición
  y documento claro. Guardar y Deshacer/Rehacer permanecen visibles en pantalla estrecha.

## Prueba adicional SAF en la APK final

Se creó un archivo de prueba separado en Documents/PDF-Pruebas, autorizado mediante el selector nativo.
Se activó Fluorescente y se dibujó sobre texto. Antes de pulsar Guardar, el SHA-256 seguía siendo:
d61d0bfc7093499b7c9c0558186d3b23b01b27a133b1b34b05710f428cf16831

Después de Guardar:
a6881dcb42c67b597ec98b99ef80d43aba055ad100603a5dcddb7147439b7af2

Se cerró y reabrió el PDF: fluorescente visible una sola vez, herramientas desactivadas y sin edición
pendiente. Una comprobación independiente con pypdf confirma dos páginas, Ink nativo y Multiply,
texto seleccionable, streams de contenido, imagen original, rotación, MediaBox/CropBox y nota ajena
conservados. No se tocó el Documento.pdf de las pruebas históricas.

Evidencias en artifacts/validation/v0.0.7-* y artifacts/screenshots/PDFiz-v0.0.7-*.
Copia verificada del código anterior: artifacts/PDFiz-v0.0.6-source-before-v0.0.7.zip.

## Límites

Los proveedores SAF no garantizan reemplazo atómico universal. Se prepara y verifica un temporal,
se conserva el destino anterior y se relee el resultado; si falla la restauración, se ofrece recuperar
la copia privada. La compatibilidad física final de lápiz y proveedor corresponde a las tablets reales.
El historial de deshacer dura la sesión abierta. No se ha implementado texto mediante teclado ni
guardado automático de anotaciones. No se ha publicado, hecho commit ni push.

Referencias: [SAF y documentos](https://developer.android.com/guide/topics/providers/document-provider)
y [modos de escritura del proveedor](https://developer.android.com/reference/android/provider/DocumentsProvider).
