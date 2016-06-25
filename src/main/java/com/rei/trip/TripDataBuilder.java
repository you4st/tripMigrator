package com.rei.trip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rei.trip.exception.DataBuilderException;
import com.rei.trip.util.TripConstants;
import com.rei.trip.util.TripUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Trip data(JSON) builder.
 * This creates JSON files under local drive(/tripJson) from xml on Documentum
 *
 * @author sahan
 */
public class TripDataBuilder {
    private Map<String, JSONObject> tripJsons;
    private Map<String, String> tripXmls;
    private List<String> extensions;
    private JSONObject list;

    public TripDataBuilder() {
    }

    /**
     * Process a xml to json.
     *
     * @throws DataBuilderException
     */
    public void buildAllTrips() throws DataBuilderException {
        extensions = new ArrayList<>();
        list = buildTripList();

        try {
            buildTripData(list.getJSONArray("tripList"));
        } catch (JSONException e) {
            throw new DataBuilderException(e.getMessage());
        }
    }

    /**
     * Builds the trip list from xml.
     */
    private JSONObject buildTripList() throws DataBuilderException {
        String listUrl = TripConstants.DOC_BASE + TripConstants.DOC_TRIP_LIST;
        tripJsons = new HashMap<>();
        tripXmls = new HashMap<>();

        try {
            JSONObject tripListOld = TripUtils.getJsonFromXml(TripUtils.getFileContent(listUrl))
                    .getJSONObject("tripList");
            JSONArray regionsOld = tripListOld.getJSONArray("region");
            JSONArray regionsNew = new JSONArray();

            for (int i = 0; i < regionsOld.length(); i++) {
                JSONObject regionOld = regionsOld.getJSONObject(i);
                JSONObject regionNew = new JSONObject();
                JSONObject tripArrayObj = regionOld.getJSONObject("trips");
                JSONArray tripArrayOld = tripArrayObj.getJSONArray("trip");
                JSONArray tripArrayNew = new JSONArray();
                for (int j = 0; j < tripArrayOld.length(); j++) {
                    JSONObject tripInfo = getTripId(tripArrayOld.getString(j));
                    tripArrayNew.put(tripInfo);
                }
                String regionName = processRegionName(regionOld.getString("regionName"));
                regionNew.put("regionName", TripUtils.toCamelCase(regionName));
                regionNew.put("trips", tripArrayNew);
                regionsNew.put(regionNew);
            }

            JSONObject tripListNew = new JSONObject();
            tripListNew.put("tripList", regionsNew);
            TripUtils.writeJsonToFile(TripConstants.TRIP_LIST_FILENAME, tripListNew);

            return tripListNew;
        } catch (JSONException e) {
            throw new DataBuilderException(e.getMessage());
        } catch (IOException e) {
            throw new DataBuilderException(e.getMessage());
        }
    }

    private String processRegionName(String regionName) {
        // replace "Australia & New Zealand" to "pacific"
        if (regionName.equals("Australia & New Zealand")) {
            regionName = "pacific";
        }
        // replace "Antarctica" to "polar"
        if (regionName.equalsIgnoreCase("Antarctica")) {
            regionName = "polar";
        }

        return regionName;
    }

    private String getRegionPath(String path) {
        return path.substring(0, path.lastIndexOf('/') + 1);
    }

