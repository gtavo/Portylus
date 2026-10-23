# PROYECTO — Lector y anotador PDF minimalista para Android

## 1. Objetivo general

Quiero desarrollar una aplicación Android nativa, extremadamente sencilla, para explorar archivos, abrir documentos PDF, realizar anotaciones y guardar los cambios.

La aplicación debe sustituir la necesidad de utilizar por separado un explorador de archivos y un lector/editor PDF.

**La filosofía fundamental es simplicidad absoluta, rapidez y ausencia de funciones innecesarias.**

No quiero desarrollar un editor PDF completo ni un gestor documental. Quiero una herramienta pequeña, rápida y cómoda, especialmente para utilizar en una tablet Android.

El flujo habitual será:

1. Abrir la aplicación.
2. Navegar por mis carpetas o utilizar un marcador.
3. Abrir un PDF.
4. Leerlo, ampliarlo o desplazarme por sus páginas.
5. Activar alguna herramienta de anotación cuando la necesite.
6. Guardar el PDF y regresar al explorador.

Todo debe funcionar localmente, sin cuentas, publicidad, conexión a Internet ni servicios externos.

El nombre definitivo de la aplicación lo decidiremos posteriormente.

---

## 2. Plataforma y tecnologías

Requisitos:

- Aplicación Android nativa.
- Preferentemente Kotlin y Jetpack Compose.
- Compatible con versiones modernas de Android, incluyendo Android 16.
- Interfaz adaptable a tablets y teléfonos.
- Funcionamiento completamente offline.
- Sin telemetría, publicidad ni permisos de Internet.
- Dependencias gratuitas y preferentemente de código abierto.
- Código organizado, sencillo y mantenible.
- APK instalable directamente, sin Google Play.

### Motor PDF

Investiga las alternativas disponibles para visualizar, modificar y guardar PDF.

Evalúa especialmente:

- AndroidX PDF y sus capacidades de edición.
- PDFBox-Android.
- Otras bibliotecas gratuitas y abiertas si ofrecen ventajas reales.

Comprueba las capacidades efectivas de cada biblioteca, sus requisitos de Android, mantenimiento, rendimiento y licencias.

No des por hecho que permiten escribir texto, dibujar, editar anotaciones y guardar archivos sin comprobarlo.

**Prioriza una solución robusta y sencilla frente a una arquitectura compleja.**

---

## 3. Explorador de archivos integrado

La pantalla principal de la aplicación será un explorador de archivos minimalista.

### Navegación

Debe permitir:

- Acceder a las ubicaciones y carpetas autorizadas por Android.
- Navegar por carpetas y subcarpetas.
- Mostrar únicamente carpetas y archivos PDF.
- Abrir cualquier PDF directamente en el visualizador integrado.
- Regresar a la carpeta anterior.
- Ordenar por nombre o fecha de modificación.
- Recordar la última carpeta visitada.
- Recordar la posición de desplazamiento al regresar desde el PDF.

No necesito funciones complejas de gestión de archivos.

### Marcadores de carpetas

Quiero poder establecer marcadores o favoritos para acceder rápidamente a determinadas carpetas.

Funciones:

- Añadir una carpeta a marcadores.
- Eliminar marcadores.
- Reordenar los marcadores.
- Mostrar los marcadores en un apartado compacto del explorador.
- Acceder directamente a cualquier carpeta marcada.
- Conservar los marcadores entre sesiones.

Los marcadores deben apuntar a las carpetas reales, sin duplicar ni copiar sus archivos.

### Acceso al almacenamiento

Utiliza preferentemente Storage Access Framework (SAF) y permisos persistentes.

Estudia cómo permitir una navegación cómoda por las carpetas autorizadas sin tener que abrir constantemente el selector de archivos de Android.

El usuario debe seleccionar o autorizar una ubicación solo cuando sea necesario y después navegar normalmente desde la propia aplicación.

Ten en cuenta las limitaciones de acceso a determinadas carpetas que impone Android.

No solicites permisos globales de almacenamiento si no son imprescindibles.

### Funciones expresamente excluidas

- NO quiero archivos recientes.
- NO quiero historial de documentos abiertos.
- NO quiero bibliotecas documentales.
- NO quiero cuentas ni almacenamiento en la nube integrado.
- NO quiero miniaturas complejas ni indexación de documentos.
- NO quiero funciones de compresión, traslado masivo o gestión avanzada de archivos.
- NO quiero pantallas de bienvenida.

