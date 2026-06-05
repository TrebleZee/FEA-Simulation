# Validation Implementation for MainPresenter.runSimulation()

## Summary
Added comprehensive validation logic to `MainPresenter.runSimulation()` that checks for three critical conditions before running the simulation:

1. **At least one support** - Ensures the structure is statically stable
2. **No floating beams** - Verifies all beam elements are connected to supported nodes
3. **Valid geometry** - Detects zero-length elements and missing nodes

## Implementation Details

### 1. Main Entry Point: `runSimulation()` (lines 74-120)
- Extracted from the original button action handler into a dedicated method
- Now calls `validateModel()` before attempting to solve
- Shows user-friendly error dialogs for validation failures
- Enhanced error handling for stiffness matrix singularity with helpful diagnostic messages

### 2. ValidationResult Helper Class (lines 122-149)
- Inner static class to encapsulate validation results
- Provides two constructors: `success()` and `failure(String message)`
- Separates validation logic from UI concerns

### 3. Main Validation Method: `validateModel()` (lines 151-190)
Performs four checks in sequence:

**Check 1: At least one element**
- Returns failure if no beams are defined
- Message guides user to draw beams first

**Check 2: At least one support**
- Returns failure if no supports are defined
- Explains that the structure is statically unstable
- Lists available support types (FIXED, PINNED, ROLLER)

**Check 3: No floating beams**
- Calls `checkForFloatingBeams()` using union-find algorithm
- Returns failure if any unsupported disconnected components exist

**Check 4: Valid geometry**
- Calls `checkGeometry()` to check for zero-length elements
- Returns failure if nodes are missing or elements have zero length

### 4. Floating Beam Detection: `checkForFloatingBeams()` (lines 192-269)
Uses a **union-find (disjoint-set) algorithm** to detect connectivity:

**Algorithm Steps:**
1. Collects all nodes involved in beam elements
2. Collects all supported nodes
3. Initializes union-find structure where each node is its own parent
4. Unions nodes that are connected by beam elements
5. Verifies all supported nodes belong to the same connected component
6. Checks that no unsupported nodes exist in a separate component

**Error Cases Detected:**
- Multiple supports connected to different beam networks
- Beam elements not connected to any supported node
- Partially disconnected structures

**Diagnostic Messages:**
- Specifically identifies whether supports are disconnected from each other
- Or if beams exist that aren't connected to any support

### 5. Union-Find Helper: `find()` (lines 271-282)
Standard union-find root-finding method with **path compression optimization**
- Recursively finds the root of a node's parent hierarchy
- Compresses paths to improve subsequent lookups

### 6. Geometry Validation: `checkGeometry()` (lines 284-307)
Validates cross-element geometry:

**Checks:**
- Each beam element has valid start and end nodes
- No beam has zero or near-zero length (tolerance: 1e-12)

**Diagnostic Messages:**
- Identifies specific element IDs with issues
- Suggests adjustments for zero-length elements

### 7. User Dialog Display: `showErrorDialog()` (lines 309-318)
Creates and displays **JavaFX Alert dialogs**:
- Uses `AlertType.ERROR` for clear visual indication
- Displays structured error messages with title and content
- Blocks further interaction until user dismisses (modal)

## Error Messages

All error messages are user-friendly and include:
1. **Clear problem statement** (what went wrong)
2. **Explanation** (why it's a problem)
3. **Guidance** (how to fix it)

### Example Messages:

**No Support:**
```
Validation failed: No supports defined.

The structure is statically unstable. Please add at least one support
(fixed, pinned, or roller) to a node.
```

**Floating Beam:**
```
Validation failed: Floating beam detected.

Some beam elements are not connected to any supported node.
Please ensure all beams are connected to nodes with supports.
```

**Zero Length Element:**
```
Validation failed: Element 1 has zero or near-zero length.

Both nodes are at the same location. Please adjust the geometry.
```

**Stiffness Matrix Singularity:**
```
Simulation failed: Stiffness matrix is singular or ill-conditioned.

Possible causes:
• Missing or insufficient supports
• Floating (disconnected) beam elements
• Beams with zero or near-zero length

Details: [original solver error message]
```

## Exception Handling

The solver now catches and reports three categories of errors:

1. **Validation Errors** - Pre-checked before solver runs
2. **Singularity Errors** - `IllegalStateException` from solver with diagnostic guidance
3. **Unexpected Errors** - Generic exception handler with stack trace

## Testing

Created `MainPresenterValidationTest.java` with test cases for:
- Valid model configurations
- Missing elements
- Missing supports
- Floating beams (disconnected from supports)
- Zero-length elements
- Multiple connected elements

## Files Modified

1. **MainPresenter.java**
   - Added imports for `javafx.scene.control.Alert` and `AlertType`
   - Extracted `runSimulation()` method from button handler
   - Added `validateModel()` orchestration method
   - Added `checkForFloatingBeams()` with union-find algorithm
   - Added `find()` helper for union-find
   - Added `checkGeometry()` method
   - Added `showErrorDialog()` method
   - Added `ValidationResult` inner class

2. **MainPresenterValidationTest.java** (new)
   - Test cases for validation logic
   - Documentation of expected behavior

## Benefits

✅ **Prevents crashes** - Catches problems before solver attempts to run
✅ **Better UX** - Clear error dialogs instead of cryptic solver errors
✅ **Educational** - Helps users understand FEA model requirements
✅ **Robust** - Comprehensive checks for common modeling mistakes
✅ **Maintainable** - Separated concerns and well-documented code
