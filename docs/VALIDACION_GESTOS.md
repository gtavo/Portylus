# Corrección de página en blanco durante gesto con dos dedos

18 de octubre de 2026. El usuario confirmó fluorescente y selección de texto en tablet, y notificó
que al desplazar con dos dedos el contenido quedaba blanco hasta soltar o realizar un zoom.

Posteriormente el usuario estableció esta entrega como **v0.0.4**, sin renombrar su APK existente.
La siguiente será **v0.0.5**; se aplica la política actual de `version.properties` y del README.
El nombre y hash siguientes identifican el binario histórico, que no se ha modificado.

## Entrega

- APK: `artifacts/PDF-1.0.0-dev-gestos.apk`.
- SHA-256: `96CEB85A1EBE5939D2A285FAEF1103C55F2DC5EC26FC7B1E894EF30C5D653B4D`.
- Firma v2 y certificado debug iguales a la entrega anterior; actualización sin desinstalar.
- APK anteriores conservadas con sus hashes. Sin permisos de Internet ni almacenamiento global.
- Alineación ZIP y segmentos ELF de 16 KiB verificados; emulación de Android 16 con páginas de 4 KiB.
- Verificación de APK: `artifacts/validation/gestos-apk-verification.txt`.

## Causa reproducida

La caché anterior se indexaba por página y ancho exacto. Un pequeño cambio de separación entre
los dedos pedía un bitmap nuevo, retiraba la imagen anterior y podía cancelar el trabajo que aún
no había terminado. Mientras tanto se dibujaba el fondo blanco. La caché seguía acotada, pero no
ofrecía una imagen de respaldo de la misma página.

Una prueba instrumentada sobre el lector real carga primero el contenido y bloquea temporalmente
el monitor de PdfSession en otro hilo para simular un render lento. Después envía movimientos de
dos dedos con pequeñas variaciones de separación y desplazamiento. Comprueba píxeles del texto
original mientras los dedos siguen apoyados, sin permitir que termine el nuevo render.
La prueba falló antes de la corrección con el mensaje de contenido PDF ausente. Resultado conservado
en `artifacts/validation/gestos-antes-correccion.xml`.

## Cambio

- Se conserva un bitmap completado por página dentro de la caché total de 32 MiB.
- Ese bitmap se transforma con el zoom y desplazamiento actuales hasta disponer del sustituto.
- Un único render simultáneo por página; no se cancela por cada variación de zoom. Al terminar,
  se solicita la resolución más reciente si hace falta. Se siguen cancelando páginas fuera de pantalla.
- Resoluciones en pasos de 128 píxeles, hasta 2400, para evitar trabajo por variaciones mínimas.
- No cambia la separación entre dedo, stylus, herramientas y selección, ni el almacenamiento del PDF.

La prueba adicional de desplazamiento sin pellizco verifica contenido visible, movimiento y escala
constante antes de levantar los dedos en lectura, fluorescente y selección. Comprueba también ausencia
de tinta. Las pruebas de entrada conservan la comprobación de bytes originales intactos.

## Resultado de validación local

21 pruebas instrumentadas correctas en Android 16/API 36 x86_64 y 12 pruebas JVM correctas.
Debug y release compilan; release sin firma de distribución. Lint: 0 errores y 7 avisos ya existentes
de versiones, sugerencias KTX y API de tamaño de ventana. La prueba que antes reproducía el blanco pasa
sin liberar el monitor de render y antes de levantar los dedos.

Una primera ejecución completa mantuvo accidentalmente la pantalla estrecha de 420 × 900 px de
la revisión anterior: las dos nuevas pruebas pasaron, pero tres pruebas de barra no encontraban botones
fuera de su área visible. Se restauró el emulador a 1280 × 800 px; el conjunto completo pasó después.
La segunda prueba espera explícitamente que el modo elegido esté aplicado en el lector.

Resultados conservados: `artifacts/validation/gestos-android-tests.xml`,
`artifacts/validation/gestos-lint.txt` y XML JVM con prefijo `gestos-`.
La corrección pendiente de comprobar en las tablets físicas no equivale a certificar su controlador de entrada.

## Prueba en tablet

Instalar sobre la APK anterior, sin desinstalar. Con un PDF abierto:

1. Desplazar con dos dedos manteniendo aproximadamente su distancia, y detenerlos sin levantarlos.
   El contenido debe permanecer visible y seguir el movimiento, sin exigir un pellizco.
2. Variar ligeramente su separación, mantenerlos apoyados y después ampliar/reducir deliberadamente.
3. Repetir con Fluorescente o Seleccionar texto activo; no debe quedar tinta accidental.
4. Confirmar también lectura con un dedo, escritura con stylus y alineación de trazos/selección.

Un PDF o una página que aún no se ha renderizado puede necesitar su carga inicial. Esta corrección
conserva el contenido ya disponible durante las sustituciones de resolución. Los trazos siguen sin
guardarse; no se han hecho commits, push ni publicaciones remotas.