    private JSONObject getTripId(String path) throws DataBuilderException {
        JSONObject trip = new JSONObject();

        try {
            String tripUrl = TripConstants.DOC_BASE + path;
            String tripXml = TripUtils.getFileContent(tripUrl);
            JSONObject tripJson = TripUtils.getJsonFromXml(tripXml).getJSONObject("trip_id");
            if (tripJson != null) {
                String tripId = tripJson.getString("trip_id_number");
                tripJsons.put(tripId, tripJson);
                tripXmls.put(tripId, tripXml);

                trip.put("path", getRegionPath(path));
                trip.put("tripId", tripId);
                trip.put("tripLegacyId", path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.')));
            }
        } catch (JSONException e) {
            throw new DataBuilderException(e.getMessage());
        } catch (IOException e) {
            throw new DataBuilderException(e.getMessage());
        }

        return trip;
    }

    private void buildTripData(JSONArray list) throws DataBuilderException {
        for (int i = 0; i < list.length(); i++) {
            try {
                JSONObject region = list.getJSONObject(i);
                processBagicInfo(region);
                processDetailInfo(region);
                processGallery(region);
            } catch (JSONException e) {
                throw new DataBuilderException(e.getMessage());
            }
        }
        processExtension();
    }

    private void processBagicInfo(JSONObject region) throws DataBuilderException {
        try {
            String regionName = region.getString("regionName");
            JSONArray regionTrips = region.getJSONArray("trips");

            for (int i = 0; i < regionTrips.length(); i++) {
                JSONObject trip = regionTrips.getJSONObject(i);
                String tripPath = trip.getString("path");
                String tripId = trip.getString("tripId");
                String tripLegacyId = trip.getString("tripLegacyId");

                createBasicJson(tripPath, regionName, tripId, tripLegacyId, null);
            }
        } catch (JSONException e) {
            throw new DataBuilderException(e.getMessage());
        }
    }

    private String getShortSummaryFromXml(String tripId) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        String shortSummary = "";

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(tripXmls.get(tripId).getBytes()));
            Element ele = (Element) doc.getElementsByTagName("trip_listing_summary").item(0);
            shortSummary = processTripSummary(ele);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return shortSummary;
    }

    private void processDetailInfo(JSONObject region) throws DataBuilderException {
        try {
            String regionName = region.getString("regionName");
            JSONArray regionTrips = region.getJSONArray("trips");

            for (int i = 0; i < regionTrips.length(); i++) {
                JSONObject trip = regionTrips.getJSONObject(i);
                String tripPath = trip.getString("path");
                String tripId = trip.getString("tripId");
                createDetailJson(tripPath, regionName, tripId);
            }

        } catch (JSONException e) {
            throw new DataBuilderException(e.getMessage());
        }
    }

    private Document getXmlDocument(String tripPath, String tripId, String type) {
        String url = TripConstants.DOC_BASE + tripPath + tripId + type;
        Document doc = null;
        try {
            URL loc = new URL(url);
            URLConnection urlConnection = loc.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(in);
            doc.getDocumentElement().normalize();
        } catch (IOException e) {
            //e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        return doc;
    }

    private JSONArray processGear(String tripPath, String tripId) throws DataBuilderException {
        try {
            Document doc = getXmlDocument(tripPath, tripId, TripConstants.DOC_GEAR_FILE_NAME);
            JSONArray gearContents = new JSONArray();
            if (doc != null) {
                NodeList list = doc.getElementsByTagName("section");
                for (int j = 0; j < list.getLength(); j++) {
                    Element ele = (Element) list.item(j);
                    NodeList children = ele.getElementsByTagName("header");
                    for (int k = 0; k < children.getLength(); k++) {
                        Node child = children.item(k);
                        String key = child.getAttributes().getNamedItem("type").getNodeValue();
                        String value = child.getTextContent();
                        if (key.equals("selector") &&
                                !(value.equals("Gear Checklist") || value.equals("Things to Consider"))) {
                            String content = "";
                            NodeList copy = ele.getElementsByTagName("p");
                            for (int l = 0; l < copy.getLength(); l++) {
                                content += TripUtils.nodeToString(copy.item(l)).
                                        replaceAll("\\r|\\n|\\t", "").replaceAll("\\s+", " ");
                            }
                            if (value.length() > 0 && content.length() > 0) {
                                JSONObject misc = new JSONObject();
                                misc.put("title", value);
                                misc.put("content", content);
                                gearContents.put(misc);
                            }
                        }
                    }
                }
            }

            return gearContents;
        } catch (JSONException e) {
            throw new DataBuilderException(e.getMessage());
        }
    }

    private void processExtension() throws DataBuilderException {
        try {
            String url = TripConstants.DOC_BASE + "/adventures/data/" + TripConstants.DOC_EXT_LIST_FILE_NAME;
            String doc = TripUtils.getFileContent(url);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(TripUtils.getJsonFromXml(doc).toString())
                    .get("cross-sell-list").get("cross-sell");
            Map<String, List<Map<String, Object>>> map = new HashMap<>();
            List<Map<String, Object>> extList = new ArrayList<>();
            List<Map<String, Object>> relatedList = new ArrayList<>();

            json.forEach(ext -> {
                String type = ext.get("type").textValue();
                String[] code = ext.get("cross-sell-trip").textValue().split("/");
                String region = code[0];
                String tripLegacyId = code[1];
                String tripId = getTripIdFromTripList(region, tripLegacyId);

                if (StringUtils.isBlank(tripId) && !tripLegacyId.contains("_dir")) {
                    // no trip data, let's create it.
                    String tripPath = "/adventures/trips/" + region + "/";
                    String tripUrl = TripConstants.DOC_BASE + tripPath + tripLegacyId + ".xml";
                    String tripXml = null;
                    try {
                        tripXml = TripUtils.getFileContent(tripUrl);
                        JSONObject tripJson = TripUtils.getJsonFromXml(tripXml).getJSONObject("trip_id");
                        if (tripJson != null) {
                            tripId = tripJson.getString("trip_id_number");
                            System.out.println("creating extension trip: " + region + ":" + tripId);
                            tripJsons.put(tripId, tripJson);
                            tripXmls.put(tripId, tripXml);
                            createBasicJson(tripPath, getRegionName(region), tripId, tripLegacyId, "regional");
                            createDetailJson(tripPath, getRegionName(region), tripId);
                            createGalleryJson(tripPath, getRegionName(region), tripId);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                // manually set the tripId for embedded extension
                if (StringUtils.isBlank(tripId)) {
                    switch (tripLegacyId) {
                        case "gan_dir":
                        case "gis_dir":
                            tripId = "npo";
                            break;
                        case "thm_dir":
                            tripId = "ths";
                            break;
                        case "fta_dir":
                            tripId = "ftk";
                            break;
                        case "vie_dir":
                            tripId = "hal";
                            break;
                        case "cro_dir":
                            tripId = "dbv";
                            break;
                        case "cyc_dir":
                            tripId = "ddc";
                            break;
                    }
                }

                Map<String, Object> item = new HashMap<>();
                item.put("crossSellTrip", tripId);
                List<String> mainTrips = new ArrayList<>();
                JsonNode node = ext.get("main-trips").get("main-trip");
                if (node.isArray()) {
                    node.forEach(main -> {
                        String[] mainCode = main.textValue().split("/");
                        mainTrips.add(getTripIdByTripLegacyId(mainCode[0], mainCode[1]));
                    });
                } else {
                    String[] mainCode = node.textValue().split("/");
                    mainTrips.add(getTripIdByTripLegacyId(mainCode[0], mainCode[1]));
                }
                item.put("mainTrips", mainTrips);

                if (type.equals("related")) {
                    relatedList.add(item);
                } else {
                    extList.add(item);
                }

            });

            map.put("extension", extList);
            map.put("related", relatedList);

            JSONObject newExtension = new JSONObject(map);
            TripUtils.writeJsonToFile("crossSellList.json", newExtension);
        } catch (Exception e) {
            throw new DataBuilderException(e.getMessage());
        }
    }

    private String getTripIdByTripLegacyId(String oRegion, String tripLegacyId) {
        try {
            String tripPath = "/adventures/trips/" + oRegion + "/";
            String tripUrl = TripConstants.DOC_BASE + tripPath + tripLegacyId + ".xml";
            String tripXml = TripUtils.getFileContent(tripUrl);
            JSONObject tripJson = TripUtils.getJsonFromXml(tripXml).getJSONObject("trip_id");

            if (tripJson.has("trip_id_number")) {
                return tripJson.getString("trip_id_number");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getTripIdFromTripList(String oRegion, String tripLegacyId) {
        try {
            String regionName = getRegionName(oRegion);
            JSONArray regions = list.getJSONArray("tripList");
            for (int i = 0; i < regions.length(); i++) {
                JSONObject region = regions.getJSONObject(i);
                if (region.getString("regionName").equals(regionName)) {
                    JSONArray trips = region.getJSONArray("trips");
                    for (int j = 0; j < trips.length(); j++) {
                        JSONObject trip = trips.getJSONObject(j);
                        if (trip.getString("tripLegacyId").equals(tripLegacyId)) {
                            return trip.getString("tripId");
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getRegionName(String oRegion) {
        switch (oRegion) {
            case "antarctica":
                return "polar";
            case "latin":
                return "latinAmerica";
            case "namer":
            case "weekend":
                return "northAmerica";
            default:
                return oRegion;
        }
    }

    private void processGallery(JSONObject region) throws DataBuilderException {
        try {
            String regionName = region.getString("regionName");
            JSONArray regionTrips = region.getJSONArray("trips");

            for (int i = 0; i < regionTrips.length(); i++) {
                JSONObject trip = regionTrips.getJSONObject(i);
                String tripPath = trip.getString("path");
                String tripId = trip.getString("tripId");

                createGalleryJson(tripPath, regionName, tripId);
            }
        } catch (JSONException e) {
            throw new DataBuilderException(e.getMessage());
        }
    }

    private String getTypedContent(Object jsonObj) throws DataBuilderException {
        if (jsonObj instanceof JSONObject) {
            try {
                Object obj = ((JSONObject) jsonObj).get("content");

                return obj.toString();
            } catch (JSONException e) {
                throw new DataBuilderException(e.getMessage());
            }
        }

        return jsonObj.toString();
    }

    private JSONObject processImages(String region, String tripId) {
        Map<String, String> images = new LinkedHashMap<>();
        String fileName = TripConstants.TRIP_IMG_ASSETS_BASE + "core/" + region + "/" + tripId;
        images.put("cardImageUrl", fileName + "_card.jpg");
        images.put("heroImageUrl", fileName + "_hero.jpg");
        return new JSONObject(images);
    }

    private JSONObject processVideo(JSONObject gallery, JSONObject videoObj) throws DataBuilderException {
        try {
            JSONArray galleryItems = new JSONArray();
            Map<String, String> video = new LinkedHashMap<>();
            video.put("url", videoObj.getString("embed_code"));
            video.put("thumbnail", videoObj.getString("thumbnail_code"));
            if (videoObj.has("h2")) {
                video.put("title", videoObj.getString("h2"));
            }
            video.put("type", "video");
            galleryItems.put(new JSONObject(video));
            Map<String, Object> newGallery = new LinkedHashMap<>();
            newGallery.put("cardImageUrl", gallery.getString("cardImageUrl"));
            newGallery.put("heroImageUrl", gallery.getString("heroImageUrl"));
            newGallery.put("galleryItems", galleryItems);
            return new JSONObject(newGallery);
        } catch (JSONException e) {
            throw new DataBuilderException(e.getMessage());
        }
    }

    private List<String> processDestinationTypes(JSONObject list) throws DataBuilderException {
        try {
            String pageStr = list.getString("page");
            List<String> pages = Arrays.asList(pageStr.split("\\s*,\\s*"));
            return pages;
        } catch (JSONException e) {
            throw new DataBuilderException(e.getMessage());
        }
    }

    private List<String> processPrimaryActivities(String activities) {
        return Arrays.asList(activities.split("\\s*,\\s*")).stream()
            .filter(activity -> !activity.contains("Multisport"))
            .collect(Collectors.toList());
    }

    private JSONArray processActivities(JSONObject activities) throws DataBuilderException {
        try {
            return TripUtils.castToJSONArray(activities.get("activity"));
        } catch (JSONException e) {
            throw new DataBuilderException(e.getMessage());
        }
    }

    private JSONArray processPrice(JSONObject tripCosts) throws DataBuilderException {
        try {
            JSONArray prices = new JSONArray();
            JSONArray costs = TripUtils.castToJSONArray(tripCosts.get("cost"));

            for (int i = 0; i < costs.length(); i++) {
                JSONObject cost = costs.getJSONObject(i);
                if (cost.has("costDescription")) {
                    if (cost.getString("costDescription").equals("Additional Fees")) {
                        JSONArray additionalFees = TripUtils.castToJSONArray(cost.get("price"));
                        for (int j = 0; j < additionalFees.length(); j++) {
                            JSONObject fee = additionalFees.getJSONObject(j);
                            Map<String, String> feeNew = new LinkedHashMap<>();
                            feeNew.put("type", "additionalFee");
                            if (fee.has("priceDescription")) {
                                feeNew.put("priceDescription",
                                        fee.getString("priceDescription"));
                            }
                            if (fee.has("amount")) {
                                feeNew.put("amount", fee.getString("amount"));
                            }
                            if (fee.has("note")) {
                                feeNew.put("note", fee.getString("note"));
                            }
                            prices.put(new JSONObject(feeNew));
                        }
                    }
                    if (cost.getString("costDescription").equals("Options")) {
                        JSONArray options = TripUtils.castToJSONArray(cost.get("price"));
                        for (int k = 0; k < options.length(); k++) {
                            JSONObject option = options.getJSONObject(k);
                            Map<String, String> optionNew = new LinkedHashMap<>();
                            optionNew.put("type", "option");
                            if (option.has("priceDescription")) {
                                optionNew.put("priceDescription",
                                        option.getString("priceDescription"));
                            }
                            if (option.has("amount")) {
                                optionNew.put("amount", option.getString("amount"));
                            }
                            if (option.has("note")) {
                                optionNew.put("note", option.getString("note"));
                            }

                            prices.put(new JSONObject(optionNew));
                        }
                    }
                }
                if (cost.has("type")) {
                    if (cost.getString("type").equals("defaultPrice")) {
                        Map<String, String> defaultPrice = new LinkedHashMap<>();
                        defaultPrice.put("type", "default");
                        if (cost.getJSONObject("price").has("priceDescription")) {
                            defaultPrice.put("priceDescription",
                                cost.getJSONObject("price").getString("priceDescription"));
                        }
                        defaultPrice.put("amount", cost.getJSONObject("price").getString("amount"));
                        prices.put(new JSONObject(defaultPrice));
                    }
                }
                if (cost.has("rel") && cost.has("def")) {
                    if (cost.getString("rel").equals("trip") && cost.getString("def").equals("static")) {
                        if (cost.has("price")) {
                            JSONArray priceArray = TripUtils.castToJSONArray(cost.get("price"));

                            for (int j = 0; j < priceArray.length(); j++) {
                                JSONObject priceOld = priceArray.getJSONObject(j);
                                if (priceOld.has("amount") && !priceOld.getString("amount").equals("{}")) {
                                    Map<String, String> price = new LinkedHashMap<>();
                                    if (cost.has("costDescription")) {
                                        price.put("year", cost.getString("costDescription"));
                                    } else if (cost.has("heading")) {
                                        price.put("year", cost.getString("heading"));
                                    }
                                    price.put("type", priceOld.getString("type"));
                                    if (priceOld.has("priceDescription")) {
                                        price.put("priceDescription", priceOld.getString("priceDescription"));
                                    }
                                    price.put("amount", priceOld.getString("amount"));
                                    prices.put(new JSONObject(price));
                                }
                            }
                        }
                    }
                }
            }

            return prices;
        } catch (JSONException e) {
            throw new DataBuilderException(e.getMessage());
        }
    }

    private JSONArray processDates(String region, String id) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(tripXmls.get(id).getBytes()));
            NodeList list = doc.getElementsByTagName("trip_dates");
            JSONArray tripDates = new JSONArray();

            for (int i = 0; i < list.getLength(); i++) {
                Element date = (Element) list.item(i);
                Map<String, Object> tripDate = new LinkedHashMap<>();

                if (date.hasAttribute("year")) {
                    tripDate.put("year", date.getAttribute("year").toString());
                    if (date.hasAttribute("price")) {
                        tripDate.put("priceFrom", date.getAttribute("price").toString());
                    }

                    // departures
                    NodeList departures = date.getElementsByTagName("departure");
                    JSONArray schedules = new JSONArray();

                    for (int j = 0; j < departures.getLength(); j++) {
                        Element departure = (Element) departures.item(j);
                        Map<String, Object> schedule = new LinkedHashMap<>();
                        schedule.put("startDate",
                                departure.getElementsByTagName("start").item(0).getTextContent());
                        schedule.put("endDate",
                                departure.getElementsByTagName("end").item(0).getTextContent());
                        Element dNote = (Element) departure.getElementsByTagName("note").item(0);
                        schedule.put("status", "open");

                        if (dNote != null) {
                            String noteStr = dNote.getTextContent();
                            if (noteStr.indexOf("Sold out") != -1) {
                                schedule.put("status", "Sold out");
                            }
                            List<String> dNotes = new ArrayList<>();
                            if (noteStr.indexOf('H') != -1) {
                                dNotes.add("H");
                            }
                            if (noteStr.indexOf('R') != -1) {
                                dNotes.add("R");
                            }
                            if (noteStr.indexOf('W') != -1) {
                                dNotes.add("W");
                            }
                            if (noteStr.indexOf('*') != -1) {
                                dNotes.add("*");
                            }
                            if (dNotes.size() > 0) {
                                schedule.put("note", new JSONArray(dNotes));
                            }
                        }
                        schedules.put(new JSONObject(schedule));
                    }

                    tripDate.put("departures", schedules);

                    // note
                    NodeList childrenList = date.getChildNodes();
                    List<String> notes = new ArrayList<>();
                    for (int k = 0; k < childrenList.getLength(); k++) {
                        Node node = childrenList.item(k);
                        if (node.getNodeName().equals("note")) {
                            String note = TripUtils.nodeToString(node).replaceAll("\\r|\\n|\\t", "");
                            if (note.length() > 13) {
                                notes.add(note.substring(6, note.length() - 7));
                            }
                        }
                    }
                    if (notes.size() > 0) {
                        tripDate.put("note", notes);
                    }
                }

                tripDates.put(new JSONObject(tripDate));
            }

            return tripDates;
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String processShortSummary(String tripPath, String tripId) throws JSONException {
        Document doc = getXmlDocument(tripPath, tripId, TripConstants.DOC_DETAIL_FILE_NAME);
        String summary = "";

        if (doc != null) {
            if (doc.getElementsByTagName("trip_summary").getLength() > 0) {
                Element ele = (Element) doc.getElementsByTagName("trip_summary").item(0);
                NodeList list = ele.getElementsByTagName("p");
                for (int i = 0; i < list.getLength(); i++) {
                    Node node = list.item(i);
                    Node nodeWithClass = node.getAttributes().getNamedItem("class");
                    String content = node.getTextContent();
                    if (nodeWithClass != null || StringUtils.isBlank(content)) {
                        continue;
                    } else {
                        summary = content;
                        break;
                    }
                }
            }
        }

        return summary;
    }

    private String processTripSummary(Element ele) {
        NodeList list = ele.getElementsByTagName("p");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.getLength(); i++) {
            builder.append(TripUtils.nodeToString(list.item(i)).replaceAll("\\r|\\n|\\t", ""));

            if (i < list.getLength() - 1) {
                builder.append('\n');
            }
        }

        return builder.toString();
    }

    private List<String> processTripHighlight(Element ele) {
        Element ul = (Element) ele.getElementsByTagName("ul").item(0);
        List<String> highlight = new ArrayList<>();

        if (ul != null) {
            NodeList list = ul.getElementsByTagName("li");

            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                highlight.add(node.getTextContent());
            }
        }

        return highlight;
    }

    private JSONObject processMap(Element map) throws DataBuilderException {
        NodeList list = map.getElementsByTagName("map_image");
        JSONObject json = new JSONObject();

        if (list.getLength() > 0) {
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                try {
                    String key = node.getAttributes().getNamedItem("type").getNodeValue();
                    String value = node.getTextContent();
                    json.put(key, value);
                } catch (JSONException e) {
                    throw new DataBuilderException(e.getMessage());
                }
            }
        }

        return json;
    }

    private JSONObject processItinerary(String itineraryNote, Element ele) {
        NodeList list = ele.getElementsByTagName("details");
        JSONObject json = new JSONObject();

        if (list.getLength() > 0) {
            try {
                List<JSONObject> items = new ArrayList<>();
                for (int i = 0; i < list.getLength(); i++) {
                    Map<String, String> item = new LinkedHashMap<>();
                    Element itinerary = (Element) list.item(i);
                    item.put("heading", itinerary.getElementsByTagName("heading").item(0).getTextContent().trim());
                    item.put("subheading", itinerary.getElementsByTagName("subhead").item(0).getTextContent().trim());

                    String description = "";
                    NodeList copy = itinerary.getElementsByTagName("p");
                    for (int j = 0; j < copy.getLength(); j++) {
                        description += TripUtils.nodeToString(copy.item(j)).replaceAll("\\r|\\n", "").replaceAll("\\s+", " ");
                    }
                    item.put("description", description);
                    items.add(new JSONObject(item));
                }
                json.put("dailyItinerary", items);
                json.put("note", itineraryNote);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return json;
    }

    private Map<String, JSONObject> processAdditionalInfo(Element info) {

        Map<String, JSONObject> additionalInfo = new HashMap<>();
        NodeList list = info.getElementsByTagName("item");

        for (int i = 0; i < list.getLength(); i++) {
            Element item = (Element) list.item(i);
            String title = item.getElementsByTagName("title").item(0).getTextContent();
            String content = "";
            NodeList copy = item.getElementsByTagName("p");
            for (int j = 0; j < copy.getLength(); j++) {
                content += TripUtils.nodeToString(copy.item(j)).replaceAll("\\r|\\n", "").replaceAll("\\s+", " ");
            }
            JSONObject json = new JSONObject();

            try {
                json.put("title", title);
                json.put("content", content);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String key = TripUtils.toCamelCase(title);

            if (title.equals("Special Payment, Cancellation, and Transfer Policy") ||
                    title.equals("Special Payment and Cancellation Policy")) {
                key = "specialPolicy";
            }

            if (title.equals("Medical & Evacuation Insurance")) {
                key = "medicalInsurance";
            }

            if (title.equals("Galapagos National Park Fee & Transit Card")) {
                key = "galapagosNationalParkFee";
            }

            if (StringUtils.isNotBlank(key)) {
                additionalInfo.put(key, json);
            }
        }

        return additionalInfo;
    }

    private void createBasicJson(
            String tripPath, String regionName, String tripId, String tripLegacyId, String extensionType) {

        JSONObject tripOld = tripJsons.get(tripId);
        Map<String, Object> tripNew = new LinkedHashMap<>();

        try {
            tripNew.put("tripId", tripId);
            tripNew.put("tripTitle", getTypedContent(tripOld.get("trip_title")));
            tripNew.put("tripSubtitle", "");
            tripNew.put("region", regionName);
            if (tripOld.has("country")) {
                tripNew.put("country", tripOld.getString("country"));
            }
            if (tripOld.has("state")) {
                tripNew.put("state", tripOld.getString("state"));
            }
            tripNew.put("pageTitle", getTypedContent(tripOld.get("page_title")));
            tripNew.put("metaDescription", getTypedContent(tripOld.get("meta_description")));
            if (tripOld.has("trip_listing_summary")) {
                tripNew.put("tripShortSummary", getShortSummaryFromXml(tripId));
            } else {
                tripNew.put("tripShortSummary", processShortSummary(tripPath, tripId));
            }
            JSONObject gallery = processImages(regionName, tripId);
            if (tripOld.has("video")) {
                gallery = processVideo(gallery, tripOld.getJSONObject("video"));
            }
            tripNew.put("tripGallery", gallery);
            tripNew.put("map",
                TripConstants.TRIP_IMG_ASSETS_BASE + "maps/" + regionName + "/" + tripId + "_map.jpg");
            tripNew.put("days", getTypedContent(tripOld.get("totalDays")));
            if (tripOld.has("groupSize")) {
                tripNew.put("groupSize", getTypedContent(tripOld.get("groupSize")));
            } else {
                System.out.println("No group size: " + regionName + ":" + tripId);
            }
            tripNew.put("activityLevel", getTypedContent(tripOld.get("activity_level")));
            tripNew.put("activities", processActivities(tripOld.getJSONObject("activities")));
            if (tripOld.has("primary_activities")) {
                tripNew.put("primaryActivities", processPrimaryActivities(tripOld.getString("primary_activities")));
            }
            // TAGS
            JSONArray tags = new JSONArray();
            if (tripOld.has("trip_subtype")) {
                String tripSubType = tripOld.getString("trip_subtype");
                tags.put(tripSubType);
            }
            if (!tripOld.getString("trip_type").equalsIgnoreCase(TripConstants.TRIP_TYPE_REGULAR)) {
                tags.put(tripOld.getString("trip_type"));
            }
            tripNew.put("tags", tags);
            if (tripOld.has("destination_bullet_list")) {
                List<String> destinationTypes =
                        processDestinationTypes(tripOld.getJSONObject("destination_bullet_list"));
                if (destinationTypes.contains(TripConstants.TRIP_TYPE_SIGNATURE_CAMPING)) {
                    tags.put(TripConstants.TRIP_TYPE_SIGNATURE_CAMPING);
                    tripNew.put("tags", tags);
                    destinationTypes = destinationTypes.stream()
                            .filter(dest -> !dest.equals(TripConstants.TRIP_TYPE_SIGNATURE_CAMPING))
                            .collect(Collectors.toList());
                }
                tripNew.put("destinationTypes",destinationTypes);
            }
            if (tripOld.has("accomodations")) {
                tripNew.put("accommodations", tripOld.getJSONObject("accomodations")
                        .getString("accomodationDescription"));
            }
            tripNew.put("heroCardAlign", "left");
            if (StringUtils.isNotBlank(extensionType)) {
                tripNew.put("extensionType", extensionType);
            }
            tripNew.put("price", processPrice(tripOld.getJSONObject("tripCosts")));
            tripNew.put("tripDates", processDates(regionName, tripId));
            tripNew.put("tripLegacyId", tripLegacyId);

            TripUtils.createDirectory(TripConstants.TRIP_JSON_PATH + regionName + "/" + tripId);
            TripUtils.writeJsonToFile(
                    regionName + "/" + tripId + ".json", new JSONObject(tripNew));

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (DataBuilderException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDetailJson(String tripPath, String regionName, String tripId) {
        Document doc = getXmlDocument(tripPath, tripId, TripConstants.DOC_DETAIL_FILE_NAME);

        if (doc != null) {
            try {
                Map<String, Object> json = new LinkedHashMap<>();

                if (doc.getElementsByTagName("trip_summary").getLength() > 0) {
                    json.put("highlights",
                            processTripHighlight((Element) doc.getElementsByTagName("trip_summary").item(0)));
                    json.put("tripSummary",
                            processTripSummary((Element) doc.getElementsByTagName("trip_summary").item(0)));
                }

                String itineraryNote = "";

                if (doc.getElementsByTagName("additional_information").getLength() > 0) {
                    Map<String, JSONObject> info = processAdditionalInfo(
                            (Element) doc.getElementsByTagName("additional_information").item(0));
                    if (info.containsKey("noteOnItinerary")) {
                        itineraryNote = info.get("noteOnItinerary").getString("content");
                        info.remove("noteOnItinerary");
                    }
                    json.put("additionalInfo", info);
                }

                if (doc.getElementsByTagName("itinerary").getLength() > 0) {
                    json.put("itinerary", processItinerary(
                            itineraryNote, (Element) doc.getElementsByTagName("itinerary").item(0)));
                }

                if (doc.getElementsByTagName("omit_general_information").getLength() > 0) {
                    json.put("noGeneralInfo", true);
                }

                // adding a fake trip guide
                JSONArray guides = new JSONArray();
                Map<String, String> guide = new LinkedHashMap<>();
                guide.put("name", "Anderson Smith");
                guide.put("description", "Anderson was born in Lesiraa Village in the heart of Maasai country. The Maasai are pastoralists, and as a young man Anderson herded his family's livestock in the bush.");
                guide.put("image", "/assets/img/adventures/trip/guides/" + regionName + "/anderson_smith.jpg");
                json.put("tripGuide", guides.put(new JSONObject(guide)));

                // adding a fake program manager
                Map<String, String> manager = new LinkedHashMap<>();
                manager.put("name", "Rebecca Taylor");
                manager.put("description", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam placerat viverra lobortis. Vestibulum gravida nec risus ut tincidunt. Fusce vitae mattis nisl. Quisque consectetur dolor pulvinar, placerat tortor at, egestas enim.");
                manager.put("image", "/assets/img/adventures/trip/programManagers/" + regionName + "/rebecca_taylor.jpg");
                json.put("programManager", new JSONObject(manager));

                // adding  a fake review
                Map<String, String> review = new LinkedHashMap<>();
                review.put("title", "Kate the safari lover");
                review.put("subtitle", "Over the top amazing trip");
                review.put("rating", "4");
                review.put("description", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam placerat viverra lobortis. Vestibulum gravida nec risus ut tincidunt. Fusce vitae mattis nisl. Quisque consectetur dolor pulvinar, placerat tortor at, egestas enim.");
                review.put("image", "/assets/img/adventures/trip/reviews/" + regionName + "/" + tripId + "_review.jpg");
                json.put("review", new JSONObject(review));

                // add gear content
                json.put("gearContents", processGear(tripPath, tripId));

                TripUtils.createDirectory(TripConstants.TRIP_JSON_PATH + regionName + "/" + tripId);
                TripUtils.writeJsonToFile(regionName + "/" + tripId + "/" + tripId + "-" +
                        TripConstants.TRIP_DETAIL_FILENAME, new JSONObject(json));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void createGalleryJson(String tripPath, String regionName, String tripId) {
        String url = TripConstants.DOC_BASE + tripPath + tripId + TripConstants.DOC_SLIDE_SHOW_FILE_NAME;
        String xml;
        try {
            xml = TripUtils.getFileContent(url);

            if (StringUtils.isNotBlank(xml)) {
                JSONObject json = new JSONObject();
                JSONArray pics = TripUtils.castToJSONArray(TripUtils.getJsonFromXml(xml)
                        .getJSONObject("images").get("pic"));
                JSONArray images = new JSONArray();

                for (int j = 0; j < pics.length(); j++) {
                    JSONObject pic = pics.getJSONObject(j);
                    Map<String, String> image = new LinkedHashMap<>();
                    String imageSrc = pic.getString("image");
                    String thumbSrc = pic.getString("thumbnail");
                    String base = TripConstants.TRIP_IMG_ASSETS_BASE + "gallery/" + regionName + "/";
                    image.put("url", base + tripId + "_" + String.format("%02d", j + 1) + ".jpg");
                    image.put("thumbnail", base + tripId + "_" + String.format("%02d", j + 1) + "_thumb.jpg");
//                    image.put("url", base + imageSrc.toLowerCase().substring(imageSrc.lastIndexOf('/') + 1));
//                    image.put("thumbnail", base + thumbSrc.toLowerCase().substring(thumbSrc.lastIndexOf('/') + 1));
                    if (pic.has("caption")) {
                        image.put("caption", pic.getString("caption"));
                    }
                    if (pic.has("imagetitle")) {
                        image.put("title", pic.getString("imagetitle"));
                    }
                    image.put("type", "image");

                    images.put(new JSONObject(image));
                }

                json.put("images", images);
                TripUtils.createDirectory(TripConstants.TRIP_JSON_PATH + regionName + "/" + tripId);
                TripUtils.writeJsonToFile(regionName + "/" + tripId + "/" + tripId + "-" +
                        TripConstants.TRIP_GALLERY_FILENAME, json);
            }
        } catch (IOException e) {
            System.out.println("Not found: " + url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
