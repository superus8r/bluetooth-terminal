package com.mpandg.mpandgbluetooth.adapter;

import android.bluetooth.BluetoothDevice;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mpandg.mpandgbluetooth.R;

import java.util.List;

/**
 * Created by Ali Kabiri on 7/3/2016.
 * Find me here: ali@kabiri.org
 */

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {

    private static final String TAG = "DevicesAdapter";
    private List<BluetoothDevice> devices;

    public DevicesAdapter (List<BluetoothDevice> devices) {

        this.devices = devices;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // inflate the list item view.
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.listitem_device, parent, false);

        // return corresponding viewHolder.
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        //holder.bind();
        holder.name.setText(devices.get(position).getName());
        holder.description.setText(devices.get(position).getAddress());
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        TextView description;

        public ViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.name);
            description = (TextView) itemView.findViewById(R.id.description);
        }

        public void bind() {

            // bind the corresponding object to its view.
            BluetoothDevice device = devices.get(getAdapterPosition());

            Log.d(TAG, "binding device at index:" + getAdapterPosition() + " name:" + device.getName() + " address:" + device.getAddress());

            name.setText(device.getName());
            description.setText(device.getAddress());
        }
    }
}
