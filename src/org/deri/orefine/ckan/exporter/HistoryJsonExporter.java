package org.deri.orefine.ckan.exporter;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonGenerator;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;


public class HistoryJsonExporter implements WriterExporter {

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public void export(Project project, Properties options, Engine engine, Writer w) throws IOException {
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
        try {
            writer.writeStartObject();
            writer.writeArrayFieldStart("history-json");
            for (HistoryEntry entry : project.history.getLastPastEntries(-1)) {
                // TODO: Needs testing
                if (entry.operation != null) {
                    writer.writeStartObject();
                    writer.writeObject(options);
                    writer.writeEndObject();
                }
            }
            writer.writeEndArray();
            writer.writeEndObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            writer.flush();
            writer.close();
            w.flush();
            w.close();
        }
    }

}
