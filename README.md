# Annotation Interface for Named Entity Recognition

![Screenshot of web interface](/src/main/resources/static/img/screenshot.png?raw=true "Screenshot")

A tool for annotating word sequences.

**This fork focuses on annotation for multiple tasks at the same time, expecting a (possibly) large number of labels.**

## Configuration

On startup, this reads a file called config/config.json, which contains different configurations for the project. If non file with that name exists, [config/config.json.template](config/config.json.template) will be copied and used instead. As shown in the template, the file must have the following elements:

 - folders: stores paths to folders containing documents to annotate, human-redable names and their format.
 - labelFile: the path to the file with the labels.
 - primaryLabelName: the human-redable name for the main label of the project.

### Input files
As described above, the input files can be stored in different directories with different formats. The path to the directories must be given in the `folders` section of the config/config.json file.

The format is either `ta` if the files are serialized TextAnnotations, or `conll` if the files are in CoNLL NER format. See [data/eng-conll/eng.conll](data/eng-conll/eng.conll) for an example of CoNLL NER format.

TextAnnotations are the core datastructure from [illinois-cogcomp-nlp](https://github.com/IllinoisCogComp/illinois-cogcomp-nlp), from [University of Illinois CogComp group](http://cogcomp.cs.illinois.edu/).

### Labels
This software supports annotation for multiple tasks at the same time (don't confuse with multiple labels). This means your project can have several "types" of labels to be assigned to each token. There must be at least one label type, called primaryLabel, which is also used to assign colors to the tokens in the annotation interface.

The labels are stored in a single file, in json format. The location of this file must be given in the config/config.json file. For the primaryLabel, you can add the color of the tokens assigned to this class. The remaining label types are stored as lists of possible values. You can see [data/example-labels.json](data/example-labels.json) for an example.

You can also specify in config/config.json the name of the primary label, that will be shown next to the selection input in the annotation interface.


## Usage

Requires Java 8 and Maven. Run:

    $ ./run.sh

This will start the server on port 8080. Point a browser to [localhost:8080](http://localhost:8080).

It will ask you to specify a username, which is then tied to your activities in that session. All annotations
that you do will be written to a path called `<orig>-annotation-<username>`, where `<orig>` is the original path
specified in `folders.txt`, and `<username>` is what you chose as username.

Suppose you do some annotations, then leave the session, and come back again. If you log in with the same
username as the previous session, it will reload all of the annotations right where you left off, so no
work is lost.

You make annotations by clicking on words and selecting a label. If you want to remove a label, you can either press the No Label
button, or you can right click on a word.

One caveat: if you label one word, and then label an adjacent word with the same tag, these tags will be automatically joined. However,
if the word to be tagged is between two words which are already tagged, it will always join to the right tag, and not the left tag. If you
want to tag consecutive tokens separately, but with the same label (as in, Denver Colorado), you need to tag Denver first, then the word after Colorado,
then Colorado, then remove the word after Colorado. Clunky, I know. Suggestions for improvement? Open an issue or a pull request!

A document is saved either by pressing the Save button, or by pressing the Next or Previous buttons. If you navigate away using
the links on the top of the page, the document is not saved.

Currently the labels supported are LOC, ORG, GPE, and PER. These can be changed easily (just grep, and replace), and may be
generalized in a future version of this interface.

This is still in development. If you want to spend a lot of time annotating something, please make sure that the annotations are being
saved correctly as you go along.

I welcome issues and pull requests.

## Main fork changes

 - Change the labels format to support multiple types of labels.
 - Change the configuration system to support more configurations easily.
 - Change the annotation interface to support a large number of labels. The buttons showed to pick the label are changed by an input with autocomplete functionalities.
 - Add support for different types of labels. (In progress)
