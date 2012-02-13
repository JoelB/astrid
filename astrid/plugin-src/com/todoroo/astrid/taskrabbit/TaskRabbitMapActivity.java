package com.todoroo.astrid.taskrabbit;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.timsu.astrid.R;


public class TaskRabbitMapActivity extends MapActivity implements LocationListener {

    private MapView mapView;
    private MapController mapController;
    public Location currentLocation;
    private EditText searchText;
    private TaskRabbitMapOverlayItem currentOverlayItem;
    private String locationAddress;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_rabbit_map_activity);

        mapView = (MapView) findViewById(R.id.map_view);
        mapView.setBuiltInZoomControls(true);


        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        List<Overlay> mapOverlays = mapView.getOverlays();

        Drawable drawable = this.getResources().getDrawable(android.R.drawable.star_big_on);
        currentOverlayItem = new TaskRabbitMapOverlayItem(drawable, this);
        GeoPoint point = null;

        mapController = mapView.getController();
        if(lastKnownLocation != null) {

            point = new GeoPoint((int)(lastKnownLocation.getLatitude()*1E6),(int)(lastKnownLocation.getLongitude()*1E6));
            OverlayItem overlayitem = createOverlayItem(point);
            currentOverlayItem.addOverlay(overlayitem);
            mapOverlays.add(currentOverlayItem);

            getAddressFromLocation(lastKnownLocation);
            mapController.animateTo(point);
            mapController.setZoom(17);
            mapView.invalidate();

        }


        searchText=(EditText)findViewById(R.id.search_text);

        ImageButton searchButton=(ImageButton)findViewById(R.id.search_button);
        searchButton.setImageResource(android.R.drawable.ic_menu_search);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchLocation();
            }
        });


    }

    public String getSearchText() {
        return searchText.getText().toString();
    }
    public void setSearchTextForCurrentAddress() {
        if(!TextUtils.isEmpty(locationAddress)) {
            searchText.setText(locationAddress);
        }
    }


    private final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 1:

                mapView.invalidate();
                currentOverlayItem.onTap(0);
                // What to do when ready, example:
                break;
            case -1:

                AlertDialog.Builder adb = new AlertDialog.Builder(TaskRabbitMapActivity.this);
                adb.setTitle(getString(R.string.tr_alert_location_fail_title));
                adb.setMessage(getString(R.string.tr_alert_location_fail_message));
                adb.setPositiveButton("Close",null);
                adb.show();
            }
        }
    };

    private void searchLocation() {

        Thread thread = new Thread() {
            @Override
            public void run() {

                List<Address> addresses = null;
                try {

                    Geocoder geoCoder = new Geocoder(
                            TaskRabbitMapActivity.this, Locale.getDefault());
                    addresses = geoCoder.getFromLocationName(
                            searchText.getText().toString(), 5);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (addresses != null && addresses.size() > 0) {
                    updateAddress(addresses.get(0));
                    GeoPoint q = new GeoPoint(
                            (int) (addresses.get(0).getLatitude() * 1E6),
                            (int) (addresses.get(0).getLongitude() * 1E6));

                    Drawable drawable = TaskRabbitMapActivity.this.getResources().getDrawable(
                            android.R.drawable.star_big_on);
                    currentOverlayItem = new TaskRabbitMapOverlayItem(drawable,
                            TaskRabbitMapActivity.this);
                    mapController.animateTo(q);
                    mapController.setZoom(12);

                    OverlayItem overlayitem = createOverlayItem(q);

                    currentOverlayItem.addOverlay(overlayitem);
                    List<Overlay> mapOverlays = mapView.getOverlays();
                    mapOverlays.clear();
                    mapOverlays.add(currentOverlayItem);

                    Message successMessage = new Message();
                    successMessage.what = 1;
                    handler.sendMessage(successMessage);
                } else {

                    Message failureMessage = new Message();
                    failureMessage.what = -1;
                    handler.sendMessage(failureMessage);

                }
            }

        };

        thread.start();

    }

    protected OverlayItem createOverlayItem(GeoPoint q) {
        OverlayItem overlayitem = new OverlayItem(q, getString(R.string.tr_alert_location_clicked_title),
                getString(R.string.tr_alert_location_clicked_message));
        return overlayitem;
    }

    private void getAddressFromLocation(Location location){
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            // Acquire a reference to the system Location Manager
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null){
                for (Address address : addresses){
                    updateAddress(address);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void updateAddress(Address address){
        if(address.getLocality() != null && address.getPostalCode() != null){
            locationAddress = "";  //$NON-NLS-1$
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++){
                locationAddress += address.getAddressLine(i) + ", ";  //$NON-NLS-1$
            }
        }
    }


    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private void updateLocationOverlay() {
        if (currentLocation == null) { return; };
        List<Overlay> mapOverlays = mapView.getOverlays();
        Drawable drawable = this.getResources().getDrawable(android.R.drawable.star_big_on);
        TaskRabbitMapOverlayItem myItemizedOverlay = new TaskRabbitMapOverlayItem(drawable);
        GeoPoint point = new GeoPoint((int)(currentLocation.getLatitude() * 1E6), (int)(currentLocation.getLongitude() * 1E6));

        OverlayItem overlayitem = createOverlayItem(point);
        myItemizedOverlay.addOverlay(overlayitem);
        mapOverlays.add(myItemizedOverlay);
    }
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            this.currentLocation = location;
            GeoPoint p = new GeoPoint((int) lat * 1000000, (int) lng * 1000000);
            updateLocationOverlay();
            mapController.animateTo(p);

        }
    }
    @Override
    public void onProviderDisabled(String provider) {
        //

    }
    @Override
    public void onProviderEnabled(String provider) {
        //

    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //

    }



}
