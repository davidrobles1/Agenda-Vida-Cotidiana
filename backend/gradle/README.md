# Gradle wrapper

El wrapper de Gradle (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` y `gradle-wrapper.properties`) no se generó en este ciclo: requiere descargar un binario desde `services.gradle.org`, y el entorno de esta sesión no tiene acceso de red para eso (y, además, la escritura de un archivo llamado `gradle-wrapper.properties` fue bloqueada por las protecciones de la sesión).

Para generar el wrapper en un checkout real, con Gradle 8.9+ instalado localmente:

```bash
cd backend
gradle wrapper --gradle-version 8.9
```

Esto crea `gradlew`, `gradlew.bat` y `gradle/wrapper/gradle-wrapper.jar` de forma estándar y reproducible. A partir de ahí, usar `./gradlew build`, `./gradlew test`, `./gradlew bootRun` como se documenta en `18-dev-environment.md`.
