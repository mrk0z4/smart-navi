package smartnavi.markargus.com.smartnavi.rest;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import java.io.IOException;

/**
 * Class that defines the resources associated to REST application in Android.
 *
 * Created by mc on 3/23/15.
 */
public class Resource extends ServerResource {

    @Get("json")
    public JsonRepresentation getModel() {
        try {
            return new JsonRepresentation("{}");
        } catch (Exception e) {
            e.printStackTrace();
        }
        setStatus(Status.SERVER_ERROR_INTERNAL);
        return null;
    }

    @Post("json")
    public JsonRepresentation acceptItem(Representation entity){
        JsonRepresentation response = null;
        try {
            JSONObject req = (new JsonRepresentation(entity)).getJsonObject();
            System.out.println(req.getString("message"));

            // Preparing response
            JSONObject responseObject = new JSONObject();
            responseObject.put("status", "ok");
            response = new JsonRepresentation(responseObject.toString());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;
    }

}
