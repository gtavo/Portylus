# Portylus v0.0.13 — página real de Ko-fi

Fecha: 21/10/2026. versionName 0.0.13, versionCode 13.

## Cambio

`supportUrl=https://ko-fi.com/gtavoapps` en distribution.properties. Información → Apoyar
el desarrollo muestra Abrir página de apoyo, que abre el destino en un navegador externo.
La página pública carga con nombre gtavo.apps en un navegador sin sesión de usuario.
Contributor está desactivado según confirmación expresa del usuario; no se han leído ni
modificado ajustes privados de Ko-fi/Stripe. No se han realizado pagos ni enviado mensajes.

Se actualizan README, privacidad local/HTML y guías de apoyo, publicación y MuPDF.
No cambia el código de lectura, anotación, guardado ni el diálogo: se habilita su rama ya
preparada para una URL no vacía. No se añaden dependencias ni permiso de Internet.

## Verificación

- Debug, release, bundleRelease, 28 pruebas JVM y Lint correctos.
- Lint: 0 errores, 6 recomendaciones UseKtx.
- Botón activo revisado en tablet 1280×800 y móvil 420×900. Desaparece el aviso de enlace futuro.
- Pulsación real abre org.chromium.webview_shell con ACTION_VIEW y el destino exacto
  https://ko-fi.com/gtavoapps, sin PDF ni información de pago adjunta.
- APK: firma debug estable, permisos, API 33/36, ARM64/x86_64 y alineación ZIP/ELF de 16 KiB.
- Se conserva como base la regresión de 40 pruebas Android de v0.0.12; esta modificación
  de configuración/textos se verifica con pruebas JVM y comprobación dirigida del nuevo enlace.
- Entregas anteriores preservadas y verificadas por sus SHA-256. Fuentes completas de cada
  versión disponibles localmente con MuPDF original, wrapper, licencias y scripts.

## Entregas

- [APK de prueba](../artifacts/Portylus-v0.0.13.apk).
- [AAB técnico sin firmar](../artifacts/Portylus-v0.0.13-unsigned.aab).
- [Fuentes completas](../artifacts/Portylus-v0.0.13-source.zip).
- [Resultados](../artifacts/validation/v0.0.13-summary.json).
- [Botón en móvil](../artifacts/screenshots/Portylus-v0.0.13-kofi-mobile.png).
- [Navegador externo](../artifacts/screenshots/Portylus-v0.0.13-kofi-browser.png).
- Logs: v0.0.13-build.log, v0.0.13-kofi-intent.log y v0.0.13-apk-verification.log.

Logo independiente de gtavo.apps para el perfil de Ko-fi: [PNG](ko-fi/gtavo-apps-logo-v1.png),
1254×1254, generado con image_gen. Prompt y procedencia: [documento](ko-fi/LOGO_GTAVO_APPS.md).
No se ha subido ni sustituido el perfil remoto, ni se cambia el icono de Portylus.

La disponibilidad del perfil no prueba el cobro de una aportación. Se mantienen los pendientes
de publicación de Google Play (titular legal, privacidad/fuentes públicas, firma y recursos de
tienda) y la revisión de la excepción de pagos aplicable al flujo final. No se ha publicado
el código ni los binarios desde esta tarea. Las pruebas de emulador y alineación binaria
no certifican stylus físico ni ejecución real con páginas de memoria de 16 KB.
