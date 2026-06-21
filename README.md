# Software Engineering Final Project - A.Y. 2025-2026

This repository contains the Java implementation of the board game *Mesos*, developed as the final project for the Software Engineering course.
The project encompasses the development of a distributed system based on a Client-Server architecture. It includes a complete server-side implementation of the game rules, a network communication protocol, and two distinct client interfaces: a Command Line Interface (TUI) and a Graphical User Interface (GUI).

---

## 📑 Table of Contents
1. [Team](#-team---gc8)
2. [Implemented Features](#-implemented-features)
3. [Testing & Coverage](#-testing--coverage)
4. [Software Architecture & Design Patterns](#-software-architecture--design-patterns)
5. [Building the Project](#-building-the-project)
6. [Execution Guide](#-execution-guide)

---

## 👥 Team - GC8
- **Andrea Turchetto** 
- **Arturo Matteo Santarlasci** 
- **Simone Sesana**
- **Lorenzo Marc Baud** 

---

## 🌟 Implemented Features

The application supports both the simplified ruleset (2 players, excluding Artist/Inventor cards and Cave Painting events) and the complete ruleset of the game. 

| Feature | Status | Description |
|:---|:---:|:---|
| **Basic Rules** | 🟢 | Complete implementation of the simplified game rules. |
| **Complete Rules** | 🟢 | Complete implementation of the advanced game rules. |
| **Socket** | 🟢 | Custom TCP Socket protocol for robust client-server communication. |
| **RMI** | 🟢 | Java Remote Method Invocation implementation. |
| **TUI** | 🟢 | Text-based User Interface. |
| **GUI** | 🟢 | Graphical User Interface implemented using JavaFX. |
| **Multiple Matches** (AF) | 🟢 | Server concurrently handles multiple lobbies and active matches. |
| **Persistence** (AF) | 🟢 | Periodic game state saving to disk, allowing matches to resume after a server crash. |
| **Resilience** (AF) | 🟢 | Game state handles client disconnections seamlessly; disconnected players can reconnect while the game skips their turns or pauses. |

*(AF = Advanced Functionality)*

---

## 🧪 Testing & Coverage

The application logic, particularly the `Model` and `Controller` components, has been tested using **JUnit**. Code coverage was monitored throughout the development process to ensure the stability and correctness of the game development.

* **Overall Line Coverage:** > 90% 
* **Overall Branch Coverage:** > 85% 

If you wish to generate and view the coverage report locally, run `mvn test`.

---

## 🏗 Software Architecture & Design Patterns

The system is designed following Object-Oriented Programming (OOP) principles and leverages several Design Patterns to ensure maintainability and scalability

### Model-View-Controller (MVC)
The entire system is structured upon the MVC architectural pattern. 
* **Model:** Contains the pure game logic (`MesosGame`, `Board`, `Tribe`, `Card`) and operates independently of network constraints.
* **View:** Implemented as `TuiView` and `GuiView`, displaying the state to the user. The views receive immutable Data Transfer Objects (`GameDTO`) representing state snapshots to render the game without direct coupling to the Model.
* **Controller:** Manages user inputs (`PlayerAction`) and alters the Model accordingly. The `ViewUpdater` acts as a bridge, utilizing the Observer pattern to broadcast model updates (`ModelUpdateMessage`) back to the connected clients.

### Design Patterns
* **State Pattern:** The `GameController` delegates action handling to specialized state classes (`PositioningState`, `ActionResolutionState`, `EndRoundState`, `PausedState`) based on the current `GamePhase`. This eliminates complex conditional logic and enforces strict phase rules.
* **Strategy Pattern:** Employed in the resolution of Event Cards (`EventCard.resolveEvent()`) and Building Effects (`BuildingEffect`), allowing diverse behaviors to be executed dynamically during the game.
* **Observer Pattern:** The network layer utilizes listeners (`GameEventListener`) to subscribe to Model changes and dispatch network messages (`NetworkMessage`) asynchronously.

---

## 🛠 Building the Project

The project is managed with Apache Maven. Ensure you have Java Development Kit (JDK) installed. 

To compile the source code, run the unit tests, and generate the executable JAR files, navigate to the project root directory and run:

```bash
mvn clean package