package smartnavi.markargus.com.smartnavi.ble;

/**
 * Describes a BLE Device that was found nearby.
 *
 * Created by mc on 10/27/14.
 */
public class BLEDevice {

    private static final int MAX_READINGS = 5;

    private int rssi;
    private String name;
    private int readingCount;
    private boolean isValid;

    @Deprecated
    public BLEDevice() {
        rssi = 0;
        name = "";
    }

    public BLEDevice(String name, int rssi) {
        this.rssi = rssi;
        this.name = name;
        readingCount = MAX_READINGS;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
        resetReadings();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getReadingCount() {
        return readingCount;
    }

    public void setReadingCount(int readingCount) {
        this.readingCount = readingCount;
    }

    public boolean isValid() {
        return readingCount > 0;
    }

    @Deprecated
    public void addReading(){
        if(readingCount < MAX_READINGS){
            readingCount++;
            isValid = true;
        }
    }

    public void resetReadings(){
        readingCount = MAX_READINGS;
        isValid = true;
    }

    public void invalidate(){
        if(readingCount <= 0){
            isValid = false;
        }
        else{
            readingCount--;
        }
    }
}
