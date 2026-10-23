# MuPDF 1.28.5: fuente y procedencia

`source-manifest.json` fija la versión, commits y SHA-256 del AAR y de las fuentes oficiales.
El AAR local coincide con el SHA-256 publicado por Artifex. El submódulo `libmupdf`
del wrapper 1.28.5 coincide con el commit del tag del motor 1.28.5.

El archivo `mupdf-1.28.5-source.tar.gz` contiene los componentes de `thirdparty`, fuentes,
recursos y scripts de compilación. No usar como sustituto el ZIP automático del repositorio
principal de GitHub, que omite los contenidos de sus submódulos.

`scripts/export-source.py` descarga/verifica los dos archivos y los incluye en el ZIP
de fuentes de cada versión. Se ofrecen junto con todo el código de Portylus. Los avisos
de licencia extraídos están integrados en `app/src/main/assets/legal/notices.txt`.

Para reconstruir el motor: extraer el ZIP del wrapper y el tar oficial; colocar el contenido
del tar bajo `libmupdf/` del wrapper y seguir su README y build.gradle. El wrapper declara
su NDK y parámetros. El artefacto de Portylus usa el AAR oficial; no se ha certificado una
reconstrucción nativa idéntica byte a byte. La app se recompila con JDK 17/SDK 36.
