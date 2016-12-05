package io.github.mayhewsw;

import org.apache.jena.query.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;


/**
 * Handles the urls for querying label information
 */
@Controller
@RequestMapping(value = "/infoquery")
public class InfoQueryController {

    private static Logger logger = LoggerFactory.getLogger(AnnotationController.class);
    private final String URI_TYPE = "uri";
    private final String CLASS_TYPE = "class";
    private final String DBPEDIA_ENDPOINT = "http://dbpedia.org/sparql";
    private final String YAGO_ENDPOINT = "https://linkeddata1.calcul.u-psud.fr/sparql";
    private ConfigurationManager config;

    public InfoQueryController() throws IOException, ParseException {
        config = new ConfigurationManager("config/config.json");
    }

    private String getEntityDescription(String labelName) {
        logger.info("Getting description " + labelName);
        String queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "SELECT ?entity ?description ?title WHERE {\n" +
                "     ?entity foaf:name ?title .\n" +
                "     ?entity owl:sameAs <http://yago-knowledge.org/resource/" + labelName + "> .\n" +
                "     ?entity <http://www.w3.org/2000/01/rdf-schema#comment> ?description .\n" +
                "     FILTER (langMatches(lang(?description),\"en\"))\n" +
                "} LIMIT 1";
        Query query = QueryFactory.create(queryString);
        ResultSet results = QueryExecutionFactory.sparqlService(DBPEDIA_ENDPOINT, query).execSelect();
        String description = "";
        if (results.hasNext()) {
            QuerySolution solution = results.next();
            description = solution.getLiteral("?description").toString();
        }
        return  description;
    }

    private String getClassGloss(String labelName) {
        String gloss = "";
        return gloss;
    }

    @RequestMapping(value = "/description", method = RequestMethod.GET)
    public String getInfo(@RequestParam(value = "labelValue") String labelValue,
                          @RequestParam(value = "labelName") String labelName, Model model) {
        String description;
        String labelType;
        try {
            labelType = ((JSONObject) config.getLabelInfoQueryType()).get(labelName).toString();
        } catch (NullPointerException e) {
            logger.warn("Error, unmapped label type " + labelName);
            model.addAttribute("errorString", "Error, unmapped label type" + labelName);
            return "info";
        }
        if (labelType.equals(URI_TYPE)) {
            description = getEntityDescription(labelValue);
        } else if (labelType.equals(CLASS_TYPE)) {
            description = getClassGloss(labelValue);
        } else {
            description = "Error: no description";
            logger.warn("Error, unknown label type " + labelName);
        }

        model.addAttribute("entity", labelValue);
        model.addAttribute("infoString", description);
        return "info";
    }
}
