/*
 * Copyright (C) 2013 Trilogis S.r.l.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.trilogis.android.ww;

import gov.nasa.worldwind.BasicView;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindowGLSurfaceView;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.kml.KMLConstants;
import gov.nasa.worldwind.kml.KMLRoot;
import gov.nasa.worldwind.kml.impl.KMLController;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GPSMarker;
import gov.nasa.worldwind.render.GPSMarker.PositionColors;
import gov.nasa.worldwind.render.SurfaceImage;
import it.trilogis.android.ww.R;
import it.trilogis.android.ww.dialogs.AddWMSDialog;
import it.trilogis.android.ww.dialogs.TocDialog;
import it.trilogis.android.ww.dialogs.AddWMSDialog.OnAddWMSLayersListener;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import it.polimi.models.Coordinate;
import it.polimi.models.ImageMarker;
import it.polimi.models.SquareMarker;
import it.polimi.snowwatch.LocationManager;
import it.polimi.snowwatch.POIDownloader;
import it.polimi.snowwatch.POIDownloader.OnPOIsUpdateEventListener;

/**
 * @author Nicola Dorigatti
 */
public class WorldWindowActivity extends Activity implements LocationManager.OnLocationEventListener, WorldWindowGLSurfaceView.OnCoordinateClickListener, OnPOIsUpdateEventListener {
    static {
        System.setProperty("gov.nasa.worldwind.app.config.document", "config/wwandroiddemo.xml");
    }

    private static final String TAG = "TrilogisWWExample";

    // This parameters are useful for WMS Addition and view.
    // Thanks to the Autonomous Province of Bolzano (Italy) for the Open WMS Server.
    // The Use of their WMS Services for commercial and/or support to companies is allowed.
    public final static String DEFAULT_WMS_URL = "http://ows.terrestris.de/osm/service";
    private final static double COMO_LATITUDE = 45.815594d;
    private final static double COMO_LONGITUDE = 9.1098543d;
    private final static double COMO_VIEW_HEADING = 0d;
    private final static double COMO_VIEW_TILT = 0d;
    private final static double COMO_VIEW_DISTANCE_KM = 13000d;

    protected WorldWindowGLSurfaceView wwd;
    
    private final List<ImageMarker> markers = new ArrayList<ImageMarker>();
    
    private LocationManager mLocationManager = null;
    private POIDownloader mDownloader = null;
    private Menu mMenu = null;

