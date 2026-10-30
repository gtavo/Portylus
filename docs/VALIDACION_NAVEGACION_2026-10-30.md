# Carpetas, apertura y navegación — 30/10/2026

Cambios locales sobre el diseño Material 3 ya presente. Versión conservada:
0.1.4 / código Android 18. Sin commit, push ni publicación en Google Play.

## Comportamiento

- Mis carpetas → ¿Cómo añadir Descargas? explica la restricción del sistema y
  ofrece Añadir subcarpeta y Abrir PDF de Descargas. El selector recibe una
  sugerencia de ubicación en Download; un proveedor que no la reconozca puede
  mostrar su ubicación predeterminada. El acceso se autoriza al elegir un
  archivo o subcarpeta, nunca por construir esa dirección.
- Android 11+ impide conceder la raíz Download mediante el selector de
  carpetas. Se puede crear/elegir una subcarpeta, como Download/PDF, y añadirla.
  Los PDF ya descargados también se abren individualmente. No se mueven archivos
  ni se añaden permisos de acceso global.
- Abrir PDF usa una carpeta abierta sin flechas superpuestas y una etiqueta
  visible, con el tamaño táctil mínimo de 48 dp.
- El contador inferior presenta anterior, número pulsable y siguiente. Las
  flechas son horizontales, con etiquetas accesibles y ayuda al mantener pulsado.
  No hay recorrido circular: se desactivan al llegar a cada extremo y durante
  Guardar. Un documento de una página desactiva ambas.
- Flechas y salto numérico comparten la navegación existente: cancelan el
  gesto provisional, cierran la búsqueda, vuelven a lectura y conservan zoom,
  tinta completada, deshacer y guardado manual. Funcionan con la barra oculta.
- Las siete cadenas nuevas están incluidas en español, inglés y francés.

## Referencias de Android

- Restricción de la raíz Download:
  https://developer.android.com/about/versions/11/privacy/storage#document-access-restrictions
- Selector de archivos/carpetas y ubicación inicial:
  https://developer.android.com/training/data-storage/shared/documents-files
- EXTRA_INITIAL_URI es una sugerencia, con alternativa del sistema:
  https://developer.android.com/reference/android/provider/DocumentsContract#EXTRA_INITIAL_URI

## Validación

- assembleDebug, assembleRelease, assembleDebugAndroidTest, testDebugUnitTest
  y lintDebug correctos. 37 pruebas JVM correctas; Lint: 0 errores y 6 avisos
  UseKtx previos.
- Android API 36, emulador aislado portylus-material3: 5 pruebas de navegación
  y 8 de regresión (carpetas, idiomas, apariencia y búsqueda) en tablet
  1280×800/densidad 160; 10 pruebas en móvil 320×800, letra 130 %, sistema
  oscuro. Todas correctas.
- Las nuevas pruebas comprueban flechas, límites, una sola página, zoom real
  con dos dedos, tinta pendiente, bytes del original, barra oculta, búsqueda,
  diálogo numérico y apertura/cancelación de los dos selectores de Descargas.
- Se conserva la primera ejecución de diagnóstico. El evento de doble toque
  simulado no declaraba TOOL_TYPE_FINGER y el cierre del selector necesitaba
  volver por su jerarquía. Se corrigen las pruebas con gesto real de dos dedos
  y cierre completo del selector. No se cambia el motor de gestos para ello.
- En la revisión visual a 320 dp/letra 130 % se detectó que el botón Cancelar
  se solapaba con Abrir PDF de Descargas. Las tres acciones se disponen en una
  sola columna. Se recompila y se repiten las 23 pruebas Android del cierre.
  También se conserva un fallo transitorio de la acción accesible en una prueba
  previa de búsqueda; su repetición completa pasa sin cambios de producción.
- 201 claves ES/EN/FR equivalentes y sin duplicados. Capturas revisadas de
  tablet y móvil, claro/oscuro, ayuda, selector y contador. El selector de
  carpetas real confirma que Download no admite Usar esta carpeta.

La validación es de emulador y no certifica un dispositivo o stylus físico.
Los registros y capturas están en artifacts/validation/navigation-2026-10-30/.

## Entrega local

- APK firmado: artifacts/Portylus-navegacion-prueba-2026-10-30.apk
- Fuentes completas: artifacts/Portylus-navegacion-prueba-2026-10-30-source.zip
- Cada archivo tiene su SHA-256 adyacente. Se conservan los artefactos y
  fuentes anteriores, incluido el diseño frío de esta misma fecha.
- Copia de los ocho archivos antes de estos cambios:
  artifacts/Portylus-before-navigation-20261030-203500.zip
