# Portylus 0.1.4 — ir directamente a una página

28/10/2026. versionName 0.1.4, versionCode 18, com.gtavoapps.portylus.

## Uso

Tocar el contador inferior, por ejemplo 128 / 240, abre Ir a la página. El
número actual aparece seleccionado para sustituirlo directamente. El campo
usa teclado numérico y permite confirmar con Ir o la acción del teclado.

Se muestra el rango válido y se desactiva Ir para campos vacíos, texto no
numérico, decimales, números desbordados o páginas fuera de 1…total. Cancelar
no mueve el documento. El diálogo conserva lo escrito al recrear la actividad.
El contador tiene flecha de desplegable, descripción accesible y altura mínima
de 48 dp. Funciona con la barra de herramientas visible u oculta.

El salto conserva el zoom y la posición horizontal, detiene el desplazamiento
en curso y vuelve a modo lectura. Cancela solo interacciones provisionales;
no guarda, modifica ni descarta trazos completados. La página se recuerda
mediante el mecanismo existente de posiciones de lectura. Si se usa durante
una búsqueda, se cierra esta para que un resultado tardío no deshaga el salto.

Se mantiene la página elegida aunque sea una última página pequeña que ocupe
menos pantalla que la anterior. Al volver a desplazar o ampliar manualmente,
el contador sigue de nuevo la página visible. La búsqueda conserva su propia
navegación y puede sustituir esa selección al avanzar por sus coincidencias.

Textos en español, inglés y francés, incluidos en la base de cada instalación
de Play. Se mantiene el paquete y la clave de subida, sin permisos nuevos.

## Pruebas

PageNavigationTest crea un PDF real de 240 páginas numeradas: rechaza entradas
inválidas, salta a 128, 240, 150 y 1, conserva el campo al recrear la actividad,
funciona con herramientas ocultas y búsqueda, y comprueba que el archivo origen
y la tinta pendiente no cambian. Otro PDF con una última página pequeña verifica
el contador y que vuelve a responder al desplazamiento con eventos de dedo.

La evidencia de compilación, pruebas y capturas se guarda en
artifacts/validation/page-014/. El resultado y el estado externo se registran
en docs/play/ESTADO_v0.1.4.md al cerrar la entrega.

Se conservan los binarios y fuentes anteriores, además de la copia previa
artifacts/Portylus-before-page-014.zip. Sin anuncios ni mensajes en grupos o foros.
