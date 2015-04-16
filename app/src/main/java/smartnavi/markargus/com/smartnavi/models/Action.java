package smartnavi.markargus.com.smartnavi.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that represents an Action for Android devices.
 *
 * Created by mc on 3/30/15.
 */
public class Action {

    /** Fields */
    public static final String ACTION = "action";
    public static final String DESCRIPTION = "description";
    public static final String PRIORITY = "priority";
    public static final String COMMAND = "command";

    private Actions action;
    private String description;
    private int priority;
    private String command;

    public Action(Actions action, String description, int priority, String command) {
        this.action = action;
        this.description = description;
        this.priority = priority;
        this.command = command;
    }

    /**
     * Default constructor.
     */
    public Action() {

    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Actions getAction() {
        return action;
    }

    public void setAction(Actions action) {
        this.action = action;
    }

    public JSONObject asJSONObject(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ACTION, getAction());
            jsonObject.put(DESCRIPTION, getDescription());
            jsonObject.put(PRIORITY, getPriority());
            jsonObject.put(COMMAND, getCommand());
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Action parseJSONObject(JSONObject object){
        Action action = new Action();
        try {
            action.setAction(Actions.valueOf(object.getString(ACTION)));
            action.setDescription(object.getString(DESCRIPTION));
            action.setPriority(object.getInt(PRIORITY));
            action.setCommand(object.getString(COMMAND));
            return action;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
