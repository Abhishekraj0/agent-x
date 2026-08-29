# Composable Guardrails

Guardrails run validations on input prompts and tool execution parameters.

## Composable Validation
Use `CompositeGuardrail` to chain validation checkers:
```java
var guardrail = new CompositeGuardrail(List.of(
    new CostGuardrail(),
    new InputGuardrail()
));
```
