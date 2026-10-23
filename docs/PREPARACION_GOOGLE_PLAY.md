> Estado 23/10/2026: cuenta Play e identidad completadas según el usuario.
> Política definitiva publicada: https://gtavoapps.com/portylus/privacidad/ (también dentro de la app).
> Package `com.gtavoapps.portylus`, versión 0.1.0 / código 14.
> Fuentes completas de v0.1.0: https://github.com/gtavo/Portylus/releases/download/v0.1.0/Portylus-v0.1.0-source.zip

# Portylus: preparación y registro en Google Play

Fecha de consulta: **22/10/2026**. Preparación v0.1.0, versionCode 14.
Este documento distingue la preparación local de la publicación efectiva.

## Situación del proyecto

- Carpeta actual: `C:\0codex\Portylus`, trasladada desde PDFiz; PDF_apk era la ruta histórica.
- Nombre visible, proyecto Gradle, recursos y carpeta de marca: Portylus.
- Identificador definitivo: `com.gtavoapps.portylus` en applicationId, namespace y paquetes de código.
  `es.gtavo.pdf` corresponde solo a las compilaciones históricas; Android considera la nueva app independiente.
  Las claves de anotaciones propias y prefijos de recuperación históricos se conservan para
  poder seguir editando los PDF y recuperar copias anteriores. El paquete no se muestra como marca.
- Se conserva el símbolo P original; procedencia en `Portylus_BrandKit/PROMPT.md`.
- Android mínimo 13/API 33; compileSdk/targetSdk 36, ARM64 y x86_64, sin acceso a Internet
  ni permisos de almacenamiento global. SAF autoriza cada documento/carpeta.
- Licencia de Portylus: AGPL-3.0-or-later. MuPDF/fitz 1.28.5 sin modificaciones al motor.
- Pantalla Información / Privacidad y licencias con textos completos disponibles sin conexión.
- Copia reversible de las fuentes anteriores en `artifacts/Portylus-before-rename-2026-10-21.zip`,
  con SHA-256; las APK y evidencias anteriores conservan sus nombres y hashes históricos.

## 1. Crear tu cuenta de desarrollador

