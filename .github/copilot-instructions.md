# Copilot instructions — FEA-Simulation

Purpose: short guidance for Copilot sessions to find build/test commands, understand the high-level architecture, and respect project conventions.

## Build, test, and run (explicit commands)
- Build (cross-platform): on Windows use `mvnw.cmd`, on Unix use `./mvnw` or `mvn` if installed.
  - Clean & package: `./mvnw clean package` (or `mvn clean package` / on Windows `mvnw.cmd clean package`)
  - Run app (recommended for JavaFX projects): `./mvnw javafx:run -DmainClass=com.treble.feasimulation.MainApp`
  - Run tests: `./mvnw test`
  - Run a single test: `./mvnw -Dtest=ClassName#method test` (or `mvn -Dtest=ClassName#method test`).

Notes:
- The project uses Maven with the javafx-maven-plugin. Prefer `javafx:run` for development because JavaFX native modules are not bundled into a plain jar by default.
- JDK 17+ is required (JavaFX 21). Ensure `JAVA_HOME` points to a compatible JDK.
- No automated linter or formatter plugin is configured in the POM (no checkstyle/spotbugs configured).

## High-level architecture (big picture)
- Pattern: thin JavaFX UI (view/) + Presenter layer (presenter/) + pure model (model/) + numerical solvers (solver/). There is also a small service layer (service/) for user-facing explanations.
- Key packages and responsibilities:
  - com.treble.feasimulation.view — JavaFX views and canvas components (MainView, CanvasView, BeamCanvasView).
  - com.treble.feasimulation.presenter — Presenter interfaces and MainPresenter that mediate UI ↔ model/solver.
  - com.treble.feasimulation.model — Data structures: Node, Element, BeamElement, Material, Load, Support, FEAData.
  - com.treble.feasimulation.solver — Numerical solvers (BeamSolver for Phase 1). Keep numerical algorithms here (no UI code).
  - com.treble.feasimulation.service — e.g., ResultExplanationService produces plain‑English explanations of results.
- Typical flow: MainApp (JavaFX) → Presenter → update Model → call Solver → Presenter receives results → update View and call ResultExplanationService.
- The project is modular (module-info.java) and exports the main module.

## Key conventions and patterns
- Java module system is used (module-info.java). Keep module exports/opens in sync when adding packages.
- UI code lives strictly in view/*; presenters handle event wiring and business logic orchestration — avoid placing solving code in presenters or views.
- Numerical code belongs in solver/* and should operate on model/* data classes (FEAData, Element, Node) so it is testable in isolation.
- Naming: model classes are singular (Node, Material, Element, BeamElement); solvers are suffixed `Solver` (BeamSolver). Presenters implement a Presenter interface where present.
- Canvas-based drawing uses raw pixel coordinates; conversion utilities (if added) should live near view/ or a small util package to avoid scattering coordinate logic.

## Repository docs & AI helper files
- Incorporated highlights from README.md (phased project: Beam → Truss → 2D FEA; educational focus).
- No existing Copilot/AI assistant configs were found (CLAUDE.md, AGENTS.md, .cursorrules, etc.). This file is added to .github/.

---

If anything should be expanded (e.g., sample commands for packaging a self-contained runtime image, or adding a linter config), say which area to add.
