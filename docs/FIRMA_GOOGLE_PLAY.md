# Firma de Portylus para Google Play

22/10/2026. Paquete com.gtavoapps.portylus, versión 0.1.0 / código 14.

Se ha creado una clave de subida RSA de 3072 bits para Portylus. Los archivos privados están
solo en .tools/signing/ (ignorado por Git y excluido del archivo de fuentes):

- portylus-upload.p12: almacén PKCS12 con la clave privada.
- credentials.clixml: contraseña aleatoria cifrada con DPAPI para el usuario Windows que la creó.
- portylus-upload-certificate.pem: certificado público para identificar la clave de subida.

scripts/build.ps1 importa esa credencial cuando no hay variables PORTYLUS_* o PDF_* explícitas.
No imprime contraseñas y no las almacena en el código, Gradle ni los archivos distribuibles.
scripts/prepare-upload-key.ps1 rechaza regenerar o sobrescribir una clave existente.

## Custodia pendiente del titular

Conservar una copia segura e independiente del almacén y de su contraseña. El fichero CLIXML
está ligado a Windows/usuario y no es una copia portátil suficiente para recuperar la clave
en otro equipo. No enviarlo por chat, ni añadirlo a Git, al ZIP de fuentes o a una publicación.
Guardar únicamente el certificado público no permite recuperar la clave privada.

La clave creada es de subida. En Play App Signing, Google puede generar y custodiar una clave
separada para firmar las APK que entrega. La APK local release se firma con la clave local de
subida y puede necesitar desinstalación antes de instalar una APK de Play con otra firma.
Exportar antes cualquier recuperación privada necesaria; los PDF externos se conservan.

## Construcción

Ejecutar scripts/build.ps1 con assembleRelease y bundleRelease para preparar los artefactos
firmados locales. preparePlayBundle añade los controles de información pública y se debe usar
antes de subir. Verifica los datos públicos, las URLs y la presencia de las variables de firma.
La versión 0.1.0 ya incorpora la URL de sus fuentes completas y supera ese control local.

Las fuentes completas se ofrecen gratuitamente, sin autenticación, en la publicación v0.1.0
de https://github.com/gtavo/Portylus. No hay una subida automática a Google Play.
