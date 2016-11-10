package io.github.mayhewsw;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Abstraction to manage different types of labels stored in json format.
 */
public class LabelSet {
    private JSONObject primaryLabels;
    private JSONObject originalObject;
    private List<String> secondaryLabelsNames;

    public void readFromFile(String filename) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        originalObject = (JSONObject) parser.parse(new FileReader(filename));
        primaryLabels = (JSONObject) originalObject.get("labels");
        secondaryLabelsNames = new ArrayList<>();
        if (originalObject.size() > 1) {  // There are secondary labels.
            for (Object labelName : originalObject.keySet()) {
                if (labelName != "labels") {
                    secondaryLabelsNames.add((String) labelName);
                }
            }
        }
    }

    public String getCssClass(String labelName) {
        Object labelClassObj = primaryLabels.get(labelName);
        String labelClass = "defaultLabel";
        if (labelClassObj != null) {
            labelClass = labelClassObj.toString();
        }
        return labelClass;
    }

    public JSONObject toHashMap() {
        return originalObject;
    }

    public List<String> getSecondaryLabelNames() {
        return secondaryLabelsNames;
    }
}
