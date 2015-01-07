package com.swarmnyc.watchfaces;

public class ConverterUtil {
    public static final String CELSIUS_STRING = "°C";
    public static final String FAHRENHEIT_STRING = "°F";

    // converts to celsius
    public static float convertFahrenheitToCelsius(float fahrenheit) {
        return ((fahrenheit - 32) * 5 / 9);
    }

    // converts to fahrenheit
    public static float convertCelsiusToFahrenheit(float celsius) {
        return ((celsius * 9) / 5) + 32;
    }
}
