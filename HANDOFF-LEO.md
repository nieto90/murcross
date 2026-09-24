# Handoff Leo — primera APK Murcross Android

## Qué hay listo

| Path | Contenido |
|------|-----------|
| `docs/ARCHITECTURE.md` | Módulos, capas validate, Play vs Reveal |
| `docs/GATES.md` | Definition of Done + CI |
| `schema/level.schema.json` | Contrato JSON v1 |
| `levels/stub/n1_cafe.json` | N1 tutorial (0-index, peones anónimos en play) |
| `scaffold/domain/.../Models.kt` | Modelos Kotlin |
| `scaffold/engine/.../Validate.kt` | Stub `validate(play, state)` |
| `.github/workflows/android-ci.yml` | Plantilla Actions |

Referencia web (reglas): `/workspace/murcross-mvp/js/{model,validator,edge}.js`  
Reglas: `/workspace/gdd/MURCROSS-reglas-congeladas-v1.md` (+ addendum anonimato)

## Qué haces tú

1. Crear proyecto Gradle (AGP actual estable + Compose BOM) con módulos `:app :domain :engine :data`.
2. Copiar `scaffold/` → módulos; implementar `validate` + occupancy/edge.
3. Tests unitarios con `n1_cafe.json` (legal + ilegales por capa).
4. UI Compose mínima (tap place / rot 90° / X / undo / Resolver gated / reveal).
5. Cuando Lucas tenga repo: push + activar workflow + tag release.

## Qué no haces en v0

- AdMob / Play Console production keystore  
- KMP  
- Gestos drag avanzados (backlog Vera)  
- Meter nombres en `LevelPlay` o en UI pre-Resolver  

## Bloqueadores externos

- Repo: https://github.com/nieto90/murcross  
- Pack v3 Dani/Nora (stub basta para APK v0)  

Dudas de arquitectura → Hugo. Gates QA spoiler/unicidad → Quim.
