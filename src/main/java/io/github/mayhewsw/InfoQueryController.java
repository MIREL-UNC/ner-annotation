package io.github.mayhewsw;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jena.query.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;


/**
 * Handles the urls for querying label information
 */
@Controller
@RequestMapping(value = "/infoquery")
public class InfoQueryController {

    private static Logger logger = LoggerFactory.getLogger(InfoQueryController.class);
    private final String URI_TYPE = "uri";
    private final String CLASS_TYPE = "class";
    private final String DBPEDIA_ENDPOINT = "http://dbpedia.org/sparql";
    private final String YAGO_ENDPOINT = "https://linkeddata1.calcul.u-psud.fr/sparql";
    private final String WIKIPEDIA_ENDPOINT = "https://en.wikipedia.org/w/api.php";
    private ConfigurationManager config;
    private final String USER_AGENT = "Mozilla/5.0";
    private final String WIKIPEDIA_PREFIX = "https://en.wikipedia.org/wiki/";
    private static final String ENCODING = "UTF-8";

    public InfoQueryController() throws IOException, ParseException, ParseException {
        config = new ConfigurationManager("config/config.json");
    }

    /**
     * Returns the dbpedia description of the entity equivalent to Yago entityUri
     * @param entityUri
     * @return
     */
    private String getEntityDescription(String entityUri) {
        logger.info("Getting description of uri " + entityUri);
        String queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                "SELECT ?entity ?description WHERE {\n" +
                "     ?entity owl:sameAs <http://yago-knowledge.org/resource/" + entityUri + "> .\n" +
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

    /**
     * Returns the YAGO gloss of className. Only YAGO types have glosses.
     * @param className
     * @return
     */
    private String getClassGloss(String className) {
        logger.info("Getting description of class " + className);
        String gloss = "";
        String queryString = "SELECT DISTINCT ?gloss WHERE { \n" +
                "  <http://yago-knowledge.org/resource/" + className + "> <http://yago-knowledge.org/resource/hasGloss> ?gloss.\n" +
                "} LIMIT 1";
        Query query = QueryFactory.create(queryString);
        ResultSet results = QueryExecutionFactory.sparqlService(YAGO_ENDPOINT, query).execSelect();
        if (results.hasNext()) {
            QuerySolution solution = results.next();
            gloss = solution.getLiteral("?gloss").toString();
        }
        return gloss;
    }

    private String getWikipediaSummary(String className) throws IOException, ParseException {
        String summary = "No wikipedia page found";
        if (!className.startsWith(WIKIPEDIA_PREFIX)) {
            return summary;
        }
        className = className.replace(WIKIPEDIA_PREFIX, "");
        final List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("action", "query"));
        params.add(new BasicNameValuePair("titles", className));
        params.add(new BasicNameValuePair("format", "json"));
        params.add(new BasicNameValuePair("prop", "extracts"));
        String query = WIKIPEDIA_ENDPOINT + "?" + URLEncodedUtils.format(params, ENCODING);

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(query);
        // add request header
        request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(request);

        System.out.println("Response Code : "
                + response.getStatusLine().getStatusCode());

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        String json = reader.readLine();
        JSONObject finalResult = (JSONObject) new JSONParser().parse(json);
        JSONObject pages = (JSONObject) ((JSONObject) finalResult.get("query")).get("pages");
        if (pages.keySet().size() != 1 || pages.keySet().iterator().next().equals("-1")) {
            return summary;
        }
        summary = ((String) ((JSONObject) pages.values().iterator().next()).get("extract")).substring(0, 200) + " ...";
        if (summary == null) {
            return "No extract available";
        }
        return  summary;
    }

    @RequestMapping(value = "/description", method = RequestMethod.GET)
    public String getInfo(@RequestParam(value = "labelValue") String labelValue,
                          @RequestParam(value = "labelName") String labelName, Model model)
            throws IOException, ParseException {
        String description = "No description available";
        String labelType;
        String url = "";
        try {
            labelType = ((JSONObject) config.getLabelInfoQueryType()).get(labelName).toString();
        } catch (NullPointerException e) {
            logger.warn("Error, unmapped label type " + labelName);
            model.addAttribute("errorString", "Error, unmapped label type " + labelName);
            return "info";
        }
        if (labelType.equals(URI_TYPE)) {
            description = getEntityDescription(labelValue);
            url = "https://gate.d5.mpi-inf.mpg.de/webyago3spotlx/Browser?entity=%3C" + labelValue + "%3E";
        } else if (labelType.equals(CLASS_TYPE)) {
            if (labelValue.startsWith("wordnet_")) {
                description = getClassGloss(labelValue);
                url = "https://gate.d5.mpi-inf.mpg.de/webyago3spotlx/Browser?entity=%3C" + labelValue + "%3E";
            } else if (labelValue.startsWith("https://en.wikipedia.org/wiki/")) {
                description = getWikipediaSummary(labelValue);
                url = labelValue;
            } else if (labelValue.startsWith("LKIF:")) {
                String lkifClass = labelValue.replace("LKIF:", "");
                final List<BasicNameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("q", lkifClass));
                params.add(new BasicNameValuePair("type", "code"));
                url = "https://github.com/RinkeHoekstra/lkif-core/search?" + URLEncodedUtils.format(params, ENCODING);
		description = "Click to search in LKIF repository";
            }
        } else {
            logger.warn("Error, unknown label type " + labelName);
        }

        model.addAttribute("name", labelValue);
        model.addAttribute("url", url);
        model.addAttribute("description", description);
        return "info";
    }
}
