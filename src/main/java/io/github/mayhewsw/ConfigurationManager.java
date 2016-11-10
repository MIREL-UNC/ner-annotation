package io.github.mayhewsw;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by mteruel2 on 11/10/16.
 */
public class ConfigurationManager {
    private JSONObject properties;
    private final String DEFAULT_LABELS_FILE = "data/example-labels.json";

    public ConfigurationManager(String filename) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        properties = (JSONObject) parser.parse(new FileReader(filename));
    }

    public Object getFolderProperties(String folder) {
        return ((JSONObject) properties.get("folders")).get(folder);
    }

    public Set<String> getFolderNames() {
        return ((JSONObject) properties.get("folders")).keySet();
    }

    public String getLabelsLocation() {
        Object labelFile = properties.get("labelFile");
        if (labelFile != null) {
            return labelFile.toString();
        }
        return DEFAULT_LABELS_FILE;
    }

    public String getPrimaryLabelName() {
        return properties.get("primaryLabelName").toString();
    }
}
