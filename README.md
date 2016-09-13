# VanillaComm

[![Build Status](https://travis-ci.org/vanilladb/vanillacomm.svg?branch=master)](https://travis-ci.org/vanilladb/vanillacomm)
[![Apache 2.0 License](https://img.shields.io/badge/license-apache%202.0-orange.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.vanilladb/comm.svg)](https://maven-badges.herokuapp.com/maven-central/org.vanilladb/comm)

VanillaComm is a collection of reliable group communication primitives (e.g., total-ordering) that can benefit the distributed database systems (e.g., eager-replication, NewSQL database systems).

## Required Tools

You will need the following tools to compile and run this project:

- Java Development Kit 1.7 (or newer)
- Maven

## System Configurations

TBA

## Architecture Tutorials

We have a series of educational slides to make the people who are not familiar with database internal architecture understand how a database works. Here is the outline of the our slides:

- Getting started (TBA)
  - Configurations, applications, etc.
- Appia (TBA)
  - Layers, sessions, Qos, channel, etc.
- Basic abstraction (TBA)
  - Perfect point-to-point link, perfect failure detection, etc.
- Reliable Broadcast (TBA)
  - Best-effort broadcast, reliable broadcast, uniform reliable broadcast, etc.
- Consensus (TBA)
  - Flooding consensus, sequencer-based consensus, Paxos, etc.
- Total-ordering (TBA)
  - Consensus-based total-ordering, Zab, etc.

## Linking via Maven

```xml
<dependency>
  <groupId>org.vanilladb</groupId>
  <artifactId>comm</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Contact Information

If you have any question, you can either open an issue here or contact [vanilladb@datalab.cs.nthu.edu.tw](vanilladb@datalab.cs.nthu.edu.tw) directly.

## License

Copyright 2016 vanilladb.org contributors

Licensed under the [Apache License 2.0](LICENSE)
