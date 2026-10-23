# Bolígrafo — prueba en tablet

Instala `artifacts/PDF-1.0.0-dev-lapiz.apk` sobre la anterior, sin desinstalarla.
La firma de desarrollo y el paquete son los mismos; se mantienen ubicaciones y marcadores.
La APK de lectura anterior se conserva en `artifacts/PDF-1.0.0-dev.apk`.

Esta etapa permite **escribir a mano con el lápiz oficial**. Color azul y grosor fijo de 2 unidades
de página, sin presión variable. Los trazos solo duran en la sesión: no se guardan todavía en el archivo.
No se han añadido fluorescente, texto, borrador ni deshacer a la interfaz.

Comprobar por separado en Xiaomi Pad 8 y Samsung Galaxy Tab S6 Lite SM-P620:

1. Abrir un PDF: «Bolígrafo» debe estar desactivado y el lápiz no debe dejar marcas.
2. Activarlo: el botón pasa a «Bolígrafo activo»; escribir varias palabras y puntos.
3. Arrastrar con el dedo: debe desplazar el documento, sin generar tinta.
4. Ampliar con dos dedos y volver al ancho: los trazos deben permanecer sobre el mismo contenido.
5. Escribir con la palma apoyada: la página no debe moverse durante el trazo ni al levantar solo la punta.
   Para volver a navegar, levantar también los contactos restantes de esa secuencia.
6. Escribir con la palma ya apoyada y levantar/apoyar de nuevo la punta varias veces: deben aparecer trazos separados.
7. Cambiar entre páginas; un trazo que sale de su página se detiene en el último punto interior y no salta a otra página.
8. Desactivar el botón: el lápiz deja de escribir; los trazos ya completados permanecen visibles.
9. Ocultar la barra: vuelve a lectura y no escribe. «Mostrar barra» permite recuperarla.
10. Girar la tablet: los trazos terminados permanecen en su página y el bolígrafo vuelve a estar desactivado.
11. Con trazos, pulsar Atrás: debe ofrecer «Continuar» o «Descartar y salir».
12. Salir descartando y abrir otra vez: debe abrir en lectura y sin los trazos de la prueba.

Los trazos sin finalizar se cancelan ante `ACTION_CANCEL`, cancelación del puntero del lápiz,
pérdida de foco, cambio de tamaño u ocultación/desactivación de la herramienta.
Los trazos completados se conservan en el ViewModel durante el giro, pero **no sobreviven al cierre del PDF
ni a la finalización del proceso de Android**. El archivo de entrada nunca se escribe en esta etapa.

Registrar dispositivo, versión real de Android, reconocimiento del lápiz, comportamiento de palma,
fluidez y cualquier desplazamiento de los trazos. Las pruebas de eventos sintéticos no certifican estos puntos físicos.
