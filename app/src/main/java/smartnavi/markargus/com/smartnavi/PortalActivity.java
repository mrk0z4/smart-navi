package smartnavi.markargus.com.smartnavi;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.engine.Engine;
import org.restlet.ext.nio.HttpClientHelper;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import smartnavi.markargus.com.smartnavi.adapters.MyAdapter;
import smartnavi.markargus.com.smartnavi.ble.BLEDevice;
import smartnavi.markargus.com.smartnavi.ble.RBLService;
import smartnavi.markargus.com.smartnavi.location.LocationService;
import smartnavi.markargus.com.smartnavi.models.Distance;
import smartnavi.markargus.com.smartnavi.models.Route;
import smartnavi.markargus.com.smartnavi.rest.Server;
import smartnavi.markargus.com.smartnavi.util.BeaconPlaceManager;
import smartnavi.markargus.com.smartnavi.util.ModelServer;
import smartnavi.markargus.com.smartnavi.util.TDBAPI;


public class PortalActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, BeaconConsumer {

    private static final int SERVER_PORT = 3030;

    private static final String TAG = "SmartNavi";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 1000;
    public static final int REQUEST_CODE = 30;
    public static final int RESULT_CODE = 31;

    public final static String EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS";
    public final static String EXTRA_DEVICE_NAME = "EXTRA_DEVICE_NAME";

    public RBLService mBluetoothLeService;
    private Map<String, BLEDevice> mDevice = new HashMap<String, BLEDevice>();
    private LocationService locationService;
    private BLEDevice pastLocationDevice;
    private Distance previousDistance;

    private String mDeviceAddress;
    private String mDeviceName;
    private boolean readRssi;
    private String mDevicePlace;

    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private NsdServiceInfo mService;
    private NsdManager mNsdManager;
    private final static String SERVICE_TYPE = "_openthings._tcp";
    private final static String SERVICE_NAME = "TDBServer";

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_CONNECTED.equals(action)) {
                readRssi = true;
                Toast.makeText(getApplicationContext(), "Connected",
                        Toast.LENGTH_SHORT).show();
                startReadRssi();
            } else if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                readRssi = false;
                Toast.makeText(getApplicationContext(), "Disconnected",
                        Toast.LENGTH_SHORT).show();
            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {

            }
        }
    };
    private ModelServer modelServer;
    private TDBAPI tdbapi;
    private Component component;
    private NsdManager.RegistrationListener mRegistrationListener;
    private String mServiceName;
    private BeaconManager beaconManager;
    private BeaconPlaceManager beaconPlaceManager;

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        beaconManager.unbind(this);
        if (mServiceConnection != null)
            unbindService(mServiceConnection);
        mNsdManager.unregisterService(mRegistrationListener);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            if (device != null) {
                if(mDevice.containsKey(device.getName()) && mDevice.get(device.getName()) != null){
                    // Get associated device and set the RSSI by calculating the average value.
                    int avgRssi = (mDevice.get(device.getName()).getRssi() + rssi) / 2;
                    mDevice.get(device.getName()).setRssi(avgRssi);
                }
                else{
                    // Create a new BLEDevice to track the readings
                    mDevice.put(device.getName(), new BLEDevice(device.getName(), rssi));
                }
            }
        }
    };

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(mGattUpdateReceiver);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portal);

//        beaconManager = BeaconManager.getInstanceForApplication(getApplicationContext());
//        beaconManager.getBeaconParsers().add(new BeaconParser().
//                setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));

//        beaconManager.bind(this);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Sets the name of the device inside ModelServer.
        modelServer = ModelServer.getInstance();
        modelServer.setDeviceName(getDeviceName());

        tdbapi = TDBAPI.getInstance();

        beaconPlaceManager = new BeaconPlaceManager();

        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        Intent gattServiceIntent = new Intent(PortalActivity.this, RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        new Thread(new Runnable() {

            public void run() {
                while(true) {
                    scanLeDevice();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateDevices();
                        }
                    });
                }
            }
        }).start();

        locationService = LocationService.getInstance();

        mNsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);

        // Initialize listeners for mDNS events.
        initializeDiscoveryListener();
        initializeResolveListener();
        initializeRegistrationListener();

        // Register service.
        registerService();

        // Search for Context service.
        searchService();

        // Start Restlet server.
        startServer();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.portal, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            if (!readRssi) {
                scanLeDevice();

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } else {
                mBluetoothLeService.disconnect();
                mBluetoothLeService.close();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "I just saw an beacon for the first time!");
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see an beacon");
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: " + state);
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private TextView textView;
        private RecyclerView mRecyclerView;
        private RecyclerView.Adapter mAdapter;
        private RecyclerView.LayoutManager mLayoutManager;
        private View roundView;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_portal, container, false);
            textView = (TextView) rootView.findViewById(R.id.section_label);
            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
            roundView = (View) rootView.findViewById(R.id.roundedView);

            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mRecyclerView.setHasFixedSize(true);

            mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);

