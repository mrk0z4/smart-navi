package smartnavi.markargus.com.smartnavi.location;

import org.altbeacon.beacon.distance.DistanceCalculator;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Vector;

import smartnavi.markargus.com.smartnavi.ble.BLEDevice;
import smartnavi.markargus.com.smartnavi.models.Distance;

/**
 * Class that provides functionality to give relations and approximations regarding the location for
 * a given list of BLEDevices.
 *
 * Created by mc on 10/27/14.
 */
public class LocationService {

    private static final int MIN_RSSI_VALUE = -90;
    private static final int MAX_RSSI_VALUE = -40;
    private static final int BOUNDARY_THRESHOLD = -75;
    private static final int NEAR_BY_THRESHOLD = -90;

    private static final double BASE_DISTANCE = 0.0;

    private double previousDistanceValue;

    private static final String NEAR_BY = "NEAR_BY";
    private static final String IS_CONTIGUOUS = "IS_CONTIGUOUS";
    private static LocationService instance;

    private Vector<BLEDevice> deviceVector;

    private DecimalFormat decimalFormat = new DecimalFormat("#.##");

    public LocationService() {
    }

    public LocationService(Vector<BLEDevice> deviceVector) {
        this.deviceVector = deviceVector;
    }

    /**
     * Obtains the device with strongest RSSI signal for inferring the actual location.
     *
     * @return the BLEDevice associated with the actual location.
     */
    public BLEDevice getActualLocationDevice(Vector<BLEDevice> devices){
        deviceVector = devices;
        BLEDevice actualLocationDevice = null;
        if (deviceVector != null && !deviceVector.isEmpty()) {
            if(deviceVector.size() == 1){
                return deviceVector.firstElement();
            }
            else if(deviceVector.size() > 1){
                // Obtain the device with the strongest signal
                actualLocationDevice = deviceVector.firstElement();
                for (int i = 1; i < deviceVector.size(); i++) {
                    if(deviceVector.get(i-1).getRssi() < deviceVector.get(i).getRssi()){
                        actualLocationDevice = deviceVector.get(i);
                    }
                }
                // Check whether is inside the threshold.
                if(actualLocationDevice.getRssi() >= BOUNDARY_THRESHOLD)
                    return actualLocationDevice;
                else{
                    return null;
                }
            }
        }
        return actualLocationDevice;
    }

    /**
     * Obtain the near by devices, without the actual location device.
     *
     * @return the near by devices.
     */
    public Vector<BLEDevice> getNearByDevices(Vector<BLEDevice> devices){
        deviceVector = devices;
        Vector<BLEDevice> nearByDevices = new Vector<BLEDevice>();
        BLEDevice actualLocationDevice = getActualLocationDevice(deviceVector);
        if(actualLocationDevice != null && deviceVector.size() > 1){
            for(BLEDevice device : deviceVector){
                if(!actualLocationDevice.getName().equals(device.getName())){
                    nearByDevices.add(device);
                }
            }
        }
        return nearByDevices;
    }

    /**
     * Obtains the relations between the actual location device and the near by devices.
     *
     * TODO: Add functionality to correctly distinguish the relations between the other devices with regard to actual location device.
     * @return a hashmap with their relations.
     */
    public HashMap<BLEDevice, String> getNearByRelations(Vector<BLEDevice> devices){
        deviceVector = devices;
        HashMap<BLEDevice, String> deviceRelationsMap = new HashMap<BLEDevice, String>();
//        BLEDevice actualLocationDevice = getActualLocationDevice(deviceVector);
        Vector<BLEDevice> nearByDevices = getNearByDevices(deviceVector);
        for(BLEDevice device : nearByDevices){
//            if(actualLocationDevice.getRssi() == device.getRssi() && device.getRssi() >= NEAR_BY_THRESHOLD){
//                deviceRelationsMap.put(device, IS_CONTIGUOUS);
//            }
//            else if(actualLocationDevice.getRssi() < device.getRssi() && device.getRssi() < NEAR_BY_THRESHOLD){
//                deviceRelationsMap.put(device, NEAR_BY);
//            }
            deviceRelationsMap.put(device, NEAR_BY);
        }
        return deviceRelationsMap;
    }

    /**
     * Obtains an approximation of a distance between this device with other.
     * This method uses the formula contained inside Android Beacon Library
     * (http://altbeacon.github.io/android-beacon-library/distance-calculations.html).
     *
     * @param device is the device to be calculated the distance with.
     * @return an approximation of the distance.
     */
    public double getDeviceDistance(BLEDevice device){
        double avgDistanceValue;

        double A = 0.42093;
        double B = 6.9476;
        double C = BASE_DISTANCE;
        double t = -72;
        // Formula obtained from Android Beacon Library
        double actualDistanceValue = A * Math.pow(device.getRssi() / t, B) + C;
        avgDistanceValue = (actualDistanceValue + previousDistanceValue) / 2;
        previousDistanceValue = actualDistanceValue;

        return Double.parseDouble(decimalFormat.format(avgDistanceValue));
    }

    /**
     * Categorizes a given device with its corresponding distance with a proxemic distance, with its phase.
     * @param device is the device to be calculated the distance with.
     * @return a Proxemic Distance. PublicFarPhase for default.
     */
    public Distance getDeviceProxemicPhase(BLEDevice device) {
        double distance = getDeviceDistance(device);
        if(distance >= BASE_DISTANCE && distance <= 0.15){
            return Distance.IntimateClosePhase;
        }
        else if(distance >= 0.16 && distance <= 0.46) {
            return Distance.IntimateFarPhase;
        }
        else if(distance >= 0.47 && distance <= 0.76) {
            return Distance.PersonalClosePhase;
        }
        else if(distance >= 0.77 && distance <= 1.22) {
            return Distance.PersonalFarPhase;
        }
        else if(distance >= 1.23 && distance <= 2.10) {
            return Distance.SocialClosePhase;
        }
        else if(distance >= 2.11 && distance <= 3.70) {
            return Distance.SocialFarPhase;
        }
        else if(distance >= 3.71 && distance <= 7.60) {
            return Distance.PublicClosePhase;
        }
        else if(distance >= 7.61) {
            return Distance.PublicFarPhase;
        }
        else{
            return Distance.PublicFarPhase;
        }
    }

    /**
     * Categorizes a given device with its corresponding distance with a proxemic distance.
     * @param device is the device to be calculated the distance with.
     * @return a Proxemic Distance. PublicFarPhase for default.
     */
    public Distance getDeviceProxemicDistance(BLEDevice device) {
        double distance = getDeviceDistance(device);
        if(distance >= BASE_DISTANCE && distance <= 0.46){
            return Distance.Intimate;
        }
        else if(distance >= 0.47 && distance <= 1.22) {
            return Distance.Personal;
        }
        else if(distance >= 1.23 && distance <= 3.70) {
            return Distance.Social;
        }
        else if(distance >= 3.71) {
            return Distance.Public;
        }
        else{
            return Distance.Public;
        }
    }

    public static LocationService getInstance() {
        if(instance == null)
            instance = new LocationService();
        return instance;
    }
}