Entra en [Play Console](https://play.google.com/console/signup) con la cuenta de Google que
quieras utilizar para mantener Portylus. Google exige mayoría de edad, aceptar su contrato,
abonar **25 USD una sola vez** y verificar la identidad y los datos de contacto. Usa información
legal real, coherente con el perfil de pagos y los documentos que solicite el asistente.

Elige **Personal** si publicas como persona; **Organización** si publica una entidad real.
La cuenta de organización normalmente necesita un número **D-U-N-S** y verificar los datos
de la entidad. El nombre de marca puede ser distinto del nombre legal del titular. Revisa
qué datos del titular/contacto aparecerán públicamente antes de confirmar el registro.

Las cuentas personales nuevas también deben verificar el acceso a un dispositivo Android
mediante la aplicación Play Console. No hace falta contratar una licencia comercial de MuPDF
si se cumple la AGPL.

Fuentes: [registro](https://support.google.com/googleplay/android-developer/answer/6112435?hl=es),
[datos del titular](https://support.google.com/googleplay/android-developer/answer/13628312?hl=es).

## 2. Crear la ficha Portylus

En Play Console: **Crear aplicación**, nombre **Portylus**, idioma **español (España)**,
tipo **Aplicación**, correo de soporte y elección de gratuita/de pago. Decide el modelo
antes de publicar: una aplicación publicada como gratuita no se puede convertir después
en aplicación de pago con el mismo paquete. La AGPL permite cobrar; cobrar no elimina
la obligación de ofrecer las fuentes. Se elige distribución gratuita con aportaciones voluntarias sin contraprestación, preparadas mediante enlace externo; no hay compras ni funciones de pago.

El identificador del AAB es `com.gtavoapps.portylus`. Verifica su disponibilidad antes de la primera
subida: Google trata los nombres de paquete como únicos y permanentes. Mantener ese nombre
permite compatibilidad técnica, pero la firma de Play es otra decisión (ver siguiente paso).

Prepara categoría Productividad, nombre, descripción breve y completa, icono de tienda
512×512, gráfico de funciones 1024×500 y capturas reales de teléfono/tablet con contenido
de muestra. Los textos iniciales están en `docs/play/FICHA_ES.md`; faltan los gráficos
finales de tienda y la revisión de las capturas. No usar PDF personales o de alumnos.

Fuente: [crear y configurar una app](https://support.google.com/googleplay/android-developer/answer/9859152?hl=es).

## 3. Preparar firma y AAB

Google Play distribuye nuevas aplicaciones mediante **Android App Bundle (.aab)**.
Activa **Play App Signing** y conserva una **clave de subida** propia con copia segura.
La clave de subida y la clave con la que Google firma la app distribuida son distintas.
No uses la firma debug para publicar. Las instalaciones debug previas pueden requerir
desinstalación para pasar a la app de Play; exporta antes los PDF pendientes y copias
de recuperación, porque desinstalar borra datos privados y permisos de carpetas.

No se ha creado una clave de producción ni se ha subido ningún binario. Cuando se cree,
guardarla fuera del repositorio y conservarla en una copia segura junto con sus contraseñas.
Ejemplo de creación interactiva con JDK, eligiendo tú ubicación, contraseñas e identidad:

```powershell
keytool -genkeypair -v -keystore C:\ruta-segura\portylus-upload.jks -alias portylus-upload -keyalg RSA -keysize 3072 -validity 10000
```

Configura en la sesión las variables `PORTYLUS_KEYSTORE_PATH`, `PORTYLUS_KEYSTORE_PASSWORD`,
`PORTYLUS_KEY_ALIAS`, `PORTYLUS_KEY_PASSWORD`. No pongas contraseñas literales en archivos,
comandos que se compartan ni historial; utiliza un gestor de secretos o entrada interactiva.
Se admiten las variables anteriores `PDF_*` por compatibilidad, si no existen las nuevas.

Completa `distribution.properties` con titular, correo real, URL HTTPS de privacidad y
URL HTTPS de las **fuentes completas de esta versión**. `supportUrl` es opcional: al crear la cuenta, incorporar su URL real y verificar las condiciones de aportaciones de Play. Son datos públicos, sin secretos.

```powershell
.\scripts\build.ps1 -Tasks preparePlayBundle
```

Ese comando rechaza datos vacíos y firma ausente. El AAB estará en
`app/build/outputs/bundle/release/app-release.aab`. `bundleRelease` sin configuración
puede generar un AAB técnico sin firmar para inspección; no es una entrega lista para Play.
La entrega preparada usa `versionName=0.1.0` y `versionCode=14`, package `com.gtavoapps.portylus`. La numeración visible
seguirá `0.1.0 → … → 0.1.9 → 0.2.0`. Conserva los nombres de las entregas anteriores.
Incrementa versionCode en cada nueva subida aceptada por Play y mantén las fuentes de
cada versión. Comprueba el AAB real con Play Console/bundletool y sus APK generadas.

Fuentes: [firma](https://developer.android.com/studio/publish/app-signing),
[app bundles](https://developer.android.com/guide/app-bundle).

## 4. Completar políticas, privacidad y AGPL

- Alojar la política en una URL HTTPS pública, estable, sin inicio de sesión, en HTML.
  `docs/play/privacy.html` y el texto sin conexión están sincronizados con la política definitiva publicada.
  Configurar esa URL en la app y Play Console. La app incluye también el texto local.
- Alojar el ZIP de fuentes completas y mantenerlo accesible gratuitamente. Debe incluir
  la app, sus scripts y los dos archivos oficiales del motor/wrapper. La autorización
  de usar AGPL no convierte una copia local en código publicado. Ver `CUMPLIMIENTO_MUPDF.md`.
- Seguridad de los datos: la implementación actual procesa documentos localmente y no los
  transmite al desarrollador. La respuesta prevista para el funcionamiento local es **no recopila ni comparte datos**,
  tras comprobar el manifest/dependencias de la versión que se suba. No equivale a afirmar
  que no accede a PDF o que un proveedor de nube elegido por el usuario no sincroniza.
- Apoyo voluntario: página [gtavo.apps en Ko-fi](https://ko-fi.com/gtavoapps) configurada; Contributor desactivado según confirmación del usuario, sin recompensas ni desbloqueos. El navegador externo y la plataforma gestionan el pago; la app no recibe confirmaciones. La política de privacidad explica que la plataforma puede comunicar al titular datos del aportante. Revisar la declaración de Seguridad de los datos respecto del flujo final; no añadir un SDK de pagos o seguimiento manteniendo automáticamente las respuestas anteriores. Ver [guía de apoyo](APOYO_VOLUNTARIO.md).
- Anuncios: no. Acceso: sin cuenta, sin partes sujetas a credenciales; el revisor puede
  abrir su propio PDF de muestra mediante Abrir PDF. No necesitas enviar documentos privados.
- Completar clasificación IARC, público objetivo real y las demás declaraciones que
  Play Console pida. No se ha declarado que esté dirigida a menores ni se ha elegido una
  edad para evitar requisitos: la selección debe describir el público al que la vas a ofrecer.
  Si se dirige a niños, revisar además la política de Familias.
- Si se va a distribuir en el EEE, responder de forma veraz sobre condición de comerciante
  y completar la verificación que Google solicite; no inferirla de que la app sea gratuita.

Fuentes: [Seguridad de los datos](https://support.google.com/googleplay/android-developer/answer/10787469?hl=es),
[preparar revisión](https://support.google.com/googleplay/android-developer/answer/9859455?hl=es).

## 5. Pruebas y lanzamiento

Comienza por prueba interna con el AAB firmado. Revisa el informe previo al lanzamiento,
instalación, permisos SAF, apertura externa, orientación, estilos, guardado y reapertura.
Prueba una tablet con stylus físico y tanto almacenamiento local como un proveedor de nube
si quieres anunciar compatibilidad. La comprobación binaria de alineación no sustituye
una prueba en un dispositivo/emulador configurado con páginas de memoria de 16 KB.

Si la cuenta es personal y se creó después del 13/11/2023, debes realizar una **prueba cerrada
con al menos 12 probadores inscritos continuamente durante 14 días**. Después se solicita
acceso a producción contestando las preguntas sobre pruebas y preparación; no es automático.
Las pruebas internas no sustituyen esta prueba cerrada.

Portylus ya apunta a API 36, que Google exige para nuevas apps/actualizaciones desde
el 31/08/2026. Sus bibliotecas nativas deben ser compatibles con páginas de 16 KB.
Se deben revisar nuevamente los requisitos en la fecha efectiva de la subida.

Fuentes: [prueba cerrada](https://support.google.com/googleplay/android-developer/answer/14151465?hl=es),
[API objetivo](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en-mt),
[16 KB](https://developer.android.com/guide/practices/page-sizes).

## Estado y pendientes para Google Play

1. Titular y política definitiva confirmados y publicados; correo gtavo.apps@gmail.com.
2. Fuentes completas de v0.1.0: descarga pública enlazada arriba; mantenerla sin coste ni autenticación.
3. URLs de privacidad y fuentes incorporadas en la app y el borrador de ficha.
4. AAB firmado localmente y controles de preparePlayBundle superados; el titular debe conservar
   una copia segura independiente de la clave/contraseña y configurar Play App Signing al subirlo.
5. Gráficos/capturas de tienda y formularios fieles al comportamiento final.
6. El enlace real de Ko-fi ya está incorporado y Contributor desactivado según el usuario. Mantener aportaciones sin ventajas y revisar su encaje en las condiciones de Play para la entrega final.
7. Validación física y de 16 KB, prueba interna/cerrada y aprobación de producción.

La preparación local no certifica la aceptación por Google ni una revisión jurídica integral.
