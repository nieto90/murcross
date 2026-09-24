# murcross-android

Primera app Android de **Murcross** (Kotlin + Jetpack Compose).

## Módulos

| Módulo | Rol |
|--------|-----|
| `:app` | Compose UI, SFX, navegación |
| `:domain` | Modelos Play/Reveal, geometría |
| `:engine` | `validate(play, state)` capas A/B/C (**sin** Reveal / must_room) |
| `:data` | JSON desde classpath/assets |

## Niveles

1. `v3a_mercado` — Mercado de abastos (v3 ejemplo A)  
2. `v3b_biblioteca` — Biblioteca municipal (v3 ejemplo B)  
3. `n1_cafe` — Café (v2)  
4. `n2_atico` — Ático (v2)

Peones anónimos en partida. Nombres / `must_room` solo en `reveal`.

## Build / tests

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # o JDK 17
export ANDROID_HOME=/ruta/al/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :engine:test :data:test
./gradlew :app:assembleDebug
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`

### Instalar

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Target: `minSdk 26` · `targetSdk 34` · ABI universal (no splits).

## Audio

SFX Sol (`sfx/*.ogg`) en `app/src/main/res/raw/`. Mute in-app + silent/vibrate.

## Docs

- [Arquitectura](docs/ARCHITECTURE.md)
- [Gates](docs/GATES.md)
- Schema: `schema/level.schema.json`
