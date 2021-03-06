# Minimal Zeebe Exporter Demo

[Exporters](https://docs.zeebe.io/basics/exporters.html) allow you to tap into the Zeebe event stream on a partition and export selected events to other systems. You can filter events, perform transformations, and even trigger side-effects from an exporter.

Read a two-part series about building Zeebe exporters on the Zeebe blog: [Part One](https://zeebe.io/blog/2019/05/exporter-part-1/) | [Part Two](https://zeebe.io/blog/2019/05/exporter-part-2/).

## Important things to know about Exporters

Two things to note are that an exporter runs in the same JVM as the broker, and intensive computation in an exporter will impact the throughput of the broker. You should do the minimal amount of processing possible in the exporter, and perform further transformation in another system after export.

Also, a badly-behaved exporter can cause broker disks to fill up. The event log is truncated only up to the earliest exporter position. If your exporter is loaded, and does not advance its position in the stream - whether due to a programming error or because of back pressure or latency from an external system - the broker log will not truncate and the broker disk can fill up.

You should plan for failure in connectivity to any external system and design the failure mode of your system.

## Building an Exporter

1. Create a new maven project:

```
mvn archetype:generate -DgroupId=io.zeebe 
    -DartifactId=zeebe-exporter-demo
    -DarchetypeArtifactId=maven-archetype-quickstart  
    -DinteractiveMode=false
```

2. Add `zeebe-exporter-api` as a dependency in the project's `pom.xml` file:

```xml
<dependency>
    <groupId>io.zeebe</groupId>
    <artifactId>zeebe-exporter-api</artifactId>
    <version>0.26.0</version>
</dependency>
```

3. Rename the file `src/main/java/io.zeebe/App.java` to `DemoExporter.java`, then edit it and import the `Exporter` interface:

```java
import io.zeebe.exporter.api.Exporter;
```

4. Remove the `main` method from the `App` class, rename it as `DemoExporter`, and implement `Exporter`:

```java
public class DemoExporter implements Exporter {
{
```

5. If your IDE supports it, use code completion to implement the methods you need to fulfil the `Exporter` interface:

```java
public class DemoExporter implements Exporter {
    
    public void configure(Context context) {    
    }
  
    public void open(Controller controller) {    
    }
    
    public void close() {
    }
    
    public void export(Record record) {       
    }
}
```

## Exporter Lifecycle

These methods are the lifecycle hooks for an exporter. 

### Configure

The `configure` method allows your exporter to read any configuration specified for it in the `zeebe.cfg.toml` file. An instance of your exporter will be created, then thrown away, during broker start up. If your exporter throws in this method, the broker will halt. This prevents the broker from starting if an exporter doesn't have sufficient configuration to operate.

### Open

If your exporter does not throw in the `configure` method, then another instance is created, and the `open` method is called. In this method you can get a reference to a `Controller`. The `Controller` provides an asynchronous scheduler that can be used to implement operation batching (we will look at that in another post), and a method to mark a record as exported.

### Close

When the broker shuts down, the `close` method is called, and you can perform any clean-up that you need to.

### Export

Whenever a record is available for export, the `export` method is called with the record to export. Remember that you must mark it as exported before you return from this method, otherwise it will persist forever.

## Exporting a record

We'll make the simplest exporter possible: we'll write a JSON representation of the record to the console.

We won't need `configure` or `close`, so we can remove them.

We will grab a reference to the `Controller` in the `open` method first of all:

```java
public class DemoExporter implements Exporter {
    Controller controller;
    
    public void open(Controller controller) {
        this.controller = controller;
    }
    
    public void export(Record record) {
    }
}
```

Now we will implement an `export` method to (a) print out the record, and (b) mark the record as exported:

```java
public class DemoExporter implements Exporter {
    Controller controller;
    
    public void open(Controller controller) {
        this.controller = controller;
    }
    
    public void export(Record record) {
        System.out.println(record.toJson());
        this.controller.updateLastExportedRecordPosition(record.getPosition());
    }
}
```

## Deploy the Exporter

1. Build the exporter, using `mvn package`.
2. Copy the resulting `zeebe-exporter-demo-1.0-SNAPSHOT.jar` file to the `lib` directory of your Zeebe broker. 
3. Edit the `zeebe.cfg.toml` file, and add an entry for the exporter:

```toml
[[exporters]]
id = "demo"
className = "io.zeebe.DemoExporter"
```

4. Start the broker.

5. Now, deploy a bpmn diagram to the broker, and you will see the deployment being logged to the console by your exporter.

The source code for this exporter is available on [GitHub](https://github.com/jwulf/zeebe-exporter-demo), and a docker-compose configuration for it is available in the `exporter-demo` folder [here](https://github.com/zeebe-io/zeebe-docker-compose).
