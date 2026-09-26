# KuvittaMovies

Aplicación Android estilo plataforma de streaming para descubrir películas completas disponibles legalmente en YouTube.

## Funciones

- Inicio con catálogo y carruseles.
- Búsqueda instantánea por título y género.
- Ficha de película con sinopsis, año y duración.
- Apertura de resultados oficiales en YouTube.
- Favoritos persistentes en el dispositivo.
- Interfaz oscura construida con Kotlin y Jetpack Compose.

## Compilar

La workflow **Build Android APK** compila automáticamente `app-debug.apk` en cada push a `main`. También puede ejecutarse manualmente desde la pestaña Actions.

Localmente: `gradle :app:assembleDebug` con JDK 17 y Android SDK 35.
