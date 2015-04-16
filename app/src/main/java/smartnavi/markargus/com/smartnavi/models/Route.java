package smartnavi.markargus.com.smartnavi.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Class that defines a list of places of a given route.
 *
 * Created by Marco Calderon on 11/11/14.
 */
public class Route {

    private static final String PLACES = "places";
    private static final String NAME = "route_name";

    private List<Place> places;
    private String name;

    public Route(List<Place> places, String name) {
        this.places = places;
        this.name = name;
    }

    public Route() {
    }

    public List<Place> getPlaces() {
        return places;
    }

    public void setPlaces(List<Place> places) {
        this.places = places;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Converts the fields to a JSONObject.
     * @return the Route's JSONObject representation.
     */
    public JSONObject asJSONObject(){
        JSONObject object = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for(Place place : places ){
            jsonArray.put(place.asJSONObject());
        }
        try {
            object.put(PLACES, jsonArray);
            object.put(NAME, getName());
            return object;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Converts from a JSONObject to a Route representation.
     *
     * @param object the JSONObject
     * @return the Route obtained from the JSONObject.
     */
    public static Route parseJSONObject(JSONObject object) {
        Route route = new Route();
        List<Place> objectPlaces = new ArrayList<Place>();
        Map<String, Place> placesMap = new HashMap<String, Place>();
        try {
            Iterator it = object.keys();
            while (it.hasNext()) {
                String arg = (String) it.next();
                if (arg.equals(PLACES)) {
                    JSONArray jsonArray = object.getJSONArray(PLACES);
                    JSONObject nestedObject;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        nestedObject = jsonArray.getJSONObject(i);
                        Place nestedPlace = Place.parseJSONObject(nestedObject);
                        objectPlaces.add(nestedPlace);
                        placesMap.put(nestedPlace.getName(), nestedPlace);
                    }
                }
                else if(arg.equals(NAME)){
                    route.setName(object.getString(NAME));
                }
            }
            Place itPlace;
            for (Place p : objectPlaces) {
                if (p.hasContiguous()) {
                    itPlace = p.getContiguous();
                    if (placesMap.containsKey(itPlace.getName())) {
                        p.setContiguous(placesMap.get(itPlace.getName()));
                    }
                }
                if (p.hasFrontof()) {
                    itPlace = p.getFrontof();
                    if (placesMap.containsKey(itPlace.getName())) {
                        p.setFrontof(placesMap.get(itPlace.getName()));
                    }
                }
            }
            route.setPlaces(objectPlaces);
            route.setName(object.getString(NAME));
            return route;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
