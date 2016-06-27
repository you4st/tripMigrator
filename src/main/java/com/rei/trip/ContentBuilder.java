package com.rei.trip;

import com.rei.trip.util.TripUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Content(JSON) builder.
 * This creates JSON files under /opt/rei/sitedocs/pageContent from xml on Documentum
 *
 * @author sahan
 */
public class ContentBuilder {

    public ContentBuilder() {
    }


    public void buildLandingPageContent() {
        Document doc = TripUtils.getXmlDocument("/adventures", "/data/", "listing-page-headers.xml");

        if (doc != null) {
            NodeList pages = doc.getElementsByTagName("page");
            Map<String, Object> map = new LinkedHashMap<>();
            Map<String, Object> typeContent;

            for (int i = 0; i < pages.getLength(); i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                Element ele = (Element) pages.item(i);
                Node nameNode = ele.getElementsByTagName("name").item(0);
                Node metaTitleNode = ele.getElementsByTagName("meta_title").item(0);
                Node metaDescNode = ele.getElementsByTagName("meta_description").item(0);
                Node analyticsNode = ele.getElementsByTagName("analytics_page_tag").item(0);
                Node pageHeadingNode = ele.getElementsByTagName("page_heading").item(0);
                Node headerImageNode = ele.getElementsByTagName("header_image").item(0);
                Node overviewNode = ele.getElementsByTagName("overview_text").item(0);

                String type = nameNode.getAttributes().getNamedItem("type").getNodeValue();
                String name = nameNode.getTextContent();
                item.put("metaTitle", metaTitleNode.getTextContent());
                item.put("metaDescription", metaDescNode.getTextContent());
                item.put("analyticsTag", analyticsNode.getTextContent());
                item.put("headText", pageHeadingNode.getTextContent());
                List<String> hero = new ArrayList<>();
                hero.add(headerImageNode.getTextContent());
                item.put("heroImages", hero);

                String intro = "";
                String readMore = "";

                if (overviewNode.hasChildNodes()) {
                    NodeList list = overviewNode.getChildNodes();
                    for (int j = 0; j < list.getLength(); j++) {
                        Node node = list.item(j);
                        String pClass = "";
                        String text = "";

                        if (node.getAttributes() != null) {
                            if (node.getAttributes().getNamedItem("class") != null) {
                                pClass = node.getAttributes().getNamedItem("class").getNodeValue();
                            }
                            text = TripUtils.nodeToString(node).replaceAll("\\r|\\n|\\t", "");
                        } else {
                            text = TripUtils.nodeToString(node).replaceAll("\\r|\\n|\\t", "");
                        }

                        if (pClass.equals("intro")) {
                            intro += text;
                        } else {
                            readMore += text;
                        }
                    }
                }

                item.put("description", intro);
                item.put("readMore", readMore);

                type = normalizeType(type);
                name = normalizeName(name);

                if (map.containsKey(type)) {
                    typeContent = (LinkedHashMap) map.get(type);
                } else {
                    typeContent = new LinkedHashMap<>();
                }

                typeContent.put(name, item);
                map.put(type, typeContent);
            }

            try {
                TripUtils.writeJsonToFile("landingPages.json", new JSONObject(map), true);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private String normalizeType(String type) {
        switch (type) {
            case "region":
                return "regions";
            case "destination":
                return "destinations";
            case "activity":
                return "activities";
            case "other":
                return "moreTravels";
            default:
                return type;
        }
    }

    private String normalizeName(String name) {
        switch (name) {
            case "antarctica":
                return "polar";
            case "latin-america":
                return "latinAmerica";
            case "north-america":
                return "northAmerica";
            case "costa-rica":
                return "costaRica";
            case "machu-picchu":
                return "machuPicchu";
            case "southwest-us":
                return "southwestUs";
            case "national-parks":
                return "nationalParks";
            case "yellowstone-national-park":
                return "yellowstone";
            case "grand-canyon":
                return "grandCanyon";
            case "grand-canyon-havasu":
                return "grandCanyonHavasu";
            case "womens-backpacking-trips":
                return "womensBackpackingTrips";
            case "europe-coastal-hiking":
                return "europeCoastalHiking";
            case "europe-cycling":
                return "europeCycling";
            case "european-alps":
                return "europeanAlps";
            case "ireland-and-british-isles":
                return "irelandAndBritishIsles";
            case "latin-family-trips":
                return "latinFamilyTrips";
            case "cruises":
                return "cruising";
            case "safaris":
                return "wildlifeSafari";
            case "winter-sports":
                return "winterSports";
            case "family-vacations":
                return "family";
            case "signature-camping":
                return "signatureCamping";
            case "new-trips":
                return "newTrip";
            case "volunteer-vacations":
                return "volunteer";
            case "weekend-getaways":
                return "weekend";
            case "holiday-vacations":
                return "holiday";
            case "national-park-getaways":
                return "nationalParkGetaways";
            default:
                return name;
        }
    }
}
