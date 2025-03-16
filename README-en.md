# StateEasy Project Overview
[切换语言：中文](README.md)

StateEasy is a collection of toolkits focused on reactive programming and persistent data state management. It comprises three main modules: `StateEasy Persistent`, `StateEasy Concurrent`, and `StateEasy Index Logging`, designed to offer developers a comprehensive and efficient solution for handling asynchronous task scheduling, state management, and logging.

## Module Introduction

### StateEasy Concurrent - Asynchronous Task Scheduling Toolkit

[See detailed usage instructions here](docs/concurrent-en.md)

- **Overview**: Provides a complete asynchronous task execution framework with a reactive programming model and rich APIs to simplify the management of concurrent operations.
- **Key Features**:
    - A flexible asynchronous event handling mechanism based on `EventStage`.
    - Supports asynchronous task execution in single-threaded or multi-threaded environments.
    - Timeout control ensures tasks are completed or canceled in a timely manner.
    - A simple and user-friendly API design facilitates rapid development.

### StateEasy Index Logging - Efficient Message Logging System

[See detailed usage instructions here](docs/index-logging-en.md)

- **Overview**: A lightweight, high-performance log reading and writing component built using mmap technology, suitable for various applications such as WAL (Write-Ahead Logging) and message queues.
- **Key Features**:
    - Purely asynchronous message appending and indexed reading capabilities.
    - Easy-to-extend message types through the implementation of the `Serializer` interface.
    - Low-latency log access speeds and powerful configurability.

### StateEasy Persistent - Reactive State Management and Persistence Tool

[See detailed usage instructions here](docs/persistent-en.md)

- **Overview**: Offers an asynchronous reactive state management and persistence mechanism, supporting customizable state update logic and persistence strategies.
- **Key Features**:
    - Asynchronous reactive design ensures performance in high-concurrency scenarios.
    - Customizable state and event handling interfaces (`StateDef
