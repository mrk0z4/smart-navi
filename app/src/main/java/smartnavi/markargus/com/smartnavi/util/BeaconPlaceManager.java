package smartnavi.markargus.com.smartnavi.util;

import java.util.HashMap;

/**
 * Created by markus-coldsmith on 25/06/15.
 */
public class BeaconPlaceManager {

    private HashMap<String, String> beaconPlaceMap;

    public BeaconPlaceManager(HashMap<String, String> beaconPlaceMap) {
        this.beaconPlaceMap = beaconPlaceMap;
    }

    public BeaconPlaceManager() {
        beaconPlaceMap = new HashMap<String, String>();
    }

    public void addBeacon(String beacon) {
        if(!beaconPlaceMap.containsKey(beacon)) {
            beaconPlaceMap.put(beacon, "");
        }
    }

    public void addBeaconPlaceRelation(String beacon, String place) {
        if(beaconPlaceMap.containsKey(beacon)) {
            beaconPlaceMap.remove(beacon);
            beaconPlaceMap.put(beacon, place);
        }
        else {
            beaconPlaceMap.put(beacon, place);
        }
    }

    public String getPlaceFromBeaconId(String beaconId) {
        return beaconPlaceMap.get(beaconId);
    }

    public boolean beaconHasPlace(String beaconId) {
        if(beaconPlaceMap.containsKey(beaconId)) {
            if(beaconPlaceMap.get(beaconId).isEmpty()){
                return false;
            }
            else {
                return true;
            }
        }
        else {
            return false;
        }
    }

}