//            downloadOrderedRoute();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((PortalActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }

        public void setText(String text){
            if(textView != null){
                textView.setText(text);
            }
        }

        public void setRoundViewColor(int color){
            roundView.setBackgroundColor(color);
        }

        public void downloadOrderedRoute(){
            final DownloadOrderedRouteTask downloadOrderedRouteTask = new DownloadOrderedRouteTask();

            new Thread(new Runnable() {
                public void run() {
                    try {
                        downloadOrderedRouteTask.execute().get(20000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        public class DownloadOrderedRouteTask extends AsyncTask<String, Integer, Long> {

            Route route;

            @Override
            protected void onPreExecute(){

            }

            @Override
            protected Long doInBackground(String... arg0) {
                route = ModelServer.getInstance().getOrderedRoute();
                return null;
            }

            protected void onPostExecute(Long result) {
                if(route != null){
                    mAdapter = new MyAdapter(route);
                    mRecyclerView.setAdapter(mAdapter);
                }
            }

        }
    }

    /**
     * This method will be called every time there is a change in the current location of device.
     * @param device is the BLEDevice detected, that contains the uID.
     */
    private void locationChange(BLEDevice device){
        Log.i("locationChange", "The location has change to " + device.getName() + ".");
        new PostPlaceTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, device.getName());
    }

    /**
     * This method will be called every time there is a change in the distance of a reference device.
     * @param distance is the new distance between this device and the another.
     * @param device is the reference device.
     */
    private void proxemicDistanceChange(Distance distance, BLEDevice device, double value){
        Log.i("proxemicDistanceChange", "The distance from " + device.getName() + " has changed to " + distance.name() + ".");
        if(distance.equals(Distance.IntimateClosePhase)) {
            new PostDistanceTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, device.getName(), distance.name(), value + "");
        }
        else if(previousDistance.equals(Distance.IntimateClosePhase) && distance.equals(Distance.IntimateFarPhase)) {
            new PostDistanceTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, device.getName(), distance.name(), value + "");
        }
    }

    /**
     * Checks whether the distance has been changed.
     * @param pastDistance is the past distance.
     * @param actualDistance is the actual distance.
     * @return whether the distance is the same or not.
     */
    private boolean hasDistanceChanged(Distance pastDistance, Distance actualDistance){
        if(pastDistance != null && actualDistance != null && actualDistance.name().equals(pastDistance.name())){
            return false;
        }
        else{
            return true;
        }
    }

    private void updateDevices() {
        PlaceholderFragment currentFragment = (PlaceholderFragment) getFragmentManager().findFragmentById(R.id.container);
        if(currentFragment != null){
            String message = "";
            Vector<BLEDevice> deviceVector = getDeviceVector();
            Map<BLEDevice, String> deviceStringMap = locationService.getNearByRelations(deviceVector);
            BLEDevice actualLocationDevice = locationService.getActualLocationDevice(deviceVector);
            if (actualLocationDevice != null) {
                message += "Actual location: " + actualLocationDevice.getName() + ", count: " + actualLocationDevice.getReadingCount() + ", distance: "
                        + locationService.getDeviceProxemicPhase(actualLocationDevice).name() + "m \n";
                if(pastLocationDevice != null && actualLocationDevice != pastLocationDevice){
                    locationChange(actualLocationDevice);
                }
                else if(pastLocationDevice == null){
                    locationChange(actualLocationDevice);
                }
                Distance actualDistance = locationService.getDeviceProxemicPhase(actualLocationDevice);
                if(hasDistanceChanged(previousDistance, actualDistance)){
                    proxemicDistanceChange(actualDistance, actualLocationDevice, locationService.getDeviceDistance(actualLocationDevice));
                    if(Distance.IntimateClosePhase.equals(actualDistance)){
                        currentFragment.setRoundViewColor(getResources().getColor(R.color.material_design_red));
                    }
                    else if(Distance.IntimateFarPhase.equals(actualDistance)){
                        currentFragment.setRoundViewColor(getResources().getColor(R.color.material_design_deep_orange));
                    }
                    else if(Distance.PersonalClosePhase.equals(actualDistance)){
                        currentFragment.setRoundViewColor(getResources().getColor(R.color.material_design_orange));
                    }
                    else if(Distance.PersonalFarPhase.equals(actualDistance)){
                        currentFragment.setRoundViewColor(getResources().getColor(R.color.material_design_yellow));
                    }
                    else if(Distance.SocialClosePhase.equals(actualDistance)){
                        currentFragment.setRoundViewColor(getResources().getColor(R.color.material_design_light_green));
                    }
                    else if(Distance.SocialFarPhase.equals(actualDistance)){
                        currentFragment.setRoundViewColor(getResources().getColor(R.color.material_design_green));
                    }
                    else if(Distance.PublicClosePhase.equals(actualDistance)){
                        currentFragment.setRoundViewColor(getResources().getColor(R.color.material_design_blue));
                    }
                    else if(Distance.PublicFarPhase.equals(actualDistance)){
                        currentFragment.setRoundViewColor(getResources().getColor(R.color.material_design_blue_grey));
                    }
                }
                previousDistance = locationService.getDeviceProxemicPhase(actualLocationDevice);
            }
            pastLocationDevice = actualLocationDevice;
            for (Map.Entry<BLEDevice, String> entry : deviceStringMap.entrySet()) {
                // Check if the Device is valid yet.
                if(entry.getKey().isValid())
                    message += entry.getKey().getName() + ", relation: " + entry.getValue() + "\n";
            }
            currentFragment.setText(message);

            beaconPlaceManager.addBeacon(actualLocationDevice.getName());

            if(tdbapi.isConnected())
                new GetPlaceTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, actualLocationDevice.getName());
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    private void startReadRssi() {
        new Thread() {
            public void run() {

                while (readRssi) {
                    mBluetoothLeService.readRssi();
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }.start();
    }

    private void searchService() {
        new Thread() {
            public void run() {
                mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            };
        }.start();
    }

    private void scanLeDevice() {
        // First, we want to decrease the number of readings by one to make sure if the device
        // is nearby.
        for (Map.Entry<String, BLEDevice> entry : mDevice.entrySet()) {
            // By invalidating we decrease the number of readings.
            if (entry.getValue() != null) {
                entry.getValue().invalidate();
            }
        }

        mBluetoothAdapter.startLeScan(mLeScanCallback);

        try {
            Thread.sleep(SCAN_PERIOD);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        } else if (requestCode == REQUEST_CODE
                && resultCode == RESULT_CODE) {
            mDeviceAddress = data.getStringExtra(EXTRA_DEVICE_ADDRESS);
            mDeviceName = data.getStringExtra(EXTRA_DEVICE_NAME);
            mBluetoothLeService.connect(mDeviceAddress);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Obtains the device vector from the HashMap.
     *
     * @return a device vector from the HashMap.
     */
    private Vector<BLEDevice> getDeviceVector(){
        Vector<BLEDevice> deviceVector = new Vector<BLEDevice>();
        for (Map.Entry<String, BLEDevice> entry : mDevice.entrySet()) {
            // Check if the Device is valid yet.
            if(entry.getValue().isValid())
                deviceVector.add(entry.getValue());
        }
        return deviceVector;
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success: " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                }
                if (service.getServiceName().equals("")) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + "");
                } else if (service.getServiceName().contains(SERVICE_NAME)){
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                mService = serviceInfo;
                int port = mService.getPort();
                InetAddress host = mService.getHost();

                tdbapi.setServerAddress(host.getHostAddress(), port);

//                Action response = modelServer.getAction();
//                if (response != null) {
//                    createAlertDialog(response.getCommand(), response.getDescription());
//                }
//                testServer();
            }
        };
    }

    public void testServer(){
        if(modelServer != null){
            modelServer.getOrderedRoute();
            Log.i("testServer", "Testing successful.");
        }
    }

    private void startServer(){
        try {
            Log.i("Server", "Initializing Android HTTP server...");
            Engine.getInstance().getRegisteredClients().clear();
            Engine.getInstance().getRegisteredClients().add(new HttpClientHelper(null));
            component = new Component();
            component.getServers().add(Protocol.HTTP, 3030);
            component.getDefaultHost().attach("", new Server());
            component.start();
        } catch (Exception e) {
            System.out.println("!!! Could not start restlet based server");
            e.printStackTrace();
        }
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mServiceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
            }
        };
    }

    private void registerService() {
        // Create the NsdServiceInfo object, and populate it.
        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName("edison");
        serviceInfo.setServiceType("_lightservice._tcp.");
        serviceInfo.setPort(SERVER_PORT);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    private void createAlertDialog(String title, String message){
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(message)
                .setTitle(title);

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getDeviceName(){
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        String deviceName = myDevice.getName();
        return deviceName;
    }

    private void executeTask(AsyncTask task, String param){
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, param);
    }

    public class PostPlaceTask extends AsyncTask<String, Integer, Long> {

        @Override
        protected void onPreExecute(){}

        @Override
        protected Long doInBackground(String... arg0) {
            modelServer.postLocation(arg0[0]);
            return null;
        }

        protected void onPostExecute(Long result) {}

    }

    public class PostDistanceTask extends AsyncTask<String, Integer, Long> {

        @Override
        protected void onPreExecute(){}

        @Override
        protected Long doInBackground(String... arg0) {
            modelServer.postProxemicDistance(arg0[0], Distance.valueOf(arg0[1]), Double.parseDouble(arg0[2]));
            return null;
        }

        protected void onPostExecute(Long result) {}

    }

    public class GetPlaceTask extends AsyncTask<String, Integer, Long> {

        private JSONObject response = null;
        private String beaconId;
        private String virtualAmbientId = null;

        @Override
        protected void onPreExecute(){}

        @Override
        protected Long doInBackground(String... arg0) {
            beaconId = arg0[0];
            if(!beaconPlaceManager.beaconHasPlace(beaconId)) {
                response = tdbapi.getPlace(beaconId);
                try {
                    virtualAmbientId = response.getString("virtual-ambient-id");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onPostExecute(Long result) {
            beaconPlaceManager.addBeaconPlaceRelation(beaconId, virtualAmbientId);
        }

    }

}
