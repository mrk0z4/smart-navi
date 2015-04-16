package smartnavi.markargus.com.smartnavi.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Class defining a place in terms of the ontology.
 *
 * Created by Marco Calderon on 11/11/14.
 */
public class Place {

    public static final String AUDITORIUM = "Auditorium";
    public static final String BATHROOM = "Bathroom";
    public static final String CAFFETERIA = "Caffeteria";
    public static final String CLASROOM = "Clasroom";
    public static final String JANITOR_ROOM = "JanitorRoom";
    public static final String LABORATORY = "Laboratory";
    public static final String MEETING_ROOM = "MeetingRoom";
    public static final String OFFICE = "Office";
    public static final String TESTING_ROOM = "TestingRoom";
    public static final String WORKSPACE = "Workspace";

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String INDEX = "index";
    private static final String FLOOR = "floor";
    private static final String CONTIGUOUS = "contiguous";
    private static final String FRONT_OF = "frontof";
    private static final String NEAR_BY = "nearby";
    private static final String BEACON = "beacon";

    private String type;
    private int floor;
    private int index;
    private String name;
    private Place contiguous;
    private Place frontof;
    private Place nearby;
    private Beacon beacon;

    public Place(String type, int floor, int index, String name) {
        this.type = type;
        this.floor = floor;
        this.index = index;
        this.name = name;
        this.contiguous = null;
        this.frontof = null;
        this.nearby = null;
    }

    public Place() {
        this.contiguous = null;
        this.frontof = null;
        this.nearby = null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Place getNearby() {
        return nearby;
    }

    public void setNearby(Place nearby) {
        this.nearby = nearby;
    }

    public Place getFrontof() {
        return frontof;
    }

    public void setFrontof(Place frontof) {
        this.frontof = frontof;
    }

    public Place getContiguous() {
        return contiguous;
    }

    public void setContiguous(Place contiguous) {
        this.contiguous = contiguous;
    }

    public Beacon getBeacon() {
        return beacon;
    }

    public void setBeacon(Beacon beacon) {
        this.beacon = beacon;
    }

    public boolean hasContiguous(){
        return getContiguous() != null;
    }

    public boolean hasFrontof(){
        return getFrontof() != null;
    }

    public boolean hasNearby(){
        return getNearby() != null;
    }

    /**
     * Returns a JSON representation of the object. Note: the relations are represented only by the name and the index
     * of the Place object.
     *
     * @return a JSONObject representation of the place.
     */
    public JSONObject asJSONObject(){
        JSONObject object = new JSONObject();
        try {
            object.put("name", getName());
            object.put("type", getType());
            object.put("index", getIndex());
            object.put("floor", getFloor());
            JSONObject nestedObject;
            if(getContiguous() != null){
                nestedObject = new JSONObject();
                nestedObject.put("name", getContiguous().getName());
                nestedObject.put("index", getContiguous().getIndex());
                object.put("contiguous", nestedObject);
            }
            if(getFrontof() != null){
                nestedObject = new JSONObject();
                nestedObject.put("name", getFrontof().getName());
                nestedObject.put("index", getFrontof().getIndex());
                object.put("frontof", nestedObject);
            }
            if(getNearby() != null){
                nestedObject = new JSONObject();
                nestedObject.put("name", getNearby().getName());
                nestedObject.put("index", getNearby().getIndex());
                object.put("nearby", nestedObject);
            }
            if(getBeacon() != null){
                nestedObject = new JSONObject();
                nestedObject.put(Beacon.NAME, getBeacon().getName());
                nestedObject.put(Beacon.IDENTIFIER, getBeacon().getIdentifier());
                object.put(BEACON, nestedObject);
            }
            return object;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Deserialize a JSONObject that represents a place.
     *
     * @param object the JSONObject to deserialize.
     * @return a Place object that represents the JSONObject.
     */
    public static Place parseJSONObject(JSONObject object){
        Place place = new Place();
        Iterator it = object.keys();
        Object arg;
        try {
            while (it.hasNext()) {
                arg = it.next();
                if (arg.equals(NAME)) {
                    place.setName(object.getString(NAME));
                } else if (arg.equals(INDEX)) {
                    place.setIndex(object.getInt(INDEX));
                } else if (arg.equals(FLOOR)) {
                    place.setFloor(object.getInt(FLOOR));
                } else if (arg.equals(TYPE)) {
                    place.setType(object.getString(TYPE));
                } else if (arg.equals(CONTIGUOUS)) {
                    JSONObject jsonObject = object.getJSONObject(CONTIGUOUS);
                    // The object strictly has name and index arguments. We assume they are inside the object.
                    Place nestedPlace = new Place();
                    nestedPlace.setName(jsonObject.getString(NAME));
                    nestedPlace.setIndex(jsonObject.getInt(INDEX));
                    place.setContiguous(nestedPlace);
                } else if (arg.equals(FRONT_OF)) {
                    JSONObject jsonObject = object.getJSONObject(FRONT_OF);
                    // The object strictly has name and index arguments. We assume they are inside the object.
                    Place nestedPlace = new Place();
                    nestedPlace.setName(jsonObject.getString(NAME));
                    nestedPlace.setIndex(jsonObject.getInt(INDEX));
                    place.setFrontof(nestedPlace);
                } else if (arg.equals(NEAR_BY)) {
                    JSONObject jsonObject = object.getJSONObject(NEAR_BY);
                    // The object strictly has name and index arguments. We assume they are inside the object.
                    Place nestedPlace = new Place();
                    nestedPlace.setName(jsonObject.getString(NAME));
                    nestedPlace.setIndex(jsonObject.getInt(INDEX));
                    place.setNearby(nestedPlace);
                } else if(arg.equals(BEACON)){
                    JSONObject jsonObject = object.getJSONObject(BEACON);
                    Beacon beacon = Beacon.parseJSONObject(jsonObject);
                    place.setBeacon(beacon);
                }
            }
            return place;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
