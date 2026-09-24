# Murcross Android — Polish sprint (CTO unblock)

**Fecha:** 2026-09-24 · Hugo  
**Mandato Lucas/Javier:** Leo = tray + remove-object + audio + look. Iris/Vera/Mira visual. Sol audio.  
**Repo:** https://github.com/nieto90/murcross  
**Release APK:** solo tras PASS Quim (Gates 0+1+2+4).

## Ya existe (no reinventar)

| Pieza | Dónde |
|-------|--------|
| Bandeja Compose | `GameScreen.kt` → `TrayBar` / `TrayChip` |
| Quitar a bandeja | `GameController.returnToTray(pieceId)` |
| Rotación O 90° CW | tap O seleccionada / `rotateObject` |
| SFX SoundPool | `com.murcross.audio.MurcrossSfx` + `res/raw/*.ogg` |
| Play ≠ Reveal | `validate(play, state)` sin must_room |

## Contratos este sprint

### 1. Tray (layout Compose)

- Zona fija bajo tablero, sobre tools/Resolver. Tablero ≤7×7 sin scroll horizontal.
- Hit target ≥48 dp.
- Estados chip: `inTray` | `selected` | `placed` (ghost ~30%).
- Orden: peones | separador | objetos.
- Seleccionar pieza cancela modo X.

### 2. Remove-object

- MVP: tap pieza en tablero (select) → tap su slot en bandeja → `returnToTray` + SFX.
- Alt OK: long-press pieza colocada → return (si Vera confirma).
- Undo cubre remove. No borrar del nivel; solo placement.

### 3. Audio (Sol)

- Map 1:1 `Sfx` ↔ `res/raw` (tap_ui, place_ok, illegal, mark_x, undo, rotate_o, reveal, coach_dismiss, tramo_ok, resolver_ready).
- Nuevos WAV/OGG de Sol en `/workspace/audio/murcross/sfx/` → copiar `.ogg` a `app/src/main/res/raw/` (snake_case).
- Mute en UI (prefs ya en `MurcrossSfx`).
- Respetar ringer silent/vibrate.
- SoundPool; no bloquear UI thread.

### 4. Look (Iris/Vera)

- Solo tokens: RoomColors+patrón, peón anónimo, V distinta, glyph X.
- Hard ban: nombres / must_room / badges sala en partida.
- Tramos 0: no ocultar sin OK Vera (WARN Quim abierto).
- Placeholder vector OK hasta assets Iris.

## Límites de módulo

- UI/SFX → `:app`. API remove nueva → `:engine` + unit test.
- No tocar JSON reveal salvo bug Gate 2.

## DoD polish (antes de pedir release)

1. Tray N1/N2: select / place / rotate O / return / X / undo  
2. Remove-object en path feliz + undo  
3. SFX place/illegal/rotate/X/undo/reveal + mute  
4. Look sin spoiler (Gate 3 Quim)  
5. `./gradlew test lint assembleDebug` PASS  
6. Quim Gate 4 → entonces tag release

## Gate 4 device (en curso en box Hugo)

- Serial: `emulator-5554` (boot aún calentando package manager)
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Launch: `adb shell am start -n com.murcross.app/.MainActivity`
