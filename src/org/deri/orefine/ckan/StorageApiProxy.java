package org.deri.orefine.ckan;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.util.ParsingUtilities;

import java.util.Iterator;
import java.io.File;


public class StorageApiProxy {

    public String uploadFile(String fileContent, String fileLabel, String apikey) {
        HttpResponse formFields = null;
        try {
            String filekey = null;
            HttpClient client = new DefaultHttpClient();

            //	get the form fields required from ckan storage
            String formUrl = CKAN_STORAGE_BASE_URI + "/auth/form/file/" + fileLabel;
            HttpGet getFormFields = new HttpGet(formUrl);
            getFormFields.setHeader("Authorization", apikey);
            formFields = client.execute(getFormFields);
            HttpEntity entity = formFields.getEntity();
            if (entity != null) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                entity.writeTo(os);

                //now parse JSON
//				JSONObject obj = new JSONObject(os.toString());
//				ObjectNode obj = ParsingUtilities.mapper.readValue(os.toString(), ObjectNode.class);
                ObjectNode obj = ParsingUtilities.evaluateJsonStringToObjectNode(os.toString());

                //post the file now
                String uploadFileUrl = obj.get("action").asText();
                HttpPost postFile = new HttpPost(uploadFileUrl);
                postFile.setHeader("Authorization", apikey);
                MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.STRICT);

//				JSONArray fields = obj.getJSONArray("fields");
                ArrayNode fields = (ArrayNode) obj.get("fields");
//				for(int i=0;i<fields.length();i++){
                for (int i = 0; i < fields.size(); i++) {
//					JSONObject fieldObj = fields.getJSONObject(i);
                    ObjectNode fieldObj = (ObjectNode) fields.get(i);
//					String fieldName = fieldObj.getString("name");
//					String fieldValue = fieldObj.getString("value");
                    String fieldName = fieldObj.get("name").asText();
                    String fieldValue = fieldObj.get("value").asText();
                    if (fieldName.equals("key")) {
                        filekey = fieldValue;
                    }
                    mpEntity.addPart(fieldName, new StringBody(fieldValue, "multipart/form-data", Charset.forName("UTF-8")));
                }

                //	assure that we got the file key
                if (filekey == null) {
                    throw new RuntimeException("failed to get the file key from CKAN storage form API. the response from " + formUrl + " was: " + os.toString());
                }

                //the file should be the last part
                //hack... StringBody didn't work with large files
                mpEntity.addPart("file", new ByteArrayBody(fileContent.getBytes(Charset.forName("UTF-8")), "multipart/form-data", fileLabel));

                postFile.setEntity(mpEntity);

                HttpResponse fileUploadResponse = client.execute(postFile);

                //check if the response status code was in the 200 range
                if (fileUploadResponse.getStatusLine().getStatusCode() < 200 || fileUploadResponse.getStatusLine().getStatusCode() >= 300) {
                    throw new RuntimeException("failed to add the file to CKAN storage. response status line from " + uploadFileUrl + " was: " + fileUploadResponse.getStatusLine());
                }

                return CKAN_STORAGE_FILES_BASE_URI + filekey;
            }
            throw new RuntimeException("failed to get form details from CKAN storage. response line was: " + formFields.getStatusLine());
//		}catch(JSONException je){
//			throw new RuntimeException("failed to upload file to CKAN Storage. A wrong API key maybe? ",je);
        } catch (IOException ioe) {
            throw new RuntimeException("failed to upload file to CKAN Storage ", ioe);
        }
    }

    //	public String resourceCreate(String ckanBaseUri,String apikey, File file,  JSONObject resource_json) {
    public String resourceCreate(String ckanBaseUri, String apikey, File file, ObjectNode resource_json) throws IOException {
        HttpResponse formFields = null;
        try {
            String filekey = null;

            // Configure Timeout Parameters
            HttpParams httpParameters = new BasicHttpParams();
            int timeoutConnection = 10000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 5000000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

            HttpClient client = new DefaultHttpClient(httpParameters);


            String formUrl = ckanBaseUri + "/api/action/resource_create";

            //post the file now
            //String uploadFileUrl = obj.getString("action");
            HttpPost postFile = new HttpPost(formUrl);
            postFile.setHeader("Authorization", apikey);

            MultipartEntity mpEntity = new MultipartEntity(HttpMultipartMode.STRICT);

            //the file should be the last part
            //hack... StringBody didn't work with large files
            System.out.println("Uploading : " + file.getName() + "...");

            mpEntity.addPart("upload", new FileBody(file, "application/octet-stream"));
            // mpEntity.addPart("package_id",  new StringBody(packageId));
            // mpEntity.addPart("name",  new StringBody(file.getName()));
            // mpEntity.addPart("url", new StringBody(""));

//                Iterator<String> iter = resource_json.keys();
//                while (iter.hasNext()) {
//                    String key = iter.next();
//                    try {
//                        String string_value = resource_json.get(key).toString();
//                        mpEntity.addPart(key, new StringBody(string_value));
//                    } catch (JSONException e) {
//                        throw new RuntimeException("something wrong with resource_create options",e);
//                    }
//                }

            Iterator<Entry<String, JsonNode>> iter = resource_json.fields();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) iter.next();
                String string_value = entry.getValue().asText();
                mpEntity.addPart(entry.getKey(), new StringBody(string_value));
            }

            postFile.setEntity(mpEntity);

            HttpResponse fileUploadResponse = client.execute(postFile);

            //check if the response status code was in the 200 range
            if (fileUploadResponse.getStatusLine().getStatusCode() < 200 || fileUploadResponse.getStatusLine().getStatusCode() >= 300) {
                throw new RuntimeException("failed to add the file to CKAN storage. response status line from " + formUrl + " was: " + fileUploadResponse.getStatusLine());
            }
//                try {
            return ckanBaseUri + "/dataset/" + resource_json.get("package_id").toString();
//                }
//                catch (JSONException e) {
//                    throw new RuntimeException("something wrong with resource_create options (package_id)",e);
//                }
        } catch (IOException ioe) {
            throw new RuntimeException("failed to upload file to CKAN Storage ", ioe);
        }
    }


    private static final String CKAN_STORAGE_BASE_URI = "http://datahub.io/api/storage";
    private static final String CKAN_STORAGE_FILES_BASE_URI = "http://datahub.io/storage/f/";

}
