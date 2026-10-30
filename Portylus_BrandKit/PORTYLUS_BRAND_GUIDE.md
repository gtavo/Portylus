# Portylus — identidad visual

## Interfaz vigente · adaptación local del 30/10/2026

La interfaz adopta Material 3 con azul y grises fríos y los modos Seguir el
sistema, Claro y Oscuro. Los roles y pares de contraste se centralizan en
`ui/PortylusTheme.kt`; la especificación y validación están en
`docs/VALIDACION_DISENO_MATERIAL3.md`. Este esquema sustituye los colores
de interfaz descritos en la referencia inicial de abajo.

El símbolo, el icono adaptativo y sus archivos originales se conservan. También
se conserva el explorador tipo Windows 11, incluidos el árbol, la ruta, la
lista de detalles y sus acentos de carpeta/bookmark. No se alteran el color
del papel ni los valores de tinta. En móvil, el lector usa las dos filas
adaptativas existentes para mantener las herramientas a la vista.

## Referencia inicial de la marca

La P integra documento y lápiz. El azul/cian, las superficies navy y las curvas suaves relacionan
Portylus con Docenda y Positio. El trazo ámbar distingue la anotación manual y aporta un acento propio.

- Símbolo libre: logo/portylus-symbol-transparent.png, PNG original transparente sin recortes.
- Cabecera Android: símbolo libre + Portylus + gtavo.apps, sin contenedor cuadrado.
- Instalación Android: icono adaptativo con fondo navy; respetar la zona segura de Android.
- Acento principal cian, azul para la marca, blanco para el documento. No recolorear las páginas del PDF.
- Superficies: fondo #061426, cabecera #071A33, panel #0B2748.
- Texto #F7FAFF y secundario #AFC2DB; ámbar #FFC857 para el gesto de lápiz del símbolo.
- Bordes suaves de 10/18/24 dp; controles táctiles con nombres accesibles.
- Guardar, Deshacer y Rehacer permanecen en la cabecera de lectura. Herramientas en la misma fila, con franja central
  desplazable en móvil; se puede ocultar la barra completa para leer.
- Sin pantallas de bienvenida, adornos sobre el PDF, cuentas ni nuevos ajustes de marca.

El símbolo se generó con image_gen integrada; la aplicación lo usa desde sus propios recursos locales.
No requiere servicios de generación, conexión ni permisos de Internet para ejecutarse.
Referencias revisadas: guías y tokens actuales de Docenda_BrandKit y Positio_BrandKit.
Los proyectos Docenda y Positio no se modificaron.


Actualización 0.1.0 (23/10/2026): Información muestra el símbolo de Portylus junto a la versión
y la marca gtavo.apps. Acerca de y Apoyo incorporan el avatar original de gtavo.apps,
procedente de docs/ko-fi/gtavo-apps-logo-v1.png, sin sustituir el icono de instalación de Portylus.
Las cuatro secciones de Información se ajustan al ancho disponible en móvil y tablet.
Los enlaces públicos se centralizan en distribution.properties y se abren externamente.
