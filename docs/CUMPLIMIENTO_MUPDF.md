# Portylus y MuPDF: distribución AGPL

Revisado el 23/10/2026. Se mantiene **AGPL-3.0-or-later**, decisión confirmada por el usuario.
La app integra `com.artifex.mupdf:fitz:1.28.5`, un AAR oficial sin cambios al motor.

## Preparado

- Licencia completa de Portylus en LICENSE y dentro de la app.
- Aviso de copyright, ausencia de garantía, identificación de MuPDF y textos de sus
  componentes; visibles sin conexión en Información → Licencias / Leer AGPL completa.
- AndroidX/Compose/DataStore/Kotlin usan licencias permisivas. No se integra Play Services,
  AdMob, Firebase, Crashlytics ni otro SDK propietario. La revisión se repite al añadir SDKs.
- Hash del AAR contrastado con Artifex y fuente del motor/wrapper fijada por commit y SHA-256.
  Ver `third_party/mupdf/source-manifest.json`. El wrapper apunta al mismo commit que el
  tag 1.28.5 del motor. Se conservan fuentes de los componentes de thirdparty y recursos.
- Script para crear un archivo completo de fuentes con los scripts y activos de Portylus,
  las fuentes oficiales de MuPDF y el wrapper Android, evitando los ZIP incompletos sin submódulos.
- La app no elimina deliberadamente atribuciones/Producer existentes de los PDF. No se
  presenta la recomendación de Artifex sobre metadatos como una cláusula nueva de la AGPL.

## Obligaciones antes de distribuir APK/AAB

Ofrecer a los destinatarios acceso gratuito al **código correspondiente completo de esa
versión**, con licencias, modificaciones de Portylus, scripts para compilar/instalar y
dependencias cubiertas. El acceso debe ser claro desde el lugar de descarga/distribución;
se prepara también el botón de fuentes de la app y un enlace en la ficha de Play.

Publicar un repositorio sin las fuentes necesarias del motor, un enlace a master, solo el
AAR o una frase «código disponible bajo petición» no completa esta preparación. Un repositorio
puede contener la app y ofrecer los archivos del motor en la misma publicación de versión,
si las instrucciones/enlaces permiten obtener todo sin coste ni credenciales.

```powershell
python .\scripts\export-source.py
```

El resultado `artifacts/Portylus-v0.1.0-source.zip` incorpora app + motor + wrapper y contiene
los textos originales de licencia. Se verifica CRC del ZIP y SHA-256 de las fuentes externas.
Publicar ese archivo junto a la versión, verificar acceso anónimo y poner su URL concreta en
`sourceCodeUrl` de `distribution.properties`. Si cambian las URLs u otros datos compilados,
generar el archivo de fuentes **después de la última modificación** y antes de distribuir el binario.
No sobrescribir archivos de fuentes ya publicados para una versión distribuida.

Conservar avisos/copyright originales, identificar cambios propios y fechas (esta preparación:
21/10/2026, renombrado y avisos/distribución) y no imponer restricciones adicionales incompatibles
con AGPL a la copia, modificación o redistribución. Revisar condiciones de distribución y uso de
Play para que no contradigan esos derechos. Permitir compilar versiones modificadas e instalarlas
con firma propia; no se incluye bloqueo por firma/DRM ni activación remota. Las claves privadas
de producción no se publican en el archivo de fuentes.

La app funciona localmente: la obligación de entregar fuentes nace al distribuir binarios,
independientemente de que no tenga servidor ni permiso de Internet. Cobrar por la app es compatible
con AGPL, pero no exime de ofrecer el código. Una futura edición cerrada necesita un acuerdo
comercial válido con Artifex antes de distribuirla.

## Límites de la verificación

Se usa el AAR precompilado oficial. Las fuentes y revisiones se han cotejado; no se ha reconstruido
el AAR nativo ni probado igualdad binaria byte a byte. Los avisos incorporan también componentes
opcionales de la distribución fuente, sin afirmar que todos estén activados en Android.
Fuentes completas v0.1.0: https://github.com/gtavo/Portylus/releases/download/v0.1.0/Portylus-v0.1.0-source.zip
Repositorio: https://github.com/gtavo/Portylus. La app enlaza esa descarga exacta. El borrador de ficha
incluye el mismo enlace; Google Play todavía no ha recibido ni publicado esta versión.
Las instrucciones de reconstrucción están en docs/COMPILAR_DESDE_FUENTES.md.

Fuentes oficiales:
- [Licencia MuPDF](https://mupdf.readthedocs.io/en/1.28.5/license.html)
- [Licencias de integración Android](https://mupdf.readthedocs.io/en/1.28.5/guide/using-with-android.html)
- [Guía de Artifex](https://artifex.com/licensing)
- [AGPL completa, secciones 5, 6, 10 y 13](https://www.gnu.org/licenses/agpl-3.0.html)

## Aportaciones voluntarias (preparación v0.0.12)

La app se ofrece gratuitamente y acepta apoyo opcional sin contraprestación. No se integra
ningún SDK de Ko-fi, PayPal, Stripe o Play Billing: el enlace abre una aplicación externa.
Recibir aportaciones no limita derechos AGPL ni elimina la obligación de facilitar las
fuentes completas. Se mantiene la misma funcionalidad para quien aporta y quien no.

Activación v0.0.13: enlace público de apoyo `https://ko-fi.com/gtavoapps`, abierto en
una aplicación externa. No se añaden dependencias ni cambia el cumplimiento AGPL.
