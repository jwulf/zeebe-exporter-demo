package io.zeebe;

import io.zeebe.exporter.api.context.Context;
import io.zeebe.exporter.api.context.Controller;
import io.zeebe.exporter.api.Exporter;
import io.zeebe.protocol.record.Record;

public class DemoExporter implements Exporter {
    Controller controller;

    public void configure(Context context) throws Exception {
    }

    public void open(Controller controller) {
        this.controller = controller;
    }

    public void close() {
    }

    public void export(Record record) {
        System.out.println(record.toJson());
        this.controller.updateLastExportedRecordPosition(record.getPosition());
    }
}
