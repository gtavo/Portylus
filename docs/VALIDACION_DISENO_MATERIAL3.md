# Diseño Material 3 en tonos fríos

Trabajo local del 30/10/2026. Base conservada: 0.1.4 / código Android 18.
El convenio de VERSIONADO.md se aplica al próximo commit; este trabajo no crea
un commit ni una publicación y no renumera entregas previas.

## Alcance

- Paletas clara y oscura propias, con azul y grises fríos: acción principal
  #315DA8 en claro y #AEC6FF en oscuro. Roles de superficie, contenedor, texto,
  borde, error e inversos definidos para cada tema.
- Seguir el sistema (valor inicial), Claro y Oscuro en Ajustes → Apariencia,
  guardados en las preferencias locales existentes, con textos ES/EN/FR.
- Componentes Material 3 existentes coordinados con las paletas: botones,
  campos, interruptores, menús, diálogos, herramientas, contador y avisos.
- Indicadores tonales de herramientas mediante el componente Material de
  alternancia. Selecciones de estilos/goma también muestran una marca.
- Tipografía de sistema y formas Material; objetivos táctiles de 48 dp,
  colores de tinta redistribuidos y contraste de etiquetas sobre muestras.
- Apariencia e Idioma mediante selectores compactos al inicio de Ajustes,
  con estado de selección accesible. El cambio de idioma sigue usando
  LocaleManager y conserva el documento y su tinta pendiente.
- Etiqueta de Ocultar con espacio para los tres idiomas y letra al 130 %.
- Iconos de barras Android claros u oscuros según la elección de la app;
  protección de contraste de la barra de navegación conservada.
- El fondo del espacio de lectura responde al tema; los píxeles del PDF y
  los colores de tinta, fluorescentes y búsqueda conservan sus valores.

## Excepción: explorador tipo Windows 11

Se conservan disposición, navegación y comportamiento de árbol, bookmarks,
ruta, filas, columnas, ordenación y panel lateral/desplegable. No se sustituye
por una biblioteca de tarjetas ni navegación inferior. Sus superficies y
textos usan el tema elegido; carpetas y bookmarks mantienen el acento cálido
reconocible del explorador, con una variante legible en claro.

## Referencias oficiales consultadas

- https://developer.android.com/develop/ui/compose/designsystems/material3
- https://developer.android.com/design/ui/mobile/guides/styles/color
- https://developer.android.com/design/ui/mobile/guides/foundations/accessibility
- https://developer.android.com/develop/ui/compose/accessibility/api-defaults
- https://developer.android.com/develop/ui/compose/system/system-bars

La paleta fija responde a la propuesta aprobada en el chat; los colores del
fondo de pantalla del dispositivo no sustituyen esa identidad. La disposición
del lector sigue adaptándose al espacio y al tamaño de letra.

## Verificación

Resultados y capturas: `artifacts/validation/material3-2026-10-30/`.

- Compilación final: `assembleDebug`, `assembleRelease`,
  `assembleDebugAndroidTest`, `testDebugUnitTest` y `lintDebug`, correctos.
- 37 pruebas JVM, sin fallos, errores ni omitidas. La regresión de contraste
  comprueba texto ≥ 4,5:1 y límites de campos ≥ 3:1 en las dos paletas.
- Lint: 0 errores y 6 avisos UseKtx preexistentes.
- Android API 36, emulador aislado x86_64: 49 pruebas completas en tablet
  1280×800/densidad 160, 11 dirigidas a móvil 360×800 y 6 a móvil 320×800
  con letra al 130 %, todas correctas en el APK final. Los escenarios móviles
  usan el sistema en oscuro; tablet usa sistema claro. La prueba de apariencia
  comprueba también ambos modos manuales independientemente del sistema.
- Capturas finales inspeccionadas: lector y ajustes claro/oscuro, explorador
  tipo Windows, texto ampliado, los tres idiomas y salto de página. La etiqueta
  francesa Masquer se ve completa a 320 dp/letra 130 %; Apariencia e Idioma
  quedan visibles al inicio de Ajustes.
- 194 claves de texto equivalentes en ES/EN/FR, sin duplicados.
- APK firmado, permisos y alineación ZIP/ELF de 16 KiB correctos para ARM64
  y x86_64. No tiene permiso de Internet ni almacenamiento global.

Logs de cierre: `build-layout-final.log`, `final-android-tablet.log`,
`final-android-mobile360.log`, `final-android-mobile320-font130.log`
y `apk-final-verification.log`. Las capturas de cierre están en los directorios
`final-tablet-captures`, `final-mobile360-captures` y
`final-mobile320-font130-captures` de esa carpeta.

Se conserva el primer resultado a doble resolución: cuatro fallos. Tres pruebas
usaban posiciones de píxel fijas y requerían la configuración original; una
aserción exigía siempre iconos claros en la barra Android. Se adapta esa aserción
al modo de apariencia y al sistema. Con la configuración prevista, la batería
completa pasa. No se cambian los algoritmos de gestos ni el guardado para resolver
estos fallos de validación.

Se añade una regresión de contraste para los pares de texto y límites de
campos en ambos temas. La prueba Android de apariencia selecciona los tres
modos desde la interfaz con tinta pendiente, verifica persistencia, instancia
del lector, página, barras del sistema y bytes originales, y recrea la actividad.

La instalación previa del emulador usa una firma diferente y ha rechazado la
actualización de prueba. Se conserva sin borrar datos y se crea un emulador
aislado `portylus-material3` para validar este cambio.

## Entrega local de revisión

- APK: `artifacts/Portylus-diseno-frio-prueba-2026-10-30.apk`.
- SHA-256 APK: `b525a199207d6e9b2b6c6f24a07dd6b85f3aa2ff477a3df72ef482d9832c2fdb`.
- Fuentes correspondientes con archivos originales de MuPDF:
  `artifacts/Portylus-diseno-frio-prueba-2026-10-30-source.zip`, con SHA-256
  en su archivo `.zip.sha256`.

Es una compilación local de prueba con la clave local de subida. Conserva
0.1.4 / 18 y no sustituye los binarios, fuentes ni etiquetas de esa entrega
histórica. Los enlaces públicos de distribución siguen siendo los de la
entrega publicada; las fuentes de este diseño se facilitan junto al APK local.

## Reversibilidad y límites

Copia previa: `artifacts/Portylus-before-material3-20261030-000037.zip`.
SHA-256: `be4cee0a891472a9f678d8ae2784f0d0dd87798521e3c685f2f11fd3e4124759`.
Incluye fuentes y el estado previo de las instrucciones/versionado.

Se conservan los cambios de versionado ya presentes al iniciar el trabajo.
No se añaden dependencias, permisos de Internet ni servicios remotos.
La validación de emulador no acredita un teléfono o lápiz físico.
Sin commit, push, publicación en Google Play ni cambios en otros proyectos.
