package com.rei.trip.util;

/**
 * Constants for the trip data service.
 *
 * @author sahan
 */
public class TripConstants {
    /**
     * Defined to prevent instances of this class from being created.
     */
    private TripConstants() {
    }

    /**
     * Constants for Documentum data
     */
    public static final String DOC_BASE = "https://corp.rei.com";
    public static final String DOC_TRIP_LIST = "/adventures/data/trip-list.xml";
    public static final String DOC_SLIDE_SHOW_FILE_NAME = "_dir/slideshow.xml";
    public static final String DOC_DETAIL_FILE_NAME = "_dir/detail.xml";
    public static final String DOC_GEAR_FILE_NAME = "_dir/gearlist.xml";
    public static final String DOC_EXT_LIST_FILE_NAME = "cross-sell-list.xml";

    public static final String TRIP_LIST_FILENAME = "tripList.json";
    public static final String TRIP_DETAIL = "detail";
    public static final String TRIP_GALLERY = "gallery";
    public static final String TRIP_DETAIL_FILENAME = TRIP_DETAIL + ".json";
    public static final String TRIP_GALLERY_FILENAME = TRIP_GALLERY + ".json";
    public static final String TRIP_JSON_PATH = "/opt/rei/sitedocs/adventures/tripData/";
    //public static final String TRIP_JSON_PATH = "/resources/adventures/tripData/";
    public static final String TRIP_IMG_ASSETS_BASE = "/assets/img/adventures/trip/";
    public static final String TRIP_TYPE_REGULAR = "Regular trip";
    public static final String TRIP_TYPE_SIGNATURE_CAMPING = "signature-camping";
}
