# Virtual Café Server

A multi-threaded client-server application demonstrating concurrent order management with capacity constraints and real-time notifications. Built with Java socket programming, this system simulates a virtual café where multiple customers can simultaneously place orders for tea and coffee while the server/barista manages brewing capacity and order completion.

### Server -> Barista.java
### Client -> Customer.java

---

## Table of Contents

- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Compilation](#compilation)
  - [Running the Application](#running-the-application)
- [Usage Guide](#usage-guide)
- [Features](#features)
- [Known Limitations](#known-limitations)
- [Reflection](#reflection)

---

## Project Structure
Ensure your terminal is opened in the project root directory (the `code` folder). Your directory structure should look like this:
```
.
├── Barista.java              # Server entry point
├── Customer.java             # Client application
└── helpers/barista/
    ├── VirtualCafe.java      # Core business logic and state management
    ├── CustomerHandler.java  # Client connection handler (Runnable)
    ├── Order.java            # Customer order entity
    └── OrderItem.java        # Individual item representation
```

---

## Getting Started

### Prerequisites

- **Java Development Kit (JDK)**: Version 8 or higher
- **Terminal/Command Prompt**: Access to command-line interface

### Compilation

Navigate to the project root directory (the folder containing `Barista.java` and `Customer.java`).

#### macOS / Linux
```bash
# Compile server and helper classes
javac -d . Barista.java helpers/barista/*.java

# Compile client
javac Customer.java
```

#### Windows
```cmd
# Compile server and helper classes
javac -d . Barista.java helpers\barista\*.java

# Compile client
javac Customer.java
```

> **Note**: Ensure your terminal is in the project root directory before compiling. The `-d .` flag ensures compiled classes are placed in the correct package structure.

### Running the Application

#### Step 1: Start the Server

Open a terminal and run:

**macOS / Linux / Windows:**
```bash
java Barista
```

You should see:
```
✓ Virtual Cafe Server Started
Barista is waiting for customers to join the Virtual cafe...
```

The server listens on **port 8888**.

#### Step 2: Connect Client(s)

Open one or more **separate terminals** and run:

**macOS / Linux / Windows:**
```bash
java Customer
```

Each client will be prompted to enter a name upon connection. Multiple clients can connect simultaneously.

---

## Usage Guide

Once connected to the Virtual Café, use the following commands:
1. **order <quantity> <tea/coffee> [and <quantity> <tea/coffee>]")**: ordering coffee/tea or both
    Examples: 
        order 1 tea.
        order 2 coffees.
        orde 1 tea and 1 coffee.
        order 1 coffe and 1 tea.
2. **order status**: returns the status of the customer order, how many items in waiting ,brewing and finish(tray) state
3. **collect**: collect and removes items
4. **exit**: exits the program safly, still u can use ctrl-c for exciting but handled differently and as emergency exit


### Order Flow

1. **Place Order**: Items are added to the waiting area
2. **Automatic Brewing**: Server moves items to brewing area when capacity is available
3. **Completion Notification**: Server notifies you when your order is ready
4. **Collection**: Use `collect` command to retrieve your completed order

---

## Notable Features

### Customer's order tracking in a class(Order.java)
- Important since it formed a class to store and check at any time how many teas or coffees are in what state for efficent status lookups

### Core Functionality

- **Multi-Client Support**: Handles multiple concurrent customer connections using separate threads per client
- **Socket-Based Communication**: TCP/IP socket programming for client-server interaction
- **Asynchronous Notifications**: Background listener thread continuously monitors server messages for order completion alerts

### Brewing System

- **Capacity Constraints**: Maximum 2 teas and 2 coffees can brew simultaneously
- **Realistic Timing**: 30-second brewing time for tea, 45 seconds for coffee
- **Thread-Per-Item**: Each item brews in its own thread, simulating concurrent preparation

### Thread Safety

- **Coarse-Grained Synchronization**: All VirtualCafe methods use synchronized keyword to prevent race conditions
- **Atomic Operations**: Customer order state updates are consistent across concurrent access

### Data Management

- **Three-Area System**: Orders flow through distinct areas:
  - **Waiting Area** (Queue): FIFO processing of pending items
  - **Brewing Area** (ArrayList): Items currently being prepared
  - **Tray Area** (Map<String, List>): Completed items organized by customer

- **OrderItem.java class**: Helping to store efficently in the above data strucutres the user and a specific item, 
    seperating in such way singe items instead of whole orders

- **Dual-Tracking Approach**: 
  - Order objects maintain counters for O(1) status checks
  - VirtualCafe maintains actual data structures for area management
  - OrderItem class pairs customer names with item types for efficient tracking

### Logging

- **Real-Time Console Output**: Live state updates showing all area contents
- **JSON Logging**: Timestamped snapshots written to `VirtualCafe_logs.json`

---

## Known Limitations

- **FIFO Blocking**: Items are processed strictly first-in-first-out from the waiting area, which may cause head-of-line blocking when early items cannot brew due to capacity constraints

- **No Order Cancellation**: Once placed, orders cannot be cancelled or modified

- **Resource Redistribution**: When a customer disconnects, their items (whether waiting, brewing, or ready) are removed rather than redistributed to other customers

- **Server Shutdown**: Server requires manual termination (Ctrl+C) with no graceful shutdown mechanism

- **Unbounded Log Growth**: The JSON log file grows indefinitely without rotation or cleanup

---

## Reflection

This assignment provided hands-on experience with fundamental concepts in distributed systems and concurrent programming, specifically focusing on client-server architecture, socket I/O, and thread management. The bank example helped my a lot to create such helper classes structure for the server which seperates concerns and make code cleaner.

Overall these project helped my 60% more than just reading slides since i had to actually research the slides and create the architecture, constractivism learning boost my undestanding and i feel confident i can build more cleint-server applications


---

**Author**: Konstantinos Patatas(2313198)
**Course**: CE303 Advanced Programming.