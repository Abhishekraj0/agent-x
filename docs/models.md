# Model Providers

AgentX defines generic model adapters to isolate core logic from provider-specific schemas.

## Interfaces
* `ChatModel`: Standard blocking execution.
* `StreamingChatModel`: Reactive publisher streams.

## Supported Providers
* **OpenAIChatModel**: Integrates with OpenAI v1 APIs.
* **OllamaChatModel**: Communicates with local Ollama endpoints.
* **MockChatModel**: Generates deterministic mock responses for testing.
