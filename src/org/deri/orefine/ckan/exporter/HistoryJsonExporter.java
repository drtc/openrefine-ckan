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
        try {
            JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
            writer.writeStartObject();
//			writer.writeStartArray();
            for (HistoryEntry entry : project.history.getLastPastEntries(-1)) {
                // TODO: Needs testing
                if (entry.operation != null) {
                    writer.writeObject(options);
                }
            }
//			writer.writeEndArray();
            writer.writeEndObject();
            writer.flush();
            writer.close();
        } catch (IOException je) {
            throw new RuntimeException(je);
        }
    }

}
