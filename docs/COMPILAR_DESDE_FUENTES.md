# Compilar Portylus y MuPDF desde las fuentes

## Obtener la versión completa

Las fuentes completas v0.1.3 están publicadas en la release indicada debajo.
La copia local está en artifacts/Portylus-v0.1.3-source.zip.

Para v0.1.3 / código 17 / com.gtavoapps.portylus descargar:

- ZIP: https://github.com/gtavo/Portylus/releases/download/v0.1.3/Portylus-v0.1.3-source.zip
- SHA-256: https://github.com/gtavo/Portylus/releases/download/v0.1.3/Portylus-v0.1.3-source.zip.sha256
- Repositorio y cambios: https://github.com/gtavo/Portylus

El ZIP completo contiene el directorio Portylus y, dentro, upstream con el motor MuPDF
1.28.5 (incluidos thirdparty y recursos) y el wrapper Android. El ZIP automático de GitHub
no incluye upstream; si se usa un clon de Git, scripts/export-source.py descarga y verifica
estos archivos según third_party/mupdf/source-manifest.json.

En Linux/macOS se puede comprobar el archivo con sha256sum -c Portylus-v0.1.3-source.zip.sha256.
En Windows usar Get-FileHash -Algorithm SHA256 y comparar con el fichero .sha256.

## Compilar la app

Requisitos: JDK 17, Android SDK platform 36 y Build Tools 35.0.0. Configurar JAVA_HOME y
ANDROID_HOME o crear local.properties con sdk.dir apuntando al SDK. Las primeras descargas
de Gradle y dependencias necesitan Internet; después se puede usar --offline si están en caché.
Las versiones y hashes de herramientas/dependencias se describen en README y el manifiesto.

En Linux/macOS, desde la carpeta Portylus:

```sh
chmod +x gradlew
./gradlew assembleDebug testDebugUnitTest lintDebug
```

En PowerShell:

```powershell
.\scripts\build.ps1 -Tasks assembleDebug,testDebugUnitTest,lintDebug
```

La APK debug se crea en app/build/outputs/apk/debug/app-debug.apk y se puede instalar con
adb install -r. Para cambiar de firma puede ser necesario desinstalar primero la app;
exportar antes cualquier recuperación privada necesaria. Los PDF externos no se borran.
El motor se resuelve inicialmente como AAR oficial desde el Maven de Artifex, sin cambios.

Para firmar una versión modificada, generar una clave propia y configurar las cuatro
variables PORTYLUS_KEYSTORE_PATH, PORTYLUS_KEYSTORE_PASSWORD, PORTYLUS_KEY_ALIAS y
PORTYLUS_KEY_PASSWORD. Ejecutar assembleRelease y bundleRelease. No se necesita la clave
privada del autor y no hay verificación que impida instalar modificaciones con firma propia.
preparePlayBundle comprueba además los datos públicos destinados a una subida a Play;
no sube nada. No se incluyen claves privadas ni credenciales en estas fuentes.

## Reconstruir el motor y el wrapper Android (opcional)

Esta parte requiere GNU make, un compilador C anfitrión, JDK 17 y un entorno Android nativo.
El wrapper original fija AGP 8.5.2, compileSdk 33 y NDK 28.2.13676358. Instalar esos componentes
con sdkmanager, además del SDK usado para la app. Los siguientes comandos son para Bash:

```sh
sdkmanager 'platforms;android-33' 'build-tools;34.0.0' 'ndk;28.2.13676358'
mkdir -p .tools/mupdf-rebuild
unzip upstream/mupdf-android-fitz-1.28.5.zip -d .tools/mupdf-rebuild
cd .tools/mupdf-rebuild/ArtifexSoftware-mupdf-android-fitz-2f03281
mkdir -p libmupdf
tar -xzf ../../../upstream/mupdf-1.28.5-source.tar.gz -C libmupdf --strip-components=1
make -C libmupdf generate
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
printf '\nABI_FILTERS=arm64-v8a,x86_64\n' >> gradle.properties
chmod +x gradlew
./gradlew assembleRelease
```

Consultar también el README, build.gradle y Android.mk originales incluidos en el ZIP/tar.
El AAR resultante está en build/outputs/aar. Para usarlo en una modificación de Portylus,
copiarlo dentro del proyecto y sustituir en app/build.gradle.kts la dependencia
implementation("com.artifex.mupdf:fitz:1.28.5") por implementation(files("RUTA/AL/ARCHIVO.aar")),
con una ruta relativa a app/. Compilar de nuevo la aplicación y firmarla con una clave propia.

La entrega oficial usa el AAR de Artifex. Se han cotejado versión, commits, archivos y hashes;
no se ha ejecutado esta reconstrucción nativa ni certificado identidad binaria byte a byte.
Las licencias y avisos se conservan en LICENSE, THIRD_PARTY_NOTICES y assets/legal.
