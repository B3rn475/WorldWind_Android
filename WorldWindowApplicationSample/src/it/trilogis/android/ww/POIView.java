package it.trilogis.android.ww;

import java.net.URI;

import com.squareup.picasso.Picasso;

import it.polimi.models.ImageMarker;
import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class POIView extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_poiview);
		
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		
		/**
		 * Get Parameters from intent bundle
		 */
		Bundle b = getIntent().getExtras();
		
		ImageMarker marker = (ImageMarker)b.getSerializable("marker");
		
		TextView title = (TextView)findViewById(R.id.title);
		ImageView image = (ImageView)findViewById(R.id.image);
		
		/**
		 * Set the title of the image
		 */
		title.setText(marker.name);
		/**
		 * Load the image
		 */
		Picasso.with(this).load(marker.url).placeholder(R.drawable.mountain).fit().centerInside().into(image);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.poiview, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_poiview,
					container, false);
			return rootView;
		}
	}

}
