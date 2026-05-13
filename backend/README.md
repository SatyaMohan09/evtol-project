# 🚁 eVTOL Trajectory Engine – Backend

## 1️⃣ Overview


> Spring Boot backend service that generates smooth 3D time-parameterized flight trajectories using custom cubic spline interpolation.

Include:

* Purpose
* Scope
* What it does
* What it does NOT do

---

## 2️⃣ System Responsibilities

Clearly define backend responsibilities:

* Load waypoint data (CSV)
* Validate mission input
* Construct cubic spline models
* Sample trajectory at fixed interval
* Expose REST API for visualization
* Handle errors consistently

Clarify:

> This is a simulation backend, not flight firmware.

---

## 3️⃣ Mathematical Model

Explain briefly:

### Time-Parameterized Cubic Splines

Each axis modeled independently:

```
X(t)
Y(t)
Z(t)
```

Spline equation:

```
S(t) = d+c(t-t₀)+b(t-t₀)²+a(t-t₀)³
```

Include:

* Continuity type (C²)
* Why cubic spline chosen
* No external math libraries

Keep this concise but technical.

---

## 4️⃣ Architecture

### Layered Architecture

```
Controller
   ↓
Service
   ↓
Spline Builder + Sampling
   ↓
Domain Model
   ↓
DTO Response
```


## 5️⃣ Package Structure

Present clean tree:

```
com.evtol.trajectoryengine
│
├── controller
├── datasource
├── domain
├── dto
├── exception
├── service
├── spline
└── validation
```

Brief explanation under each folder.

---

## 6️⃣ Data Flow

Explain execution pipeline:

1. Load CSV waypoints
2. Validate input
3. Build spline model
4. Sample trajectory
5. Return JSON response

This helps new contributors understand flow quickly.

---

## 7️⃣ Waypoint Input Specification

### CSV Format

```
t,x,y,z
0.0,0.0,0.0,0.0
1.0,10.0,5.0,3.0
...
```

### Validation Rules

* Minimum 2 waypoints
* Strictly increasing timestamps
* No duplicates
* No NaN
* No missing values

Define behavior on failure (HTTP 400).

---

## 8️⃣ API Specification

### Endpoint

```
GET /api/trajectory
```

### Response Format

Provide clean JSON example.

Explain:

* `t` used for animation
* totalDuration meaning
* deterministic sampling

---

## 9️⃣ Sampling Strategy

Explain:

* Fixed interval (e.g., 0.02s)
* 50 samples/sec
* Smooth visualization
* Deterministic playback

Mention configurability (future enhancement).

Perfect 👍 this is a **very clean structure**.
Below is a **concise technical description (2–3 strong points each)** for:

* Every subfolder
* Every file inside `com.evtol.trajectoryengine`

You can directly reuse this in your backend documentation.

---

# 📦 `com.evtol.trajectoryengine`

Root package containing the complete backend implementation of the trajectory engine.

* Implements layered clean architecture
* Separates domain, service, and infrastructure logic
* Keeps spline computation independent of API layer

---

# 📂 controller

### Purpose

Handles HTTP requests and exposes REST endpoints.

* Acts as entry point to the backend
* Delegates business logic to service layer
* Returns structured DTO responses

---

### 📄 `TrajectoryController.java`

* Exposes `/api/trajectory` endpoint
* Calls `TrajectoryService` to generate trajectory
* Returns `TrajectoryResponse` as JSON
* Does not contain business logic (thin controller design)

---

# 📂 datasource

### Purpose

Responsible for reading external waypoint data.

* Isolates file handling logic
* Converts CSV rows into domain `Waypoint` objects
* Keeps I/O separate from computation

---

### 📄 `CsvWaypointDataProvider.java`

* Reads waypoint data from CSV file
* Parses `(t, x, y, z)` values
* Converts data into list of `Waypoint`
* Throws exception if file format is invalid

---

# 📂 domain

### Purpose

Contains core immutable domain models.

* Represents trajectory system’s core data structures
* Independent of framework (pure Java objects)
* No Spring annotations inside domain layer

---

### 📄 `Waypoint.java`

* Represents a single mission waypoint
* Contains time and 3D coordinates
* Immutable value object

---

### 📄 `CubicSegment.java`

* Represents one cubic spline segment
* Stores coefficients `a, b, c, d`
* Evaluates position for time inside segment

---

### 📄 `TrajectoryModel.java`

* Holds full spline model for X, Y, Z axes
* Contains list of cubic segments
* Represents continuous mathematical trajectory

---

### 📄 `TrajectoryPoint.java`

* Represents a sampled trajectory point
* Contains `(t, x, y, z)`
* Used during sampling stage

---

# 📂 dto

### Purpose

Defines API response objects.

* Used for REST communication only
* Mutable for JSON serialization
* Keeps API layer separate from domain models

---

### 📄 `TrajectoryResponse.java`

* Wraps final trajectory output
* Contains total duration
* Contains list of `TrajectoryPoint`
* Returned by controller

---

# 📂 exception

### Purpose

Centralized exception handling.

* Handles validation and runtime errors
* Converts exceptions into HTTP responses
* Prevents stack traces leaking to client

---

### 📄 `InvalidInputException.java`

* Custom exception for validation failures
* Thrown when CSV data is invalid
* Results in HTTP 400 response

---

### 📄 `GlobalExceptionHandler.java`

* Uses `@ControllerAdvice`
* Converts exceptions into structured JSON error responses
* Maps validation errors to 400, unexpected errors to 500

---

# 📂 service

### Purpose

Contains business logic layer.

* Orchestrates data flow
* Calls spline builder and sampler
* Keeps controller thin

---

### 📄 `TrajectoryService.java`

* Main coordination service
* Loads waypoints
* Validates input
* Builds spline model
* Calls sampling service
* Returns final response DTO

---

### 📄 `SamplingService.java`

* Samples spline at fixed interval (e.g., 0.02s)
* Converts continuous spline into discrete points
* Ensures deterministic output size

---

# 📂 spline

### Purpose

Implements mathematical spline algorithm.

* Builds cubic spline coefficients
* Ensures C² continuity
* No external math libraries used

---

### 📄 `CubicSplineBuilder.java`

* Computes cubic coefficients per segment
* Solves spline interpolation system
* Builds `TrajectoryModel` for X, Y, Z axes

---

# 📂 validation

### Purpose

Validates waypoint input before processing.

* Ensures mission-level correctness
* Protects spline builder from invalid data
* Throws structured validation exceptions

---

### 📄 `WaypointValidator.java`

* Validates minimum waypoint count
* Ensures strictly increasing timestamps
* Checks for duplicates and NaN values
* Throws `InvalidInputException` on failure

---

# 📄 `TrajectoryEngineApplication.java`

* Spring Boot main entry point
* Bootstraps application context
* Starts embedded server (Tomcat)
* Enables component scanning

