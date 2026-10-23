# v0.0.5 — color, grosor y tres estilos por herramienta

El usuario confirmó la corrección de los gestos de v0.0.4 y autorizó color/grosor para bolígrafo y
fluorescente, con tres memorias para cada uno, manteniendo la opacidad existente.

## Entrega

- APK: `artifacts/PDF-v0.0.5.apk`, versión Android 0.0.5 y versionCode 5.
- SHA-256: `1492555C47848606B61D3496F5061A1E67BE0C780093923539ABA0BE2028ECFA`.
- Mismo paquete y certificado debug que la entrega anterior; actualización sin desinstalar.
- Firma v2, permisos sin Internet ni almacenamiento global y alineación ZIP/ELF de 16 KiB verificados.
- APK anteriores conservadas sin cambios; la próxima entrega será v0.0.6.

## Uso

1. Abrir un PDF. Junto a Bolígrafo y Fluorescente aparece un botón con color y número de memoria.
2. Pulsar ese botón para abrir los estilos de la herramienta correspondiente.
3. Elegir Estilo 1, 2 o 3. Se cargan el color y grosor que contiene esa memoria.
4. Elegir un color de la paleta o introducir un color hexadecimal RGB; ajustar el deslizador de grosor.
5. Pulsar **Guardar y usar**: se almacena esa memoria y queda seleccionada para los siguientes trazos.
   Cancelar o cerrar el diálogo descarta los ajustes sin guardar. Cambiar de memoria dentro del diálogo
   carga su estilo guardado; guardar explícitamente antes de pasar a otra memoria.

La selección de estilos no activa las herramientas. Siguen comenzando desactivadas al abrir documento
y tras recrear la pantalla. Mientras se cargan las preferencias no se permite activar el dibujo.
Los estilos y la memoria seleccionada se conservan entre documentos y al reiniciar la aplicación.
Los trazos ya dibujados no cambian al elegir otro estilo.

Memorias iniciales: bolígrafo azul/2, negro/1 y rojo/2; fluorescente amarillo/12, verde/12 y rosa/12.
Son valores iniciales sustituibles, no estilos adicionales a las tres memorias.

Bolígrafo: 0,5 a 8, en pasos de 0,5. Fluorescente: 4 a 32, en pasos de 1.
Los grosores se guardan en unidades de página, como antes; su apariencia sigue el zoom.
El fluorescente conserva la recta, la entrada con dedo y la opacidad **72/255**, con mezcla Multiply.
Bolígrafo opaco y entrada exclusiva de stylus. Sin control de opacidad añadido.

## Implementación y almacenamiento

`InkStyle` y `InkPreferences` representan seis estilos RGB opacos y dos índices seleccionados.
`InkStylePreferences` usa un DataStore privado separado de carpetas/marcadores. La escritura de todos
los estilos es atómica; la interfaz solo confirma el guardado después de completarla y conserva
el diálogo ante error. No se guarda el estado de activación de las herramientas.

`PdfReaderView` toma una copia del estilo al comenzar cada trazo y la usa en su vista previa y
confirmación. La anotación completada conserva color/grosor propios; los cambios posteriores no
modifican los trazos anteriores. Se mantienen los eventos de cancelación y el renderizado de v0.0.4.

## Pruebas locales

Se amplía el lector real con cinco pruebas instrumentadas: tres memorias del bolígrafo con colores
y grosores distintos, tres del fluorescente y recuperación desde nuevos ViewModels, cancelación
sin sobrescribir memorias ni activar dibujo, captura del estilo al comenzar un trazo y verificación
por píxeles de grosor/opacidad, y color RGB personalizado con entrada incompleta sin guardar.
Se conserva la comprobación de los bytes originales y las regresiones de zoom/desplazamiento.

La automatización necesitó localizar el rango accesible del Slider en un nodo distinto de su etiqueta
y esperar a que Compose aplicara la selección de memoria antes de editarla. Se prueban colores nuevos
en las tres memorias, evitando un clic de radio redundante sobre el color ya seleccionado.

Resultado final: **26 pruebas Android y 12 JVM correctas**, sin saltos ni fallos. Compilaciones debug
y release correctas (release sin firma de distribución). Lint: 0 errores, 7 avisos previos de versión,
sugerencias KTX y tamaño de ventana; aviso de API obsoleta únicamente en el ayudante de accesibilidad del test.
Opacidad y grosor de los trazos se verifican por píxeles sobre el lector real. La emulación no sustituye
la prueba con los lápices y controladores de las tablets físicas.

Informes: `artifacts/validation/v0.0.5-android-tests.xml`, `v0.0.5-lint.txt`, XML JVM con prefijo
`v0.0.5-` y `v0.0.5-apk-verification.txt`.

Revisión visual realizada sobre la APK de entrega a 1280 × 800 y 420 × 900 px. También se guardó
bolígrafo morado en memoria 3 y fluorescente cian en memoria 2 mediante el selector, se cerró el
proceso con `am force-stop` y se reabrió un PDF: se recuperaron ambos colores y memorias, con el
dibujo desactivado. Capturas revisadas: `artifacts/screenshots/v0.0.5-boligrafo.png`,
`v0.0.5-fluorescente.png` y `v0.0.5-estilos-estrecho.png`.

## Comprobación en tablet

- Configurar las tres memorias del bolígrafo con colores/grosores distintos; alternarlas y escribir.
- Repetir con las tres del fluorescente, comprobando rectas y transparencia habitual.
- Cerrar completamente la aplicación, volver a abrir un PDF y comprobar las seis memorias y su selección.
- Confirmar herramientas inicialmente desactivadas, cancelación de cambios y trazos previos intactos.
- Repetir zoom y desplazamiento con dos dedos: el contenido debe permanecer visible.

Los estilos se guardan como preferencias. **Los trazos en el PDF siguen sin guardado**, como en la
versión anterior. No se han hecho commits, push ni publicaciones remotas.
