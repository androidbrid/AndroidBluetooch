package com.example.androidbluetooch;

import java.util.ArrayList;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LeDeviceListAdapter extends BaseAdapter {
	private Context context;
	private ArrayList<BluetoothDevice> data = null;

	public LeDeviceListAdapter(Context context, ArrayList<BluetoothDevice> data) {
		this.context = context;
		this.data = data;
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			LayoutInflater mInflater = LayoutInflater.from(context);

			convertView = mInflater.inflate(R.layout.listitem, null);
			holder.address = (TextView) convertView.findViewById(R.id.tv_deviceAddr);
			holder.name = (TextView) convertView.findViewById(R.id.tv_deviceName);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.address.setText(data.get(position).getAddress());
		holder.name.setText(data.get(position).getName());
		return convertView;
	}

	static class ViewHolder {
		public TextView address;
		public TextView name;
	}
}