    // private CompassLayer cl;
    // private WorldMapLayer wml;
    // private SkyGradientLayer sgl;
    // private ScalebarLayer sbl;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setting the location of the file store on Android as cache directory. Doing this, when user has no space left
        // on the device, if he asks to the system to free Cache of apps, all the MB/GB of WorldWindApplication will be cleared!
        File fileDir = getExternalCacheDir();// getFilesDir();
        if (null != fileDir && fileDir.exists() && fileDir.canWrite()) {
            // create .nomedia file, so pictures will not be visible in the gallery (otherwise, it's really awful to see all of the tiles as images!)
            File output = new File(fileDir, ".nomedia");
            if (output.exists()) {
                Log.d(TAG, "No need to create .nomedia file, it's already there! : " + output.getAbsolutePath());
            } else {
                // lets create the file
                boolean fileCreated = false;
                try {
                    fileCreated = output.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "IOException while creating .nomedia: " + e.getMessage());
                }
                if (!fileCreated) {
                    Log.e(TAG, ".nomedia file not created!");
                } else {
                    Log.d(TAG, ".nomedia file created!");
                }
            }
        }
        // Setup system property for the file store
        System.setProperty("gov.nasa.worldwind.platform.user.store", fileDir.getAbsolutePath());

        // set the contentview
        this.setContentView(R.layout.main);
        // And initialize the WorldWindow Model and View
        this.wwd = (WorldWindowGLSurfaceView) this.findViewById(R.id.wwd);
        this.wwd.setModel((Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME));
        this.setupView();
        this.setupTextViews();
        this.setupLocationListener();
        
        mLocationManager = new LocationManager(this);
        mLocationManager.setLocationUpdateEventListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause the OpenGL ES rendering thread.
        this.wwd.onPause();
        this.mLocationManager.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Resume the OpenGL ES rendering thread.
        this.wwd.onResume();
        this.mLocationManager.resume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Configure the application's options menu using the XML file res/menu/options.xml.
        this.getMenuInflater().inflate(R.menu.options, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_show_camera:
            	Intent intent = new Intent(this, ARActivity.class);
            	Bundle b = new Bundle();
            	b.putDouble("latitude", mLocationManager.getLatitude());
            	b.putDouble("longitude", mLocationManager.getLongitude());
            	Globe globe = this.wwd.getModel().getGlobe();
            	b.putDouble("altitude", globe.getElevation(Angle.fromDegrees(mLocationManager.getLatitude()), Angle.fromDegrees(mLocationManager.getLongitude())));
            	b.putSerializable("markers", new ArrayList<ImageMarker>(markers));
            	intent.putExtras(b);
            	startActivity(intent);
                break;
            case R.id.menu_my_location:
            	moveToMyLocation();
                break;
            case R.id.show_layers_toc:
                // Toast.makeText(getApplicationContext(), "Showing TOC!", Toast.LENGTH_LONG).show();
                showLayerManager();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private <T extends Layer> T searchSpecificLayer(Class<T> classToSearch) {
        if (null == this.wwd || null == this.wwd.getModel() || null == this.wwd.getModel().getLayers()) {
            Log.e(TAG, "No layers in model!");
            return null;
        }
        LayerList layers = this.wwd.getModel().getLayers();
        for (Layer lyr : layers) {
            if (classToSearch.isInstance(lyr)) {
                return (T) lyr;
            }
        }
        return null;
    }

    protected void setupView() {
        BasicView view = (BasicView) this.wwd.getView();
        Globe globe = this.wwd.getModel().getGlobe();
        // set the initial position to "Como", where you can see the WMS Layers
        view.setLookAtPosition(Position.fromDegrees(COMO_LATITUDE, COMO_LONGITUDE,
            globe.getElevation(Angle.fromDegrees(COMO_LATITUDE), Angle.fromDegrees(COMO_LONGITUDE))));
        view.setHeading(Angle.fromDegrees(COMO_VIEW_HEADING));
        view.setTilt(Angle.fromDegrees(COMO_VIEW_TILT));
        view.setRange(COMO_VIEW_DISTANCE_KM);
    }

    protected void setupTextViews() {
        TextView latTextView = (TextView) findViewById(R.id.latvalue);
        this.wwd.setLatitudeText(latTextView);
        TextView lonTextView = (TextView) findViewById(R.id.lonvalue);
        this.wwd.setLongitudeText(lonTextView);
    }
    
    protected void setupLocationListener(){
    	this.wwd.setCoordinatesClickListener(this);
    }

    // ============== Add WMS ======================= //
    private void openAddWMSDialog() {
        AddWMSDialog wmsLayersDialog = new AddWMSDialog();
        wmsLayersDialog.setOnAddWMSLayersListener(mListener);
        wmsLayersDialog.show(getFragmentManager(), "addWmsLayers");
    }

    private OnAddWMSLayersListener mListener = new OnAddWMSLayersListener() {

        @Override
        public void onAddWMSLayers(List<Layer> layersToAdd) {
            if (null == layersToAdd || layersToAdd.isEmpty()) {
                Log.w(TAG, "Null or empty layers to add!");
                return;
            }
            for (Layer lyr : layersToAdd) {
                boolean added = WorldWindowActivity.this.wwd.getModel().getLayers().addIfAbsent(lyr);
                Log.d(TAG, "Layer '" + lyr.getName() + "' " + (added ? "correctly" : "not") + " added to WorldWind!");
            }
        }
    };

    // ============== Show Layer Manager ============ //
    private void showLayerManager() {
        TocDialog tocDialog = new TocDialog();
        tocDialog.setWorldWindData(this.wwd);
        tocDialog.show(getFragmentManager(), "tocDialog");
    }

    private void moveToMyLocation(){
    	BasicView view = (BasicView) this.wwd.getView();
        Globe globe = this.wwd.getModel().getGlobe();
        view.animateTo(Position.fromDegrees(mLocationManager.getLatitude(), mLocationManager.getLongitude(),
                globe.getElevation(Angle.fromDegrees(mLocationManager.getLatitude()), Angle.fromDegrees(mLocationManager.getLongitude()))),
                COMO_VIEW_DISTANCE_KM, 
                Angle.fromDegrees(COMO_VIEW_HEADING),
                Angle.fromDegrees(COMO_VIEW_TILT), this.wwd);
        /*view.setLookAtPosition(Position.fromDegrees(mLocationManager.getLatitude(), mLocationManager.getLongitude(),
            globe.getElevation(Angle.fromDegrees(mLocationManager.getLatitude()), Angle.fromDegrees(mLocationManager.getLongitude()))));*/
        //view.setRange(COMO_VIEW_DISTANCE_KM);
    }
    
	@Override
	public void onLocationUpdate(double latitude, double longitude,
			Double altitude) {
		mMenu.findItem(R.id.menu_my_location).setEnabled(true);
		mMenu.findItem(R.id.menu_show_camera).setEnabled(true);
		final SquareMarker marker = new SquareMarker(new Coordinate(latitude, longitude));
		for (Layer l : this.wwd.getModel().getLayers()){
			if (l instanceof RenderableLayer){
				if (!l.getName().startsWith("My Location Scale x")) continue;
				int scale = Integer.parseInt(l.getName().substring("My Location Scale x".length()));
				RenderableLayer rl = (RenderableLayer)l;
				rl.removeAllRenderables();
				final double factor = 18;
				final double size = factor * scale;
				final Coordinate lb = marker.getLowerBoundaryCoordinate(size);
				final Coordinate ub = marker.getUpperBoundaryCoordinate(size);
				rl.addRenderable(new SurfaceImage(getFilePath("gps.png"), new Sector(Angle.fromDegrees(lb.latitude), Angle.fromDegrees(ub.latitude), Angle.fromDegrees(lb.longitude), Angle.fromDegrees(ub.longitude))));
			}
		}
		updateMarkers(latitude, longitude);
	}
	
	private void updateMarkers(double latitude, double longitude){
		markers.clear();
		if (mDownloader == null) {
			mDownloader = new POIDownloader();
			mDownloader.setLandscapeUpdateListener(this);
		}
		mDownloader.setCoordinates(latitude, longitude);
	}
	
	private String getFilePath(final String name){
		if (!this.getFileStreamPath(name).exists()){
			try {
				FileOutputStream oStream = this.openFileOutput(name, MODE_WORLD_READABLE);
				InputStream iStream = this.getAssets().open(name);
				pipe(iStream, oStream);
				oStream.flush();
				oStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		return this.getFileStreamPath(name).getAbsolutePath();
	}
	
	private void pipe(InputStream is, OutputStream os) throws IOException {
		  int n;
		  byte[] buffer = new byte[1024];
		  while((n = is.read(buffer)) > -1) {
		    os.write(buffer, 0, n);   // Don't allow any extra bytes to creep in, final write
		  }
		 os.close ();
	}

	@Override
	public void onGPSStatusUpdate(int status) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void CoordianteClick(double latitude, double longitude) {
		final BasicView view = (BasicView) this.wwd.getView();
		final double elevation = view.getEyePosition(this.wwd.getModel().getGlobe()).elevation;
		final Coordinate clickLocation = new Coordinate(latitude, longitude);
		for (Layer l : this.wwd.getModel().getLayers()){
			if (l instanceof RenderableLayer){
				if (!l.getName().startsWith("Image Location Scale x")) continue;
				int scale = Integer.parseInt(l.getName().substring("Image Location Scale x".length()));
				RenderableLayer rl = (RenderableLayer)l;
				if (!rl.isEnabled()) continue;
				if (elevation > rl.getMaxActiveAltitude()) continue;
				if (elevation < rl.getMinActiveAltitude()) continue;
				final double factor = 100;
				final double size = factor * scale;
				ImageMarker nearer = null;
				double distance = Double.MAX_VALUE;
				for (final ImageMarker marker : markers){
					if (marker.isInside(new Coordinate(latitude, longitude), size)){
						final double d = clickLocation.distance(marker.center);
						if (d < distance){
							nearer = marker;
							distance = d;
						}
					}
				}
				if (nearer != null){
					onMarkerClick(nearer);
				}
				return;
			}
		}
	}
	
	private void onMarkerClick(ImageMarker marker){
		//Toast.makeText(this, marker.name, Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(this, POIView.class);
    	Bundle b = new Bundle();
    	b.putSerializable("marker", marker);
    	intent.putExtras(b);
    	startActivity(intent);
	}

	@Override
	public void onPOIsUpdate(List<ImageMarker> ms) {
		Globe globe = this.wwd.getModel().getGlobe();
		for (ImageMarker marker : ms) {
			markers.add(new ImageMarker(marker.center, globe.getElevation(Angle.fromDegrees(marker.center.latitude), Angle.fromDegrees(marker.center.longitude)), marker.name, marker.url));
		}
		final String fPath = getFilePath("photo.png");
		for (Layer l : this.wwd.getModel().getLayers()){
			if (l instanceof RenderableLayer){
				if (!l.getName().startsWith("Image Location Scale x")) continue;
				int scale = Integer.parseInt(l.getName().substring("Image Location Scale x".length()));
				RenderableLayer rl = (RenderableLayer)l;
				rl.removeAllRenderables();
				final double factor = 21;
				final double size = factor * scale;
				for (final SquareMarker marker : markers){
					final Coordinate lb = marker.getLowerBoundaryCoordinate(size);
					final Coordinate ub = marker.getUpperBoundaryCoordinate(size);
					rl.addRenderable(new SurfaceImage(fPath, new Sector(Angle.fromDegrees(lb.latitude), Angle.fromDegrees(ub.latitude), Angle.fromDegrees(lb.longitude), Angle.fromDegrees(ub.longitude))));
				}
			}
		}
	}

	@Override
	public void onDownloadError(String error) {
		// TODO Auto-generated method stub
		
	}
}
