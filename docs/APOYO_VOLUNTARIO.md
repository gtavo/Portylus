# Portylus: apoyo voluntario

Decisión: 21/10/2026. App gratuita, AGPL-3.0-or-later y aportaciones opcionales sin ventajas.
Preparación v0.0.13. El usuario ha creado la cuenta y facilitado la página pública
**https://ko-fi.com/gtavoapps**, con nombre visible **gtavo.apps**. La página se ha comprobado
en un navegador sin sesión de usuario. Contributor está desactivado **según confirmación
expresa del usuario**, sin inspección de sus ajustes privados por el agente.
No se han realizado pagos ni modificado ajustes de Ko-fi/Stripe desde esta tarea.

## Plataforma elegida y alternativas

Para unas pocas aportaciones puntuales se recomienda **Ko-fi gratuito**, con **Contributor
desactivado**. No requiere cuota de pago; así la comisión de plataforma por aportaciones
puntuales es 0 %. PayPal/Stripe mantienen sus costes de procesamiento, que dependen del
medio, importe y país. No se promete recibir el importe bruto íntegro.

| Plataforma | Comisión de plataforma | Encaje con Portylus |
|---|---|---|
| Ko-fi, Contributor desactivado | 0 % en aportaciones puntuales; procesamiento aparte | Primera opción por sencillez, sin cuota ni espera de saldo mínimo |
| Liberapay | No cobra porcentaje de plataforma; procesamiento aparte | Buena alternativa de software libre para apoyo recurrente; las aportaciones únicas no tienen soporte directo completo |
| Buy Me a Coffee | 5 %, más procesamiento | No aporta una ventaja clara para ingresos ocasionales pequeños |

La recomendación se refiere a simplicidad y costes, no predice cuántas aportaciones habrá.
En importes pequeños pesa la parte fija del procesamiento: revisar la tarifa del medio
elegido antes de fijar un importe sugerido. No hace falta contratar un plan premium.

Fuentes oficiales consultadas el 21/10/2026:
- [Ko-fi Contributor y cómo desactivarlo](https://help.ko-fi.com/hc/en-us/articles/25143210488477-Contributor-status)
- [Pagos, comisiones y datos de aportantes](https://help.ko-fi.com/hc/en-us/articles/360013140633-Supporter-payments-FAQ)
- [Primeros pasos Ko-fi](https://help.ko-fi.com/hc/en-us/articles/360014098514-Getting-started-on-Ko-fi)
- [Liberapay FAQ](https://liberapay.com/about/faq)
- [Buy Me a Coffee FAQ](https://help.buymeacoffee.com/en/articles/4539170-frequently-asked-questions)

## Crear y preparar Ko-fi

1. Crear la cuenta con el correo elegido, gtavo.apps@gmail.com, y usar gtavo.apps o Portylus
   como nombre visible según disponibilidad. El identificador actual de la página es **gtavoapps**.
2. Conectar PayPal, Stripe o ambos siguiendo la verificación real del proveedor.
3. En Settings → Payment, desactivar **Contributor**: las cuentas nuevas comienzan con él
   activado y el 5 % también se aplica a aportaciones puntuales hasta desactivarlo.
4. Mantener aportaciones puntuales. No crear tienda, recompensas, membresías, insignias,
   acceso anticipado ni funciones exclusivas vinculadas a Portylus. No ofrecer prioridad.
5. Revisar qué nombre/contacto ve el aportante y qué datos de éste recibe el titular.
   La plataforma comunica datos como nombre visible/correo; los proveedores pueden comunicar
   datos transaccionales. Completar la política de privacidad y titular reales antes de cobrar.
6. Probar desde una sesión anónima que la página identifica correctamente al destinatario y
   permite la aportación sin comprar un producto. No es necesario realizar un pago de prueba
   sin autorización. Comprobar comisiones y configuración de aportaciones.
7. Incorporar la dirección real, sin parámetros de seguimiento, a `supportUrl` en
   distribution.properties. En esta entrega ya está configurado `https://ko-fi.com/gtavoapps`.
8. Recompilar, revisar apertura en navegador y actualizar el archivo de fuentes correspondiente
   a la nueva entrega. La URL queda compilada; cambiarla requiere una nueva compilación.

Texto propuesto para la página:

> Portylus es una aplicación gratuita y de código abierto para leer y anotar PDF.
> Si te resulta útil, puedes apoyar su desarrollo con una aportación voluntaria.
> Todas las funciones están disponibles aportes o no; no se ofrecen recompensas ni ventajas.

## Comportamiento implementado

Información → **Apoyo** explica el apoyo sin contraprestación.
**Abrir Ko-fi** abre `https://ko-fi.com/gtavoapps` en una aplicación externa.
Si se deja `supportUrl` vacío, se conserva el estado alternativo sin botón operativo.
El proveedor no se fija en el código: se puede cambiar la URL si se elige otra plataforma.

Se abre ACTION_VIEW en una aplicación externa; no hay WebView, SDK, permiso de Internet,
identificador de usuario, transmisión de PDF, eventos de conversión, retorno de pago ni estado
de «donante». El texto de privacidad explica el tratamiento independiente de la plataforma.
El correo de soporte y todas las funciones de la app permanecen disponibles para todos.

## Google Play

Google exceptúa de Play Billing las aportaciones directas cuando el 100 % va al creador y
no concede contenido ni servicios digitales, ni siquiera insignias. Es la regla publicada;
la documentación citada no aclara aquí cómo trata cada comisión de procesamiento del flujo
Ko-fi. Por tanto no se da por aprobada esa configuración automáticamente. Verificar el flujo
y su encaje antes de activar el enlace en la entrega de Play, usando la asistencia de Play
si persiste esa duda. Puede publicarse Portylus con `supportUrl` vacío hasta resolverla.

No se presenta el apoyo como donativo fiscalmente deducible ni como entidad benéfica exenta.
No añadir enlaces externos para vender funciones digitales basándose en esta excepción.
Mantener la ficha sin enlaces de pago hasta confirmar su admisibilidad para el flujo final.

Fuente: [FAQ oficial de pagos, aportaciones directas](https://support.google.com/googleplay/android-developer/answer/10281818?hl=en-GB).
