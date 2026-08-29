# Memory and Vector Store

AgentX supports short-term and semantic retrieval memory systems.

## Memory Types
1. **Working Memory**: In-execution variables.
2. **Conversation Memory**: Historical chat threads.
3. **Semantic Memory**: Cosine-similarity searches over vectors.

## InMemoryVectorMemoryStore Example
The standard store uses cosine calculations to compare vectors dynamically:
```java
var store = new InMemoryVectorMemoryStore();
store.save(new Memory(...));
var results = store.search(new MemoryQuery(...));
```
