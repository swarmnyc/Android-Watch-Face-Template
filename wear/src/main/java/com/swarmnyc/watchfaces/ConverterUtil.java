package com.swarmnyc.watchfaces;

public class ConverterUtil
{
	public static final String CELSIUS_STRING    = "°C";
	public static final String FAHRENHEIT_STRING = "°F";
	public static final int    FAHRENHEIT        = 0;
	public static final int    TIME_UNIT_12      = 0;
	public static final int    TIME_UNIT_24      = 1;

	// converts to celsius
	public static int convertFahrenheitToCelsius( int fahrenheit )
	{
		return ( ( fahrenheit - 32 ) * 5 / 9 );
	}

	// converts to fahrenheit
	public static int convertCelsiusToFahrenheit( int celsius )
	{
		return ( ( celsius * 9 ) / 5 ) + 32;
	}

	public static String convertToMonth( int month )
	{
		switch ( month )
		{
			case 0:
				return "January ";
			case 1:
				return "February ";
			case 2:
				return "March ";
			case 3:
				return "April ";
			case 4:
				return "May ";
			case 5:
				return "June ";
			case 6:
				return "July ";
			case 7:
				return "August ";
			case 8:
				return "September ";
			case 9:
				return "October ";
			case 10:
				return "November ";
			default:
				return "December";
		}
	}

	public static String getDaySuffix( int monthDay )
	{
		switch ( monthDay )
		{
			case 1:
				return "st";
			case 2:
				return "nd";
			case 3:
				return "rd";
			default:
				return "th";
		}
	}

	public static int convertHour( int hour, int timeUnit )
	{
		if ( timeUnit == TIME_UNIT_12 )
		{
			int result = hour % 12;
			return ( result == 0 ) ? 12 : result;
		}
		else
		{
			return hour;
		}
	}
}
