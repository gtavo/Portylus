# Validación — bolígrafo de prueba

El usuario confirmó el funcionamiento de la primera APK de lectura y autorizó incorporar únicamente
escritura con lápiz. No detalló dispositivos ni un protocolo concreto en esa confirmación.

## Entrega

- APK: `artifacts/PDF-1.0.0-dev-lapiz.apk`.
- SHA-256: `BB7C708B859ECEF45E165334DC7207EBB11DB54074C041E7A06622F80619AC9F`.
- Mismo paquete y certificado que la APK anterior: actualización sin desinstalar.
- APK de lectura anterior conservada, con su hash original, en `artifacts/PDF-1.0.0-dev.apk`.
- Debug instalable firmado; release compila sin firmar.
- Lint: 0 errores, 12 avisos de versiones y sugerencias KTX.
- Permisos, firma v2 y alineación ZIP/ELF de 16 KiB verificados. El emulador ejecutado utiliza 4 KiB.

## Pruebas

12 pruebas JVM correctas: las 9 previas y 3 de transformación de coordenadas, zoom, desplazamiento,
orígenes de recorte, cambio de tamaño y rechazo de puntos fuera de página o no finitos.

10 pruebas instrumentadas correctas en Android 16/API 36, x86_64: las 2 del motor existentes y 8
del bolígrafo sobre el lector real, usando la barra Compose para activar y desactivar la herramienta:

1. Lectura inicial; activación explícita; dedo, borrador físico y tipo desconocido sin tinta; stylus con tinta azul comprobada por píxeles.
2. Navegación con dedo mientras el bolígrafo está activado, sin añadir trazos.
3. Identificador estable del stylus cuando cambian los índices de los punteros; inmovilidad de la página y bloqueo de la cola de palma.
4. `ACTION_CANCEL`, stylus cancelado con `FLAG_CANCELED` y desactivación durante el trazo sin confirmar parciales.
5. Cancelación del contacto de palma sin perder el trazo legítimo del lápiz.
6. Zoom con dos dedos conservando coordenadas y comprobando los píxeles azules en la posición transformada.
7. Recreación de la actividad conservando trazos completados y volviendo a lectura.
8. Ocultación de barra, confirmación al salir, continuación y reapertura sin conservar trazos descartados.

En cada caso se comprueba que los bytes del archivo de entrada permanecen intactos.
La automatización utiliza eventos nativos sintéticos; no acredita hardware, latencia ni rechazo de palma físico.

Además se abrió un PDF mediante el explorador SAF, se activó el botón y se dibujaron trazos con
`input stylus` del emulador. Captura local revisada: `artifacts/screenshots/boligrafo.png`.

## Límites de esta etapa

Solo bolígrafo azul con grosor fijo. Trazos vectoriales en memoria de la sesión, sin guardar en PDF,
sin presión variable y sin nuevas herramientas. Se conservan durante el giro; un proceso finalizado por Android los pierde.
Al cerrar el PDF se pide descartar o continuar. Ocultar la barra desactiva la escritura.

Pendientes las pruebas físicas con los lápices oficiales de Xiaomi Pad 8 y Samsung SM-P620,
la fluidez prolongada y el comportamiento de palma de cada controlador. Procedimiento en `PRUEBAS_LAPIZ.md`.
No se ha hecho commit, push ni publicación remota.
