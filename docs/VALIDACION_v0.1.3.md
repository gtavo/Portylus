# Portylus 0.1.3 — búsqueda y controles de barra

27/10/2026. versionName 0.1.3, versionCode 17, paquete com.gtavoapps.portylus.

## Búsqueda

Lupa visible en el lector y entrada Buscar texto en Más opciones. Por defecto
busca fragmentos; activar Palabras completas exige límites Unicode de palabra.
La búsqueda es literal, sin distinguir mayúsculas, con normalización Unicode
para acentos compuestos/descompuestos y ligaduras. Admite frases y espacios
entre líneas. Los dígitos y el guion bajo forman parte de una palabra; signos
de puntuación y guiones normales actúan como delimitadores.

Se extrae una página cada vez con MuPDF y se conservan sus cuadriláteros en
coordenadas PDF, incluidas páginas giradas o recortadas. La búsqueda se hace en
IO, cancela la consulta anterior y comprueba su generación antes de publicar
resultados. La interfaz sigue disponible. Se muestran hasta 5000 coincidencias;
al superar ese límite se indica 5000+ y se pide concretar la búsqueda.

Anterior/siguiente recorren circularmente las coincidencias de todas las páginas.
El resultado activo se resalta en naranja #FF8A24 y los demás en amarillo. Son
superposiciones temporales: no crean anotaciones ni se guardan en el PDF.
La búsqueda conserva consulta, modo y resultado al recrear la actividad.
La barra de búsqueda sustituye a la de anotación y entrar desactiva los modos
de dibujo. Al cerrar vuelve la barra normal y se retiran los resaltados.

Un PDF sin capa de texto muestra un aviso específico; no se ha añadido OCR.
Interfaz y avisos disponibles en español, inglés y francés, sin conexión.

## Ocultar y mostrar

El botón muestra un panel con flecha de repliegue y la palabra Ocultar; su
descripción accesible y ayuda son Ocultar barra. Se mantiene el área de 48 dp.
El botón flotante para recuperar la barra incluye el icono y Mostrar barra,
dejando clara la acción sin depender de interpretar una flecha aislada.

## Comprobaciones

TextSearchTest cubre fragmentos, límites de palabra, mayúsculas Unicode, acentos,
ligaduras, signos literales, frases multilínea, geometría y límite de resultados.
PdfSearchTest usa un PDF de prueba real con dos páginas de texto (una girada y
recortada) y una sin texto; comprueba resultados y conservación exacta del origen.
SearchWorkflowTest prueba interfaz, navegación entre páginas y vuelta al inicio,
cambio de modo, consulta nueva, limpieza, recreación, cierre y tinta pendiente.

Resultados finales y artefactos: artifacts/validation/search-013/. El registro
de entrega se actualiza al cerrar las pruebas y el estado de Play.

CI anterior: la prueba de restaurar página consultaba la vista vieja y se corrigió
en 91b4376. Después falló la prueba del AAB por una ruta inexistente de la clave
debug. CI ahora fija ANDROID_USER_HOME para que Gradle y bundletool compartan
la ruta. La clave debug de CI es independiente de la firma release privada.

No se han enviado mensajes ni anuncios a Google Groups, Reddit u otros foros.
La creación/configuración del grupo queda pendiente para retomarla después.
