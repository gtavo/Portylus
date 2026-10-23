# PDFiz v0.0.9 — explorador con árbol y lista de detalles

Fecha: 20/10/2026. versionName 0.0.9, versionCode 9.

## Entrega

[APK instalable](../artifacts/PDFiz-v0.0.9.apk), 50.895.581 bytes, firma debug estable.
SHA-256: ED8112C60184D9B4BD09842B3E2350FCDCCA94B4832F88337A6B144F0BB53E12.

## Cambios y comprobación

| Petición | Resultado comprobado |
| --- | --- |
| Carpetas añadidas a la izquierda | Árbol de raíces SAF, desplegable por flechas. Selección visible. |
| Bookmarks debajo | Accesos en la misma barra lateral, sin pestañas. Se conservan los existentes. |
| Carpetas antes de archivos | Una única lista, sin cabeceras de grupos; directorios primero al ordenar por nombre o modificación. |
| Subcarpetas sincronizadas con árbol | Entrar desde el listado, bookmark o última carpeta guardada despliega los antecesores y revela la selección. |
| Editar raíces y bookmarks | Editar → menú de cada acceso → Subir, Bajar o Quitar. Orden persistente probado mediante lectura de DataStore. |
| Quitar sin borrar | Confirmación de raíz, cancelación probada, retirada de bookmarks dependientes; PDF conserva los mismos bytes y la carpeta sigue accesible en el proveedor de prueba. |
| Tamaño y modificación | COLUMN_SIZE y fecha/hora por archivo. Columnas en espacio amplio; metadatos bajo nombre en móvil. Datos ausentes muestran una raya. |
| Navegación y errores | Ruta pulsable, regreso del PDF a la misma carpeta, Atrás Android y salida recuperable de una ubicación sin acceso. |

Panel fijo desde 800 dp; en móvil el botón Carpetas abre el mismo árbol como panel lateral.
No se añaden funciones de movimiento, renombrado o borrado de archivos reales.
Las consultas SAF resuelven las columnas por nombre; la prueba devuelve columnas adicionales
para detectar lecturas accidentales de FLAGS como tamaño.

## Validación final

- Compilación debug, 28 pruebas JVM y Lint correctos.
- Suite completa: 39 pruebas Android, 0 fallos, 0 errores y 0 omitidas.
- Total: 67 pruebas distintas. Los ciclos específicos adicionales repiten casos de esta suite.
- Tres casos del explorador ejecutados por separado en tablet y móvil; la repetición móvil final usa la APK entregada.
- Revisión visual de capturas: tablet 1280 × 800 y móvil 420 × 900, densidad 160.
- Lint debug: 0 errores, 6 avisos UseKtx; sin nuevos errores.
- assembleRelease correcto. La variante release queda sin firmar al no configurar clave de distribución; la entrega instalable es la debug firmada.
- APK: CRC ZIP, firma, alineación ZIP/ELF de 16 KiB, ARM64 y x86_64, mínimo API 33 y objetivo API 36 verificados.
- Sin permiso Internet ni permisos de almacenamiento global.
- APK v0.0.8 conservada y comprobada contra su hash anterior.
- Copia de fuentes previa: artifacts/PDFiz-v0.0.8-source-before-v0.0.9.zip, ZIP íntegro.

## Incidencia detectada durante la regresión y resuelta

La primera ejecución completa dejó 1 fallo en la prueba previa de goma.
La revisión del gesto anterior llegaba desde Compose después del DOWN del siguiente;
PdfReaderView lo interpretaba como una edición externa y cancelaba el nuevo provisional.

El lector reconoce ahora su propia revisión inmediatamente tras add/replace.
Deshacer sigue cancelando un gesto provisional cuando entrega una revisión externa.
Dos pruebas fuerzan la actualización tardía entre DOWN y UP en un solo turno del hilo
principal. Ambas fallan contra la APK anterior a la corrección y pasan contra la final.
La suite completa posterior pasa con sus 39 casos. Se conservan las evidencias del
fallo inicial y de la comparación antes/después; no se han añadido esperas para evitarlo.

## Evidencias

- [Captura tablet: lista y árbol](../artifacts/screenshots/PDFiz-v0.0.9-carpeta.png)
- [Ruta desplegada](../artifacts/screenshots/PDFiz-v0.0.9-ruta.png)
- [Modo edición](../artifacts/screenshots/PDFiz-v0.0.9-edicion.png)
- [Lista móvil](../artifacts/screenshots/PDFiz-v0.0.9-carpeta-movil.png)
- [Panel móvil](../artifacts/screenshots/PDFiz-v0.0.9-edicion-movil.png)
- [Resumen de pruebas y APK](../artifacts/validation/v0.0.9-summary.json)
- [Resultados Android XML](../artifacts/validation/v0.0.9-android-tests.xml)
- [Compilación debug/JVM/Lint](../artifacts/validation/v0.0.9-debug-jvm-lint.log)
- [Compilación Android/release](../artifacts/validation/v0.0.9-android-release-build.log)
- [Explorador móvil final](../artifacts/validation/v0.0.9-browser-mobile.log)
- [Prueba de gestos antes](../artifacts/validation/v0.0.9-gesture-before.log)
- [Prueba de gestos después](../artifacts/validation/v0.0.9-gesture-after.log)
- [Verificación APK](../artifacts/validation/v0.0.9-apk-verification.log)

Los resultados proceden de un emulador Android 16/API 36. No certifican aquí
proveedores SAF externos concretos ni comportamiento de un stylus físico.
No quedan peticiones del explorador pendientes de implementar en esta etapa.
