# Portylus 0.1.4 — estado de entrega, 28/10/2026

Paquete com.gtavoapps.portylus, versión 0.1.4, código Android 18.

## Google Play

AAB firmado subido a Alpha, notas ES/EN/FR guardadas y lanzamiento completo
enviado. La consola confirma «Cambios en revisión» para 18 (0.1.4), todavía
realizando comprobaciones rápidas. El envío formal a revisión queda encolado
automáticamente al superarlas. Publicación gestionada desactivada: quedará
disponible al aprobarse Google. No se afirma aprobación ni disponibilidad actual.
Captura: artifacts/validation/page-014/play-review.png.

La 0.1.3 / 17 figura disponible para determinados testers, publicada el 27/10
a las 21:36. Alpha conserva 178 países/regiones. No se han cambiado los testers
ni el canal de producción.

Play muestra dos advertencias informativas: no hay archivo de desofuscación
(minificación desactivada) ni símbolos nativos del motor precompilado. No hay
errores que bloqueen el envío. No se han aceptado nuevos contratos.

## Fuentes y archivos

Release pública: https://github.com/gtavo/Portylus/releases/tag/v0.1.4
Fuentes de f3ef425b073d2ef89f5010b1abb34bd173cfaeae, incluyendo MuPDF y wrapper
originales verificados. ZIP sin claves privadas. Descarga anónima HTTP 200,
tamaño y SHA-256 público comprobados. Digest del servidor GitHub coincidente.

- AAB: e50035f0280260f927b3d6cc89373032845b92a22c58df7c7ede03a2e38cb944
- APK: 1f347155697b4c039ebdba74c2f1999295e0735a9cd35c4752884e619344572f
- Fuentes: 4796429df19b30fa2594df23b3f9a60ed7465897fabb03d9fe9a67e678c56028

Los tres archivos y sus SHA-256 están en artifacts/. Entregas anteriores y
copia previa artifacts/Portylus-before-page-014.zip preservadas.

## Validación

36 pruebas JVM, 48 Android en tablet 1280×800, 9 dirigidas en móvil 360×800,
4 en 320×800 con letra al 130 % y 3 sobre APK divididos del AAB release
sin conexión, correctas. Firma APK/AAB y alineación ZIP/ELF 16 KiB verificadas;
bundletool validate correcto. Lint: 0 errores, 6 avisos UseKtx previos.
188 claves localizadas equivalentes en español, inglés y francés.
Sin permisos de Internet ni acceso global al almacenamiento.

CI completo correcto (build y native-tests) sobre f3ef425:
https://github.com/gtavo/Portylus/actions/runs/40602186882
Incluye las 48 pruebas de tablet, 9 de móvil y 2 de idiomas sobre APK de un AAB.
Las pruebas locales son de emulador; no certifican el móvil o stylus físico.

## Grupos y foros

Por instrucción del titular, anuncios y mensajes en grupos quedan pendientes.
No se ha retomado Google Groups, publicado en Reddit, enviado invitaciones
ni contactado con terceros en esta sesión.
