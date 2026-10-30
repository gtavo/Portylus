# Versionado de Portylus

## Versiones futuras por fecha · decisión del 29 de octubre de 2026

Esta regla sustituye la numeración incremental anterior solo para nuevas entregas. Conservar la versión actual y todas las anteriores, sus binarios, etiquetas e historial. No cambiar de versión al editar archivos, compilar, probar o preparar este convenio.

Al crear el próximo commit, usar la fecha real del commit en Europe/Madrid: `AA.MM.DD` (por ejemplo `26.10.29`). El segundo commit del mismo día de este proyecto será `26.10.29_2`, después `_3`, etc. Cada proyecto tiene su propio contador; al cambiar de día se vuelve al nombre sin sufijo. Se cuentan commits creados, no intentos de validación, compilaciones ni reintentos de push. Una entrega preparada antes de medianoche debe recalcular fecha/versión antes de crear el commit; después del commit se conserva su identidad al recuperarla.

Mantener las fuentes únicas y los procedimientos de validación, copia, autorización, commit/tag/push y publicación propios de este proyecto. Este cambio de convenio no solicita un commit, publicación ni despliegue. No usar `npm version patch` ni incrementar ya el número antiguo. Los esquemas de datos, protocolos, dependencias y revisiones de documentos no son la versión de la aplicación y conservan su numeración técnica.

Fuente: `version.properties`, campo `versionName`; conservar `versionCode` como entero creciente para actualizaciones Android/Play. Los APK/AAB, notas y tags nuevos usan la versión por fecha. Conservar los códigos por versión, fuentes de terceros y entregas publicadas anteriores.