Al abrir la aplicación debe aparecer directamente el explorador, recuperando la última ubicación accesible.

---

## 4. Visualizador PDF

Al pulsar un documento debe abrirse inmediatamente el visualizador integrado.

### Funcionalidades

- Visualización de documentos PDF multipágina.
- Desplazamiento vertical fluido.
- Zoom mediante gestos multitáctiles.
- Ajuste al ancho de pantalla.
- Buena resolución de texto e imágenes.
- Indicador discreto del número de página.
- Correcto funcionamiento con documentos grandes.
- Regreso al explorador mediante el botón Atrás.
- Recuperación de la carpeta desde la que se abrió el documento.

La interfaz de lectura debe ocupar prácticamente toda la pantalla.

Las herramientas de anotación deben poder ocultarse.

No necesito una pantalla adicional de gestión del documento.

---

## 5. Comportamiento táctil: requisito fundamental

Quiero diferenciar estrictamente entre el dedo y el lápiz digital.

Esta es una de las características más importantes del proyecto.

### Modo predeterminado: lectura

**Al abrir cualquier PDF, todas las herramientas de dibujo estarán DESACTIVADAS.**

En este estado:

- El dedo permite desplazarse por el documento.
- Los gestos de dos dedos permiten ampliar o reducir.
- El lápiz digital no dibuja accidentalmente.
- Ninguna interacción genera anotaciones manuscritas.

La aplicación siempre debe abrir los documentos en este estado seguro.

### Bolígrafo: exclusivamente lápiz digital

La herramienta Bolígrafo permitirá dibujar y escribir a mano.

Reglas:

- Solo puede generar trazos mediante un lápiz digital reconocido por Android como stylus.
- El dedo NUNCA debe generar trazos de bolígrafo.
- Estará desactivada por defecto.
- Se activará mediante un botón específico.
- Mientras esté activada, el dedo debe seguir permitiendo navegar por el PDF.
- Permitir configurar el grosor y color.
- Aprovechar la sensibilidad a la presión si está disponible, sin convertirla en un requisito.
- Implementar rechazo de palma cuando sea técnicamente posible.

Un lápiz capacitivo que Android identifica como dedo no debe confundirse con un stylus real.

### Fluorescente: exclusivamente dedo

La herramienta Fluorescente permitirá dibujar trazos semitransparentes sobre el documento.

Reglas:

- Solo se utilizará con el dedo.
- El lápiz digital no deberá generar trazos fluorescentes.
- Estará desactivada por defecto.
- Se activará mediante su propio botón.
- Permitir configurar color y grosor.
- Los trazos serán semitransparentes.
- Con el fluorescente activo, deslizar un dedo dibujará.
- Los gestos de dos dedos seguirán permitiendo ampliar y desplazar el documento, siempre que técnicamente sea viable.

### Activación independiente

Bolígrafo y Fluorescente dispondrán de controles de activación independientes.

Idealmente podrán estar ambos activos al mismo tiempo, porque utilizan dispositivos de entrada diferentes:

- Lápiz digital: bolígrafo.
- Dedo: fluorescente.

Si ambas herramientas están desactivadas, la aplicación permanecerá exclusivamente en modo lectura.

Al cerrar y volver a abrir un documento, las herramientas estarán nuevamente desactivadas.

Implementa esta diferenciación mediante las APIs nativas de Android, identificando correctamente los eventos de stylus y dedo.

### Prioridad

Es preferible impedir una anotación accidental que incorporar comportamientos táctiles complejos.

---

## 6. Texto mediante teclado

Quiero poder incorporar notas escritas con el teclado de Android.

Funciones:

- Activar una herramienta Texto.
- Pulsar en cualquier posición del PDF.
- Introducir texto mediante el teclado.
- Elegir tamaño y color.
- Mover el texto a otra posición.
- Editarlo posteriormente.
- Eliminarlo si es necesario.

El texto no debe convertirse innecesariamente en una imagen.

El modo Texto será una herramienta separada del Bolígrafo y del Fluorescente.

Mientras no esté activado, tocar el PDF no creará cuadros de texto.

---

## 7. Herramientas de anotación

El conjunto de herramientas debe ser reducido.

### Herramientas necesarias

