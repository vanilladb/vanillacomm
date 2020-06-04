# VanillaComm

[![Build Status](https://travis-ci.org/vanilladb/vanillacomm.svg?branch=master)](https://travis-ci.org/vanilladb/vanillacomm)
[![Apache 2.0 License](https://img.shields.io/badge/license-apache%202.0-orange.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.vanilladb/comm.svg)](https://maven-badges.herokuapp.com/maven-central/org.vanilladb/comm)

VanillaComm is a collection of reliable group communication primitives (e.g., total-ordering) that can benefit the distributed database systems (e.g., eager-replication, NewSQL database systems).

## Required Tools

You will need the following tools to compile and execute the demonstration in this project:

- Java Development Kit 1.7 (or newer)
- Maven (>= 3.6.0)

## Getting Started

This project includes a simple demonstration that have 2 clients continously sending messages and 3 servers processing these messages using Zab (which is an total-ordering protocol). Here are the instructions to start the demonstration.

### Step 1: Cloning this project

The first step is to clone this project to your working space, open your terminal and navigate to the project directory.

### Step 2: Configuring the addresses

A server/client process must know where the other processes are, so you have to provide such configuration first. We have prepared an example configuration for you, which is `vanillacomm.properties` in `src\main\resources\org\vanilladb\comm`. The following shows its content:

```properties
# The views of the machine
# A machine is represented by "ID IP PORT"
# Each machine is split by a comma (,)
org.vanilladb.comm.view.ProcessView.SERVER_VIEW=0 127.0.0.1 42961, 1 127.0.0.1 42962, 2 127.0.0.1 42963
org.vanilladb.comm.view.ProcessView.CLIENT_VIEW=0 127.0.0.1 30000, 1 127.0.0.1 30001
```

This configuration specifies 3 server and 2 client processes in this demonstration. You can, of course, change this configuration to any number of servers and clients as you like. See Section "Addresses Configuration" for more information about how to configure the properties file.

### Step 3: Starting the servers

Once you have set up the configuration, you can now start each server. Since we have set up all run configurations in `pom.xml`, the only thing you need to do is to start servers using the folloing command:

```bash
mvn exec:java@server-demo -Dexec.args="0"
```

The program argument is the id of the process, so please be sure that you specify distinct IDs to each server.

Then, Maven will start compiling and executing the project. Before going to the next step, please ensure that every server have started properly, otherwise the group communication protocol will not work.

If everything goes well, you should see `The server is ready!` on all the server processes as the following shows:

```txt
May 12, 2020 4:07:40 PM org.vanilladb.comm.server.ServerDemo main
INFO: Initializing the server...
May 12, 2020 4:07:40 PM org.vanilladb.comm.server.VanillaCommServer run
INFO: Starts the network service
May 12, 2020 4:07:40 PM org.vanilladb.comm.protocols.totalorderappl.TotalOrderApplicationSession handleChannelInit
INFO: Socket registration request sent.
May 12, 2020 4:07:40 PM org.vanilladb.comm.protocols.totalorderappl.TotalOrderApplicationSession handleRegisterSocketEvent
INFO: Socket registration completed. (/127.0.0.1:42961)
May 12, 2020 4:09:00 PM org.vanilladb.comm.server.VanillaCommServer onAllProcessesReady
INFO: All processes are ready.
May 12, 2020 4:09:00 PM org.vanilladb.comm.server.ServerDemo onServerReady
INFO: The server is ready!
```

### Step 4: Starting the clients

After all the servers are ready, you can now start the client processes using the following command:

```bash
mvn exec:java@client-demo -Dexec.args="0"
```

Note that you should specify distinct IDs for each client process in the program arguments as we do for servers.

### Done!

After the clinet starts, it will connect to one of the servers and start sending messages. You will see those messages on all the servers in the same order.

## Linking via Maven

To use this project as a library, we highly recommand to link via Maven. Please add the following configuration to your `pom.xml`.

```xml
<dependency>
  <groupId>org.vanilladb</groupId>
  <artifactId>comm</artifactId>
  <version>0.2.2</version>
</dependency>
```

## Addresses Configuration

VanillaComm needs users to provide the addresses of all participating processes so that it can connect them through sockets. To provide such configurations, one must provide a properties file which contains the following properties:

```properties
org.vanilladb.comm.view.ProcessView.SERVER_VIEW=[Address of Server 0], [Address of Server 1], ...
org.vanilladb.comm.view.ProcessView.CLIENT_VIEW=[Address of Client 0], [Address of Client 1], ...
```

The format of each address is `[Process ID] [IP Address] [Port]`. Addresses must be seperated by commas (`,`) and IDs must be monotonically increasing. Here is an example with 3 servers and 2 clinets:

```properties
org.vanilladb.comm.view.ProcessView.SERVER_VIEW=0 127.0.0.1 42961, 1 127.0.0.1 42962, 2 127.0.0.1 42963
org.vanilladb.comm.view.ProcessView.CLIENT_VIEW=0 127.0.0.1 30000, 1 127.0.0.1 30001
```

We also provide an example file in `src\main\resources\org\vanilladb\comm\vanillacomm.properties`.

Once you have the properties file, the next step is to tell VanillaComm where the file is. To provide the path, you must specify the path in system property `org.vanilladb.comm.config.file`. For example, assuming that the properties file is in `properties\vanillacomm.properties`, the system property will be:

```properties
org.vanilladb.comm.config.file=properties/vanillacomm.properties
```

## Tutorials

Group Communication is to provide multipoint to multipoint communication. There are some challenges in group communication:

- Message delay or loss
- Node Failure
- Link Failure

We provide a series of tutorials to identify the difficulties of group communication and how to solve these problem:

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

## Contact Information

If you have any question, you can either open an issue here or contact [vanilladb@datalab.cs.nthu.edu.tw](vanilladb@datalab.cs.nthu.edu.tw) directly.

## License

Copyright 2016-2020 vanilladb.org contributors

Licensed under the [Apache License 2.0](LICENSE)
