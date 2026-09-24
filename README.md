# murcross-android

Primera app Android de **Murcross** (puzzles F2P + ads).  
Mandato: Kotlin + Jetpack Compose, arquitectura limpia testeable, APK en GitHub Releases.

## Docs

- [Arquitectura](docs/ARCHITECTURE.md)
- [Gates release](docs/GATES.md)
- Schema: `schema/level.schema.json`
- Stub nivel: `levels/stub/n1_cafe.json` (Play vs Reveal separados)

## Estado

- Scaffold de contratos listo (Hugo).
- Implementación Gradle/Compose: **Leo**.
- Repo: cuenta personal GitHub de Javier (Lucas autentica; URL pendiente).
- Pack v3 mínimo Dani/Nora: stub OK mientras tanto.

## Orden de implementación (Leo)

1. Gradle multi-módulo `:app :domain :engine :data`
2. Portar modelos + `validate` capas A/B/C (goldens desde stub N1)
3. Compose: tablero / borde / bandeja / Resolver gated / reveal
4. CI según `.github/workflows/android-ci.yml` cuando exista repo
5. Tag `v0.1.0-android` → Release con APKs

Coordinación QA: Quim (gates spoiler + unicidad).