**Navegar**
- Modo de lectura y navegación sin anotaciones.

**Bolígrafo**
- Escritura manual exclusivamente con lápiz digital.

**Fluorescente**
- Resaltado libre exclusivamente con dedo.

**Texto**
- Inserción de texto mediante teclado.

**Borrador**
- Eliminación de anotaciones generadas por la aplicación.
- Nunca debe eliminar accidentalmente el contenido original del PDF.

**Deshacer**
- Deshacer las últimas operaciones de anotación.

No añadir inicialmente figuras, sellos, firmas, comentarios colaborativos, OCR, formularios ni herramientas avanzadas.

### Coordenadas y precisión

Las anotaciones deben mantener su posición exacta al:

- Cambiar el zoom.
- Desplazarse por la página.
- Girar la pantalla.
- Cambiar entre páginas.
- Guardar y volver a abrir el documento.

Utiliza coordenadas vinculadas al documento PDF, no a los píxeles de la pantalla.

---

## 8. Guardado de documentos

El guardado es otro requisito fundamental.

### Guardar

Quiero poder modificar el documento original directamente siempre que Android permita escribir en él.

- Botón Guardar claramente accesible.
- Guardar los cambios en el archivo PDF original.
- No generar automáticamente copias con nombres distintos.
- No crear archivos duplicados innecesarios.
- Ofrecer «Guardar como» para elegir otro destino.

### Integridad del PDF

Es fundamental:

- Conservar el contenido original.
- Mantener el texto seleccionable.
- Preservar las imágenes y su calidad.
- No convertir las páginas completas en imágenes.
- Mantener tamaños, orientaciones y estructura de páginas.
- Guardar las anotaciones con coordenadas correctas.
- Procurar compatibilidad con otros lectores PDF.

Investiga cómo almacenar anotaciones como objetos PDF nativos y editables.

Me gustaría que, después de guardar y volver a abrir un documento, las anotaciones sigan pudiendo modificarse.

Si esto requiere una arquitectura más compleja o presenta limitaciones, explícalo antes de implementar.

No utilices un sistema que rasterice el PDF completo como solución rápida.

### Seguridad del guardado

- Evitar la pérdida de documentos ante errores.
- Utilizar escritura temporal o mecanismos seguros cuando el proveedor de almacenamiento lo permita.
- Gestionar correctamente los permisos de escritura.
- Informar cuando no sea posible sobrescribir el archivo original.
- Ofrecer «Guardar como» en esos casos.
- No declarar guardado un documento hasta comprobar que la operación ha terminado correctamente.

Al salir de un PDF con modificaciones pendientes, preguntar si deseo guardar, descartar o continuar editando.

---

## 9. Diseño e interfaz

Quiero una interfaz extremadamente minimalista.

### Principios generales

- Modo oscuro por defecto.
- Interfaz limpia.
- Sin pantallas innecesarias.
- Sin menús redundantes.
- Controles táctiles cómodos para una tablet.
- Pocos iconos, claramente identificables.
- Animaciones mínimas.
- Transiciones rápidas.
- No mostrar herramientas que no sean necesarias.

### Pantallas

La aplicación tendrá esencialmente dos pantallas.

**Pantalla 1 — Explorador**
- Navegación por carpetas.
- Marcadores.
- Archivos PDF.
- Ordenación sencilla.

**Pantalla 2 — PDF**
- Documento ocupando casi toda la pantalla.
- Barra compacta de herramientas.
- Activación explícita de Bolígrafo, Fluorescente y Texto.
- Botón Guardar.
- Navegación y zoom.

La barra de herramientas debe poder ocultarse sin dificultar su recuperación.

El documento PDF mantendrá sus colores originales aunque la interfaz esté en modo oscuro.

---

## 10. Configuración y persistencia

Mantén la configuración al mínimo.

Guardar únicamente los datos necesarios:

- Marcadores de carpetas.
- Orden de los marcadores.
- Última carpeta utilizada.
- Preferencias de color y grosor de herramientas, si procede.
- Preferencias mínimas de visualización.

No almacenar un historial de documentos.

Las herramientas de dibujo siempre deben comenzar desactivadas al abrir un PDF, independientemente de las preferencias guardadas.

No es necesaria una base de datos compleja si las preferencias pueden gestionarse con DataStore u otra solución sencilla.

---

## 11. Arquitectura del proyecto

