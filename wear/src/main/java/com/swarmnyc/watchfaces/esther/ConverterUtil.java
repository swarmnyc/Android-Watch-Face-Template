package com.swarmnyc.watchfaces.esther;

public class ConverterUtil {
    public static final String CELSIUS_STRING = "°C";
    public static final String FAHRENHEIT_STRING = "°F";
    public static final int FAHRENHEIT = 0;

    // converts to celsius
    public static int convertFahrenheitToCelsius(int fahrenheit) {
        return ((fahrenheit - 32) * 5 / 9);
    }

    // converts to fahrenheit
    public static int convertCelsiusToFahrenheit(int celsius) {
        return ((celsius * 9) / 5) + 32;
    }
}
