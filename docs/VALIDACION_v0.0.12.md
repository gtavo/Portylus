# Portylus v0.0.12 — apoyo voluntario

Fecha: 21/10/2026. versionName 0.0.12, versionCode 12.

## Cambio

Información → Apoyar el desarrollo explica que Portylus es gratuita y de código abierto,
y que aportar no concede funciones, contenido, insignias, prioridad ni ventajas. No hay
SDK de pagos, WebView, telemetría nueva, permiso de Internet ni estado de donante.

`supportUrl` en distribution.properties es opcional y queda vacío hasta crear la cuenta.
La pantalla muestra que el enlace estará disponible próximamente, sin botón de pago activo.
Cuando se configure una página HTTPS real, el botón Abrir página de apoyo la abrirá con
ACTION_VIEW en una aplicación externa. Gradle rechaza URL no HTTPS, sin host o con credenciales.

Se actualizan privacidad local/HTML, ficha de tienda, README y guías de publicación/MuPDF.
La guía [APOYO_VOLUNTARIO.md](APOYO_VOLUNTARIO.md) incluye la comparación de plataformas,
la configuración de Ko-fi sin Contributor y los pasos pendientes. Las comisiones del
procesador se distinguen del 0 % de plataforma. No se afirma aprobación de Play del flujo.

## Verificación

- Debug, release y bundleRelease compilan.
- 28 pruebas JVM y 40 Android, sin fallos/errores/omisiones.
- Lint: 0 errores, 6 recomendaciones UseKtx.
- APK: firma debug estable, permisos locales, API mínima 33 y objetivo 36, ARM64/x86_64
  y alineación ZIP/ELF de 16 KiB comprobados con verify-apk.ps1.
- Apoyo sin URL revisado visualmente en tablet 1280×800 y móvil 420×900: texto voluntario,
  aviso de enlace próximo, sin botón de pago operativo y navegación a privacidad.
- Las entregas anteriores conservan sus SHA-256. La fuente completa de v0.0.11 se conserva
  como reversión del estado anterior. Las fuentes de v0.0.12 incluyen el motor MuPDF original.

## Entrega

- [APK de prueba](../artifacts/Portylus-v0.0.12.apk), firmada con clave debug.
- [AAB técnico sin firmar](../artifacts/Portylus-v0.0.12-unsigned.aab).
- [Fuentes completas](../artifacts/Portylus-v0.0.12-source.zip), con motor y wrapper.
- [Resultados](../artifacts/validation/v0.0.12-summary.json).
- Logs: v0.0.12-build.log, v0.0.12-android.log y v0.0.12-apk-verification.log.
- [Apoyo en tablet](../artifacts/screenshots/Portylus-v0.0.12-support-tablet.png) y
  [móvil](../artifacts/screenshots/Portylus-v0.0.12-support-mobile.png).

No se ha creado cuenta, realizado pagos, enviado mensajes ni publicado código/binarios.
No se prueba un pago real ni una URL personal, porque la cuenta está pendiente. La apertura
del destino real debe revisarse cuando se configure. Se mantienen los pendientes de titular,
URLs públicas de privacidad/fuentes, firma de subida y comprobaciones de Play.
Las pruebas del emulador y la alineación binaria no certifican stylus físico ni ejecución
real con páginas de memoria de 16 KB. La obligación de fuentes de la AGPL no cambia.
