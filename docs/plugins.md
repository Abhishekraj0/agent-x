# Plugin Architecture

AgentX supports modular extension points discovery using the ServiceLoader API.

## Custom Plugin
Implement `AgentPlugin` interface:
```java
public class MyCustomPlugin implements AgentPlugin {
    @Override
    public PluginMetadata metadata() { return new PluginMetadata("custom"); }
    
    @Override
    public void initialize(PluginContext context) {
        context.tools().register(new MyCustomTool());
    }
    
    @Override
    public void shutdown() {}
}
```
