package io.zeebe;

import io.zeebe.exporter.api.context.Controller;
import io.zeebe.exporter.api.record.Record;
import io.zeebe.exporter.api.spi.Exporter;

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
