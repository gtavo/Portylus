# PDFiz v0.0.8 — explorador de archivos — 20/10/2026

Entrega: artifacts/PDFiz-v0.0.8.apk. versionName 0.0.8, versionCode 8.
APK debug instalable, misma firma de desarrollo y paquete es.gtavo.pdf.
SHA-256: 6560A58937C4966C582D4362479D489A229E752F8E8C51AC02C294F85DD2636D

## Cambios

- Inicio Mis carpetas con dos pestañas: Carpetas añadidas y Favoritos. Se muestran en
  listas verticales distintas; los accesos no se mezclan con los PDF ni con la ruta.
- Cada carpeta muestra título propio, antecesoras pulsables y regreso directo a Mis carpetas.
  Atrás desde una raíz autorizada vuelve al inicio, en lugar de salir de la aplicación.
- Contenido agrupado en Carpetas y Archivos PDF, con iconos distintos y fechas.
- Menú explícito Nombre A–Z / Más recientes. Las acciones de reordenar favoritos son
  Subir favorito / Bajar favorito y no se confunden con ordenar los archivos.
- Estrella para añadir o quitar la carpeta actual de favoritos.
- Menú propio para cada carpeta añadida: Quitar de PDFiz con explicación y confirmación.
  Solo se elimina el acceso y sus favoritos dependientes, nunca el directorio real.
- Los marcadores, permisos y carpetas anteriores conservan el formato DataStore browser_v1.
- Se recupera la última carpeta; si se vuelve al inicio, esa elección también se guarda.
  Se conserva la carpeta y la posición del listado al abrir y cerrar un PDF.
- Fallos de acceso diferenciados de carpeta vacía, con Reintentar e Inicio, sin avisos duplicados.
- Se conserva el nombre PDFiz: no se había elegido aún un nombre definitivo.

## Verificación de la entrega final

- 28 pruebas JVM, 0 fallos, 0 errores.
- 38 pruebas Android en emulador Android 16/API 36 x86_64, 0 fallos, 0 omitidas.
- Las 3 pruebas del nuevo explorador se repitieron a 420 x 900: 3 superadas.
- Navegación real de Compose y proveedor de documentos del APK de pruebas con concesiones
  Android por árbol: inicio, favoritos, subcarpetas, ruta, Atrás, ordenación, apertura y
  cierre de PDF, reordenación/quitar favoritos y quitar acceso sin alterar los bytes del PDF.
- Restauración de última carpeta y salida de una ubicación no disponible.
- Suite anterior de lectura, entrada de lápiz/dedo, goma, selección, guardado manual,
  deshacer/rehacer y posiciones de lectura conservada.
- Debug y release compiladas; release sin firma, no se distribuye como instalable.
- Lint debug: 0 errores, 7 avisos de recomendaciones KTX.
- APK verificada: CRC ZIP, firma, alineación ZIP/ELF a 16 KiB; ARM64 y x86_64.
  Sin permiso de Internet ni acceso global al almacenamiento.
- Capturas revisadas visualmente a 1280 x 800 y 420 x 900: inicio, favoritos, contenido
  con carpetas/PDF, ruta con varios niveles y fallo de acceso. No se observan solapamientos.
- APK v0.0.7 preservada y su hash comprobado. Copia de fuentes anterior verificada:
  artifacts/PDFiz-v0.0.7-source-before-v0.0.8.zip.

## Ajustes realizados durante las pruebas

El proveedor nuevo vive solo en el APK de pruebas. Se configuró con la protección
MANAGE_DOCUMENTS exigida por Android y una actividad del propio proveedor que concede
acceso de lectura a sus árboles de prueba, sin cambiar permisos de la aplicación final.

La primera regresión completa tuvo un fallo en la última aserción de borrado de una
prueba que inyecta eventos directamente a la vista. Se añadió la espera de foco del lector
tras cerrar las opciones de goma, antes de inyectar los eventos. La ejecución completa
posterior pasó las 38 pruebas; no se modificó el lector ni el algoritmo de la goma.

Las rutas locales de JDK, SDK y caché compartida se actualizaron al proyecto hermano
actual Positio_viewer, porque la antigua carpeta pruebas_apk ya no existe.

## Evidencias y límites

Informes y registros: artifacts/validation/v0.0.8-*.
Capturas: artifacts/screenshots/PDFiz-v0.0.8-*.

La revisión Android y visual corresponde a un emulador. No sustituye la comprobación
en las tablets físicas ni certifica todos los proveedores SAF de terceros. La organización
afecta solo a la navegación de PDFiz: no reorganiza archivos físicos. Las anotaciones siguen
guardándose exclusivamente por acción explícita; la página se recuerda independientemente.
No se ha publicado en Google Play, hecho commit ni push.
