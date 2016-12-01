package io.github.mayhewsw;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.core.utilities.StringUtils;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sun.reflect.generics.tree.Tree;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * This contains the main logic of the whole thing.
 */
@SuppressWarnings("ALL")
@Controller
public class AnnotationController {

    private static Logger logger = LoggerFactory.getLogger(AnnotationController.class);

    private LabelSet labels;
    private ConfigurationManager config;
    private final String FOLDERTA = "ta";
    private final String FOLDERCONLL = "conll";

    /**
     * When this class is loaded, it reads a file called config/folders.txt. This is made up
     * of lines formatted as:
     * name path
     * The name is an identifier, the path is the absolute path to the folder. This
     * folder path must contain TextAnnotations.
     *
     * @throws FileNotFoundException
     */
    public AnnotationController() throws IOException, ParseException {

        logger.debug("Loading properties");
        config = new ConfigurationManager("config/config.json");

        logger.debug("Loading labels.txt");
        labels = new LabelSet();
        try {
            labels.readFromFile(config.getLabelsLocation(), config.getPrimaryLabelName());
        } catch (ParseException e) {
            logger.info(e.toString());
        }
    }

    /**
     * Given a foldername (first field in folders.txt), this will get the path to that folder (second field
     * in folders.txt) and will read all textannotations from that folder.
     * <p>
     * This returns a TreeMap from integer id to TextAnnotation. These ids are assigned only here -- they do
     * not correspond to the internal TextAnnotation id.
     *
     * @param folder folder identifier
     * @return
     * @throws IOException
     */
    public TreeMap<String, TextAnnotation> loadFolder(String folder, String username) throws IOException {

        JSONObject folderConfig = (JSONObject) config.getFolderProperties(folder);
        String folderurl = folderConfig.get("location").toString();
        String foldertype = folderConfig.get("format").toString();

        File f = new File(folderurl);

        // This will be ordered by it's keys.
        TreeMap<String, TextAnnotation> ret = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int retval;
                try {
                    retval = Integer.compare(Integer.parseInt(o1), Integer.parseInt(o2));
                } catch (NumberFormatException e) {
                    retval = o1.compareTo(o2);
                }
                return retval;
            }
        });

        if (foldertype.equals(FOLDERTA)) {
            String[] files = f.list();
            int limit = Math.min(files.length, 300);
            for (int i = 0; i < limit; i++) {
                String file = files[i];
                TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(folderurl + "/" + file);
                ret.put(file, ta);
            }
        } else if (foldertype.equals(FOLDERCONLL)) {
            CoNLLNerReader cnl = new CoNLLNerReader(folderurl);
            while (cnl.hasNext()) {
                TextAnnotation ta = cnl.next();
                logger.info("Loading: " + ta.getId());
                ret.put(ta.getId(), ta);
            }
        }

        // now check the annotation folder to see what this user has already annotated.
        // if there is anything, load it here.
        String outfolder = folderurl.replaceAll("/$", "") + "-annotation-" + username + "/";

        logger.info("Now looking in user annotation folder: " + outfolder);

        if ((new File(outfolder)).exists()) {

            if (foldertype.equals(FOLDERTA)) {
                File outf = new File(outfolder);
                String[] files = outf.list();
                int limit = Math.min(files.length, 300);
                for (int i = 0; i < limit; i++) {
                    String file = files[i];
                    TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(outfolder + "/" + file);
                    ret.put(file, ta);
                }
            } else if (foldertype.equals(FOLDERCONLL)) {
                CoNLLNerReader cnl = new CoNLLNerReader(outfolder);
                while (cnl.hasNext()) {
                    TextAnnotation ta = cnl.next();
                    logger.info("Loading: " + ta.getId());
                    ret.put(ta.getId(), ta);
                }
            }
        }

        return ret;
    }

    /**
     * Given a foldername (first field in folders.txt), this will get the path to that folder (second field
     * in folders.txt) and will read the files in that folder.
     *
     * @param folder folder identifier
     * @return
     * @throws IOException
     */
    public String[] listFolder(String folder) throws IOException {
        String[] files = new String[0];
        files = IOUtils.ls(folder);
        return files;
    }

    /**
     * This is called when the user clicks on the language button on the homepage.
     *
     * @param folder
     * @param hs
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/loaddata", method = RequestMethod.GET)
    public String dummy(@RequestParam(value = "folder") String folder, HttpSession hs) throws IOException {
        String username = (String) hs.getAttribute("username");
        String[] tas = listFolder(folder);
        hs.setAttribute("tas", tas);
        hs.setAttribute("dataname", folder);

        return "redirect:/annotation";
    }

    @RequestMapping(value = "/save", method = RequestMethod.GET)
    public String save(@RequestParam(value = "taid", required = true) String taid, HttpSession hs) throws IOException {

        // write out to
        String username = (String) hs.getAttribute("username");
        String folder = (String) hs.getAttribute("dataname");
        JSONObject folderConfig = (JSONObject) config.getFolderProperties(folder);
        String folderpath = folderConfig.get("location").toString();
        String foldertype = folderConfig.get("format").toString();

        if (username != null && folderpath != null) {

            folderpath = folderpath.replaceAll("/$", "");
            String outpath = folderpath + "-annotation-" + username + "/";
            logger.info("Writing out to: " + outpath);
            logger.info("id is: " + taid);

            TreeMap<String, TextAnnotation> tas = (TreeMap<String, TextAnnotation>) hs.getAttribute("tas");
            TextAnnotation taToSave = tas.get(taid);
            String savepath = outpath + taid;

            if (foldertype.equals(FOLDERTA)) {
                SerializationHelper.serializeTextAnnotationToFile(taToSave, savepath, true);
            } else if (foldertype.equals(FOLDERCONLL)) {
                CoNLLNerReader.TaToConll(Collections.singletonList(taToSave), outpath);
            }

            // Save new classes
            logger.info("Saving new classes " + labels.getNewLabels());
            labels.writeToFile(config.getLabelsLocation());

        }
        // nothing happens to this...
        return "redirect:/";
    }

    @RequestMapping("/")
    public String home(Model model) {
        model.addAttribute("folders", config.getFolderNames());
        model.addAttribute("user", new User());
        return "home";
    }

    @RequestMapping(value = "/setname")
    public String setname(@ModelAttribute User user, HttpSession hs) {
        logger.info("Setting name to: " + user.getName());
        // Just make sure everything is clear first... just in case.
        logger.info("Logging in!");
        hs.removeAttribute("username");
        hs.removeAttribute("dataname");
        hs.removeAttribute("tas");

        hs.setAttribute("username", user.getName());
        return "redirect:/";
    }

    @RequestMapping(value = "/logout")
    public String logout(HttpSession hs) {
        logger.info("Logging out...");
        hs.removeAttribute("username");
        hs.removeAttribute("dataname");
        hs.removeAttribute("tas");
        return "redirect:/";
    }

    @RequestMapping(value = "/annotation", method = RequestMethod.GET)
    public String annotation(@RequestParam(value = "taid", required = false) String taid, HttpSession hs, Model model,
                             RedirectAttributes redirectAttributes) {

        TreeMap<String, TextAnnotation> tas = (TreeMap<String, TextAnnotation>) hs.getAttribute("tas");

        // Go to the homepage.
        if (tas == null) {
            return "redirect:/";
        }

        // If there's no taid, then return the getstarted page (not a redirect).
        if (taid == null) {
            return "getstarted";
        }

        if (!tas.containsKey(taid)) {
            return "redirect:/annotation";
        }

        TextAnnotation ta = tas.get(taid);
        View ner = ta.getView(ViewNames.NER_CONLL);

        model.addAttribute("ta", ta);
        model.addAttribute("taid", taid);

        logger.info(String.format("Viewing TextAnnotation (id=%s)", taid));
        logger.info("Text (trunc): " + ta.getTokenizedText().substring(
                0, Math.min(20, ta.getTokenizedText().length())));
        logger.info("Num Constituents: " + ner.getConstituents().size());
        logger.info("Constituents: " + ner.getConstituents());

        String[] text = ta.getTokenizedText().split(" ");

        // add spans to every word that is not a constituent.
        for (int t = 0; t < text.length; t++) {
            text[t] = "<span class='token pointer' id='tok-" + t + "'>" + text[t] + "</span>";
        }

        for (Constituent c : ner.getConstituents()) {

            int start = c.getStartSpan();
            int end = c.getEndSpan();

            // important to also include 'cons' class, as it is a keyword in the html
            String labelClass = labels.getCssClass(c.getLabel());
            text[start] = String.format(
                    "<span class='%s pointer cons %s' id='cons-%d-%d'>%s", c.getLabel(), labelClass,
                    start, end, text[start]);
            text[end - 1] += "</span>";
        }

        String out = StringUtils.join(" ", text);

        model.addAttribute("htmlstring", out);

        if (!tas.firstKey().equals(taid)) {
            model.addAttribute("previd", tas.lowerKey(taid));
        } else {
            model.addAttribute("previd", -1);
        }

        if (!tas.lastKey().equals(taid)) {
            model.addAttribute("nextid", tas.higherKey(taid));
        } else {
            model.addAttribute("nextid", -1);
        }

        model.addAttribute("labels", labels.toHashMap());
        model.addAttribute("secondaryLabels", labels.getSecondaryLabelNames());
        model.addAttribute("primaryLabelName", config.getPrimaryLabelName());
        model.addAttribute("labelPositions", config.getLabelPositions());
        logger.info("NEW labels " + labels.getNewLabels().toString());
        model.addAttribute("newLabels", labels.getNewLabels());

        return "annotation";
    }

    /**
     * This should never get label O
     *
     * @param labelValue
     * @param labelType
     * @param hs
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/addlabel", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void addlabel(@RequestParam(value = "labelValue") String labelValue,
                         @RequestParam(value = "labelType") String labelType, HttpSession hs,
                         Model model) throws Exception {
        logger.info("Adding new label " + labelValue + " " + labelType);
        labels.addLabel(labelValue, labelType);
    }

    /**
     * This should never get label O
     *
     * @param labelValue
     * @param labelType
     * @param hs
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/removelabel", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void removelabel(@RequestParam(value = "labelValue") String labelValue,
                         @RequestParam(value = "labelType") String labelType, HttpSession hs,
                         Model model) throws Exception {
        logger.info("Removing new label " + labelValue + " " + labelType);
        labels.removeLabel(labelValue, labelType);
    }

    /**
     * This should never get label O
     *
     * @param label
     * @param spanid
     * @param idstring
     * @param hs
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/addtoken", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void addtoken(@RequestParam(value = "label") String label, @RequestParam(value = "spanid") String spanid,
                         @RequestParam(value = "id") String idstring, HttpSession hs, Model model) throws Exception {

        logger.info(String.format("TextAnnotation with id %s: change span (id:%s) to label: %s.",
                idstring, spanid, label));

        String[] ss = spanid.split("-");
        Pair<Integer, Integer> span = new Pair<>(Integer.parseInt(ss[1]), Integer.parseInt(ss[2]));

        TreeMap<String, TextAnnotation> tas = (TreeMap<String, TextAnnotation>) hs.getAttribute("tas");

        TextAnnotation ta = tas.get(idstring);
        String[] spantoks = ta.getTokensInSpan(span.getFirst(), span.getSecond());

        String text = StringUtils.join(" ", spantoks);
        logger.info(text);
        logger.info(spanid);

        View ner = ta.getView(ViewNames.NER_CONLL);
        List<Constituent> lc = ner.getConstituentsCoveringSpan(span.getFirst(), span.getSecond());

        int origstart = span.getFirst();
        int origend = span.getSecond();
        String origlabel = null;
        if (lc.size() > 0) {
            Constituent oldc = lc.get(0);
            ner.removeConstituent(oldc);
        }

        // an O label means don't add the constituent.
        if (label.equals("O")) {
            System.err.println("Should never happen: label is O");
        } else {
            Constituent newc = new Constituent(label, ViewNames.NER_CONLL, ta, span.getFirst(), span.getSecond());
            ner.addConstituent(newc);
        }

    }

    @RequestMapping(value = "/removetoken", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void removetoken(@RequestParam(value = "tokid") String tokid, @RequestParam(value = "id") String idstring, HttpSession hs, Model model) throws Exception {

        logger.info(String.format("TextAnnotation with id %s: remove token (id:%s).", idstring, tokid));

        String[] ss = tokid.split("-");
        int inttokid = Integer.parseInt(ss[1]);
        Pair<Integer, Integer> tokspan = new Pair<>(inttokid, inttokid + 1);

        TreeMap<String, TextAnnotation> tas = (TreeMap<String, TextAnnotation>) hs.getAttribute("tas");

        TextAnnotation ta = tas.get(idstring);

        String[] spantoks = ta.getTokensInSpan(tokspan.getFirst(), tokspan.getSecond());
        String text = StringUtils.join(" ", spantoks);
        logger.info(text);

        View ner = ta.getView(ViewNames.NER_CONLL);
        List<Constituent> lc = ner.getConstituentsCoveringSpan(tokspan.getFirst(), tokspan.getSecond());

        if (lc.size() > 0) {
            Constituent oldc = lc.get(0);

            int origstart = oldc.getStartSpan();
            int origend = oldc.getEndSpan();
            String origlabel = oldc.getLabel();
            ner.removeConstituent(oldc);

            if (origstart != tokspan.getFirst()) {
                // this means last token is being changed.
                Constituent newc = new Constituent(origlabel, ViewNames.NER_CONLL, ta, origstart, tokspan.getFirst());
                ner.addConstituent(newc);
            } else if (origend != tokspan.getSecond()) {
                // this means first token is being changed.
                Constituent newc = new Constituent(origlabel, ViewNames.NER_CONLL, ta, tokspan.getSecond(), origend);
                ner.addConstituent(newc);
            }
        }
    }
}
