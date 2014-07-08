package it.polimi.snowwatch;

import it.polimi.models.Coordinate;
import it.polimi.models.ImageMarker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Panorama Download Manager
 * 
 * @author b3rn475
 * 
 */
public class POIDownloader {

	private final HttpClient mClient;

	/**
	 * Listener
	 */
	private OnPOIsUpdateEventListener mListener = null;
	private HttpTask mTask = null;

	/**
	 * Instantiate a new Downloader
	 */
	public POIDownloader() {
		mClient = new DefaultHttpClient();
	}

	/**
	 * Set the new coordinates
	 * 
	 * @param latitude
	 * @param longitude
	 */
	public void setCoordinates(double latitude, double longitude) {
		if (mTask != null){
			mTask.cancel(true);
		}
		mTask = new HttpTask();
		mTask.execute("http://gis2014-project02-ws.appspot.com/getClosestPOI?lat="
				+ latitude + "&lon=" + longitude);
	}

	/**
	 * Set the new listener for POIs changes
	 * 
	 * @param listener
	 */
	public void setPOIsUpdateListener(
			OnPOIsUpdateEventListener listener) {
		mListener = listener;
	}
	
	public void sendError(String error){
		if (mListener != null){
			mListener.onDownloadError(error);
		}
	}
	
	
	private void setPOIs(JSONArray json){
		mTask = null;
		
		if (mListener != null && json != null) {
			
			final List<ImageMarker> markers = new ArrayList<ImageMarker>();

			int num = json.length();
			for (int i = 0; i < num; i++) {
				try {
					JSONObject jsonPOI = json.getJSONObject(i);
					final String title = jsonPOI.getString("title");
					final String url = jsonPOI.getString("picture");
					final double lat = jsonPOI.getJSONObject("coordinates").getDouble("lat");
					final double lon = jsonPOI.getJSONObject("coordinates").getDouble("lon");
					
					markers.add(new ImageMarker(new Coordinate(lat, lon), title, url));
				} catch (JSONException e) {
					e.printStackTrace();
					continue;
				}
			}
			
			mListener.onPOIsUpdate(markers);
		}
	}

	/**
	 * Asynchronous task for JSON downloads
	 * 
	 * @author b3rn475
	 * 
	 */
	private class HttpTask extends AsyncTask<String, Void, JSONArray> {

		@Override
		protected JSONArray doInBackground(String... args) {
			HttpGet request = new HttpGet();
			JSONArray ret = null;
			URI website;
			try {
				website = new URI(args[0]);
				request.setURI(website);
				HttpResponse response;
				response = mClient.execute(request);
				if (response.getStatusLine().getStatusCode() == 200){
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(response.getEntity().getContent()));
					StringBuilder builder = new StringBuilder();
					String aux = "";
	
					while ((aux = reader.readLine()) != null) {
						builder.append(aux);
					}
					ret = new JSONArray(builder.toString());
				}
			} catch (URISyntaxException e) { // impossible
			} catch (ClientProtocolException e) { // impossible
			} catch (IOException e) { // already managed the ret is null;
			} catch (JSONException e) { // already managed the ret is null;
			}
			return ret;
		}

		@Override
		protected void onPostExecute(JSONArray array) {
			setPOIs(array);
		}
	}

	/**
	 * Listener for panorama changes
	 * 
	 * @author b3rn475
	 * 
	 */
	public interface OnPOIsUpdateEventListener {
		public void onPOIsUpdate(List<ImageMarker> markers);
		public void onDownloadError(String error);
	}
}