package io.github.mayhewsw;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.FileWriter;
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
    private HashMap<String, List<String>> newLabels;
    private String primaryLabelName;
    private final String DEFAULT_LABEL_CLASS = "defaultLabel";

    public void readFromFile(String filename, String primaryLabel) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        originalObject = (JSONObject) parser.parse(new FileReader(filename));
        primaryLabels = (JSONObject) originalObject.get("labels");
        secondaryLabelsNames = new ArrayList<>();
        if (originalObject.size() > 1) {  // There are secondary labels.
            for (Object labelName : originalObject.keySet()) {
                if (!labelName.equals("labels")) {
                    secondaryLabelsNames.add((String) labelName);
                }
            }
        }
        primaryLabelName = primaryLabel;
        newLabels = new HashMap<>();
    }

    public String getCssClass(String labelName) {
        Object labelClassObj = primaryLabels.get(labelName);
        String labelClass = DEFAULT_LABEL_CLASS;
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

    public void writeToFile(String filename) throws IOException {
        for (String labelType : newLabels.keySet()) {
            if (labelType.equals(primaryLabelName)) {
                for (String labelValue: newLabels.get(labelType)) {
                    ((JSONObject) originalObject.get("labels")).put(labelValue, DEFAULT_LABEL_CLASS);
                }
            } else {
                ((JSONArray) originalObject.get(labelType)).addAll(newLabels.get(labelType));
            }
        }
        try (FileWriter file = new FileWriter(filename)) {
            file.write(originalObject.toJSONString());
        }
        newLabels = new HashMap<>();
    }

    public void addLabel(String labelValue, String labelType) {
        if (newLabels.get(labelType) == null) {
            newLabels.put(labelType, new ArrayList<>());
        }
        newLabels.get(labelType).add(labelValue);
    }

    public void removeLabel(String labelValue, String labelType) {
        if (newLabels.get(labelType) != null) {
            newLabels.get(labelType).remove(labelValue);
        }
    }

    public HashMap<String, List<String>> getNewLabels() {
        return newLabels;
    }
}
