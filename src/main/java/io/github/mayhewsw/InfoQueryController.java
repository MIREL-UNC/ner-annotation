package io.github.mayhewsw;

import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Handles the urls for querying label information
 */
@Controller
@RequestMapping(value = "/infoquery")
public class InfoQueryController {

    private static Logger logger = LoggerFactory.getLogger(AnnotationController.class);

    public InfoQueryController() {
        logger.info("Creating controller");
    }

    @RequestMapping(value = "/description", method = RequestMethod.GET)
    public String getDescription(@RequestParam(value = "labelName") String labelName, Model model) {
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
        ResultSet results = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query).execSelect();
        String description = "";
        if (results.hasNext()) {
            QuerySolution solution = results.next();
            description = solution.getLiteral("?description").toString();
        }
        model.addAttribute("entity", labelName);
        model.addAttribute("infoString", description);

        return "info";
    }
}
