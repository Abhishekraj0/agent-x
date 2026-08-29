# Agent Tool System

AgentX tools represent external capabilities exposed to LLM planning strategies.

## Risk Levels
Tools define risk boundaries:
* `LOW`
* `MEDIUM`
* `HIGH`
* `CRITICAL`

## Custom Java Tool Example
```java
public class CalculatorTool implements AgentTool {
    @Override
    public ToolId id() { return new ToolId("calc"); }
    
    @Override
    public String description() { return "Add numbers"; }
    
    @Override
    public ToolSchema inputSchema() { return new ToolSchema(Map.of()); }
    
    @Override
    public ToolResult execute(ToolContext context) {
        return new ToolResult("success", "result");
    }
}
```
