package com.rei.trip;

import com.rei.trip.exception.DataBuilderException;

public class Main {

    public static void main(String[] args) {
        try {
            TripDataBuilder builder = new TripDataBuilder();
            builder.buildAllTrips();
        } catch (DataBuilderException e) {
            System.out.println(e.getMessage());
        }
    }
}
