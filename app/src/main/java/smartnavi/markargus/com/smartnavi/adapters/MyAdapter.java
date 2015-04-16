package smartnavi.markargus.com.smartnavi.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import smartnavi.markargus.com.smartnavi.R;
import smartnavi.markargus.com.smartnavi.models.Place;
import smartnavi.markargus.com.smartnavi.models.Route;

/**
 * Created by mc on 11/13/14.
 */
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private String[] mDataset;
    private Route route;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView placeNameTextView;
        public TextView placeBeaconTextView;
        public TextView placeFloorTextView;
        public TextView placeTypeTextView;

        public ViewHolder(View v) {
            super(v);
            placeNameTextView = (TextView) v.findViewById(R.id.place_textview);
            placeBeaconTextView = (TextView) v.findViewById(R.id.place_beacon_textview);
            placeFloorTextView = (TextView) v.findViewById(R.id.floor_textview);
            placeTypeTextView = (TextView) v.findViewById(R.id.type_textview);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(String[] myDataset) {
        mDataset = myDataset;
    }

    public MyAdapter(Route route){
        this.route = route;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row, parent, false);
        // set the view's size, margins, paddings and layout parameters

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        List<Place> places = route.getPlaces();
        holder.placeNameTextView.setText(places.get(position).getName());
        holder.placeTypeTextView.setText(places.get(position).getType());
        holder.placeFloorTextView.setText(places.get(position).getFloor() + "");
        if(places.get(position).getBeacon() != null) {
            holder.placeBeaconTextView.setText(places.get(position).getBeacon().getName() + " (" + places.get(position).getBeacon().getIdentifier() + ")");
        }
        else{
            holder.placeBeaconTextView.setText("None");
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return route.getPlaces().size();
    }
}
