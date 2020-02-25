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

## Tutorials

Group Communication is to provide multipoint to multipoint communication. There are some challenges in group communication:

- Message delay or loss
- Node Failure
- Link Failure

We provide a series of tutorials to identify the difficulties of group communication and how to solve these problem:

- Getting Started (TBA)
  - Configurations, applications, etc.
- [Appia](http://www.vanilladb.org/slides/comm/Appia.pdf)
  - Layers, sessions, Qos, channel, etc.
- [Basic Abstraction](http://www.vanilladb.org/slides/comm/Basic_Abstraction.pdf)
  - Perfect point-to-point link, perfect failure detection, etc.
- [Reliable Broadcast](http://www.vanilladb.org/slides/comm/Reliable_Broadcast.pdf)
  - Best-effort broadcast, reliable broadcast, uniform reliable broadcast, etc.
- [Consensus](http://www.vanilladb.org/slides/comm/Consensus.pdf)
  - Flooding consensus, sequencer-based consensus, Paxos, etc.
- [Total-ordering](http://www.vanilladb.org/slides/comm/Total_Ordering.pdf)
  - Consensus-based total-ordering, Zab, etc.

## Linking via Maven

```xml
<dependency>
  <groupId>org.vanilladb</groupId>
  <artifactId>comm</artifactId>
  <version>0.1.1</version>
</dependency>
```

## Contact Information

If you have any question, you can either open an issue here or contact [vanilladb@datalab.cs.nthu.edu.tw](vanilladb@datalab.cs.nthu.edu.tw) directly.

## License

Copyright 2016-2020 vanilladb.org contributors

Licensed under the [Apache License 2.0](LICENSE)
