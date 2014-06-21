package it.polimi.snowwatch;

import it.trilogis.android.ww.R;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

/**
 * Adapter for showing a Mountain list in a ListView
 * @author B3rn475
 *
 */
public class MountainAdapter extends ArrayAdapter<Mountain> {

	private List<Mountain> mMuntainList;

	public MountainAdapter(Context context, int textViewResourceId,
			List<Mountain> mountainList) {
		super(context, textViewResourceId, mountainList);
		mMuntainList = mountainList;
	}

	private class ViewHolder {
		CheckBox name;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder holder = null;

		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater) this.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.mountain_list_view, null);

			holder = new ViewHolder();
			holder.name = (CheckBox) convertView
					.findViewById(R.id.mountain_selected_checkbox);
			convertView.setTag(holder);

			holder.name.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					CheckBox cb = (CheckBox) v;
					Mountain item = (Mountain) cb.getTag();
					item.setSelected(cb.isChecked());
				}
			});
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		Mountain item = mMuntainList.get(position);
		holder.name.setText(item.name);
		holder.name.setChecked(item.isSelected());
		holder.name.setTag(item);

		return convertView;

	}
}
