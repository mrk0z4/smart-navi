package smartnavi.markargus.com.smartnavi.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that defines the properties of a Beacon from the locations ontology.
 *
 * Created by Marco Calderon on 11/12/14.
 */
public class Beacon {

    public static final String IDENTIFIER = "identifier";
    public static final String NAME = "name";

    private String identifier;
    private String name;

    public Beacon(String identifier, String name) {
        this.identifier = identifier;
        this.name = name;
    }

    public Beacon() {
    }

    public void setIdentifier(String identifier){
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isValid(){
        return name != null && identifier != null;
    }

    public JSONObject asJSONObject(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(NAME, getName());
            jsonObject.put(IDENTIFIER, getIdentifier());
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Beacon parseJSONObject(JSONObject object){
        Beacon beacon = new Beacon();
        try {
            beacon.setName(object.getString(NAME));
            beacon.setIdentifier(object.getString(IDENTIFIER));
            return beacon;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
