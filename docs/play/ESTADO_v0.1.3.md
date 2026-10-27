# Portylus 0.1.3 — estado de entrega, 27/10/2026

Paquete com.gtavoapps.portylus, versión 0.1.3, código Android 17.

## Google Play

AAB firmado subido a Alpha. Notas ES/EN/FR guardadas. Enviado a Google para
lanzamiento completo; la consola confirma «Cambios en revisión» para 17 (0.1.3).
Publicación gestionada desactivada: quedará disponible al aprobarse la revisión.
Esto no implica que Google ya la haya aprobado ni que esté en producción.

La 0.1.2 / 16 ya figura disponible para determinados testers, publicada el 26/10
a las 12:03. Alpha conserva 178 países/regiones. No se han cambiado los testers,
el canal de producción ni los requisitos para solicitar acceso a producción.
Captura: artifacts/validation/search-013/play-review.png.

## Fuentes y archivos

Release pública: https://github.com/gtavo/Portylus/releases/tag/v0.1.3
El ZIP completo incluye el motor MuPDF y el wrapper originales verificados.
Descarga anónima HTTP 200, tamaño y SHA-256 público comprobados; el digest del
servidor GitHub coincide con el del ZIP local.

- AAB: d7b4d39d2bab8f3f9f22218bb4ab42d8cca80553559c6f65982a630e6e8eaa9e
- APK: b158f303e09f2d05aa2f034d8befb630dc474c07ee2ce6af5a6b736cf3de244d
- Fuentes: 9b97d2b35105bd13ee5e7afb16529a49a068e6984655bed4dde326ff188e70e8

Los tres archivos y sus SHA-256 están en artifacts/. Se preservan las entregas
anteriores y la copia previa artifacts/Portylus-before-search-013.zip.

## Validación

36 pruebas JVM, 46 Android en tablet, 7 dirigidas en móvil, 4 en 320 dp/letra
130 % y 3 sobre los APK divididos del AAB release sin conexión, correctas.
Lint: 0 errores y 6 avisos previos. Firma y alineación 16 KiB verificadas.
182 claves localizadas equivalentes en ES/EN/FR.

CI correcto (build y native-tests):
https://github.com/gtavo/Portylus/actions/runs/40578314447
La aplicación corresponde a 9c4af29. e6c6275 corrige solo la ruta/contexto de
Android en CI y 7f2e5e7 reinstala el paquete de instrumentación que Gradle retira
al terminar sus pruebas. No cambian los binarios ni las fuentes de la aplicación.
La ejecución anterior confirmó las 46 pruebas de tablet y 7 de móvil; su último
paso falló por esa ausencia del paquete de instrumentación, ya corregida.

La validación local usa emulador y no certifica un teléfono o stylus físico.

## Grupos y foros

Por instrucción del titular, los anuncios y mensajes en grupos quedan pendientes.
No se ha retomado la creación/configuración de Google Groups en esta sesión ni
se ha publicado en Reddit, enviado invitaciones o contactado con terceros.
No compartir un enlace de grupo como operativo sin verificar y registrar antes
el grupo en Alpha → Testers.
