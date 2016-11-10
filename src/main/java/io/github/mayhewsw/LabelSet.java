package io.github.mayhewsw;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Abstraction to manage different types of labels stored in json format.
 */
public class LabelSet {
    private JSONObject primaryLabels;

    public void readFromFile(String filename) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject labels = (JSONObject) parser.parse(new FileReader(filename));
        primaryLabels = (JSONObject) labels.get("labels");
    }

    public String getCssClass(String labelName) {
        Object labelClassObj = primaryLabels.get(labelName);
        String labelClass = "defaultLabel";
        if (labelClassObj != null) {
            labelClass = labelClassObj.toString();
        }
        return labelClass;
    }

    public HashMap<String, JSONObject> toHashMap() {
        HashMap<String, JSONObject> map = new HashMap<>();
        map.put("labels", primaryLabels);
        return map;
    }
}
