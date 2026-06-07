# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
mvnw.cmd clean package

# Run application
mvnw.cmd javafx:run

# Run all tests
mvnw.cmd test

# Run a single test class
mvnw.cmd -Dtest=BeamSolverTest test

# Run a single test method
mvnw.cmd -Dtest=BeamSolverTest#testSimpleBeam test
```

## Architecture

**MVP pattern** with strict layer separation:

- `model/` — Pure data structures. `FEAData` is the single source of truth for all model state. `Element` is an interface implemented by `BeamElement`, `TrussElement`, and `TriangularElement`.
- `solver/` — Numerical FEA algorithms. `FEASolver` interface implemented by `BeamSolver` (Euler-Bernoulli + direct stiffness), `TrussSolver`, and `PlaneStressSolver` (CST triangular elements). `SolverFactory` selects the appropriate solver. Matrix math via EJML (`DMatrixRMaj`).
- `mesh/` — `TriangularMeshGenerator` converts `PolygonRegion` definitions into meshes of `TriangularElement`s using an ear-clipping algorithm.
- `presenter/` — `MainPresenter` wires UI events to model mutations and solver calls. `ModelValidator` checks structural validity before solving (supports present, no floating elements).
- `view/` — JavaFX UI. `MainView` owns layout; `BeamCanvasView`/`CanvasView` handle interactive drawing; `HeatmapPlotter` and `ContourPlotter` render solver results.
- `service/` — `ResultExplanationService` generates plain-English summaries of solver output for educational use.

**Data flow:** `MainView` user actions → `MainPresenter` → validates via `ModelValidator` → selects solver via `SolverFactory` → solver writes results back to `FEAData` → `MainView` renders via plotters.

## Module System

The project uses JPMS (`module-info.java`). When adding new packages or external dependencies, update `module-info.java` with the required `requires` and `opens` directives. Surefire is configured with `useModulePath=false` to keep tests outside the module graph.

## Tech Stack

Java 17, JavaFX 21, Maven, JUnit 5, EJML 0.41 for matrix operations.