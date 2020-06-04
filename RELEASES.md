# Release Notes

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
