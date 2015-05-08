package com.swarmnyc.watchfaces;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.inject.Inject;
import com.swarmnyc.watchfaces.weather.IWeatherApi;
import com.swarmnyc.watchfaces.weather.WeatherInfo;
import com.swarmnyc.watchfaces.weather.openweather.OpenWeatherApi;

import roboguice.RoboGuice;

public class WeatherService extends WearableListenerService
{

	public static final  String KEY_WEATHER_CONDITION   = "Condition";
	public static final  String KEY_WEATHER_SUNRISE     = "Sunrise";
	public static final  String KEY_WEATHER_SUNSET      = "Sunset";
	public static final  String KEY_WEATHER_TEMPERATURE = "Temperature";
	public static final  String PATH_WEATHER_INFO       = "/WeatherWatchFace/WeatherInfo";
	public static final  String PATH_SERVICE_REQUIRE    = "/WeatherService/Require";
	private static final String TAG                     = "WeatherService";
	private GoogleApiClient mGoogleApiClient;
	private LocationManager mLocationManager;
	private Location        mLocation;
	private String          mPeerId;

	@Override
	public int onStartCommand( Intent intent, int flags, int startId )
	{
		if ( intent != null )
		{
			if ( WeatherWatchFaceConfigActivity.class.getSimpleName().equals( intent.getAction() ) )
			{
				mPeerId = intent.getStringExtra( "PeerId" );
				startTask();
			}
		}

		return super.onStartCommand( intent, flags, startId );
	}

	@Override
	public void onMessageReceived( MessageEvent messageEvent )
	{
		super.onMessageReceived( messageEvent );
		mPeerId = messageEvent.getSourceNodeId();
		Log.d( TAG, "MessageReceived: " + messageEvent.getPath() );
		if ( messageEvent.getPath().equals( PATH_SERVICE_REQUIRE ) )
		{
			startTask();
		}
	}

	private void startTask()
	{
		Log.d( TAG, "Start Weather AsyncTask" );
		mGoogleApiClient = new GoogleApiClient.Builder( this ).addApi( Wearable.API ).build();

		mLocationManager = (LocationManager) this.getSystemService( Context.LOCATION_SERVICE );
		mLocation = mLocationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );

		if ( mLocation == null )
		{
			mLocationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 0, 0, new LocationListener()
				{
					@Override
					public void onLocationChanged( Location location )
					{
						Log.d( TAG, "onLocationChanged: " + location );
						mLocationManager.removeUpdates( this );
						mLocation = location;
						Task task = new Task();
						task.execute();
					}

					@Override
					public void onStatusChanged( String provider, int status, Bundle extras )
					{

					}

					@Override
					public void onProviderEnabled( String provider )
					{

					}

					@Override
					public void onProviderDisabled( String provider )
					{

					}
				}
			);
		}
		else
		{
			Task task = new Task();
			task.execute();
		}
	}

	private class Task extends AsyncTask
	{
		@Inject
		IWeatherApi api;

		@Override
		protected Object doInBackground( Object[] params )
		{
			try
			{
				Log.d( TAG, "Task Running" );
				RoboGuice.getInjector( WeatherService.this.getApplicationContext() ).injectMembers( this );

				if ( !mGoogleApiClient.isConnected() )
				{ mGoogleApiClient.connect(); }

				DataMap config = new DataMap();
				WeatherInfo info = api.getCurrentWeatherInfo( mLocation.getLatitude(), mLocation.getLongitude() );

				//real
				config.putInt( KEY_WEATHER_TEMPERATURE, info.getTemperature() );
				config.putString( KEY_WEATHER_CONDITION, info.getCondition() );
				config.putLong( KEY_WEATHER_SUNSET, info.getSunset() );
				config.putLong( KEY_WEATHER_SUNRISE, info.getSunrise() );

				//test
				//Random random = new Random();
				//config.putInt("Temperature",random.nextInt(100));
				//config.putString("Condition", new String[]{"clear","rain","snow","thunder","cloudy"}[random.nextInt
				// (4)]);

				Wearable.MessageApi.sendMessage( mGoogleApiClient, mPeerId, PATH_WEATHER_INFO, config.toByteArray() )
				                   .setResultCallback(
					                   new ResultCallback<MessageApi.SendMessageResult>()
					                   {
						                   @Override
						                   public void onResult( MessageApi.SendMessageResult sendMessageResult )
						                   {
							                   Log.d( TAG, "SendUpdateMessage: " + sendMessageResult.getStatus() );
						                   }
					                   }
				                   );
			}
			catch ( Exception e )
			{
				Log.d( TAG, "Task Fail: " + e );
			}
			return null;
		}
	}
}

