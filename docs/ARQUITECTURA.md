# Decisiones iniciales — 16 de octubre de 2026

Se adopta la propuesta revisada: Kotlin, Compose, vista Android de lectura, SAF, DataStore y núcleo MuPDF Java/JNI.
Nombre provisional PDF, paquete `es.gtavo.pdf`, licencia AGPL-3.0-or-later.

## Base inicial de lectura

Se prepara y verifica el motor antes de exponer herramientas de escritura. El producto abre en el
explorador y permite lectura; no existen botones de anotación o guardado que prometan una función incompleta.
La exportación del motor escribe exclusivamente un archivo distinto y comprueba su reapertura.
La sobrescritura SAF, Guardar como y recuperación ante errores pertenecen a la fase de guardado.

## Anotaciones

`Ink` para bolígrafo y fluorescente libre (opacidad 0,28); `FreeText` para texto.
Identificador PDF `NM`, propietario `GtavoPdfOwner=es.gtavo.pdf/v1` y tipo propio `GtavoPdfKind`.
El borrado solo opera sobre objetos con propietario e identificador propios; nunca ejecuta redacciones ni edita los contenidos originales.
El borrado inicial es por anotación completa. Deshacer utiliza el journal del documento, limitado a la sesión.
No se aplanan anotaciones ni se guardan capturas de páginas.
La prueba inicial usa Helvetica para texto latino; fuentes Unicode incrustadas y presión variable quedan pendientes.

## Entrada — plan inicial

La política pura distingue dedo, stylus, borrador físico y tipo desconocido. El dedo jamás produce
bolígrafo; el stylus jamás produce fluorescente. Dos dedos tienen prioridad de navegación.
Durante contacto del stylus se ignoran nuevos contactos de dedo para reducir marcas de palma.
La conexión de esta política con trazos provisionales, `ACTION_CANCEL`, `FLAG_CANCELED` y las herramientas
se describía como la etapa siguiente de la base de lectura.

## Etapa bolígrafo

El usuario confirmó la primera APK y pidió solo escritura con lápiz. Se incorpora un único interruptor
de bolígrafo, desactivado al abrir y al recrear pantalla. Ocultar la barra lo desactiva.
El dedo navega y no genera tinta. Los eventos con stylus se siguen por `pointerId`, comprobando
`TOOL_TYPE_STYLUS`; ni un borrador físico ni un lápiz capacitivo identificado como dedo generan bolígrafo.

El trazo provisional usa muestras históricas y se confirma solo al levantar el stylus sin cancelación.
`ACTION_CANCEL` descarta el provisional; `FLAG_CANCELED` se aplica únicamente al puntero que se levanta.
Un contacto de palma cancelado no cancela el trazo legítimo del stylus. Los dedos quedan fuera de navegación
desde que aparece un puntero no dedo hasta levantar todos los contactos de esa secuencia.

`PenSession` vive junto al documento en el ViewModel y contiene únicamente trazos vectoriales de esta sesión.
`PagePlacement` comparte la transformación entre renderizado, muestras de entrada y trazos, incluyendo origen,
zoom y desplazamiento. La capa se dibuja recortada a su página; no modifica el bitmap ni el documento nativo.
El color es azul y el grosor fijo, sin presión variable todavía. La conversión a `Ink` nativo se reserva a guardado.
Al salir con trazos se pide descartar o continuar; aún no existe una acción Guardar en la interfaz.

## Fluorescente recto y selección — 18 de octubre de 2026

El fluorescente conserva la separación de entrada de la especificación original: solo dedo,
activación independiente del bolígrafo. Una recta entre ancla y extremo sustituye al recorrido libre,
tanto en la vista previa como en los dos puntos almacenados. Amarillo, 12 unidades de página y
opacidad 72/255 con mezcla Multiply en pantalla para conservar la legibilidad del contenido.
La recta admite cualquier dirección y se recorta a la página donde comenzó.

Un segundo dedo cancela el provisional y entrega la secuencia a los detectores de zoom/desplazamiento.
El dedo restante continúa navegando hasta levantar todos los contactos; no comienza un nuevo resaltado.
La aparición de un stylus cancela un fluorescente provisional que pudiera haber sido una palma.
PenSession almacena ambos tipos en memoria. Ocultar herramientas, recrear pantalla y abrir documento
desactiva los modos. El guardado sigue pendiente.

