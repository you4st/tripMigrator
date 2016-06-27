package com.rei.trip;

import com.rei.trip.exception.DataBuilderException;

public class Main {

    public static void main(String[] args) {
        try {
            // trip data
            TripDataBuilder builder = new TripDataBuilder();
            builder.buildAllTrips();

            // content
            ContentBuilder contentBuilder = new ContentBuilder();
            contentBuilder.buildLandingPageContent();
        } catch (DataBuilderException e) {
            System.out.println(e.getMessage());
        }
    }
}
