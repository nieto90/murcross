# Murcross Android — Arquitectura v0 (primera APK)

**Fecha:** 2026-09-24 · Hugo · CTO  
**Estado:** mandato GO (Javier vía Lucas). Leo implementa; Hugo define gates.  
**Objetivo:** APK debug/release (firmado debug OK en v0) en GitHub Releases. Cero crashes en release.

## Principios

1. **Motor puro, UI tonta.** Reglas de legalidad y unicidad viven fuera de Android UI.
2. **LevelPlay ≠ LevelReveal.** Identidad (nombres, `must_room`, nombres narrativos de O) solo en reveal. El runtime y el validador no la ven.
3. **Tolerancia cero a fallos en release.** Tipos estrictos, tests de contrato sobre N1/N2 (stubs primero), lint, sin `!!` injustificado.
4. **Stub levels OK** hasta pack v3 mínimo Dani/Nora. Schema versionado desde el día 1.

## Módulos (Gradle)

```
:app          → Compose UI, Application, navegación, Releases entry
:domain       → modelos Play/Reveal, GameState, casos de uso (sin Android)
:engine       → validate en capas + occupancy/edge (Kotlin puro, cero Android)
:data         → LevelRepository (assets JSON), schema version check
```

Sin KMP en v0 (coste). Contrato = mismos fixtures JSON + golden tests que el verificador Python cuando exista. Revisitar KMP si el web MVP sigue vivo >1 mes.

## Capas de validación (`:engine`)

| Capa | Qué | UI |
|------|-----|-----|
| A | Colocación: piezas exactas 1×, sin solapes, O en una sala, no sobre V/X | soft feedback |
| B | Bordes: tramos ocupados + inventario P/O por fila/col | soft feedback |
| C | Regla dura: exactamente 1 peón en sala de V | hard → habilita Resolver |

`validate(levelPlay, state) → ValidationResult(ok, softReasons, hardReasons, culpritSlot?)`  
Reveal asigna nombres **después** de `ok`, desde `LevelReveal`, nunca antes.

## Estado de partida

```
GameState {
  placements: List<Placement>  // pieceId, kind, r, c, rot
  marksX: List<Cell>
}
```

Peones = slots anónimos (`pawn_0`…); no llevan nombre en estado ni en UI de tablero.

## Data de nivel (JSON)

Ver `schema/level.schema.json` y `levels/stub/n1_cafe.json`.

- `schemaVersion`: 1  
- `play`: size, rooms[][], victim, pawns (count o ids anónimos), objects+shapes, edge  
- `reveal`: suspectNames[], must_room?, objectNames{}, narrativePlane?

CI rechaza nivel si `play` contiene campos de identidad.

## UI (Compose) — alcance APK v0

1. Selector de nivel (lista stub).  
2. Pantalla partida: tablero + borde + bandeja + X + undo + Resolver gated.  
3. Reveal mínimo (culpable + nombres).  

Gestos avanzados (drag ghost, dial) = backlog Leo/Vera; v0 puede ser tap-to-place + rotación 90° CW.

## Ads / Play Console

Fuera de APK v0. Stub `AdsPort` no-op. Firmar release con debug keystore en CI v0; keystore de producción = paso posterior con Lucas/Javier.

## Dependencias de equipo

| Quién | Qué |
|-------|-----|
| Lucas | Org/cuenta GitHub Murcross + repo + secrets CI |
| Dani/Nora | Pack v3 mínimo (o seguimos stub) |
| Leo | Implementación módulos + UI |
| Quim | Gates QA + checklist spoiler / unicidad |
| Hugo | Este doc, schema, revisión PR, CI gates |
