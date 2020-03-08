package org.deri.orefine.ckan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.deri.orefine.ckan.model.Resource;
import org.deri.orefine.ckan.model.Slugify;
import org.deri.orefine.ckan.rdf.ProvenanceFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.JSONUtilities;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.Exporter;
import com.google.refine.exporters.StreamExporter;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;
import com.hp.hpl.jena.rdf.model.Model;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;


public class CkanApiProxy {

    //	public String registerPackageResources(String packageUrl, Set<Resource> resources, String apiKey) throws ClientProtocolException, IOException, JSONException{
    public String registerPackageResources(String packageUrl, Set<Resource> resources, String apiKey) throws ClientProtocolException, IOException {
        //get the package
        HttpClient client = new DefaultHttpClient();
        //head request does not work!
        HttpGet get = new HttpGet(packageUrl);
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("package " + packageUrl + " not found");
        }
        //parse resources
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        response.getEntity().writeTo(os);
        ObjectNode packageObj = ParsingUtilities.evaluateJsonStringToObjectNode(os.toString());
        ArrayNode resourcesArr = (ArrayNode) packageObj.withArray("resources");
        String ckan_url = packageObj.get("ckan_url").asText();

        //add the new resources
        resources.forEach((Resource resource) -> {
            resourcesArr.add(resource.asJsonObject());
        });
        //save
        ObjectNode obj = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(obj, "resources", resourcesArr);

        HttpPost post = new HttpPost(packageUrl);
        post.setHeader("Authorization", apiKey);
        post.setHeader("X-CKAN-API-Key", apiKey);
        StringEntity entity = new StringEntity(obj.toString(), "UTF-8");
        post.setHeader("Content-type", "application/x-www-form-urlencoded");
        post.setEntity(entity);

