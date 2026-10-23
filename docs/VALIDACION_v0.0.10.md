# PDFiz v0.0.10 — barra del PDF en una sola fila

Fecha: 20/10/2026. versionName 0.0.10, versionCode 10.

## Entrega

[APK instalable](../artifacts/PDFiz-v0.0.10.apk), 50.471.033 bytes, firma debug estable.
SHA-256: 05247B71AFF3C588FDE60C6A3527D62C7D1F6DB7388E6DA124F013ADBFE76B07.

## Diseño y comportamiento

- Una fila de 56 dp, además del inset nativo superior. Se elimina la segunda fila de herramientas.
- Izquierda: Atrás y nombre limitado de ancho, con puntos suspensivos. Nombre completo accesible y ayuda al mantener pulsado.
- Centro: iconos nativos de bolígrafo, fluorescente, selección de texto, goma y ocultar; accesos de estilo con su color y número. Ajustes de goma junto a la herramienta cuando está activa.
- Derecha: Deshacer, Rehacer, Guardar y Más opciones. Guardar mantiene su texto en espacio amplio y usa un icono con nombre accesible por debajo de 600 dp.
- Herramientas activas en cian y estado expuesto a accesibilidad. Nombres disponibles en ayudas al mantener pulsado.
- Solo la franja central se desplaza horizontalmente si falta espacio. El nombre y las acciones del documento no se desplazan.
- Ocultar desactiva herramientas y conserva trazos. El botón de restaurar utiliza un icono.
- No cambian las reglas de stylus/dedo, estilos, guardado manual, aviso al salir, página persistente ni Deshacer/Rehacer.
- La validación de versiones admite parches de varios dígitos, necesarios para 0.0.10.

## Comprobaciones

- 28 pruebas JVM y 40 Android: 68 pruebas distintas, sin fallos, errores ni omisiones en la ejecución final.
- Caso específico de barra ejecutado además en tablet 1280 × 800 y móvil 420 × 900, densidad 160.
- Comprobación de alineación en una fila, nombre largo y orden izquierda/centro/derecha.
- Activación simultánea de bolígrafo y fluorescente, goma y sus opciones, desplazamiento central móvil y ocultar/restaurar.
- El documento original conserva sus bytes al anotar sin guardar, también en la prueba nueva.
- Regresión de guardado explícito, salida, lectura, anotaciones, estilos y explorador.
- Capturas revisadas visualmente en ambos tamaños.
- Lint: 0 errores y 6 avisos UseKtx.
- Debug y release compilan correctamente. Release permanece sin firmar al no configurar clave de distribución; la entrega instalable es la debug firmada.
- APK: CRC ZIP, firma, alineación ZIP/ELF de 16 KiB, ARM64 y x86_64, API mínima 33 y objetivo 36 verificados.
- Sin permiso de Internet ni almacenamiento global.
- APK v0.0.9 conservada y verificada por su hash anterior. Copia previa de fuentes y configuración en artifacts/PDFiz-v0.0.9-source-before-v0.0.10.zip, ZIP íntegro.

La primera suite agotó la espera de una prueba anterior al abrir Aula.pdf desde el
explorador. Se conserva el resultado inicial. La prueba sincroniza ahora los clics
de accesibilidad con la interfaz Android y registra cambios de estado si la apertura
falla. Después pasaron los tres casos del explorador y los 40 de la suite completa;
no se modificó el explorador de producción en esta etapa.

## Evidencias

- [Barra en lectura, tablet](../artifacts/screenshots/PDFiz-v0.0.10-barra-lectura.png)
- [Bolígrafo y fluorescente activos](../artifacts/screenshots/PDFiz-v0.0.10-barra-escritura.png)
- [Goma y sus opciones](../artifacts/screenshots/PDFiz-v0.0.10-barra-goma.png)
- [Barra móvil](../artifacts/screenshots/PDFiz-v0.0.10-barra-lectura-movil.png)
- [Herramientas desplazadas en móvil](../artifacts/screenshots/PDFiz-v0.0.10-barra-goma-movil.png)
- [Resumen de resultados](../artifacts/validation/v0.0.10-summary.json)
- [Resultados Android](../artifacts/validation/v0.0.10-android-tests.xml)
- [Compilación y regresión completas](../artifacts/validation/v0.0.10-final-validation.log)
- [Prueba específica en tablet](../artifacts/validation/v0.0.10-toolbar-tablet.log)
- [Prueba específica en móvil](../artifacts/validation/v0.0.10-toolbar-mobile.log)
- [Verificación de APK](../artifacts/validation/v0.0.10-apk-verification.log)

Validación realizada en emulador Android 16/API 36. No sustituye una comprobación
con stylus físico. No quedan cambios de la barra pendientes de implementar.
