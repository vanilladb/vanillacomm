# Release Notes

## Version 0.2.6 (2022-01-06)

### Security Issue

- Bump log4j from 1.2.17 to 2.17.1 and adapt to the new log4j interface.

## Version 0.2.5 (2021-07-22)

### API Changes

- Change `getServerCount()` and `getClientCount()` in `VanillaCommClient` and `VanillaCommServer` from non-static methods to static methods.
- Update the constructor of `ZabElectionLayer` so that we can give it a machine ID as the default Zab leader.
- Add a constructor for `VanillaCommServer` so that we can set the default Zab leader.

## Version 0.2.4 (2021-06-13)

### Bug Fixes

- Fix the concurrency bugs in Appia (which is included in Appia-Core).

## Version 0.2.3 (2021-06-13)

### Enhancements

- Add a message to identify the unrecognized sources in the failure detection layer.
- Remove failure detection layer from P2P channels to reduce the number of exchange messages.

## Version 0.2.2 (2020-06-04)

### Bug Fixes

- Add heartbeat signals to the failure detection layer in order to prevent disconnection due to TCP timeout.

## Version 0.2.1 (2020-05-14)

### Bug Fixes

- Fix the bug that some classes cannot find the package of flooding consensus.

## Version 0.2.0 (2020-05-12)

### Refactor

- Refactor the whole architecture to improve readability and effciency.

## Version 0.1.1 (2020-02-25)

### Enhancements

- Extend the API of `ServerAppl` for sending messages.
- Add a support to send P2P messages between client applications.

## Version 0.1.0 (2016-09-13)

- A communication module with the following protocols
  - Zab
  - Paxos
  - P2P
  - Failure Detection
