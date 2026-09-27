# Discord as a Distributed Computing System

> A study guide to Discord's **Architecture**, **Distributed Computing Algorithms/Mechanisms**, and **Distributed Computing Paradigms**.
>
> **Primary evidence:** Discord's official Engineering & Developers blog and official Developer Documentation.
>
> **Important scope note:** Discord does not publish one complete internal architecture specification. This document reconstructs the publicly documented architecture from Discord's engineering publications. Where Discord explicitly documents a mechanism, it is marked **[Discord-documented]**. Where a concept is a useful distributed-computing interpretation or a prerequisite to understand Discord, it is marked **[DC concept]** rather than being claimed as a Discord implementation.

---

# Table of Contents

- [0. How to use this document](#0-how-to-use-this-document)
- [Part I — Discord Architecture](#part-i--discord-architecture)
  - [1. Global architecture](#1-global-architecture)
  - [2. Client layer](#2-client-layer)
  - [3. API layer](#3-api-layer)
  - [4. Gateway and real-time architecture](#4-gateway-and-real-time-architecture)
  - [5. Sessions and guild processes](#5-sessions-and-guild-processes)
  - [6. Message flow](#6-message-flow)
  - [7. Voice and video architecture](#7-voice-and-video-architecture)
  - [8. Distributed database architecture](#8-distributed-database-architecture)
  - [9. Search architecture](#9-search-architecture)
  - [10. Data processing and analytics](#10-data-processing-and-analytics)
  - [11. Distributed ML architecture](#11-distributed-ml-architecture)
  - [12. Kubernetes and infrastructure](#12-kubernetes-and-infrastructure)
  - [13. Observability and distributed tracing](#13-observability-and-distributed-tracing)
  - [14. Control planes and operational architecture](#14-control-planes-and-operational-architecture)
  - [15. End-to-end architecture summary](#15-end-to-end-architecture-summary)
- [Part II — Distributed Computing Algorithms and Mechanisms](#part-ii--distributed-computing-algorithms-and-mechanisms)
  - [16. Partitioning and sharding](#16-partitioning-and-sharding)
  - [17. Hash rings and consistent hashing](#17-hash-rings-and-consistent-hashing)
  - [18. Replication](#18-replication)
  - [19. Quorum and consistency](#19-quorum-and-consistency)
  - [20. Fault tolerance and failure handling](#20-fault-tolerance-and-failure-handling)
  - [21. Load shedding and overload control](#21-load-shedding-and-overload-control)
  - [22. Fan-out and parallel message distribution](#22-fan-out-and-parallel-message-distribution)
  - [23. Message queues and asynchronous work](#23-message-queues-and-asynchronous-work)
  - [24. Distributed routing and destination grouping](#24-distributed-routing-and-destination-grouping)
  - [25. DAG scheduling and dependency execution](#25-dag-scheduling-and-dependency-execution)
  - [26. Distributed task scheduling](#26-distributed-task-scheduling)
  - [27. Caching](#27-caching)
  - [28. Retry storms and backpressure](#28-retry-storms-and-backpressure)
  - [29. Dual writes and online migration](#29-dual-writes-and-online-migration)
  - [30. Cell architecture and blast-radius reduction](#30-cell-architecture-and-blast-radius-reduction)
  - [31. Leader election, consensus, and gossip — what not to claim](#31-leader-election-consensus-and-gossip--what-not-to-claim)
  - [32. Distributed algorithms study map](#32-distributed-algorithms-study-map)
- [Part III — Distributed Computing Paradigms](#part-iii--distributed-computing-paradigms)
  - [33. Client-server paradigm](#33-client-server-paradigm)
  - [34. Service-oriented / distributed-service paradigm](#34-service-oriented--distributed-service-paradigm)
  - [35. Actor/process-oriented concurrency](#35-actorprocess-oriented-concurrency)
  - [36. Message-passing paradigm](#36-message-passing-paradigm)
  - [37. Event-driven architecture](#37-event-driven-architecture)
  - [38. Asynchronous programming](#38-asynchronous-programming)
  - [39. Publish-subscribe paradigm](#39-publish-subscribe-paradigm)
  - [40. Dataflow and DAG paradigm](#40-dataflow-and-dag-paradigm)
  - [41. Queue-based asynchronous processing](#41-queue-based-asynchronous-processing)
  - [42. Edge computing paradigm](#42-edge-computing-paradigm)
  - [43. Cell-based architecture](#43-cell-based-architecture)
  - [44. Distributed database paradigm](#44-distributed-database-paradigm)
  - [45. Distributed ML paradigm](#45-distributed-ml-paradigm)
  - [46. Control-plane / data-plane separation](#46-control-plane--data-plane-separation)
  - [47. Polyglot distributed systems](#47-polyglot-distributed-systems)
- [48. Important concepts to learn alongside Discord](#48-important-concepts-to-learn-alongside-discord)
- [49. Recommended learning order](#49-recommended-learning-order)
- [50. Official Discord source library](#50-official-discord-source-library)
- [51. Final mental model](#51-final-mental-model)

---

# 0. How to use this document

This is intended to be a **learning document**, not merely a list of links.

For every topic, ask four questions:

1. **What problem does the distributed system have?**
2. **How does Discord's published architecture address it?**
3. **What distributed-computing algorithm or mechanism is involved?**
4. **What programming/system paradigm does the mechanism represent?**

For example:

```text
Problem:
Millions of users need real-time updates.

        ↓

Discord architecture:
Gateway + sessions + guild processes

        ↓

Distributed mechanism:
message passing + fan-out + partitioning

        ↓

Paradigm:
event-driven + actor/process-oriented + asynchronous
```

Another example:

```text
Problem:
A database node or entire availability zone can fail.

        ↓

Architecture:
multi-zone ScyllaDB cluster

        ↓

Mechanisms:
replication + quorum reads + failure handling

        ↓

Distributed-systems concepts:
fault tolerance + consistency + availability
```

---

# Part I — Discord Architecture

# 1. Global architecture

Discord is a large distributed real-time application. Its public engineering material shows several major subsystems rather than one single server.

A useful conceptual model is:

```text
                         DISCORD CLIENTS
             ┌──────────────┬───────────────┐
             │              │               │
           Web           Desktop          Mobile
             │              │               │
             └──────────────┼───────────────┘
                            │
                 HTTP / WebSocket / Voice
                            │
             ┌──────────────┴──────────────┐
             │                             │
             ▼                             ▼
        API SERVICES                    GATEWAY
             │                             │
             │                    WebSocket sessions
             │                             │
             │                    ┌────────┴────────┐
             │                    ▼                 ▼
             │                 Guilds           Sessions
             │                    │                 │
             └──────────────┬─────┴─────────────────┘
                            │
                            ▼
                   Distributed data layer
             ┌──────────────┼───────────────┐
             ▼              ▼               ▼
          ScyllaDB      Elasticsearch      Redis/
          clusters        clusters         PubSub
             │              │               │
             └──────────────┼───────────────┘
                            │
                            ▼
                  Data / analytics systems
                            │
                            ▼
                  Distributed ML systems

                 VOICE / VIDEO SUBSYSTEM
                            │
                            ▼
                       Voice servers
                            │
                            ▼
                       Edge network
```

This is a **conceptual synthesis** of Discord's public engineering material, not an official single architecture diagram.

Discord's older voice architecture describes Gateway, Guilds, and Voice as major backend services, with signaling written in Elixir. Its newer voice architecture has moved much of voice/video traffic onto Cloudflare's global edge network. [Discord-documented]

Official sources:

- https://discord.com/blog/how-discord-handles-two-and-half-million-concurrent-voice-users-using-webrtc
- https://discord.com/blog/how-we-moved-discord-voice-to-the-edge
- https://discord.com/blog/behind-the-scenes-of-the-3-25-26-voice-outage

---

# 2. Client layer

Discord supports multiple clients:

```text
Web
Desktop
Android
iOS
Linux
Windows
macOS
```

The client is not simply a traditional request/response web application.

It has two important communication styles:

```text
                Discord Client
                     │
          ┌──────────┴──────────┐
          │                     │
          ▼                     ▼
       HTTP/API             WebSocket
          │                     │
     request/response       real-time events
```

Voice/video adds another communication path.

The important distributed-computing idea is that a client maintains a long-lived relationship with the real-time backend rather than repeatedly polling the server for every update.

---

# 3. API layer

The API handles ordinary application operations.

Examples include:

- authentication
- sending messages
- retrieving data
- modifying server/channel state
- interacting with user settings
- persistence operations

A simplified message path is:

```text
Client
  │
  │ HTTP request
  ▼
API service
  │
  ├── authenticate
  │
  ├── validate request
  │
  ├── persist message
  │
  └── dispatch event
          │
          ▼
       Elixir stack
```

Discord's 2026 tracing article documents a message path in which a client sends a message over HTTP to the API service, the API records it in the database, and then sends it to the Elixir stack over gRPC. [Discord-documented]

Source:

https://discord.com/blog/tracing-discords-elixir-systems-without-melting-everything

---

# 4. Gateway and real-time architecture

The Gateway is central to Discord's real-time behavior.

Discord's 2026 voice-outage analysis describes the Gateway as the ingress and egress point for WebSocket traffic. A client connects to the Gateway, obtains information through the sessions service, and keeps the socket open while the Gateway dispatches messages to the client. [Discord-documented]

Conceptually:

```text
                    Client
                      │
                 WebSocket
                      │
                      ▼
                  Gateway
                      │
             ┌────────┴────────┐
             │                 │
             ▼                 ▼
          Sessions           Events
             │                 │
             └────────┬────────┘
                      ▼
                   Client
```

The Gateway therefore provides:

- persistent WebSocket connections
- event delivery
- session connectivity
- reconnect/resume behavior
- communication between Discord's distributed real-time backend and clients

Official documentation:

https://discord.com/developers/docs/topics/gateway

---

# 5. Sessions and guild processes

This is one of the most important discoveries in Discord's newer engineering documentation.

Discord's 2026 tracing article states that:

- Elixir programs consist of lightweight processes.
- Processes communicate using message passing.
- Each guild runs as an Elixir process.
- Each session is itself an Elixir process.
- A guild process fans actions out to connected session processes.

Conceptually:

```text
                       API
                        │
                        │ message
                        ▼
                 ┌─────────────┐
                 │ Guild       │
                 │ Process     │
                 └──────┬──────┘
                        │
                message passing
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
     Session P1     Session P2     Session P3
          │             │             │
          ▼             ▼             ▼
       Client A       Client B       Client C
```

This is extremely important for learning the **actor/process model**.

A guild is effectively isolated as a unit of concurrent work.

Source:

https://discord.com/blog/tracing-discords-elixir-systems-without-melting-everything

---

# 6. Message flow

A simplified modern message path is:

```text
User A
  │
  │ HTTP
  ▼
API
  │
  ├──────────────► Database
  │                    │
  │                    ▼
  │                persistent
  │                 message
  │
  │ gRPC
  ▼
Guild process
  │
  │ fan-out
  ├─────────────► Session A
  ├─────────────► Session B
  ├─────────────► Session C
  └─────────────► Session D
                         │
                         ▼
                    WebSocket
                         │
                         ▼
                    User clients
```

This single flow demonstrates many distributed concepts:

- client-server communication
- RPC
- asynchronous processing
- message passing
- fan-out
- process isolation
- persistent storage
- distributed tracing
- real-time event delivery

Discord's tracing article provides a concrete example of this path and shows spans for the API, guilds, and sessions services.

---

# 7. Voice and video architecture

Voice is a different distributed problem from text.

## Historical architecture

Discord described a backend involving:

```text
Gateway
Guilds
Voice
```

When a user joins a voice channel, the system assigns a Discord Voice server to the guild. [Discord-documented]

Source:

https://discord.com/blog/how-discord-handles-two-and-half-million-concurrent-voice-users-using-webrtc

## Modern edge architecture

Discord's June 2026 article says it moved voice/video traffic onto Cloudflare's edge network. The article states that the edge network has more than 300 cities and that more than 80% of Discord voice/video traffic runs there. [Discord-documented]

Conceptually:

```text
                         Discord
                            │
                    Voice assignment
                            │
                            ▼
                    Global edge layer
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
      Edge A              Edge B              Edge C
        │                   │                   │
      Users               Users               Users
```

Why?

Because voice is highly latency-sensitive.

The distributed-computing objective becomes:

```text
minimize network distance
        +
reduce packet loss
        +
increase availability
        +
handle large geographic distribution
```

Source:

https://discord.com/blog/how-we-moved-discord-voice-to-the-edge

---

# 8. Distributed database architecture

Discord has gone through multiple database generations.

A simplified historical evolution is:

```text
MongoDB
   │
   ▼
Cassandra
   │
   ▼
ScyllaDB
```

Discord's 2023 article explains that it originally moved from MongoDB to Cassandra because it needed a scalable and fault-tolerant database, but eventually migrated message storage to ScyllaDB. [Discord-documented]

Source:

https://discord.com/blog/how-discord-stores-trillions-of-messages

Discord's 2022 network-disk article describes ScyllaDB clusters as sources of truth for their respective datasets and describes very high request rates and large data volumes. [Discord-documented]

Source:

https://discord.com/blog/how-discord-supercharges-network-disks-for-extreme-low-latency

## Conceptual topology

```text
                     Application
                          │
                    Data service
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
          Scylla node              Scylla node
              │                       │
              └───────────┬───────────┘
                          │
                      Scylla node
```

Discord's authentication cluster is replicated across three availability zones. It uses a replication factor spread across zones and quorum consistency reads. [Discord-documented]

Source:

https://discord.com/blog/authentication-outage

---

# 9. Search architecture

Discord's message search is itself a distributed system.

The 2025 search architecture article describes:

- Elasticsearch
- sharded indices
- message queues
- workers
- PubSub
- multiple Elasticsearch clusters
- logical cells
- Kubernetes
- master/ingest/data node roles
- cross-zone resilience

Source:

https://discord.com/blog/how-discord-indexes-trillions-of-messages

## Legacy conceptual architecture

```text
New message
    │
    ▼
Redis queue
    │
    ▼
Index workers
    │
    ▼
Elasticsearch
    │
    ▼
Sharded indices
```

## Modern architecture

```text
Message
   │
   ▼
PubSub
   │
   ▼
Message Router
   │
   ├── Destination A ──► Cluster A ──► Index
   ├── Destination B ──► Cluster B ──► Index
   └── Destination C ──► Cluster C ──► Index
```

Discord introduced smaller Elasticsearch clusters grouped into logical **cells**.

A cell can contain several clusters:

```text
                   Cell
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
     Cluster A   Cluster B   Cluster C
        │           │           │
      indices     indices     indices
```

This reduces the coordination overhead and blast radius of very large clusters.

---

# 10. Data processing and analytics

Discord has large-scale data pipelines.

Earlier Discord engineering work described a system called **Derived**, which modeled dependencies between datasets as a Directed Acyclic Graph.

Conceptually:

```text
Raw data
   │
   ├────────► Table A
   │             │
   │             ▼
   └────────► Table B
                 │
                 ▼
              Table C
                 │
                 ▼
              Table D
```

If A and B are independent, they can be computed concurrently.

```text
           Raw data
          /        \
         /          \
        ▼            ▼
    Compute A    Compute B
        \            /
         \          /
          ▼        ▼
            Compute C
```

This is a distributed **DAG execution** problem.

Discord later published its work scaling dbt to petabytes of data and thousands of models.

Source:

https://discord.com/blog/overclocking-dbt-discords-custom-solution-in-processing-petabytes-of-data

---

# 11. Distributed ML architecture

Discord now has an explicit distributed-computing platform for machine learning.

The 2025 article describes a platform built around:

```text
Dagster
   │
   ▼
KubeRay
   │
   ▼
Ray
   │
   ▼
Multi-GPU cluster
```

The workflow is:

```text
Engineer
   │
   ▼
Dagster
   │
   │ job specification
   ▼
Ray Job Operator
   │
   ▼
KubeRay
   │
   ▼
Kubernetes
   │
   ├── GPU node
   ├── GPU node
   ├── GPU node
   └── GPU node
          │
          ▼
         Ray
          │
          ▼
 distributed training / inference
```

Discord describes:

- Ray for distributed computing
- Dagster for workflows
- KubeRay for dynamically provisioning Ray clusters
- Kubernetes GPU node pools
- centralized observability using X-Ray
- sharded neural networks
- multi-GPU training

Source:

https://discord.com/blog/from-single-node-to-multi-gpu-clusters-how-discord-made-distributed-compute-easy-for-ml-engineers

This is a separate and very useful distributed-computing case study inside Discord.

---

# 12. Kubernetes and infrastructure

Discord increasingly uses Kubernetes for stateless services and infrastructure orchestration.

Examples from public engineering material include:

```text
Kubernetes
   │
   ├── stateless services
   ├── Elasticsearch clusters
   ├── Ray clusters
   ├── service deployments
   └── infrastructure automation
```

The 2025 Elasticsearch architecture uses Kubernetes plus the Elasticsearch Kubernetes Operator.

The 2025 distributed-ML platform uses Kubernetes + KubeRay.

The 2026 voice-outage article describes an ongoing Kubernetes migration for Elixir services.

Sources:

- https://discord.com/blog/how-discord-indexes-trillions-of-messages
- https://discord.com/blog/from-single-node-to-multi-gpu-clusters-how-discord-made-distributed-compute-easy-for-ml-engineers
- https://discord.com/blog/behind-the-scenes-of-the-3-25-26-voice-outage

---

# 13. Observability and distributed tracing

A distributed system is difficult to debug because one user operation may cross many processes and machines.

Discord's 2026 tracing article describes its use of distributed tracing and OpenTelemetry.

Example:

```text
Trace
 │
 ├── API span
 │      │
 │      └── dispatch_message
 │
 ├── Guild span
 │      │
 │      └── fan-out
 │
 ├── Session A span
 ├── Session B span
 └── Session C span
```

The important concept is **trace context propagation**.

```text
Service A
   │
   │ trace context
   ▼
Service B
   │
   │ trace context
   ▼
Service C
```

Discord's Elixir processes communicate using arbitrary messages, so ordinary HTTP-header propagation does not directly solve tracing between Elixir processes. Discord built an internal `Transport` abstraction using an `Envelope` to carry metadata such as trace context.

Source:

https://discord.com/blog/tracing-discords-elixir-systems-without-melting-everything

Distributed tracing lets engineers answer:

- Where did latency occur?
- Which service caused it?
- Which downstream services were affected?
- Which operations were sequential?
- Which operations ran concurrently?
- How long did fan-out take?

---

# 14. Control planes and operational architecture

Discord's 2026 ScyllaDB article describes a **Scylla Control Plane (SCP)**.

The persistence team operates:

- ScyllaDB
- Elasticsearch
- PostgreSQL
- many clusters
- hundreds of database nodes

Manual scripts became difficult to operate safely.

The control-plane idea is:

```text
                 Control Plane
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
   Provisioning     Validation      Upgrades
        │              │              │
        └──────────────┼──────────────┘
                       ▼
                 Database clusters
```

This is important because distributed systems need not only a **data plane** but also mechanisms to safely manage the distributed infrastructure itself.

Source:

https://discord.com/blog/how-discord-automates-scylladb-clusters-at-scale

---

# 15. End-to-end architecture summary

A broad conceptual model is:

```text
                         USERS
                           │
           ┌───────────────┼────────────────┐
           │               │                │
           ▼               ▼                ▼
        HTTP/API       WebSocket         Voice/Video
           │               │                │
           ▼               ▼                ▼
       API services      Gateway           Edge
           │               │                │
           │          ┌────┴────┐            │
           │          ▼         ▼            │
           │       Guilds    Sessions        │
           │          │         │            │
           │          └────┬────┘            │
           │               │                 │
           └───────────────┼─────────────────┘
                           │
                           ▼
                 Distributed data layer
            ┌──────────────┼──────────────┐
            ▼              ▼              ▼
         ScyllaDB      Elasticsearch    PubSub
            │              │              │
            ▼              ▼              ▼
       Persistent       Search         Async work
          state          state
                           │
                           ▼
                  Data / ML platforms
                           │
                 ┌─────────┴─────────┐
                 ▼                   ▼
               DAG                 Ray
             pipelines          multi-GPU
```

---

# Part II — Distributed Computing Algorithms and Mechanisms

# 16. Partitioning and sharding

**Definition**

Partitioning means dividing a large dataset or workload into smaller pieces.

```text
Dataset
  │
  ├── Partition 1
  ├── Partition 2
  ├── Partition 3
  └── Partition 4
```

Sharding is a practical form of partitioning in which different shards are handled by different machines or logical resources.

Discord uses sharding in multiple contexts.

Examples:

- database data
- Elasticsearch indices
- guild/message search data
- large-guild search
- ML data/model workloads

[Discord-documented]

Why partition?

Without partitioning:

```text
100 TB
  │
  ▼
one machine
  │
bottleneck
```

With partitioning:

```text
100 TB
 │
 ├── 25 TB ── Node A
 ├── 25 TB ── Node B
 ├── 25 TB ── Node C
 └── 25 TB ── Node D
```

Benefits:

- horizontal scalability
- parallel access
- smaller failure domains
- workload isolation

Costs:

- routing complexity
- cross-shard queries
- rebalancing
- coordination

---

# 17. Hash rings and consistent hashing

Discord's authentication outage document explains that Scylla replicates data around the cluster via a hash ring. [Discord-documented]

A simplified hash ring:

```text
             hash space

                0
          ┌────────────┐
       ┌──┘            └──┐
      │                    │
  Node A                  Node B
      │                    │
      │                    │
  Node D                  Node C
       └──┐            ┌──┘
          └────────────┘
               max
```

A key is hashed:

```text
key = user_id
       │
       ▼
     hash()
       │
       ▼
 position on ring
       │
       ▼
 responsible node(s)
```

With replication:

```text
Key K
 │
 ▼
Primary position
 │
 ├── replica 1
 └── replica 2
```

## Why consistent hashing matters

When a node is added or removed, ideally only part of the keyspace moves.

Without consistent hashing:

```text
N nodes → add node → remap huge amount of data
```

With consistent hashing:

```text
N nodes → add node → move affected ring ranges
```

**Important:** Discord's public document confirms the use of a hash ring in its ScyllaDB infrastructure; it does not mean Discord independently implemented a textbook consistent-hashing algorithm.

---

# 18. Replication

Replication means maintaining multiple copies of data.

```text
                 Data
                  │
          ┌───────┼───────┐
          ▼       ▼       ▼
        Copy 1  Copy 2  Copy 3
```

Discord's authentication cluster is replicated across three cloud availability zones.

```text
             Authentication data
                     │
       ┌─────────────┼─────────────┐
       ▼             ▼             ▼
    Zone A         Zone B         Zone C
       │             │             │
     Node(s)       Node(s)       Node(s)
```

Why replicate?

- availability
- failure tolerance
- redundancy
- geographic/zone resilience

But replication introduces problems:

- stale data
- consistency
- write coordination
- network partitions
- repair/reconciliation

---

# 19. Quorum and consistency

A quorum is a sufficient subset of replicas required to make a decision.

Suppose there are 3 replicas:

```text
        R1
        │
        ├── read
        │
        R2 ── read
        │
        R3 ── read
```

A majority quorum is:

```text
N = 3
quorum = 2
```

Discord explicitly documents quorum consistency reads for its ScyllaDB authentication cluster.

Why?

If one zone fails:

```text
Zone A ✓
Zone B ✓
Zone C ✗

2/3 replicas available
       │
       ▼
quorum
       │
       ▼
service can continue
```

But if two zones become unavailable:

```text
Zone A ✗
Zone B ✗
Zone C ✓

1/3
 │
 ▼
no quorum
```

This illustrates the relationship:

```text
Replication
      +
Quorum
      +
Failure tolerance
      =
High availability under expected failures
```

Source:

https://discord.com/blog/authentication-outage

---

# 20. Fault tolerance and failure handling

Distributed systems assume failures happen.

Discord's public incident reports are valuable because they show failures in real systems.

Types of failures documented by Discord include:

- degraded disks
- zone outages
- overloaded database nodes
- retry storms
- queue backlogs
- Elasticsearch node failures
- session losses
- cascading load
- cluster coordination problems

The 2023 authentication incident is especially useful.

A simplified failure chain:

```text
Zone B offline
      │
      ▼
Reduced capacity
      │
      +
Degraded disk in another zone
      │
      ▼
Database overload
      │
      ▼
High latency / timeouts
      │
      ▼
Application retries
      │
      ▼
Retry storm
      │
      ▼
More database load
      │
      ▼
Availability collapse
```

This is an excellent real-world example of a **cascading failure**.

---

# 21. Load shedding and overload control

When a distributed service becomes overloaded, doing more work can make the problem worse.

Discord's authentication incident describes intentionally disabling lower-priority behavior such as:

- typing events
- marking messages as read

This reduced load and partially restored functionality.

Conceptually:

```text
System overloaded
       │
       ▼
Identify non-critical work
       │
       ├── disable typing events
       ├── reduce optional work
       └── preserve authentication
                │
                ▼
          lower load
                │
                ▼
          partial recovery
```

This is **load shedding**.

It is a fundamental distributed-systems reliability technique.

---

# 22. Fan-out and parallel message distribution

Suppose one message needs to reach 100,000 connected clients.

Naively:

```text
API
 │
 ├── Client 1
 ├── Client 2
 ├── Client 3
 ...
 └── Client 100000
```

That is difficult for one process.

Discord's Elixir architecture introduces a useful hierarchy:

```text
API
 │
 ▼
Guild process
 │
 ├── Session process
 ├── Session process
 ├── Session process
 └── ...
```

This creates a **fan-out tree**.

The tracing article describes a guild process fanning actions out to session processes.

This gives:

- parallelism
- isolation
- concurrency
- reduced centralized coordination

---

# 23. Message queues and asynchronous work

Discord's search infrastructure demonstrates queue-based asynchronous processing.

Modern conceptual flow:

```text
Message
   │
   ▼
PubSub
   │
   ▼
Worker / Router
   │
   ▼
Destination
   │
   ▼
Elasticsearch
```

The queue separates producers from consumers.

Producer:

```text
API → PubSub
```

Consumer:

```text
PubSub → indexing worker
```

Benefits:

- buffering
- asynchronous processing
- decoupling
- handling traffic spikes
- retry/recovery
- independent scaling

Discord moved message indexing from Redis to PubSub because PubSub provided guaranteed message delivery and tolerated large backlogs better.

[Discord-documented]

Source:

https://discord.com/blog/how-discord-indexes-trillions-of-messages

---

# 24. Distributed routing and destination grouping

Discord's modern search architecture contains an especially good distributed routing example.

A message has a destination:

```text
Destination =
    Elasticsearch cluster + index
```

The router groups messages by destination.

```text
Input messages

M1 → Destination A
M2 → Destination B
M3 → Destination A
M4 → Destination C
M5 → Destination A

             │
             ▼

Destination A: M1 M3 M5
Destination B: M2
Destination C: M4
```

Then separate tasks can process each destination.

This avoids a bulk operation touching many unrelated Elasticsearch nodes.

This is a form of **key-based routing** and **work partitioning**.

Source:

https://discord.com/blog/how-discord-indexes-trillions-of-messages

---

# 25. DAG scheduling and dependency execution

A DAG represents dependencies without cycles.

```text
A ─────► C ─────► E
          ▲
          │
B ────────┘
```

A and B can run concurrently:

```text
Time →

A: ███████
B: ███████
C:       █████
E:            █████
```

A scheduler determines which tasks are ready.

Algorithmically:

```text
1. Construct dependency graph.
2. Find nodes with no unfinished dependencies.
3. Schedule ready nodes.
4. Mark completed nodes.
5. Release newly-ready nodes.
6. Repeat until all nodes finish.
```

This is topological-order scheduling plus parallel execution.

Discord's data systems use DAG-oriented workflows.

---

# 26. Distributed task scheduling

Discord's distributed ML platform provides another example.

```text
Dagster
   │
   ▼
job definition
   │
   ▼
KubeRay
   │
   ▼
Ray cluster
   │
   ├── Worker 1
   ├── Worker 2
   ├── Worker 3
   └── Worker 4
```

The scheduler must decide:

- where resources are available
- how many GPUs are required
- which worker nodes participate
- when a job starts
- when resources are released

This is a distributed resource-allocation problem.

Source:

https://discord.com/blog/from-single-node-to-multi-gpu-clusters-how-discord-made-distributed-compute-easy-for-ml-engineers

---

# 27. Caching

Discord uses caches in several systems.

Example:

```text
Application
    │
    ▼
 Cache
  │  │
  │  └── hit → return
  │
  └── miss
       │
       ▼
    Database
```

The Read States service described by Discord used an LRU cache backed by Cassandra.

```text
Read State
   │
   ▼
LRU cache
   │
   ├── hit → fast response
   │
   └── miss → database
```

Caching reduces:

- database traffic
- latency
- repeated computation

But caching introduces:

- stale data
- invalidation
- memory pressure
- cache stampedes
- inconsistent views

Discord's authentication incident also demonstrates why cache state matters during node restarts.

---

# 28. Retry storms and backpressure

Retries can be dangerous.

Suppose:

```text
1000 requests
   │
   ▼
Service overloaded
   │
   ▼
500 fail
   │
   ▼
500 retries
   │
   ▼
Service becomes more overloaded
   │
   ▼
more failures
```

This is a **retry storm**.

Discord explicitly identified retry behavior as one factor that worsened the 2023 authentication outage.

A better conceptual strategy includes:

```text
failure
  │
  ├── bounded retries
  ├── exponential backoff
  ├── jitter
  ├── circuit breaking
  └── load shedding
```

**Important:** The list above is general distributed-systems practice. Discord's public incident specifically documents the retry storm; do not assume Discord uses every mitigation listed here unless its documentation says so.

---

# 29. Dual writes and online migration

Discord's search migration describes a powerful distributed migration technique.

During migration:

```text
                  New message
                       │
              ┌────────┴────────┐
              ▼                 ▼
          Old index          New index
```

Historical data is copied:

```text
Old historical data
        │
        ▼
New index
```

Queries initially use the old index:

```text
Queries → old index
```

After the new index is complete:

```text
Queries → new index
```

Then the old index can be retired.

This is useful because the service remains operational while data is migrated.

The migration sequence documented for very large guilds includes:

1. Identify the large guild.
2. Create a new index with more primary shards.
3. Dual-index new messages.
4. Backfill historical messages.
5. Switch queries to the new index.
6. Stop indexing the old index.
7. Clean up old data.

[Discord-documented]

Source:

https://discord.com/blog/how-discord-indexes-trillions-of-messages

---

# 30. Cell architecture and blast-radius reduction

A **cell** is a logical grouping of smaller independent clusters.

Instead of:

```text
One enormous cluster
─────────────────────
200+ nodes
```

Discord's newer search architecture uses:

```text
                  Search Cell
        ┌────────────┼────────────┐
        ▼            ▼            ▼
    Cluster A    Cluster B    Cluster C
```

Why?

A huge cluster creates:

- large coordination state
- larger failure domain
- more complicated upgrades
- more fan-out
- more operational complexity

Smaller cells provide:

- isolation
- easier scaling
- smaller failure domains
- independent resource allocation
- more manageable upgrades

This is a major distributed-architecture principle:

> **Scale by adding independent units rather than making one unit indefinitely larger.**

---

# 31. Leader election, consensus, and gossip — what not to claim

Distributed-computing courses commonly cover:

- Paxos
- Raft
- leader election
- gossip
- distributed locks
- Byzantine fault tolerance

These should be studied, but **do not automatically label them as Discord algorithms**.

For example:

```text
Discord uses ScyllaDB
        │
        ▼
Scylla internally has distributed coordination
        │
        X
        │
        └── does NOT prove:
             "Discord uses Raft"
```

Likewise:

```text
Discord has replication
        │
        X
        │
        └── does not prove:
             "Discord uses Paxos"
```

Use precise wording:

> "Discord documents quorum reads and replicated ScyllaDB clusters."

Not:

> "Discord uses Paxos."

unless Discord explicitly documents that.

---

# 32. Distributed algorithms study map

Use this map to connect Discord to your Distributed Computing course.

| Concept | Discord connection | Status |
|---|---|---|
| Partitioning | ScyllaDB / Elasticsearch | Discord-documented |
| Sharding | Search, data, ML | Discord-documented |
| Hash ring | ScyllaDB | Discord-documented |
| Replication | Multi-zone databases | Discord-documented |
| Quorum | Authentication | Discord-documented |
| Fan-out | Guild → sessions | Discord-documented |
| Message passing | Elixir processes | Discord-documented |
| Queue processing | PubSub/search | Discord-documented |
| DAG scheduling | Data workflows | Discord-documented |
| Distributed task scheduling | Ray/KubeRay | Discord-documented |
| Load shedding | Authentication incident | Discord-documented |
| Retry storm | Authentication incident | Discord-documented |
| Caching | Read States / infrastructure | Discord-documented |
| Online migration | Search/BFG reindexing | Discord-documented |
| Multi-cluster cells | Search | Discord-documented |
| Consistent hashing | Learn alongside hash rings | DC concept |
| Leader election | Learn for theory | Not established here as Discord implementation |
| Raft | Learn for theory | Not established here as Discord implementation |
| Paxos | Learn for theory | Not established here as Discord implementation |
| Gossip | Learn for theory | Not established here as Discord implementation |
| Distributed locking | Learn for theory | Not established here as Discord implementation |

---

# Part III — Distributed Computing Paradigms

# 33. Client-server paradigm

The simplest model is:

```text
Client
  │
  ▼
Server
```

Discord expands this into many distributed servers:

```text
Client
  │
  ├── API service
  ├── Gateway
  ├── Voice infrastructure
  └── other services
```

The client does not need to know where every backend component runs.

---

# 34. Service-oriented / distributed-service paradigm

Instead of one enormous program:

```text
             Monolith
 ┌─────────────────────────────┐
 │ Auth                        │
 │ Messages                    │
 │ Guilds                      │
 │ Sessions                    │
 │ Voice                       │
 │ Search                      │
 │ Data                        │
 └─────────────────────────────┘
```

Discord's modern backend is composed of specialized services.

Conceptually:

```text
API
 │
 ├── Authentication
 ├── Guilds
 ├── Sessions
 ├── Read States
 ├── Data services
 ├── Search
 └── Voice
```

Different services can use different languages and runtimes.

---

# 35. Actor/process-oriented concurrency

This is one of Discord's strongest paradigms.

Elixir uses lightweight processes.

A process has:

```text
state
+
mailbox
+
behavior
```

Communication:

```text
Process A
    │
    │ message
    ▼
Process B
```

Discord's public tracing article says guilds and sessions are represented as Elixir processes.

This makes the actor/process model directly relevant.

---

# 36. Message-passing paradigm

Instead of sharing memory between components:

```text
Shared memory
 ┌─────────────┐
 │             │
 │ A + B share │
 │ same state  │
 │             │
 └─────────────┘
```

message passing does:

```text
Process A
    │
    │ message
    ▼
Process B
```

The message may contain:

```text
{
    event: "MESSAGE_CREATE",
    channel_id: ...,
    message_id: ...,
    content: ...
}
```

In Discord's Elixir stack, message passing is fundamental.

---

# 37. Event-driven architecture

Discord is fundamentally event-heavy.

Examples:

```text
MESSAGE_CREATE
MESSAGE_UPDATE
MESSAGE_DELETE
PRESENCE_UPDATE
VOICE_STATE_UPDATE
GUILD_CREATE
```

Conceptually:

```text
Event source
    │
    ▼
Event
    │
    ├── consumer A
    ├── consumer B
    └── consumer C
```

This enables loose coupling between producers and consumers.

---

# 38. Asynchronous programming

Asynchronous programming allows a service to handle many operations without blocking one OS thread per operation.

Conceptually:

```text
Request A ────────┐
                  │
Request B ──┐     │
            │     │
Request C ──┴─────┴──► async runtime
                       │
                       ▼
                 completion events
```

Discord's Rust engineering article explicitly discusses asynchronous programming as important for networked services.

Source:

https://discord.com/blog/why-discord-is-switching-from-go-to-rust

---

# 39. Publish-subscribe paradigm

PubSub separates producers from consumers.

```text
Producer
   │
   ▼
Topic
   │
   ├── Consumer A
   ├── Consumer B
   └── Consumer C
```

Discord's modern search indexing architecture uses PubSub for message delivery to indexing infrastructure.

This gives:

- buffering
- decoupling
- asynchronous execution
- independent consumer scaling

Source:

https://discord.com/blog/how-discord-indexes-trillions-of-messages

---

# 40. Dataflow and DAG paradigm

Data transformations can be viewed as a graph:

```text
Source
  │
  ├──► Transform A ──┐
  │                  │
  └──► Transform B ──┤
                     ▼
                  Transform C
```

Each node consumes data and produces data.

This naturally supports parallelism.

Discord's analytics infrastructure and modern dbt/data orchestration are examples of this paradigm.

---

# 41. Queue-based asynchronous processing

Queue architecture:

```text
Producer
   │
   ▼
Queue
   │
   ├── Worker 1
   ├── Worker 2
   └── Worker 3
```

If producers are faster than consumers:

```text
Queue length ↑
```

The queue absorbs bursts temporarily.

This is useful for:

- search indexing
- background processing
- task scheduling
- asynchronous jobs

---

# 42. Edge computing paradigm

Voice/video is a strong example.

Traditional:

```text
User
  │
  ▼
central region
```

Edge:

```text
                  Global edge
              /       |       \
             /        |        \
          Edge A    Edge B    Edge C
             │        │        │
           Users    Users    Users
```

The objective is to reduce physical/network distance.

Discord's 2026 voice article documents the move to Cloudflare's edge network.

---

# 43. Cell-based architecture

Cell architecture divides a large system into semi-independent groups.

```text
                 Discord Search
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
       Cell A        Cell B       Cell C
          │            │            │
      clusters      clusters     clusters
```

A failure in one cell should have a smaller blast radius than a failure in one giant global cluster.

Discord's modern Elasticsearch architecture explicitly introduces logical cells containing multiple smaller clusters.

---

# 44. Distributed database paradigm

A distributed database spreads data and computation across nodes.

```text
                Application
                    │
                    ▼
             Distributed DB
          ┌────────┼────────┐
          ▼        ▼        ▼
        Node A   Node B   Node C
```

Core concepts:

- partitioning
- replication
- consistency
- quorum
- failure recovery
- rebalancing

Discord's ScyllaDB infrastructure provides a real-world example.

---

# 45. Distributed ML paradigm

Distributed machine learning divides computation across multiple workers/GPUs.

```text
                   Dataset
                      │
                      ▼
                 Ray cluster
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
        GPU 1       GPU 2       GPU 3
          │           │           │
          └───────────┼───────────┘
                      ▼
                 Model update
```

Discord's ML platform uses:

```text
Dagster
   +
KubeRay
   +
Ray
   +
Kubernetes
   +
GPU clusters
```

This is a direct distributed-computing use case.

---

# 46. Control-plane / data-plane separation

A distributed system has two different concerns.

## Data plane

Actually serves user traffic.

```text
Client
  │
  ▼
Service
  │
  ▼
Database
```

## Control plane

Manages the infrastructure.

```text
Control plane
   │
   ├── provision
   ├── configure
   ├── validate
   ├── upgrade
   └── monitor
        │
        ▼
   data-plane clusters
```

Discord's Scylla Control Plane is an example of building a dedicated system to safely operate distributed database infrastructure.

---

# 47. Polyglot distributed systems

Discord does not use one programming language for everything.

Discord has publicly described a polyglot monorepo involving:

- Python
- TypeScript
- Rust
- Elixir
- C
- C++

Source:

https://discord.com/blog/how-discord-moved-engineering-to-cloud-development-environments

This means a distributed operation can cross language boundaries:

```text
Python API
    │
    │ gRPC
    ▼
Elixir guilds
    │
    │ message passing
    ▼
Elixir sessions
    │
    ▼
client
```

Another service might use Rust for performance-sensitive work.

Discord's Rust/Elixir article describes using Rust-backed native components while retaining Elixir for core real-time communication logic.

Source:

https://discord.com/blog/using-rust-to-scale-elixir-for-11-million-concurrent-users

---

# 48. Important concepts to learn alongside Discord

To understand Discord at a Distributed Computing course level, learn these topics separately.

## Foundations

```text
Distributed system
Processes
Threads
RPC
Message passing
Network failures
Clock and time
Partial failure
```

## Communication

```text
TCP
HTTP
WebSocket
gRPC
PubSub
Message queues
```

## Partitioning

```text
Partitioning
Sharding
Consistent hashing
Hash rings
Data locality
Rebalancing
```

## Replication

```text
Primary/replica
Replication factor
Synchronous replication
Asynchronous replication
Quorum
Read quorum
Write quorum
```

## Consistency

```text
Strong consistency
Eventual consistency
Causal consistency
Linearizability
CAP theorem
PACELC
```

## Coordination

```text
Leader election
Consensus
Raft
Paxos
Distributed locks
Failure detectors
```

## Reliability

```text
Fault tolerance
Failover
Retry
Backoff
Jitter
Circuit breaker
Backpressure
Load shedding
Cascading failure
Bulkheads
```

## Distributed computation

```text
DAG
Task scheduling
Dataflow
MapReduce
Worker pools
Distributed queues
Distributed ML
```

## Architecture

```text
Microservices
Service-oriented architecture
Cell architecture
Edge computing
Control plane
Data plane
```

---

# 49. Recommended learning order

Do NOT start with Raft.

A good learning order is:

```text
LEVEL 1 — Understand Discord
        │
        ▼
Client
API
Gateway
Sessions
Guilds
Voice
Database
        │
        ▼
LEVEL 2 — Communication
        │
        ▼
HTTP
WebSocket
gRPC
Message passing
PubSub
        │
        ▼
LEVEL 3 — Distribution
        │
        ▼
Partitioning
Sharding
Hashing
Replication
Quorum
        │
        ▼
LEVEL 4 — Reliability
        │
        ▼
Failure
Failover
Load shedding
Retry storms
Backpressure
        │
        ▼
LEVEL 5 — Coordination
        │
        ▼
Leader election
Consensus
Raft
Paxos
        │
        ▼
LEVEL 6 — Distributed computation
        │
        ▼
DAG
Scheduling
Queues
Dataflow
Ray
Distributed ML
        │
        ▼
LEVEL 7 — Advanced architecture
        │
        ▼
Cells
Edge
Control planes
Observability
Distributed tracing
```

---

# 50. Official Discord source library

These are the primary sources for this document.

## Core architecture

### Discord Engineering

https://discord.com/category/engineering

General index of Discord's engineering publications.

### Discord Developer Gateway Documentation

https://discord.com/developers/docs/topics/gateway

Official Gateway documentation.

---

## Real-time and process architecture

### Tracing Discord's Elixir Systems

https://discord.com/blog/tracing-discords-elixir-systems-without-melting-everything

Important for:

- Elixir
- processes
- message passing
- guild processes
- session processes
- fan-out
- distributed tracing
- OpenTelemetry
- gRPC context propagation

### Using Rust to Scale Elixir for 11 Million Concurrent Users

https://discord.com/blog/using-rust-to-scale-elixir-for-11-million-concurrent-users

Important for:

- Elixir
- BEAM
- concurrency
- Rust NIFs
- performance-sensitive components

### Why Discord is switching from Go to Rust

https://discord.com/blog/why-discord-is-switching-from-go-to-rust

Important for:

- async Rust
- memory management
- service architecture
- caching
- concurrency
- latency

---

## Database architecture

### How Discord Stores Trillions of Messages

https://discord.com/blog/how-discord-stores-trillions-of-messages

Important for:

- MongoDB → Cassandra → ScyllaDB history
- distributed storage
- data services
- partitioning
- hot partitions
- concurrency

### How Discord Supercharges Network Disks for Extreme Low Latency

https://discord.com/blog/how-discord-supercharges-network-disks-for-extreme-low-latency

Important for:

- ScyllaDB
- distributed databases
- storage
- low latency
- high request rates

### Authentication Outage

https://discord.com/blog/authentication-outage

Important for:

- replication
- three availability zones
- quorum
- hash rings
- failure handling
- load shedding
- retry storms
- cascading failures

### How Discord Automates ScyllaDB Clusters at Scale

https://discord.com/blog/how-discord-automates-scylladb-clusters-at-scale

Important for:

- distributed database operations
- control planes
- cluster provisioning
- validation
- upgrades
- automation

---

## Search architecture

### How Discord Indexes Trillions of Messages

https://discord.com/blog/how-discord-indexes-trillions-of-messages

Important for:

- Elasticsearch
- sharding
- queues
- PubSub
- message routing
- cells
- Kubernetes
- multi-cluster architecture
- replicas
- rolling upgrades
- large-guild scaling
- dual indexing

---

## Data and computation

### Overclocking dbt: Discord's Custom Solution in Processing Petabytes of Data

https://discord.com/blog/overclocking-dbt-discords-custom-solution-in-processing-petabytes-of-data

Important for:

- DAGs
- data transformations
- large-scale data processing
- scheduling
- parallelism

### From Single-Node to Multi-GPU Clusters

https://discord.com/blog/from-single-node-to-multi-gpu-clusters-how-discord-made-distributed-compute-easy-for-ml-engineers

Important for:

- Ray
- Dagster
- KubeRay
- Kubernetes
- GPU clusters
- distributed ML
- distributed training
- scheduling
- observability

---

## Voice

### How Discord Handles Two and Half Million Concurrent Voice Users Using WebRTC

https://discord.com/blog/how-discord-handles-two-and-half-million-concurrent-voice-users-using-webrtc

Important for:

- Gateway
- Guilds
- Voice
- WebRTC
- signaling
- voice servers
- real-time distributed systems

### How We Moved Discord Voice to the Edge

https://discord.com/blog/how-we-moved-discord-voice-to-the-edge

Important for:

- edge computing
- geographic distribution
- latency
- packet loss
- global voice infrastructure

### Behind the Scenes of the 3/25/26 Voice Outage

https://discord.com/blog/behind-the-scenes-of-the-3-25-26-voice-outage

Important for:

- sessions
- Gateway
- cascading load
- Kubernetes migration
- recovery
- distributed failure

---

# 51. Final mental model

The easiest way to remember Discord as a distributed system is:

```text
                         DISCORD
                            │
       ┌────────────────────┼────────────────────┐
       │                    │                    │
       ▼                    ▼                    ▼
   APPLICATION          REAL-TIME             VOICE
       │                    │                    │
       ▼                    ▼                    ▼
      API                 Gateway               Edge
       │                    │                    │
       │              ┌─────┴─────┐             │
       │              ▼           ▼             │
       │           Guilds      Sessions          │
       │              │           │              │
       └──────────────┼───────────┘              │
                      │                          │
                      ▼                          │
                DATA SYSTEMS                    │
                      │                          │
          ┌───────────┼────────────┐             │
          ▼           ▼            ▼             │
       ScyllaDB   Elasticsearch   PubSub          │
          │           │            │              │
          ▼           ▼            ▼              │
     replication    sharding     queues           │
     quorum        cells         routing           │
     hash ring     replicas      workers           │
          │           │            │              │
          └───────────┼────────────┘              │
                      │                           │
                      ▼                           │
             DISTRIBUTED COMPUTE                 │
                      │                           │
               ┌──────┴──────┐                    │
               ▼             ▼                    │
              DAG           Ray                   │
               │             │                    │
               ▼             ▼                    │
          Data systems    Multi-GPU ML            │
                                                   │
                      ┌────────────────────────────┘
                      ▼
                GLOBAL USERS
```

And the three-part mental model is:

```text
╔════════════════════════════════════════════════════════════╗
║                     PART I — ARCHITECTURE                 ║
╠════════════════════════════════════════════════════════════╣
║ Client → API → Gateway → Guilds/Sessions → Data           ║
║                                  │                          ║
║                                  ├── ScyllaDB               ║
║                                  ├── Elasticsearch          ║
║                                  ├── PubSub                 ║
║                                  └── Voice/Edge             ║
╚════════════════════════════════════════════════════════════╝

                         │
                         ▼

╔════════════════════════════════════════════════════════════╗
║              PART II — DISTRIBUTED ALGORITHMS             ║
╠════════════════════════════════════════════════════════════╣
║ Partitioning                                               ║
║ Sharding                                                   ║
║ Hash rings                                                 ║
║ Replication                                                ║
║ Quorum                                                     ║
║ Fan-out                                                    ║
║ Queue processing                                           ║
║ Scheduling                                                 ║
║ DAG execution                                              ║
║ Failure handling                                           ║
║ Load shedding                                              ║
║ Retry/backpressure                                         ║
║ Online migration                                           ║
╚════════════════════════════════════════════════════════════╝

                         │
                         ▼

╔════════════════════════════════════════════════════════════╗
║             PART III — DISTRIBUTED PARADIGMS              ║
╠════════════════════════════════════════════════════════════╣
║ Client-server                                              ║
║ Distributed services                                       ║
║ Actor/process model                                        ║
║ Message passing                                            ║
║ Event-driven architecture                                  ║
║ Asynchronous programming                                   ║
║ Publish-subscribe                                          ║
║ Dataflow/DAG                                               ║
║ Queue-based processing                                     ║
║ Cell architecture                                          ║
║ Edge computing                                             ║
║ Distributed databases                                      ║
║ Distributed ML                                             ║
║ Control plane / data plane                                 ║
║ Polyglot distributed systems                               ║
╚════════════════════════════════════════════════════════════╝
```

## The most important distinction

When writing an academic report, use these labels:

**Discord-documented**

> "Discord's authentication cluster uses replication across three zones and quorum consistency reads."

**Distributed-computing interpretation**

> "This architecture demonstrates replication and quorum-based fault tolerance."

**General theory**

> "Raft and Paxos are distributed consensus algorithms that should be studied to understand distributed coordination, but the cited Discord documents do not establish that Discord uses either algorithm."

That distinction keeps the case study technically accurate.

---

# Suggested next step

Study this document in the following sequence:

```text
1. Architecture
   ↓
2. Gateway + Sessions + Guild processes
   ↓
3. Message flow
   ↓
4. ScyllaDB
   ↓
5. Replication + Quorum
   ↓
6. Search + Sharding + Cells
   ↓
7. Voice + Edge
   ↓
8. Failure scenarios
   ↓
9. Distributed algorithms
   ↓
10. Distributed paradigms
   ↓
11. Ray + Distributed ML
   ↓
12. Distributed tracing
```

Do not memorize the diagrams first. For every subsystem, understand:

**problem → architecture → communication → state → algorithm/mechanism → failure → recovery → paradigm.**