        response = client.execute(post);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("something went wrong whil registering the new resource of package " + packageUrl);
        }

        return ckan_url;
    }

    public String registerPackageResources_CKAN_2_3(String ckanBaseUri, String packageId, Set<Resource> resources, String apiKey) throws ClientProtocolException, IOException {
        //get the package
        HttpClient client = new DefaultHttpClient();
        //head request does not work!
        String packageUrl = ckanBaseUri + "/api/action/package_show?id=" + packageId;
        HttpGet get = new HttpGet(packageUrl);
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("package " + packageUrl + " not found");
        }
        //parse resources
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        response.getEntity().writeTo(os);
        ObjectNode packageObj = ParsingUtilities.mapper.readValue(os.toString(), ObjectNode.class);
        ArrayNode resourcesArr = (ArrayNode) packageObj.get("results").withArray("resources");
        String ckan_url = packageUrl;
        //add the new resources
        resources.forEach((Resource resource) -> {
            resourcesArr.add(resource.asJsonObject());
        });
        //save
        ObjectNode obj = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(obj, "resources", resourcesArr);

        HttpPost post = new HttpPost(packageUrl);
        post.setHeader("Authorization", apiKey);
        post.setHeader("X-CKAN-API-Key", apiKey);
        StringEntity entity = new StringEntity(obj.toString(), "UTF-8");
        post.setHeader("Content-type", "application/x-www-form-urlencoded");
        post.setEntity(entity);

        response = client.execute(post);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("something went wrong whil registering the new resource of package " + packageUrl);
        }

        return ckan_url;
    }


    /**
     * @param packageUrl
     * @return checks whether a package with the URL given exists
     * @throws IOException
     * @throws ClientProtocolException
     */
    public boolean exists(String packageUrl) throws ClientProtocolException, IOException {
        HttpClient client = new DefaultHttpClient();
        //head request does not work!
        HttpGet get = new HttpGet(packageUrl);
        HttpResponse response = client.execute(get);
        return response.getStatusLine().getStatusCode() == 200;
    }

    public ObjectNode getPackage(String packageUrl) throws ClientProtocolException, IOException {
        HttpClient client = new DefaultHttpClient();
        //head request does not work!
        HttpGet get = new HttpGet(packageUrl);
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() != 200) {
            return null;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        response.getEntity().writeTo(os);
        return ParsingUtilities.mapper.readValue(os.toString(), ObjectNode.class);
    }

    public void registerNewPackage(String packageName, ObjectNode options, String ckanBaseUri, String apikey) throws ClientProtocolException, IOException {
        HttpClient client = new DefaultHttpClient();
        //head request does not work!
        HttpPost post = new HttpPost(ckanBaseUri + "/api/action/package_create");
        post.setHeader("Authorization", apikey);
        post.setHeader("Content-type", "application/x-www-form-urlencoded");
        StringEntity entity = new StringEntity(getNewPackageInJson(packageName, options).toString(), "UTF-8");
        post.setEntity(entity);
        HttpResponse response = client.execute(post);
        int responseCode = response.getStatusLine().getStatusCode();

        if (responseCode < 200 || responseCode >= 300) {
            throw new RuntimeException("failed to register a new package with response code " + responseCode + " at " + ckanBaseUri);
        }
    }

    //return the URL of the package
    public String addGroupOfResources(String ckanBaseUri, String packageName, ObjectNode json, Set<Exporter> exporters, Project project,
                                      Engine engine, ProvenanceFactory provFactory, String apikey, boolean createNewIfNonExisitng, boolean provenanceRequired) throws IOException {
        Slugify slg = new Slugify();
        String packageId = slg.slugify(packageName);
        Map<String, String> resourceFormatsUrlsMap = new HashMap<String, String>();
        String packageUrl = ckanBaseUri + "/dataset/" + packageId;
        Set<Resource> resources = new HashSet<Resource>();
        if (!exists(packageUrl)) {
            if (createNewIfNonExisitng) {
                //create a new package
                registerNewPackage(packageName, json, ckanBaseUri, apikey);
            } else {
                //fail
                throw new RuntimeException("Package with id " + packageId + " does not exist on " + ckanBaseUri);
            }
        }
        StorageApiProxy storage = new StorageApiProxy();

        String seed = "-" + getRandomString(project.id);
        for (Exporter exporter : exporters) {
            String url;

            String project_name = project.getMetadata().getName();
            String format = translateFileFormat(exporter.getContentType());
            String filename = json.get("name").toString();

            if (format.equalsIgnoreCase("JSON")) {
                filename += " Operations History";
            }


            Slugify slg2 = new Slugify(false);
            File temp_file = createTempFile(slg2.slugify(filename), format);

            ObjectNode resource_json = ParsingUtilities.mapper.createObjectNode();
            JSONUtilities.safePut(resource_json, "package_id", packageId);
            JSONUtilities.safePut(resource_json, "description", json.get("description"));
            JSONUtilities.safePut(resource_json, "name", filename);
            JSONUtilities.safePut(resource_json, "format", format.toUpperCase());
            JSONUtilities.safePut(resource_json, "url", "");

            if (exporter instanceof WriterExporter) {
                StringWriter out = new StringWriter();
                ((WriterExporter) exporter).export(project, new Properties(), engine, out);
                BufferedWriter bw = new BufferedWriter(new FileWriter(temp_file));
                bw.write(out.toString());
                bw.close();
                url = storage.resourceCreate(ckanBaseUri, apikey, temp_file, resource_json);
            } else if (exporter instanceof StreamExporter) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ((StreamExporter) exporter).export(project, new Properties(), engine, out);
                BufferedWriter bw = new BufferedWriter(new FileWriter(temp_file));
                bw.write(out.toString());
                bw.close();
                url = storage.resourceCreate(ckanBaseUri, apikey, temp_file, resource_json);
            } else {
                temp_file.deleteOnExit();
                throw new RuntimeException("Unknown exporter type");
            }
            temp_file.deleteOnExit();
        }

        return packageUrl;
    }

    private Resource getProvenance(Map<String, String> resourceFormatsUrlsMap, StorageApiProxy storage, ProvenanceFactory provFactory, String provFileLabel, String apikey) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        //build the RDF
        Model model = provFactory.getProvenance(resourceFormatsUrlsMap.get("application/x-unknown"), resourceFormatsUrlsMap.get("text/turtle"),
                resourceFormatsUrlsMap.get("application/json"), calendar);
        //upload the RDF data to CKAN Storage
        StringWriter sw = new StringWriter();
        model.write(sw, null, "TURTLE");
        sw.flush();
        String url = storage.uploadFile(sw.toString(), provFileLabel, apikey);
        //build and return a representing resource
        String description = "RDF provenance description (from Google Refine)";
        return new Resource("text/turtle", description, url);
    }

    private String getRandomString(long id) {
        return String.valueOf(id) + "-" + System.currentTimeMillis();
    }

    private Resource getCkanResource(String format, String url, String packageId) {
        //Google Refine CSV exporter returns "application/x-unknown" as format I'll replace this with text/csv as it is more intuitive
        if (format.equals("application/x-unknown")) {
            format = "text/csv";
        }
        String description = translate(format) + " (from Google Refine)";
        return new Resource(format, description, url);
    }

    private ObjectNode getNewPackageInJson(String packageName, ObjectNode options) throws IOException {
        ObjectNode obj = ParsingUtilities.mapper.createObjectNode();
        Slugify slg = new Slugify();
        obj.put("name", slg.slugify(packageName));
        obj.put("title", packageName);
        obj.put("notes", "This package was created using Google Refine extension.");
        if (options.has("owner_org")) {
            obj.put("owner_org", options.get("owner_org"));
        }

        return obj;
    }

    //provide a more friendly label for formate e.g. text/csv ==> CSV table
    private String translate(String format) {
        return formatLabels.get(format);
    }

    private String translateFileFormat(String format) {
        return fileFormats.get(format);
    }

    private static final Map<String, String> formatLabels = new HashMap<String, String>();

    static {
        formatLabels.put("text/plain", "CSV table");
        formatLabels.put("text/csv", "CSV table");
        formatLabels.put("application/json", "Operation history");
        formatLabels.put("text/turtle", "RDF data");
    }

    private static final Map<String, String> fileFormats = new HashMap<String, String>();

    static {
        fileFormats.put("text/plain", "csv");
        fileFormats.put("text/csv", "csv");
        fileFormats.put("application/json", "json");
        fileFormats.put("text/turtle", "rdf");
    }

    public File createTempFile(String prefix, String suffix) {
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = (prefix != null ? prefix : "") + (suffix != null ? "." + suffix : "");
        return new File(tempDir, fileName);
    }


}
