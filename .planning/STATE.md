# CraftCV Improvement — Project State

**Current Phase:** Phase 1 — Edge-to-Edge & Theme
**Last Build:** versionCode 8 (v1.0.4)
**Backend:** FastAPI on Railway (`craftcv-production.up.railway.app`)

## Key Facts

- Package: `com.craftcv.app`
- compileSdk / targetSdk: 35
- Jetpack Compose (BOM 2024.12.01), Material3
- Current theme: `android:Theme.Material.Light.NoActionBar` (deprecated for edge-to-edge)
- `enableEdgeToEdge()` already called in MainActivity
- Scaffold with innerPadding already handles system bar insets
- Dark mode: currently system-only via `isSystemInDarkTheme()`
- UserPrefs: DataStore-based, no theme preference key yet
- Java: `B:\Android Studio\jbr`
- Signing: release.jks / craftcv@123 / alias craftcv

## Decisions

- Locked: Keep existing ad-gating + billing monetization as-is
- Locked: No new backend changes needed for these phases
- Locked: Theme toggle goes in DashboardScreen header area (settings icon)