Organiza el código separando responsabilidades:

- Explorador de archivos.
- Acceso al almacenamiento y permisos.
- Marcadores y preferencias.
- Visualización PDF.
- Gestión de entrada táctil y stylus.
- Anotaciones.
- Guardado de documentos.
- Interfaz.

No crear capas de abstracción innecesarias.

Evita la sobreingeniería.

No añadir dependencias externas si Android ya ofrece una solución adecuada.

Prioriza un proyecto fácil de entender, mantener y depurar.

---

## 12. Git, GitHub y versionado

Quiero trabajar con Git y GitHub desde el inicio.

- Crear una estructura clara de proyecto.
- Preparar `.gitignore` para Android.
- Incorporar un README breve.
- Documentar dependencias y requisitos.
- Facilitar compilaciones reproducibles.

### Versionado

Quiero un sistema automático de versiones, similar al que utilizo en otros proyectos.

Formato visible:

`1.0.0`

Avance automático:

`1.0.1 → 1.0.2 → ... → 1.0.9 → 1.1.0`

La versión debe actualizarse automáticamente con los commits o mediante un mecanismo equivalente de Git/CI.

Propón la implementación más sencilla y fiable, evitando commits automáticos recursivos.

---

## 13. Compilación y APK

El resultado debe ser una APK Android instalable directamente.

Requisitos:

- Proyecto Gradle funcional.
- Compilaciones debug y release.
- Posibilidad de generar APK localmente.
- GitHub Actions para compilar automáticamente.
- Documentación de instalación.
- Sin dependencia de Google Play.
- Sin necesidad de servidor propio, TrueNAS ni servicios externos para ejecutar la aplicación.

No necesito servicios de backend.

---

## 14. Fases de desarrollo

Organiza el trabajo en etapas pequeñas y verificables.

### Fase 1 — Análisis técnico
- Investigar motores PDF.
- Comprobar capacidades reales de anotación.
- Comprobar compatibilidad de lápiz digital y dedo.
- Investigar almacenamiento SAF.
- Proponer arquitectura.
- Identificar limitaciones.

### Fase 2 — Explorador
- Estructura inicial Android.
- Exploración de carpetas.
- Marcadores persistentes.
- Última carpeta utilizada.

### Fase 3 — Visualizador PDF
- Apertura.
- Renderizado.
- Zoom.
- Desplazamiento.
- Integración con explorador.

### Fase 4 — Entrada táctil y anotaciones
- Diferenciar stylus y dedo.
- Modos desactivados por defecto.
- Bolígrafo.
- Fluorescente.
- Texto.
- Borrador y deshacer.

### Fase 5 — Guardado
- Guardar sobre el original.
- Guardar como.
- Comprobar compatibilidad.
- Verificar que no se pierde calidad.
- Comprobar reapertura y edición.

### Fase 6 — Interfaz y distribución
- Refinamiento visual.
- Pruebas en tablet.
- Optimización.
- Compilación APK.
- Automatización mediante GitHub Actions.

Cada fase debe quedar funcional y comprobada antes de introducir complejidad adicional.

---

## 15. Primera tarea para Codex

Antes de escribir código, quiero que realices un análisis técnico y me presentes una propuesta.

Concretamente:

1. Compara las bibliotecas PDF adecuadas para este proyecto.
2. Recomienda una combinación tecnológica específica.
3. Comprueba cómo diferenciar correctamente los eventos de lápiz digital y dedo en Android.
4. Explica cómo implementarías Bolígrafo y Fluorescente como herramientas independientes.
5. Investiga cómo guardar anotaciones reales en PDF sin rasterizar el documento.
6. Explica cómo conseguir un explorador propio utilizando SAF, con marcadores persistentes.
7. Propón la estructura inicial del repositorio.
8. Indica los riesgos técnicos y las limitaciones relevantes.
9. Propón un orden de implementación con pruebas verificables.
10. Señala cualquier requisito que pueda exigir una solución más compleja de lo previsto.

**De momento, no empieces a programar.**

Primero quiero revisar contigo la arquitectura y las decisiones técnicas.

No añadas funcionalidades que no he solicitado.

Si existen varias formas de resolver algo, prioriza la más sencilla que cumpla correctamente los requisitos.

**Principio rector del proyecto: abrir, leer, anotar, guardar. Nada más.**