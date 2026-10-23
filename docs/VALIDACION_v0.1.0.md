# Portylus 0.1.0 — identidad definitiva, privacidad y apoyo

Preparada el 22/10/2026; verificación final el 23/10/2026.
versionName 0.1.0, versionCode 14, applicationId y namespace com.gtavoapps.portylus.

## Cambios

- Migrados applicationId, namespace, paquetes Kotlin/Java, carpetas y proveedores de pruebas.
  Android considera esta app independiente de las compilaciones históricas es.gtavo.pdf.
  Las autorizaciones y preferencias anteriores no se transfieren automáticamente; los PDF
  externos se conservan. El marcador interno es.gtavo.pdf/v1 permanece deliberadamente en
  PdfSession para reconocer las anotaciones guardadas por versiones anteriores.
- Temporales nuevos con prefijo portylus; el detector de recuperación admite también el
  prefijo histórico. Sin cambios funcionales en el motor de anotación/guardado.
- Política definitiva publicada copiada íntegramente a assets/legal/privacy.txt, accesible
  sin conexión. Identifica titular, domicilio público, contacto, package, PDF, preferencias,
  permisos, copias de trabajo/recuperación, eliminación, portapapeles, nube y enlaces externos.
- Describe por separado el correo, las aportaciones y el alojamiento de la web. Correo:
  consulta abierta y máximo 3 meses después de cerrarla, salvo datos necesarios para obligaciones
  o reclamaciones. Justificantes y documentación fiscal tienen conservación separada.
- privacyPolicyUrl=https://gtavoapps.com/portylus/privacidad/ configurada. Snapshot público
  descargado el 22/10/2026; decodificados los correos protegidos por Cloudflare al generar el
  texto offline. Sin placeholders ni enlaces de protección de correo en la copia de la app.
- Información: Acerca de, Privacidad, Licencias y Apoyo, todas visibles en móvil. Símbolo propio
  de Portylus, versión y marca gtavo.apps; avatar original de gtavo.apps en Acerca de y Apoyo.
  Se mantienen el icono adaptativo y monocromo de Portylus.
- Enlaces operativos a la web, política y Ko-fi mediante ACTION_VIEW externo. No se añaden SDKs,
  permisos de Internet, WebView, identificadores de aportantes ni confirmaciones de pago.
- Licencias y avisos de terceros permanecen disponibles offline. Información → Licencias →
  Código de esta versión abre el ZIP completo de v0.1.0 en GitHub. Los botones inferiores
  se distribuyen en varias líneas cuando hace falta, para que el acceso al código sea visible.
- Clave de subida local RSA 3072 creada, contraseña aleatoria protegida con DPAPI de Windows.
  Custodia y recuperación documentadas en FIRMA_GOOGLE_PLAY.md; no hay claves en Git/ZIP/APK/AAB.

## Validación final

- assembleDebug, assembleRelease, bundleRelease, testDebugUnitTest y lintDebug correctos.
- 28 pruebas JVM y 40 pruebas Android correctas; 0 fallos/errores/omitidas. Lint: 0 errores,
  6 recomendaciones UseKtx. Los resultados se refieren a esta actualización.
- Revisión dirigida de la APK release firmada en tablet 1280×800 y móvil 420×900, densidad 160.
  Doce capturas de inicio, Acerca de, Privacidad, Ko-fi, avisos y AGPL. El primer diseño ocultaba
  Apoyo en móvil; se corrigió el reparto de ancho y se repitió la revisión sobre la release final.
- Pulsaciones verificadas: Web de Portylus, Política en la web y Abrir Ko-fi resuelven las URL
  exactas en org.chromium.webview_shell, mediante navegador externo y sin extras de documentos.
- APK: paquete com.gtavoapps.portylus, versión 0.1.0/14, Android 13+ (API 33), objetivo API 36,
  arm64-v8a y x86_64. Sin permisos INTERNET ni acceso global al almacenamiento.
