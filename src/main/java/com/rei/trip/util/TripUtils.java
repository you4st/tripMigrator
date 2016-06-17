package com.rei.trip.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;

/**
 * Utility class for trip API.
 *
 * @author sahan
 */
public class TripUtils {

    public static String getFileContent(String url) throws IOException {
        InputStream input = new URL(url).openStream();
        String content = IOUtils.toString(input);
        IOUtils.closeQuietly(input);

        return content;
    }
    /**
     * Gets json object from xml
     */
    public static JSONObject getJsonFromXml(String xmlString) throws JSONException {
        return XML.toJSONObject(xmlString);
    }

    /**
     * writes json file from json object
     */
    public static void writeJsonToFile(String filename, JSONObject obj) throws IOException, JSONException {

        File file = new File(TripConstants.TRIP_JSON_PATH + filename);

        if (!file.exists()) {
            file.createNewFile();
        }
        ObjectMapper mapper = new ObjectMapper();
        Object json = mapper.readValue(obj.toString(), Object.class);
        String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        FileWriter writer = new FileWriter(file);
        writer.write(jsonStr);
        writer.flush();
        writer.close();
    }

    /**
     * creates directory for json files
     */
    public static void createDirectory(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * Casts JSONObject to JSONArray
     */
    public static JSONArray castToJSONArray(Object obj) {
        JSONArray jsonArray = new JSONArray();

        if (obj instanceof JSONArray) {
            jsonArray = (JSONArray) obj;
        } else {
            jsonArray.put(obj);
        }

        return jsonArray;
    }

    public static String toCamelCase(String s) {
        String[] parts = s.split(" ");
        String camelCaseString = "";
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                camelCaseString = part.toLowerCase();
            } else {
                camelCaseString = camelCaseString + toProperCase(part);
            }
        }
        return camelCaseString;
    }

    public static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }
}
