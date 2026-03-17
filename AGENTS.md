# AGENTS.md

Project: SignalSketch

Goal:
Build an Android app in Kotlin + Jetpack Compose that lets a user walk through a house/building,
collect Wi-Fi signal samples, estimate movement using motion sensors, and render a live floorplan/heatmap.
AR should be supported as an optional enhancement, not a requirement.
Sessions must be saved locally and shareable.

Non-negotiables:

- Use MVVM
- Use ViewModels for UI state and logic
- Use Room for structured local storage
- Use DataStore for simple settings
- Use Navigation Compose
- Keep code organized by feature/domain
- Prefer small, focused files
- Do not break compilation
- After every task, run a build and fix compile issues
- Keep comments minimal and useful

Core features:

1. Read connected Wi-Fi info and visible network scan results
2. Use motion sensors for simple walking/path estimation
3. Build and display a live map/heatmap as the user walks
4. Correlate path position with Wi-Fi signal strength
5. Save and share floorplans/heatmaps
6. Support AR as an optional mapping mode on compatible devices

Architecture guidance:

- Separate UI, ViewModels, repositories, and platform APIs
- Keep Wi-Fi code separate from sensor code
- Keep AR code separate in its own package
- Keep rendering code separate from data collection code
- Keep AR optional and never let it replace the non-AR flow
- Prefer simple implementations first, then refine

Definition of done for each task:

- Project compiles
- No obvious runtime crash for the touched flow
- State survives rotation where reasonable
- New feature is testable in emulator/device