Seleccionar texto es un modo separado y excluyente del dibujo. Permite tocar una palabra o arrastrar
sobre texto existente, usando dedo o stylus; selección por palabras de una única página. Copiar escribe
en el portapapeles Android solo tras pulsar el botón. No crea anotaciones, OCR ni notas con teclado.
Cambiar de herramienta limpia la selección; dos dedos permiten navegar.

PdfSession utiliza StructuredText de MuPDF para ajustar a palabras, recuperar el texto y sus cuadriláteros.
Solo mantiene una página extraída y libera su objeto nativo al cambiar de página, editar o cerrar.
Extracción fuera del hilo principal; canal conflado y revisión de resultados para no acumular gestos
ni mostrar selecciones antiguas después de cancelar/cambiar modo. Los cuadriláteros usan PagePlacement,
igual que la tinta, por lo que siguen el zoom, desplazamiento, rotación y recorte del PDF.
Un comienzo alejado del texto no selecciona palabras de otra zona. Se respeta el permiso PDF de copia.
Fuentes: [API Java StructuredText 1.28.5](https://github.com/ArtifexSoftware/mupdf/blob/1.28.5/platform/java/src/com/artifex/mupdf/fitz/StructuredText.java)
y [implementación JNI](https://github.com/ArtifexSoftware/mupdf/blob/1.28.5/platform/java/jni/structuredtext.c).

## Estilos — v0.0.5

Dos grupos independientes de tres estilos (RGB y grosor) en un DataStore privado específico, con
índice de memoria seleccionada. Escritura atómica; la interfaz espera su resultado antes de confirmar.
Abrir el editor de estilos no activa el dibujo. Las herramientas siguen desactivadas al abrir PDF
y no se habilitan hasta cargar las preferencias. El estilo se captura al inicio del trazo y queda
incluido en cada anotación completada. No se parametriza opacidad: fluorescente 72/255 y bolígrafo 255.
Rangos, uso y validación en `VALIDACION_v0.0.5.md`.

## Recursos y archivos

Todo acceso nativo se serializa para evitar carreras entre renderizado, edición y cierre.
Renderizado fuera del hilo de interfaz, caché de bitmaps limitada a 32 MiB y resolución máxima acotada.
La caché conserva el último bitmap completado de cada página y lo transforma durante el gesto hasta
tener el sustituto. Un único render por página y resolución en pasos de 128 px evita cancelaciones
continuas por pequeñas variaciones de separación entre dedos. Solo se cancela trabajo de páginas
que salen de pantalla. Ver `VALIDACION_GESTOS.md` para reproducción del blanco y comprobación por píxeles.
Los PDF se leen a una copia temporal privada; no se escriben los originales.
Las ubicaciones SAF guardan URI y el recorrido autorizado, sin intentar obtener rutas físicas.
Las preferencias de carpeta no incluyen un historial de documentos.

## Fases siguientes

### Incidencias del 19/10/2026 — PDFiz v0.0.6

El nombre del producto pasa a PDFiz; el paquete Android se conserva para actualizar instalaciones.
El manifest admite ACTION_VIEW + application/pdf para content/file. MainActivity procesa tanto
el arranque como onNewIntent; el ViewModel copia la fuente de solo lectura a caché sin exigir permisos
persistentes. Una petición externa con trazos sin guardar pide descartarlos o continuar antes de sustituirlos.
La limpieza inicial de caché termina antes de abrir un documento externo.

Quitar ubicación actualiza DataStore, elimina marcadores dependientes y última ubicación, cancela
listados pendientes y libera el permiso SAF. No ejecuta ninguna operación de borrado en el proveedor.

ReadingPositions almacena pares SHA-256/última página en SharedPreferences privadas con apply,
que Android sincroniza durante las transiciones de ciclo de vida. Los documentos SAF se identifican
por autoridad + documentId, independientemente del árbol. El lector aplica la página inicial después
de disponer de tamaños y dimensiones; el ViewModel conserva la página durante recreaciones.

InkEraser recorta polilíneas contra la cápsula barrida por una goma circular, mediante intersecciones
analíticas de segmentos con disco y franja; no depende de la frecuencia de muestras. Conserva color,
grosor, tipo y página en cada fragmento. El modo completo elimina los trazos alcanzados. El filtro
acepta exclusivamente PEN o HIGHLIGHTER. Solo afecta a PenSession, nunca al contenido nativo del PDF.
El lector muestra una previsualización transaccional: confirma al levantar el contacto y descarta
ante cancelación, cambio de herramienta, pérdida de foco o navegación con dos dedos. Diámetro de 24 dp.

Las barras usan enableEdgeToEdge con SystemBarStyle.dark, contraste nativo de navegación habilitado
y fondo oscuro de Scaffold hasta los bordes. safeDrawing protege el área interactiva del lector.
No se dibujan botones del sistema ni se añade espacio aparte del inset nativo.

### Trabajo pendiente

1. Validar en las tablets el explorador, la lectura y los permisos persistentes.
2. Ajustar el renderizado y la transformación de coordenadas con documentos grandes.
3. Incorporar herramientas independientes, rechazo de palma y presión si está disponible.
4. Incorporar texto editable mediante teclado a la interfaz.
5. Validar guardado SAF en los proveedores y tablets reales del usuario.
6. Pulido visual, firma estable y distribución.

## v0.0.7 — identidad, guardado manual y deshacer/rehacer

La paleta y forma de la interfaz parten de los BrandKits actuales de Docenda y Positio.
PDFiz_BrandKit contiene el símbolo PNG transparente, sus tokens y las instrucciones de uso.
PdfizTheme centraliza la paleta; el icono de instalación es adaptativo y la cabecera usa el símbolo libre.
Guardar, Deshacer y Rehacer permanecen accesibles; las herramientas usan una segunda fila compacta.

PenSession almacena estados vectoriales inmutables, historial acotado de 50 operaciones y un punto
de guardado separado. Un gesto de goma confirmado equivale a un paso. Cancelar no crea historial.
La comparación con el último estado verificado determina isDirty: puede haber trazos y ningún
cambio pendiente, o ningún trazo y un borrado pendiente. Deshacer y Rehacer nunca escriben archivos.

Al abrir, se extrae la tinta propia del PDF, se carga como estado inicial limpio y se retira de la
sesión nativa de lectura para dibujarla una sola vez en la capa editable. El archivo privado de origen
permanece intacto durante toda la sesión. Una revisión invalida los paths/gestos al deshacer o rehacer.

AppViewModel.savePdf se invoca exclusivamente por botones de guardado. Congela la entrada, captura
el estado vectorial y abre otra sesión nativa sobre la copia original. Sustituye solo la tinta propia,
preservando texto, imágenes, estructura y anotaciones ajenas. Exporta un temporal distinto, lo reabre y
verifica páginas, IDs, tipos, colores, grosores y coordenadas. El fluorescente conserva opacidad 72/255
y utiliza Multiply en su apariencia PDF.

VerifiedDocumentWrite copia el destino anterior en filesDir/save-recovery, comprueba su SHA-256
frente a la revisión conocida y escribe con truncado explícito. Relee el destino completo y compara
su SHA-256 con el temporal preparado. Solo entonces se marca el punto guardado y se permite salir.
Ante fallo, verifica/restaura los bytes anteriores; si no puede confirmarlos, conserva la copia privada
y muestra Recuperar original. Las copias pendientes también se detectan al iniciar. No se reintenta
ni guarda anotación automáticamente. La atomicidad real depende del proveedor SAF.

Guardar como utiliza ACTION_CREATE_DOCUMENT y concesiones URI de lectura/escritura. El documento
abierto pasa a apuntar al destino elegido solo tras verificarlo; su posición se asocia a la nueva URI.
Una fuente externa de solo lectura conserva sus bytes y ofrece Guardar como. Un cambio externo del
original bloquea la sobrescritura y conserva la edición para elegir otro destino.

ReadingPositions permanece independiente: cada cambio de página se registra automáticamente
aunque no se guarden las anotaciones. Al salir se puede guardar, no guardar o cancelar; la misma
protección se aplica a una apertura externa con edición pendiente.


### Explorador replanteado — v0.0.8, 20/10/2026

BrowserScreen separa la biblioteca (Carpetas / Favoritos) del contenido de una carpeta.
Las raíces autorizadas se muestran una sola vez en Carpetas; los marcadores existentes
se mantienen en Favoritos, con su ruta y opciones de reordenación. No cambia el esquema
DataStore browser_v1 ni se mueven, renombran o eliminan carpetas o documentos reales.

Cada carpeta tiene un título, navegación por antecesoras, salida directa a Mis carpetas
y grupos visuales de subcarpetas y PDF. El menú de ordenación elige expresamente nombre
o fecha. La ordenación de favoritos tiene acciones con nombres distintos. Solo una fila
de carpeta/PDF lleva a su contenido; los menús de acceso no abren el archivo.

El regreso Android desde una raíz autorizada lleva a la biblioteca. showHome cancela
el listado anterior y su revisión, limpia solo la vista y persiste lastLocation = null;
esto permite reabrir en el inicio si el usuario salió desde allí. Una última carpeta
existente sigue recuperándose. No se abre automáticamente otra raíz al quitar un acceso.
La apertura/cierre de PDF conserva carpeta y desplazamiento del listado.

Los errores de acceso tienen estado propio con Reintentar e Inicio, sin presentarse
como una carpeta vacía ni duplicarse en un snackbar. El proveedor sigue determinando
qué archivos existen y qué permisos se conceden; la nueva organización es solo de PDFiz.

### Árbol y listado de detalles — v0.0.9, 20/10/2026

Esta etapa sustituye las pestañas y los grupos visuales de v0.0.8 por el esquema
del explorador solicitado: árbol de raíces autorizadas a la izquierda, bookmarks
debajo y una única lista de contenido a la derecha, siempre con directorios primero.
Desde 800 dp la navegación es permanente; en móvil se abre como panel lateral.
Los PDF muestran tamaño y fecha/hora, en columnas si caben o bajo el nombre.
Los metadatos ausentes no se inventan: se muestra una raya.

La expansión y los hijos de cada nodo viven en AppViewModel, identificados por
concesión SAF y URI del documento. Al entrar se despliegan los antecesores de
FolderLocation.trail, incluidas las aperturas desde bookmarks y la restauración.
Solo se consultan directorios expandidos y la ruta activa, fuera del hilo de UI;
no se escanea recursivamente todo el almacenamiento. Los errores del árbol
permiten reintentar y las peticiones se cancelan al retirar su raíz.

Editar activa opciones de subir, bajar y quitar exclusivamente para raíces y
bookmarks. El orden se persiste en las listas existentes de browser_v1 sin cambiar
su esquema. Quitar raíz confirma antes, retira las referencias dependientes y
libera su concesión persistente; no llama a borrado ni a movimiento de documentos.
El listado único conserva ordenación, regreso del lector y desplazamiento.

DocumentStorage consulta COLUMN_SIZE y resuelve cada columna por nombre, porque
los proveedores SAF pueden añadir columnas o devolverlas en otro orden. Tamaño
y modificación son opcionales; los archivos continúan limitados a PDF. Guardado
manual, página persistente y herramientas de anotación no cambian en esta etapa.

### Sincronización de revisiones al completar un gesto — v0.0.9

La regresión completa detectó una cancelación intermitente con gestos consecutivos.
AndroidView podía entregar la revisión del gesto anterior cuando ya había empezado
el siguiente; el lector la interpretaba como cambio externo y cancelaba su provisional.
PdfReaderView reconoce ahora pen.revision inmediatamente después de sus propios
add/replace. La actualización posterior de Compose no cancela otro gesto. Una
revisión externa distinta (por ejemplo Deshacer) sigue cancelando el provisional.
La prueba Android fuerza la entrega tardía entre DOWN y UP, sin esperas artificiales.

### Barra del lector en una fila — v0.0.10, 20/10/2026

ReaderToolbar reserva una única fila de 56 dp, además del inset nativo superior.
El nombre queda limitado a una fracción del ancho (máximo 240 dp) y se representa
con Ellipsis, conservando su texto completo en accesibilidad y en una ayuda al
mantener pulsado. Las herramientas se centran en el espacio restante; únicamente
esa franja permite desplazamiento horizontal cuando no cabe. Atrás, nombre y
acciones del documento permanecen fijos. Desde 600 dp Guardar conserva su botón
con texto; con menos espacio utiliza un icono accesible.

ActionIcon incorpora vectores nativos de bolígrafo, fluorescente, selección de
texto, goma, ajustes, ocultar/mostrar y guardar, con el mismo trazo del resto de la
aplicación. ToolbarAction ofrece ayuda y nombre accesible sin etiquetas permanentes.
Los interruptores exponen su estado y se resaltan en cian; los accesos a estilos
conservan color y número. Las opciones de goma aparecen junto a ella al activarla.

Los callbacks existentes se conservan: editar estilos no activa herramientas,
bolígrafo y fluorescente pueden coexistir y mantienen sus reglas de entrada,
ocultar desactiva herramientas y conserva trazos; Guardar sigue siendo explícito,
con protección al salir y página persistente independiente.

La validación de versionName admite ahora parches de más de un dígito, necesarios
para 0.0.10. No cambia el identificador Android ni la clave de firma.
