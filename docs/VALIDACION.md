# Validación de la primera entrega

Evidencia histórica de la base de lectura. La etapa actual está documentada en [VALIDACION_LAPIZ.md](VALIDACION_LAPIZ.md).

Fecha: 16 de octubre de 2026. Nombre provisional: PDF. Versión de trabajo: `1.0.0-dev`.

APK entregada: `artifacts/PDF-1.0.0-dev.apk` (49.748.851 bytes).
SHA-256: `65872C6761773136A2FB1E7BA1408851ADC614627473566965A228608576A646`.

## Alcance

Primera base instalable: explorador y lector, con el núcleo de anotaciones comprobado mediante pruebas.
Las herramientas de anotación y el guardado sobre el original todavía no están expuestos en la interfaz.
No se considera completada la aplicación ni cerrada su validación física.

## Comprobaciones automáticas

- `assembleDebug`: APK de desarrollo instalable y firmada.
- `assembleRelease`: compilación de distribución sin firmar; no se ha creado una clave de release.
- `testDebugUnitTest`: 9 pruebas JVM, sin fallos (6 de política de entrada, 3 de navegación/ordenación).
- `connectedDebugAndroidTest`: 2 pruebas del motor en emulador Android 16/API 36, x86_64.
- `lintDebug`: sin errores; quedan avisos de versiones de dependencias y sugerencias de extensiones Kotlin.
- Firma v2 de la APK debug comprobada con `apksigner`.
- ZIP y todos los segmentos ELF de ARM64/x86_64 comprobados para alineación de 16 KiB.
- Manifest sin `INTERNET` ni permisos globales de almacenamiento. Solo permiso propio de receptor interno de AndroidX.

El emulador ejecutado utiliza páginas de memoria de **4 KiB**. La inspección estática de 16 KiB
no sustituye ejecutar la aplicación en un dispositivo o emulador con ese tamaño de página.

## Prueba del motor

Se utiliza un PDF sintético reproducible (`scripts/generate-fixture.ps1`) con dos páginas,
texto, una imagen, una página girada y recortada, y una anotación ajena preexistente.

Se comprueba:

1. Crear `Ink` opaco, `Ink` semitransparente y `FreeText` con texto latino acentuado.
2. Exportar a otro archivo y comprobar que los bytes del documento de entrada no cambian.
3. Reabrir y recuperar identificadores, puntos, tamaño y contenido del texto.
4. Editar y mover el texto después de reabrir.
5. Borrar un trazo propio, deshacer y exportar de nuevo.
6. Comparar contenidos de página e imagen, MediaBox, CropBox y Rotate entre origen y resultado.
7. Comprobar que el texto original continúa siendo localizable y que permanece la anotación ajena.
8. Comprobar opacidad y presencia de apariencia PDF del fluorescente.
9. Renderizar el resultado con MuPDF y, de forma independiente, con `PdfRenderer` de Android.
10. Rechazar el borrado por identificador ajeno y la exportación sobre el propio archivo de entrada.

Los informes se generan en `app/build/reports/tests/testDebugUnitTest/`,
`app/build/reports/androidTests/connected/debug/` y `app/build/reports/lint-results-debug.html`.

## Recorrido visible comprobado en emulador

Autorización SAF de una subcarpeta local; listado de carpeta y PDF; navegación a subcarpeta y vuelta;
creación de marcador; apertura y renderizado del documento; desplazamiento; regreso al explorador;
cierre del proceso y reapertura recuperando carpeta, marcador y acceso sin un nuevo selector.
Se revisan capturas locales en `artifacts/screenshots/` (no se incluyen en Git).

## Límites pendientes

- Lápiz oficial, presión, palma y multitáctil real de Xiaomi Pad 8 y Samsung SM-P620.
- Documentos grandes y rendimiento/memoria sostenidos en ambas tablets.
- Zoom por regiones a alta resolución; el lector inicial limita la resolución y memoria de cada bitmap.
- Conservación exacta del punto de lectura al girar la pantalla.
- Herramientas, texto editable y deshacer conectados a la interfaz.
- Fuente Unicode incrustada y apariencia de grosor variable por presión.
- Guardar sobre SAF, Guardar como, recuperación y pruebas de interrupción/espacio insuficiente.
- Guardado compatible con lectores externos concretos: hay prueba entre dos motores, no revisión en todas las aplicaciones.
- Ejecución de ARM64 y de un entorno real de 16 KiB.
- Firma de distribución y ejecución real del workflow en GitHub.

Git se ha inicializado localmente. No se han hecho commits, push, creado remoto ni publicado releases.
