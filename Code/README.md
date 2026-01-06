# Virtual Café Server ☕

A multi-threaded Java client-server application demonstrating concurrent order management, socket programming, and thread synchronization. This system simulates a virtual café where multiple customers can simultaneously place orders while the server manages brewing capacity constraints and order completion with real-time notifications.

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Compilation](#compilation)
    - [Running the Application](#running-the-application)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Design Decisions](#design-decisions)
- [Known Limitations](#known-limitations)
- [Author](#author)

---

## 🎯 Overview

The Virtual Café Server is an educational project that demonstrates key concepts in network programming and concurrent systems:

- **Client-Server Architecture**: TCP/IP socket communication between multiple clients and a central server
- **Multi-threading**: Each client connection handled by a dedicated thread with concurrent order processing
- **Thread Synchronization**: Coarse-grained locking to prevent race conditions in shared state
- **Asynchronous Notifications**: Background threads for real-time server-to-client messaging
- **Resource Management**: Capacity-constrained brewing system with automatic queue processing

**Use Case**: Multiple customers connect to a virtual café, place orders for tea and coffee, and receive notifications when their orders are ready for collection. The server enforces brewing capacity limits (maximum 2 teas and 2 coffees brewing simultaneously) and manages orders through three distinct areas: waiting, brewing, and ready for pickup.

---

## ✨ Features

### Core Functionality
- **Multi-Client Support**: Handles unlimited concurrent customer connections, each in its own thread
- **Socket-Based Communication**: Full-duplex TCP/IP communication between clients and server
- **Natural Language Commands**: Intuitive order syntax with flexible parsing (e.g., "order 2 teas and 3 coffees")
- **Real-Time Notifications**: Asynchronous alerts when orders complete, delivered via background listener threads

### Order Management System
- **Three-Area Architecture**:
    - **Waiting Queue**: FIFO queue for pending items awaiting brewing capacity
    - **Brewing Area**: Active preparation zone with capacity constraints
    - **Tray Area**: Completed orders organized by customer, ready for collection

- **Capacity Enforcement**: Maximum 2 teas and 2 coffees brewing simultaneously
- **Realistic Brewing Times**: 30 seconds for tea, 45 seconds for coffee
- **Automatic Processing**: Items automatically move from waiting → brewing → tray as capacity becomes available

### Order Tracking
- **O(1) Status Checks**: Fast order status lookups using dual-tracking counters
- **Per-Customer State**: Each customer has an `Order` object tracking items across all areas
- **Complete Order Detection**: Server notifies customers immediately when all items are ready

### Thread Safety
- **Synchronized Methods**: All `VirtualCafe` methods use the `synchronized` keyword for coarse-grained locking
- **Race Condition Prevention**: Consistent state updates across concurrent client threads
- **Safe Disconnection Handling**: Proper cleanup when customers exit (gracefully or via Ctrl-C)

### Logging & Monitoring
- **Real-Time Console Logs**: Live updates showing all area contents and customer counts
- **JSON Logging**: Timestamped state snapshots written to `VirtualCafe_logs.json`
- **Comprehensive State Tracking**: Full visibility into waiting, brewing, and ready items

---

## 🏗️ Architecture

### System Components

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│  Customer   │◄───────►│  CustomerHandler │◄───────►│ VirtualCafe │
│  (Client)   │  Socket │   (Runnable)     │  Shared │   (State)   │
└─────────────┘         └──────────────────┘  Access └─────────────┘
                               Thread                        │
                                                             │
                        Multiple CustomerHandler        Manages:
                        threads running                 - Orders
                        concurrently                    - Areas
                                                       - Capacity
```

### Data Flow

1. **Client Connection**: `Barista.java` accepts socket connections and spawns `CustomerHandler` threads
2. **Command Processing**: `CustomerHandler` parses commands and invokes `VirtualCafe` methods
3. **Order Placement**: Items added to waiting queue, `Order` counters updated
4. **Automatic Brewing**: Background check starts brewing threads when capacity available
5. **Item Completion**: Brewing threads sleep for brew time, then move items to tray
6. **Client Notification**: Server sends async message when entire order complete
7. **Collection**: Customer collects order, items removed from tray

### Thread Model

- **Main Thread**: Accepts incoming client connections in infinite loop
- **CustomerHandler Threads**: One per connected client, handles command I/O
- **Message Listener Threads**: One per client, receives async server notifications
- **Brewing Threads**: One per brewing item, sleeps for brew duration then completes

---

## 🛠️ Technologies

- **Java 8+**: Core language with socket and threading APIs
- **Java Sockets**: `ServerSocket` and `Socket` for TCP/IP communication
- **Java Threads**: `Thread` and `Runnable` for concurrency
- **Java Collections**: `Queue`, `List`, `Map` for area management
- **JSON Logging**: Manual JSON formatting for state persistence

---

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK)**: Version 8 or higher
- **Terminal/Command Prompt**: For compilation and execution

Verify Java installation:
```bash
java -version
javac -version
```

### Compilation

Navigate to the project root directory (containing `Barista.java` and `Customer.java`).

#### macOS / Linux
```bash
# Compile server and all helper classes
javac -d . Barista.java helpers/barista/*.java

# Compile client
javac Customer.java
```

#### Windows
```cmd
# Compile server and all helper classes
javac -d . Barista.java helpers\barista\*.java

# Compile client
javac Customer.java
```

**Note**: The `-d .` flag ensures compiled `.class` files are placed in the correct package structure (`helpers/barista/`).

### Running the Application

#### Step 1: Start the Server

Open a terminal window and run:

```bash
java Barista
```

**Expected Output:**
```
✓ Virtual Cafe Server Started
Barista is waiting for customers to join the Virtual cafe...
```

The server will listen on **port 8888** and accept connections indefinitely.

#### Step 2: Connect Client(s)

Open **one or more separate terminals** and run:

```bash
java Customer
```

**Expected Output:**
```
=== Welcome to Virtual Café  ===

Welcome Please enter your name: 
```

Enter a customer name when prompted. Multiple clients can connect simultaneously, each with a unique name.

#### Step 3: Interact with the Café

Once connected, you'll see the available commands. Type commands at the `V-Cafe>` prompt.

---

## 📖 Usage

### Available Commands

```
V-Cafe> order <quantity> <tea/coffee> [and <quantity> <tea/coffee>]
V-Cafe> order status
V-Cafe> collect
V-Cafe> exit
```

### Command Examples

#### Placing Orders
```bash
# Single item orders
V-Cafe> order 1 tea
Order received for Alice (1 teas and 0 coffees)

V-Cafe> order 2 coffees
Order received for Alice (0 teas and 2 coffees)

# Combined orders
V-Cafe> order 2 teas and 3 coffees
Order received for Alice (2 teas and 3 coffees)

V-Cafe> order 1 coffee and 4 teas
Order received for Alice (4 teas and 1 coffees)
```

#### Checking Status
```bash
V-Cafe> order status
Order status for Alice:
  - 2 coffee and 1 teas in waiting area
  - 1 coffee and 2 tea currently being prepared
  - 0 coffees and 0 teas currently in the tray
```

#### Collecting Orders
```bash
# Server notification (automatic when order completes)
Order completed for Alice (3 teas and 2 coffees). Please collect!

V-Cafe> collect
Order collected for Alice (3 teas and 2 coffees)
```

#### Exiting
```bash
V-Cafe> exit
Goodbye Alice

Thank you for visiting Virtual Café!
```

**Note**: You can also press `Ctrl-C` for emergency exit. The client handles this gracefully with a shutdown hook.

### Order Flow

1. **Place Order** → Items added to waiting queue, server acknowledges receipt
2. **Automatic Brewing** → Server moves items to brewing area when capacity available (max 2 teas, 2 coffees)
3. **Item Completion** → Each item brews for its designated time (tea: 30s, coffee: 45s)
4. **Notification** → Server sends async message when *entire order* is ready
5. **Collection** → Customer collects completed order from tray

---

## 📁 Project Structure

```
.
├── Barista.java                    # Server entry point
├── Customer.java                   # Client application
└── helpers/barista/
    ├── VirtualCafe.java            # Core business logic and state management
    ├── CustomerHandler.java        # Client connection handler (Runnable)
    ├── Order.java                  # Customer order entity with area counters
    └── OrderItem.java              # Individual item representation (customer + type)
```

### File Descriptions

| File | Purpose |
|------|---------|
| `Barista.java` | Creates `ServerSocket`, accepts connections, spawns `CustomerHandler` threads |
| `Customer.java` | Client program with command-line interface and async message listener |
| `CustomerHandler.java` | Handles client communication, parses commands, invokes `VirtualCafe` methods |
| `VirtualCafe.java` | Manages three areas, tracks customers, enforces capacity, handles brewing |
| `Order.java` | Tracks per-customer item counts across areas for O(1) status lookups |
| `OrderItem.java` | Pairs customer name with item type (tea/coffee) for efficient storage |

---

## 🧠 Design Decisions

### Why This Architecture?

**Dual-Tracking Approach**: The system maintains both `Order` objects (with counters) and explicit data structures (Queue, List, Map) for areas. This provides:
- **O(1) Status Checks**: `Order` counters enable instant status queries without iterating through areas
- **Efficient Operations**: Area data structures optimized for their specific operations (FIFO queue, fast iteration, customer-keyed map)

**Coarse-Grained Synchronization**: All `VirtualCafe` methods are `synchronized` on the instance. While not the most fine-grained approach, it:
- Guarantees thread safety with simple reasoning
- Prevents all race conditions in shared state
- Acceptable for educational purposes and moderate load

**Separate `OrderItem` Class**: Pairing customer names with item types in a dedicated class enables:
- Clean data structure declarations (no ugly nested generics)
- Type safety with enum for tea/coffee
- Easy iteration and filtering by customer

**Three Explicit Areas**: Using distinct data structures for waiting, brewing, and tray makes the state machine clear and mirrors real-world café operations.

### Data Structure Choices

| Area | Structure | Rationale |
|------|-----------|-----------|
| **Waiting** | `Queue<OrderItem>` | FIFO order processing ensures fairness |
| **Brewing** | `List<OrderItem>` | Fast iteration to find items by customer; supports removal during brewing |
| **Tray** | `Map<String, List<OrderItem>>` | O(1) lookup by customer name; supports partial collection |

### Threading Strategy

- **One Thread Per Client**: Simplifies command handling; blocking I/O is acceptable
- **One Thread Per Brewing Item**: Simulates concurrent preparation; uses `Thread.sleep()` for timing
- **Background Listener Thread**: Enables async notifications without blocking command input

---

## ⚠️ Known Limitations

### Functional Limitations

- **FIFO Blocking**: Items are processed strictly first-in-first-out from the waiting queue. If the first item in the queue cannot brew due to capacity constraints (e.g., 2 coffees already brewing and next item is coffee), processing halts even if later items could brew (e.g., teas).

- **No Order Cancellation**: Once placed, orders cannot be cancelled or modified. Customers must wait for completion or disconnect.

- **Disconnect Behavior**: When a customer disconnects, all their items (waiting, brewing, or ready) are removed rather than being redistributed to other customers or retained.

### Operational Limitations

- **No Graceful Shutdown**: Server must be terminated with `Ctrl-C`. No cleanup of brewing threads or client notifications on shutdown.

- **Unbounded Log Growth**: `VirtualCafe_logs.json` grows indefinitely without rotation, compression, or cleanup.

- **No Authentication**: Customers can use duplicate names, potentially causing confusion or order mix-ups.

- **Port Hardcoded**: Server listens on port 8888 with no configuration option.

### Performance Considerations

- **Coarse-Grained Locking**: All `VirtualCafe` methods lock the entire instance, potentially creating contention under high load.

- **Synchronous Logging**: Every state change triggers console and file I/O, which could become a bottleneck.

- **O(m) Status Logging**: Console logs iterate through all areas to count items, though this is acceptable for debugging.

---

## 👤 Author

**Konstantinos Patatas**

---

## 📄 License

This project is an educational coursework assignment. Please respect academic integrity policies if you're a student.

---

## 🙏 Acknowledgments

Architecture inspired by the Bank client-server example from course materials, adapted for concurrent order management with capacity constraints.

---

**Questions or suggestions?** Feel free to open an issue or submit a pull request!