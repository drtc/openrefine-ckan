package org.deri.orefine.ckan.commands;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.deri.orefine.ckan.CkanApiProxy;
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.json.JSONWriter;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.refine.util.ParsingUtilities;

import com.google.refine.commands.Command;

public class GetPackageDetailsCommand extends Command {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String packageId = request.getParameter("package_id");
        String ckanApiBase = request.getParameter("ckan_base_api");
        String packageUrl = ckanApiBase + "/" + packageId;
        try {
            if (ckanApiBase == null || ckanApiBase.isEmpty()) {
                throw new RuntimeException("Some required parameters are missing: CKAN API base URL");
            }
            if (packageId == null || packageId.isEmpty()) {
                throw new RuntimeException("Some required parameters are missing: package ID");
            }
            CkanApiProxy ckanApiClient = new CkanApiProxy();
//			JSONObject o = ckanApiClient.getPackage(packageUrl);
            ObjectNode o = ckanApiClient.getPackage(packageUrl);
            if (o == null) {
                respond(response, "{\"packageId\":null}");
                return;
            }
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            Writer w = response.getWriter();
//            JSONWriter writer = new JSONWriter(w);
//            writer.object();
//            writer.key("packageId"); writer.value(packageId);
//            writer.key("resources");
//            writer.array();
//			JSONArray rsrcsArr = o.getJSONArray("resources");
            JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
            writer.writeStartObject();
            writer.writeStringField("packageId", packageId);
            writer.writeArrayFieldStart("resources");
            ArrayNode rsrcsArr = (ArrayNode) o.get("resources");
//			for (int i=0;i<rsrcsArr.length();i++){
//				JSONObject rsrcObj = rsrcsArr.getJSONObject(i);
//				String webStoreUrl = rsrcObj.getString("webstore_url");
//					writer.object();
//					writer.key("name"); writer.value(rsrcObj.getString("name"));
//					writer.key("format"); writer.value(rsrcObj.getString("format"));
//					writer.key("description"); writer.value(rsrcObj.getString("description"));
//					writer.key("webstore_url"); writer.value(rsrcObj.getString("webstore_url"));
//					writer.endObject();
            for (int i = 0; i < rsrcsArr.size(); i++) {
                JsonNode rsrcObj = rsrcsArr.get(i);
                String webStoreUrl = rsrcObj.get("webstore_url").asText();
                if (webStoreUrl != null && webStoreUrl.startsWith("http://")) {
                    writer.writeStartObject();
                    writer.writeStringField("name", rsrcObj.get("name").asText());
                    writer.writeStringField("format", rsrcObj.get("format").asText());
                    writer.writeStringField("description", rsrcObj.get("description").asText());
                    writer.writeStringField("webstore_url", webStoreUrl);
                    writer.writeEndObject();
                }
            }
//			writer.endArray();
//			writer.endObject();
            writer.writeEndArray();
            writer.writeEndObject();
            writer.flush();
            writer.close();
        } catch (Exception e) {
            respondException(response, e);
        }
    }


}
