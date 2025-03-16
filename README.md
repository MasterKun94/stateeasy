# StateEasy 项目简介

StateEasy 是一个专注于响应式编程和数据状态管理持久化的工具库集合。它包括了三个主要模块：
`StateEasy Persistent`、`StateEasy Concurrent` 和 `StateEasy Index Logging`
，旨在为开发者提供一套完整且高效的解决方案来处理异步任务调度、状态管理和日志记录等需求。

## 模块介绍

### StateEasy Concurrent - 异步任务调度工具类库

[点击查看详细使用说明](docs/concurrent.md)

- **功能概述**：提供了一套完整的异步任务执行框架，包含响应式编程模型以及丰富的API以简化并发操作的管理。
- **核心特性**：
    - 基于`EventStage`的灵活异步事件处理机制。
    - 支持单线程或多线程环境下的异步任务执行。
    - 超时控制功能保证任务及时完成或取消。
    - 简洁易用的API设计促进快速开发。

### StateEasy Index Logging - 高效消息日志系统

[点击查看详细使用说明](docs/index-logging.md)

- **功能概述**：基于mmap技术构建的轻量级高性能日志读写组件，适用于多种应用场景如WAL（Write-Ahead
  Logging）、消息队列等。
- **核心特性**：
    - 纯异步的消息追加写入与索引读取能力。
    - 易于扩展的消息类型支持通过实现`Serializer`接口达成。
    - 低延迟的日志访问速度与强大的可配置性。

### StateEasy Persistent - 响应式状态管理和持久化工具

[点击查看详细使用说明](docs/persistent.md)

- **功能概述**：提供了异步响应式的状态管理和持久化机制，支持自定义的状态更新逻辑及持久化策略。
- **核心特性**：
    - 异步响应式设计确保高并发场景下的性能。
    - 可定制的状态与事件处理接口 (`StateDef` 和 `EventSourceStateDef`)。
    - 自动化状态持久化至本地存储。
    - 事件源集成，便于管理和回放事件流。

