# Security Infrastructure

Protect executions through fine-grained control mechanisms.

## PermissionManager
The `DefaultPermissionManager` enforces security configurations based on tool risk classifications.
* `ALLOW`: Automated run.
* `DENY`: Hard-stop rejection.
* `REQUIRES_APPROVAL`: Forces approval request.