- Firma APK validada por apksigner. AAB validado criptográficamente con jarsigner. La advertencia
  de cadena no emitida por CA corresponde al certificado autofirmado de Android; no es una firma rota.
- Alineación ZIP de 16 KiB y segmentos ELF de las seis bibliotecas nativas comprobados.
  El emulador ejecuta páginas de 4 KiB; no se certifica ejecución real de 16 KiB ni stylus físico.
- Se comprueba que privacidad.txt del APK y del AAB coincide byte a byte con el archivo de fuentes.
  No aparecen almacenes ni credenciales privadas en esos binarios.
- La repetición de tests encontró una APK release instalada con otra firma; la limpieza de UTP
  la retiró. Se verificó su ausencia y se repitieron las 40 pruebas correctamente antes de instalar
  y revisar la release final. No se tocaron dispositivos físicos ni datos de usuario.

## Entregas y evidencias

- artifacts/Portylus-v0.1.0.apk: release instalable, firmada con la clave local de subida.
- artifacts/Portylus-v0.1.0.aab: bundle firmado localmente; todavía no enviado a Google Play.
- artifacts/Portylus-v0.1.0-source.zip: fuentes completas, motor/wrapper MuPDF originales y scripts.
- artifacts/Portylus-v0.1.0-source.zip.sha256 y artifacts/SHA256SUMS.txt.
- artifacts/screenshots/Portylus-v0.1.0-{tablet,mobile}-{home,about,privacy,kofi,licences,agpl}.png.
- artifacts/validation/v0.1.0-summary.json y logs asociados de compilación, Android, firma y enlaces.
- Copia previa: artifacts/Portylus-before-v0.1.0.zip, 104 archivos, CRC comprobado,
  SHA256 edbf0ba3fdeeb5f3b358611a33239c97a8f4ca86f49978e2ae1b4f6f401d48b1.

## Cierre de fuentes y entrega firmada (23/10/2026)

Repositorio de la app: https://github.com/gtavo/Portylus
Fuentes completas: https://github.com/gtavo/Portylus/releases/download/v0.1.0/Portylus-v0.1.0-source.zip

Se conserva 0.1.0 / código 14: los archivos anteriores eran entregas locales previas a la
primera publicación. Se preservaron en artifacts/pre-publication-v0.1.0 junto a sus evidencias.
La configuración definitiva incluye la URL exacta de fuentes. Se reconstruyen APK y AAB
firmados y se ejecuta preparePlayBundle, con pruebas JVM y Lint. La revisión dirigida verifica
el nuevo botón de fuentes en tablet y móvil, además de privacidad, Ko-fi y licencias.
Las 40 pruebas Android documentadas arriba preceden al ajuste final del reparto de botones;
no se modificó el motor, la entrada de lápiz ni la lógica de guardado en ese ajuste.

El archivo de fuentes se genera después de finalizar código, configuración y documentación.
Contiene app, activos, licencias, scripts, motor MuPDF y wrapper Android fijados por hash;
se excluyen claves, credenciales, cachés, datos locales y artefactos de compilación.
Se ofrece con su SHA-256 en la publicación v0.1.0. Los binarios firmados se entregan localmente;
no se publican APK/AAB en esta primera publicación de fuentes ni se suben a Google Play.

Conservar una copia independiente del almacén de subida y de su contraseña: DPAPI no es una
copia portátil. Quedan gráficos y capturas de tienda, pruebas en dispositivos reales/16 KiB,
configuración de Play App Signing y las etapas de prueba/aprobación que indique Play Console.
La disponibilidad del enlace Ko-fi no prueba pagos ni aceptación por Google.

La primera ejecución de CI falló antes de compilar: setup-android intentó instalar el paquete
obsoleto tools. Se fijó packages: platform-tools y se evitó duplicar trabajos al subir etiquetas.
Esta corrección afecta únicamente a CI, sin cambios en las fuentes de la app ni en los binarios.
