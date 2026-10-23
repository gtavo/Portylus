# PDFiz v0.0.6 — 19/10/2026

Entrega local: `artifacts/PDFiz-v0.0.6.apk`, versionName `0.0.6`, versionCode `6`.
Nombre visible y proyecto Gradle: PDFiz. Se conserva `es.gtavo.pdf` y la firma debug anterior
para actualizar instalaciones. Las APK y validaciones históricas no se renombran.

## Cambios y comprobaciones

1. **Quitar carpetas:** navegador → Ubicaciones → Quitar. Elimina raíz, marcadores dependientes
   y última ubicación; libera el permiso SAF. Prueba instrumentada de persistencia y prueba real
   con el selector SAF del emulador, una carpeta y su marcador. Tras quitar y forzar cierre/reabrir,
   ambos desaparecen de PDFiz. La carpeta, subcarpeta y PDF permanecen. SHA-256 antes y después de
   `/sdcard/Documents/PDF-Pruebas/Documento.pdf`:
   `d61d0bfc7093499b7c9c0558186d3b23b01b27a133b1b34b05710f428cf16831`.
   No quedan permisos URI de esa ubicación para PDFiz.
2. **Goma:** parcial o trazo completo; solo escritura o solo resaltados. Las cuatro combinaciones
   se comprueban en JVM, incluyendo trazos escasamente muestreados, puntos, curvas, extremos y
   conservación de estilos y anotaciones ajenas al filtro. Prueba en lector Android con dedo/lápiz,
   cancelación por segundo dedo y desaparición de «Sin guardar» al borrar el último trazo.
   Diálogo inspeccionado a 420 × 900; todas las opciones y Cerrar son accesibles.
3. **Abrir con:** manifest ACTION_VIEW, DEFAULT, BROWSABLE, application/pdf, content/file.
   Prueba del resolvedor y apertura en frío desde proveedor content:// de otro paquete, sin
   concesión SAF persistente. Prueba de onNewIntent con documento abierto, protección de trazos
   sin guardar, cancelación y confirmación de cambio. Apertura implícita por ADB también correcta.
4. **Navegación Android:** barras nativas oscuras con protección de contraste, iconos claros y
   safeDrawing; no se añaden círculos ni imitaciones de botones. Verificación instrumentada de
   contraste/configuración e inspección sobre PDF blanco en emulador Android 16 con navegación
   de tres botones. El taskbar de tablet sigue siendo una superficie controlada por Android.
5. **Última página:** almacén privado de clave SHA-256/página por documento. URI SAF directa y
   a través de árbol comparten clave. Pruebas de cierre/reapertura, nuevo ViewModel, posición real
   del lector, documentos independientes, nuevos desde página 1 y límite ante reducción de páginas.
   Comprobación adicional tras `am force-stop`: apertura ACTION_VIEW en frío muestra **2 / 2**.

## Resultado automatizado

```powershell
.\scripts\build.ps1 -Tasks testDebugUnitTest,assembleDebug,assembleRelease,connectedDebugAndroidTest,lintDebug -Offline
.\scripts\verify-apk.ps1 -SdkRoot C:\0codex\pruebas_apk\.tools\android-sdk
```

- Compilaciones debug y release: correctas. Release sin firmar; la APK entregada es debug instalable.
- JVM: **19 pruebas, 0 fallos**.
- Android 16/API 36 x86_64: **30 pruebas, 0 fallos, 0 omitidas**.
- Apertura externa repetida mediante instrumentación directa: **1 prueba, 0 fallos**.
- Lint debug: **0 errores, 8 avisos** (recomendaciones de actualización/KTX y medida de ancho).
- Firma APK, alineación ZIP/ELF de 16 KiB, ARM64/x86_64: correctas.
- Sin INTERNET ni permisos de almacenamiento global.
- SHA-256 APK: `BC4897B023895DD736917C139D6901AA7A73D6891DA04E81081BE3D78C7FD12B`.

Evidencias en `artifacts/validation/v0.0.6-*` y `artifacts/screenshots/PDFiz-v0.0.6-*`.
La primera ronda instrumentada se descartó por un aviso ANR de System UI y un fallo del proveedor
auxiliar del APK de pruebas. Se corrigió el proveedor aislado y la ronda final completa pasó.

## Límites

La goma edita los trazos en memoria del documento abierto. El guardado de anotaciones en PDF,
deshacer en la interfaz y borrado de anotaciones PDF preexistentes siguen fuera de esta etapa.
Los archivos originales se abren solo para lectura. La última página sí persiste al reiniciar.
Si un proveedor cambia la identidad/URI de un archivo, su posición anterior no se puede asociar
automáticamente. Falta confirmar comportamiento físico en las tablets del usuario y sus exploradores.
No se ha publicado, hecho commit ni push.

Referencias de Android usadas para revisar [intent-filters](https://developer.android.com/guide/components/intents-filters)
y [barras nativas y contraste](https://developer.android.com/develop/ui/views/layout/edge-to-edge).

## Traslado de carpeta

El proyecto completo está en `C:\0codex\PDFiz`. Se trasladaron los 21 elementos de primer
nivel, incluida `.git`, y se verificaron 13.992 archivos por ruta relativa y tamaño, el mismo
estado Git y los SHA-256 de las APK actual y anterior. Las rutas internas del AVD se actualizaron.
`build.ps1 -Tasks projects -Offline` finaliza correctamente desde la nueva carpeta y reconoce
`Root project 'PDFiz'`. Evidencia: `artifacts/validation/v0.0.6-relocation-check.log`.

Windows impidió renombrar/eliminar la raíz abierta por este chat. `C:\0codex\PDF_apk` queda
vacía, sin archivos ni repositorio; podrá eliminarse cuando se cierre el proceso que la mantiene
abierta. Para continuar en la nueva ubicación, abrir `C:\0codex\PDFiz` como proyecto en Codex.
