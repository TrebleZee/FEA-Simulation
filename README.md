# 2D FEA Simulator – Project Overview

## What This Project Is

This project is a step‑by‑step journey toward building a fully interactive 2D Finite Element Analysis (FEA) simulator using Java, JavaFX, and Maven. It’s designed to be approachable for students while still powerful enough to demonstrate real engineering concepts like deformation, stress, and structural behaviour.

The simulator grows in complexity over three major phases:

- Beam Simulator – simple bending behaviour
- Truss Simulator – axial forces and structural systems
- True 2D FEA – meshing, stress fields, and full plane‑stress analysis

Each phase builds on the last, expanding both the codebase and the user’s understanding.

## Project Goals

The aim is to create a tool that is:

- Educational – explains results in plain English
- Visual – shows deformation, stress, and failure regions
- Interactive – users draw structures directly on the canvas
- Fast – simulations run in seconds
- Beginner‑friendly – minimal jargon, guided workflow

The final product should feel like a lightweight teaching tool rather than a professional engineering package.

## Development Phases

### Phase 1 – Beam Simulator (Low Difficulty)
A simple introduction to structural analysis.

**Features**

- Draw beams by clicking start/end points
- Add supports (fixed, pinned, roller)
- Apply point loads
- Run a basic beam solver
- Visualise bending and deformation

**Learning Outcomes**

- Understanding of bending behaviour
- How loads and supports affect a structure
- Basic MVP architecture for the app

### Phase 2 – Truss Simulator (Medium Difficulty)
A step up into full structural systems.

**Features**

- Create nodes on the canvas
- Connect nodes into truss members
- Apply loads at nodes
- Solve using the direct stiffness method
- Visualise axial forces and displacements

**Learning Outcomes**

- Global stiffness matrix assembly
- Boundary conditions in FEA
- Visualising tension/compression in members

### Phase 3 – True 2D FEA (High Difficulty)
The full experience: meshing, stress fields, and 2D deformation.

**Features**

- Draw polygonal regions
- Auto‑generate triangular meshes
- Apply loads and supports
- Solve plane‑stress/plane‑strain problems
- Display stress heatmaps and deformed shapes

**Learning Outcomes**

- Mesh generation
- Element stiffness matrices
- Stress computation and visualisation

## Success Criteria

**Essential**

- Geometry: Draw, edit, and delete 2D structures; Move points and modify shapes
- Materials: Choose materials; Material properties influence results; Custom materials supported
- Boundary Conditions: Add supports; Add and edit loads
- Solver: Compute deformation; Compute stresses; Produce results quickly
- Visualisation: Show deformed shapes; Show stress heatmaps; Provide numerical values
- Usability: Student‑friendly interface; Minimal jargon; Guided workflow

**Stretch Goals**

- Multiple load cases
- Animation of deformation
- Mesh density control
- Compare results between designs
- Export reports
- Material library
- Save/load projects
- Undo/redo

## Core Feature List (Human‑Readable)

- Drawing: Draw beams, trusses, and 2D regions; Move points; Connect points; Delete components
- Simulation Setup: Choose materials; Add supports; Add forces; Adjust force magnitude
- Running Simulations: One‑click “Run Simulation”; Automatic mesh generation (Phase 3); Automatic solving
- Results: Deformation view; Stress concentrations; Highlight likely failure locations; Maximum stress and displacement values
- Educational Tools: Plain‑English explanations; Why failure occurs; Compare materials; Compare designs; Visual demonstrations of engineering concepts

## Project Vision

By the end of development, this simulator should feel like a small, intuitive engineering lab. Students should be able to:

- Draw a structure
- Apply loads
- Run a simulation
- Understand the results

All without needing a background in advanced mechanics.
