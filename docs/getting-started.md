# Getting Started with AgentX

## Prerequisites
* **Java**: Version 21 or higher
* **Maven**: Version 3.8+

## Quick Installation
Run maven package installation locally:
```bash
mvn clean install
```

## Creating Your First Agent
Define a basic agent using the builder API:
```java
var agent = AgentX.builder()
    .model(new MockChatModel())
    .build();

var response = agent.run("Hello AgentX!");
System.out.println(response.output());
```
