# Murcross Android — Gates (release)

**Norma:** tolerancia cero a fallos en release. Prevenir > parchear.  
**Crosswalk QA:** `/workspace/gdd/MURCROSS-QA-gate-MVP.md` (addendum Android).

## Ownership

| Gate | Owner | Notas |
|------|-------|-------|
| 0 Build | Leo (+ Hugo review) | assembleDebug/Release |
| 1 Estática | Leo (+ Hugo review) | lint / detekt |
| 2 Motor / contrato | **Quim firma** | N=1 + lint 0,0 + densidad; paridad `verify_levels_v2.py`; ≥1 ilegal/capa + 1 legal en `:engine` |
| 3 Spoiler | **Quim** | Bloqueante al meter nombres reales. Hard ban: `must_room` en validate/partida o badges pre-Resolver = FAIL |
| 4 Smoke | **Quim** | Gama baja cuando haya APK; sin Gate 0/4 no hay PASS Quim |
| 5 CI | Hugo/Leo | Cuando exista repo |

**DoD primera APK (PASS Quim):** Gates **0 + 1 + 2 (stub) + 4**. Gate 3 al integrar nombres reales. Gate 5 al existir remoto.

**Protocolo:** Leo entrega APK → Hugo avisa a Quim (build + device target) → Quim corre Gate 4 el mismo día.

## Gate 0 — Build

- [ ] `./gradlew :app:assembleDebug` PASS  
- [ ] `./gradlew :app:assembleRelease` PASS (keystore debug OK en v0)  
- [ ] APK en GitHub Releases (tag `v0.x.x-android`) cuando haya remoto

## Gate 1 — Calidad estática

- [ ] `./gradlew lint` sin errors (warnings triageados)  
- [ ] Detekt o equivalente: sin `!!` nuevos sin justificación en review  
- [ ] `minifyEnabled` release ON cuando haya Proguard rules mínimas (v0.1+)

## Gate 2 — Motor / contrato (firma Quim)

- [ ] Unit tests `:engine` sobre fixtures stub N1 (y N2 cuando exista)  
- [ ] `validate` capas A/B/C: ≥1 test ilegal por capa + 1 legal  
- [ ] Unicidad: N=1 (CI o paridad con `python3 gdd/murcross_tools/verify_levels_v2.py`)  
- [ ] Schema: JSON levels pasan `schema/level.schema.json`  
- [ ] Lint autoría: ban línea P/O = 0,0; densidad ~35–45% (warning stub tutorial OK documentado)

## Gate 3 — Spoiler / anonimato (Quim)

- [ ] Strings de `reveal` no aparecen en layout de partida (UI + TalkBack)  
- [ ] Analytics/events de partida no llevan nombres de sospechosos  
- [ ] Assets de reveal no precargados en pantalla juego (o gated)  
- [ ] **Hard ban:** no `must_room` en validate de partida ni badges pre-Resolver

## Gate 4 — Crash-free smoke (Quim)

- [ ] open → place/remove/rot/X/undo → Resolver legal → reveal → back  
- [ ] Rotación de dispositivo: no pierde estado o reinicia limpio  
- [ ] Sin ANR en gama baja

## Gate 5 — CI (GitHub Actions)

Workflow mínimo: JDK 17 → `test lint assembleDebug assembleRelease` → artifacts → Release en tag `v*`.

**Repo:** cuenta GitHub **personal de Javier** (no org Murcross nueva). URL pendiente (Lucas autentica gh/conector). Seguir en `/workspace/murcross-android/` sin remoto.
