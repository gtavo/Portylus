# Validación — fluorescente recto y selección de texto

18 de octubre de 2026. El usuario confirmó el bolígrafo anterior y autorizó fluorescente recto y el
inicio de la selección de texto. Se respeta la entrada original: fluorescente con dedo y bolígrafo
con stylus, con activaciones independientes. La selección de texto es excluyente del dibujo.

## Entrega

- APK: `artifacts/PDF-1.0.0-dev-fluorescente-texto.apk`.
- SHA-256: `76CD1B1AA8E68BF49F59C535F2C00F706F8618807C816CFDC613375A89C53DD7`.
- Mismo paquete y certificado debug que las dos APK anteriores, conservadas sin cambios.
- Firma v2, permisos sin Internet ni almacenamiento global, alineación ZIP y segmentos ELF de 16 KiB verificados.
- El emulador ejecuta Android 16 con páginas de memoria de 4 KiB; 16 KiB se verifica estáticamente.
- Evidencia: `artifacts/validation/fluorescente-texto-apk-verification.txt` y resultados XML de pruebas.

## Alcance

- Amarillo semitransparente, grosor fijo de 12 unidades PDF; siempre dos extremos, sin puntos del
  recorrido intermedio. Vista previa y resultado rectos, incluida dirección diagonal o vertical.
- Dos dedos cancelan el resaltado provisional y pasan a zoom/desplazamiento. No se dibuja con el dedo restante.
- Stylus cancela el provisional del dedo/palma; permite escribir si el bolígrafo estaba activado.
- Selección por palabras con dedo o stylus, en una página; tocar palabra o arrastrar, Copiar y Quitar selección.
- Selección azul con cuadriláteros PDF, ampliación/desplazamiento; varias líneas, acentos y páginas giradas/recortadas.
- Una página de texto en caché, extracción JNI serializada fuera del hilo de interfaz, cola conflada y rechazo de resultados antiguos.
- Respeta el permiso PDF de copiar. Sin texto no se inventa una selección de otra zona. Sin OCR.
- No hay guardado todavía: trazos de ambos tipos solo en memoria; el original permanece intacto.

## Pruebas

12 pruebas JVM y 19 instrumentadas correctas en Android 16/API 36 x86_64. Compilaciones debug y
release correctas; release sin firma de distribución. Lint final: 0 errores, 7 avisos de versión Gradle,
sugerencias KTX y recomendación de API de tamaño de ventana para la barra adaptable.

Se conserva el conjunto del motor nativo y del bolígrafo, ampliándolo con entrada de fluorescente,
convivencia de herramientas, cancelaciones, zoom y selección/copiar sobre el lector real con barra Compose.
Cada prueba del lector verifica que el archivo original conserva exactamente sus bytes.
La prueba de giro ahora comprueba ambos tipos de trazo y el reinicio de los tres modos.

Las primeras 18 pruebas instrumentadas dieron 17 correctas y un fallo en la espera de la prueba de
salida/reapertura: detectaba la vista antigua antes de completar la apertura asíncrona. Se ajustó la
espera para exigir un documento abierto y una vista nueva; no se debilitó la comprobación de contenido.

Se añadió después una prueba de selección conservada durante zoom/desplazamiento con dos dedos y
comprobación de coordenadas tras volver a seleccionar con stylus. La ejecución de 19 pruebas pasó
completa, junto con JVM y Lint. En la revisión visual se detectó que los controles de herramientas
podían invadir Atrás en anchura de 420 px; se limitó el ancho de su fila desplazable para reservar
espacio al título y navegación.
La APK final se volvió a compilar y superó de nuevo las 19 pruebas Android, 12 JVM y Lint.
Se revisó a 420 × 900 px: Atrás y título visibles, fila de herramientas desplazable y acceso mediante
deslizamiento a Seleccionar texto y Ocultar. Captura: `artifacts/screenshots/herramientas-estrecho.png`.

Capturas revisadas del PDF sintético mediante el explorador SAF del emulador:
`artifacts/screenshots/fluorescente.png` y `artifacts/screenshots/seleccion-texto.png`.
Se comprobó el fluorescente amarillo recto conservando el texto negro legible y la selección azul
ajustada al texto con botones Copiar y Quitar selección.

El procedimiento manual de las nuevas herramientas está en `PRUEBAS_FLUORESCENTE_TEXTO.md`.
Los eventos del emulador no certifican los controladores de palma, precisión o fluidez de las tablets físicas.
Pendiente confirmación del usuario de esta nueva etapa en Xiaomi Pad 8 y Samsung SM-P620.
No se han hecho commits, push ni publicaciones remotas.
