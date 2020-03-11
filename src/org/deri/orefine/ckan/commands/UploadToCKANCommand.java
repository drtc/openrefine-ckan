package org.deri.orefine.ckan.commands;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.orefine.ckan.CkanApiProxy;
import org.deri.orefine.ckan.exporter.HistoryJsonExporter;
import org.deri.orefine.ckan.rdf.ProvenanceFactory;
import org.deri.orefine.ckan.model.Slugify;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.exporters.Exporter;
import com.google.refine.exporters.ExporterRegistry;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;


public class UploadToCKANCommand extends Command {

    private static final String CKAN_DEFAULT_BASE_URL = "http://master.ckan.org/";

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Project project = getProject(request);
        String apikey = request.getParameter("apikey");
        String ckanApiBase = request.getParameter("ckan_base_api");

        Slugify slg = new Slugify();
        final String packageName = request.getParameter("package_id");
        final String packageId = slg.slugify(packageName);

        boolean createNewIfNonExisitng = getBoolean(request, "create_new");
        boolean addProvenanceInfo = false;
        boolean rememberApiKey = getBoolean(request, "remember_api_key");

        //get (comma-separated list of ) formats required to be uploaded
        String files = request.getParameter("files");

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);

        try {

            ObjectNode options = ParsingUtilities.evaluateJsonStringToObjectNode(request.getParameter("options"));
            if (ckanApiBase == null || ckanApiBase.isEmpty()) {
                throw new RuntimeException("Some required parameters are missing: CKAN API base URL");
            }
            if (packageId == null || packageId.isEmpty()) {
                throw new RuntimeException("Some required parameters are missing: package ID");
            }
            if (rememberApiKey) {
                saveApiKey(apikey);
                saveCKANBase(ckanApiBase);
            } else {
                forgetApiKey();
                forgetCKANBase();
            }
            //remove the last "/" from CKAN API base if it exists
            ckanApiBase = ckanApiBase.trim();
            if (ckanApiBase.endsWith("/")) {
                ckanApiBase = ckanApiBase.substring(0, ckanApiBase.length() - 1);
            }

            CkanApiProxy ckanApiClient = new CkanApiProxy();

            Engine engine = getEngine(request, project);
            StringTokenizer tokenizer = new StringTokenizer(files, ",");
            Set<Exporter> exporters = new HashSet<Exporter>();
            while (tokenizer.hasMoreTokens()) {
                String format = tokenizer.nextToken();
                Exporter exporter = ExporterRegistry.getExporter(format);

                if (exporter == null) {
                    //handle the specialcase of provenance
                    if (format.equals("add_provenance_info")) {
                        addProvenanceInfo = true;
                        continue;
                    }
                    //either JSON representation of the history or something went wrong
                    if (format.equals("history-json")) {
                        exporter = new HistoryJsonExporter();
                    } else {
                        //fail
                        respondException(response, new RuntimeException("Unknown exporter format"));
                        return;
                    }
                }
                exporters.add(exporter);
            }

            final String packageUrl = ckanApiClient.addGroupOfResources(ckanApiBase, packageName, options, exporters, project, engine,
                    new ProvenanceFactory(), apikey, createNewIfNonExisitng, addProvenanceInfo);

            writer.writeStartObject();
            writer.writeStringField("code", "ok");
            writer.writeFieldName("package_details");
            writer.writeStartObject();
            writer.writeStringField("packageUrl", packageUrl);
            writer.writeStringField("packageId", packageId);
            writer.writeEndObject();
            writer.writeEndObject();
        } catch (Exception e) {
            respondException(response, e);
        } finally {
            writer.flush();
            writer.close();
            w.flush();
            w.close();
        }
    }

    private void saveApiKey(String key) {
        ProjectManager.singleton.getPreferenceStore().put("CKAN.api_key", key);
    }

    private void forgetApiKey() {
        ProjectManager.singleton.getPreferenceStore().put("CKAN.api_key", "");
    }

    private void saveCKANBase(String key) {
        ProjectManager.singleton.getPreferenceStore().put("CKAN.ckan_base_url", key);
    }

    private void forgetCKANBase() {
        ProjectManager.singleton.getPreferenceStore().put("CKAN.ckan_base_url", CKAN_DEFAULT_BASE_URL);
    }

    private boolean getBoolean(HttpServletRequest request, String paramName) {
        return request.getParameter(paramName) != null && request.getParameter(paramName).toLowerCase().equals("true");
    }
}
