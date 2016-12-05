
$(document).ready(function() {
    /*
     * Class to represent a group of labels as a single string.
     */
    var CompoundLabel = function() {
        this.reset();
    };

    // constructor
    CompoundLabel.prototype.addValue = function(labelName, value) {
        if (value !== undefined) {
            this.labelMap[labelName] = value.split('|')[0];
            this.hasValue = true;
        }
    };

    CompoundLabel.prototype.reset = function() {
        this.labelMap = {};
        this.hasValue = false;
    };

    CompoundLabel.prototype.fromString = function(labelString) {
        this.reset();
        var labelParts = labelString.split('#');
        var labelTypes = Object.keys(labelPositions);
        for (var index = 0; index < labelTypes.length; index++) {
            var labelName = labelTypes[index];
            var value = labelParts[labelPositions[labelName]];
            this.addValue(labelName, value);
        }
        return this;
    };

    CompoundLabel.prototype.getByName = function(labelName) {
        return this.labelMap[labelName];
    };

    CompoundLabel.prototype.toString = function() {
        if (!this.hasValue) {
            return '';
        }
        var orderedLabels = [];
        var labelTypes = Object.keys(labelPositions);
        for (var index = 0; index < labelTypes.length; index++) {
            var labelName = labelTypes[index];
            orderedLabels[labelPositions[labelName]] = this.labelMap[labelName];
        }
        return orderedLabels.join('#');
    };

    /*
     * Class to abstract the set of new labels
     */
    var NewLabelSet = function(divId, newLabels) {
        this.newLabelsMap = {};
        this.div = $('#' + divId);
        newLabels = Object.keys(rawNewLabels);
        for (var typeInd = newLabels.length - 1; typeInd >= 0; typeInd--) {
            var labelType = newLabels[typeInd];
            console.log('Adding label type' + labelType);
            this.addLabelType(labelType);
            for (var valueInd = rawNewLabels[labelType].length - 1;
                 valueInd >= 0; valueInd--) {
                this._addLabelToDiv(labelType,
                                    rawNewLabels[labelType][valueInd]);
            }
        }
    };

    NewLabelSet.prototype.deleteNewLabel = function(event) {
        var labelValue = event.target.text;
        var labelType = event.target.getAttribute('value');
        var labelIndex = this.newLabelsMap[labelType].indexOf(labelValue);
        $.ajax({
                method: 'POST',
                url: '/removelabel',
                data: {labelValue: labelValue, labelType: labelType}
            }).done(function(msg) {
            });
        this.newLabelsMap[labelType].splice(labelIndex, 1);
        $('#' + event.target.id).remove();
    };

    NewLabelSet.prototype.addLabelType = function(labelType) {
        this.newLabelsMap[labelType] = [];
    };

    NewLabelSet.prototype._addLabelToDiv = function(labelType, labelValue) {
        var newElement = $('<a></a>');
        newElement.text(labelValue);
        newElement.click(function(element) {
            newLabels.deleteNewLabel(element);
        });
        newElement.attr('id', ('newlabel-' + labelType + '-' + labelValue)
            .replace(' ', '_'));
        newElement.attr('value', labelType);
        this.div.append(newElement);
        this.newLabelsMap[labelType].push(labelValue);
    };

    NewLabelSet.prototype.addLabel = function(labelType, labelValue) {
        // The label is new
        if (this.newLabelsMap[labelType] === undefined) {
            this.addLabelType(labelType);
        }

        if (this.newLabelsMap[labelType].indexOf(labelValue) == -1) {
            this._addLabelToDiv(labelType, labelValue);
            // Call the server to add the label
            $.ajax({
                method: 'POST',
                url: '/addlabel',
                data: {labelValue: labelValue, labelType: labelType}
            }).done(function(msg) {
                console.log('successful adding label.');
            });
        }
    };

    NewLabelSet.prototype.getNewLabelsByType = function(labelType) {
        return this.newLabelsMap[labelType] || [];
    };

    var primaryLabels = labels.labels;
    var newLabels = new NewLabelSet('new-labels');

    function getParameterByName(name) {
        var match = RegExp('[?&]' + name + '=([^&]*)').exec(
            window.location.search);
        return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
    }

    function addTypeaheadCompletion(labelIndex, possibleValues, focus,
                                    defaultValue) {
        var input = $('.typeahead-' + labelIndex);
        input.typeahead({
            source: possibleValues, autoselect: false,
            minLength: 0, showHintOnFocus: 'all'
        });
        if (focus === true) {
            input.focus();
        }
        if (defaultValue !== undefined) {
            input.val(defaultValue);
        }
    }

    // Add lookahead autocompletion for all label input fields.
    // Fill the inputs with the existitng label of the entity.
    function setTypeaheads(tokenElement) {
        // Get current labels
        var stringLabel = getLabel(tokenElement);
        // Fill the current label if token is labeled.
        var label = new CompoundLabel();
        if (stringLabel) {
            label.fromString(stringLabel);
        }
        // Add autocomplete for primary labels
        var possibleValues = Object.keys(primaryLabels).concat(
            newLabels.getNewLabelsByType(primaryLabelName));
        addTypeaheadCompletion('primary', possibleValues, true,
                               label.getByName(primaryLabelName));
        // Add autocomplete for secondary labels
        for (var index = 0; index < secondaryLabelNames.length; index++) {
            var labelName = secondaryLabelNames[index];
            possibleValues = labels[labelName].concat(
                newLabels.getNewLabelsByType(labelName));
            addTypeaheadCompletion(index, possibleValues, false,
                                   label.getByName(labelName));
        }

    }

    // Returns the string representing the (compound) variable
    function getLabelFromInputs(addNew) {
        var addValue = function(selector, labelName, label) {
            // Do not use typeahead('getActive'), it will return the last
            // selected element, not the current input.
            var value = $(selector).val();
            if (value != '') {
                if (addNew && $(selector).typeahead('getActive') != value) {
                    newLabels.addLabel(labelName, value);
                }
                label.addValue(labelName, value);
            }
        };
        var label = new CompoundLabel();
        addValue('.typeahead-primary', primaryLabelName, label);
        for (var index = 0; index < secondaryLabelNames.length; index++) {
            addValue('.typeahead-' + index, secondaryLabelNames[index], label);
        }
        return label.toString();
    }

    // Shows extra info about the selected label with labelName
    function showInfo(labelName) {
        // Delete old value
        $('#labelInfo').html('Loading...');
        // Read label from input
        if (labelName == primaryLabelName) {
            var inputValue = $('.typeahead-primary').val();
        } else {
            var inputIndex = secondaryLabelNames.indexOf(labelName);
            var inputValue = $('.typeahead-' + inputIndex).val();
        }
        console.log(inputValue);
        $.ajax({
            method: "GET",
            url: "/infoquery/description",
            data: {labelValue: inputValue, labelName: labelName},
            success: function(response) {
                console.log(response);
                $('#labelInfo').html(response);
                $('.collapse').collapse('show');
            }
        }).done(function (msg) {
            console.log("Information retrieved");
        });
    }

    // Add popover listener to elements.
    $('[id^=tok]').popover({
        placement: 'bottom',
        content: function() {
            var html = $('#buttons').html();
            var out = "<div id='" + $(this)[0].id + "'>" + html + "</div>";
            return out;
        },
        title: function() {
            var spanElement = $(this);
            return 'Labeled: ' + (getLabel(spanElement) || 'No label') + ', id: ' + spanElement[0].id;
        },
        html: true,
        trigger: 'manual'
    }).on('click', function() {
        $(this).popover('toggle');
        setTypeaheads($(this));
    });

    $('#removebutton').click(function() {
        $.each($('[id^=tok]'), function(i, v) {
            removelabel(v.id);
        });
    });

    // on right click, change label to nothing.
    $('[id^=tok]').contextmenu(function(event){
        event.preventDefault();
        var spanid = event.currentTarget.id;
        console.log('Right clicked on ' + spanid);

        removelabel(spanid);
    });

    // Listener for clicks in the container element.
    // Used by the buttons in the tooltip.
    $('.container').on('click', 'button', function() {
        var buttonId = $(this)[0].id;
        var spanId = $(this).parents('[id^=tok]')[0].id;

        if (buttonId == 'O') {
            removelabel(spanId);
            $('#' + spanId).popover('hide');
        } else if (buttonId == 'addClass') {
            // Get the value from inputs
            var label = getLabelFromInputs(true);
            if (label !== '') {
                addlabel(spanId, label);
            }
            $('#' + spanId).popover('hide');
        } else if (buttonId.indexOf('showInfo') != -1) {
            showInfo($(this)[0].value);
        }
    });

    // TODO (milit) Decide what to do whit this code. It closes the popover
    // if you click in the body and it's not a button. But it consumes a lot of
    // time doing the each function when the document is long.
    // The problem: I don't know the behaviour when two popovers are open at the
    // same time.
    // $('body').on('click', function(e) {
    //     $('[id^=tok]').each(function() {
    //         //the 'is' for buttons that trigger popups
    //         //the 'has' for icons within a button that triggers a popup
    //         if (!$(this).is(e.target) && $(this).has(e.target).length === 0 && $('.popover').has(e.target).length === 0) {
    //             $(this).popover('hide');
    //         }
    //     });

    // });

    // given a span id (e.g. tok-4), return the integer
    function getnum(spanid) {
        return parseInt(spanid.split("-")[1]);
    }

    // check if s is labeled...
    // this expects a string.
    function isLabeled(spanid) {
        if (spanid.charAt(0) != '#') {
            spanid = '#' + spanid;
        }
        var p = $(spanid).parent();
        return p.hasClass('cons');
    }

    // Returns the label of the spanElement or undefined, checking the first class of the parent
    function getLabel(spanElement) {
        if (isLabeled(spanElement[0].id)) {
            return spanElement.parent()[0].classList[0];
        }
    }

    function isLabel(s, label) {
        return $(s).parent().hasClass(label);
    }

    function sameparent(a, b) {
        var pa = $(a).parent().attr('id');
        var pb = $(b).parent().attr('id');
        return pa == pb;
    }

    $.fn.removeText = function() {
        for (var i = this.length - 1; i >= 0; --i) removeText(this[i]);
    };

    function removeText(node) {
        console.log(node.childNodes);
        if (!node || !node.childNodes || !node.childNodes.length) return;
        for (var i = node.childNodes.length - 1; i >= 0; --i) {
            var childNode = node.childNodes[i];
            console.log(childNode + " " + i);
            if (childNode.nodeType === 3 && (i == 0 || i == node.childNodes.length - 1))
                node.removeChild(childNode);
            //else if (childNode.nodeType === 1) removeText(childNode);
        }
    }

    // Given a span, remove the label.
    function removelabel(spanid) {
        if (!isLabeled(spanid)) {
            return;
        }
        $("#savebutton").html("<span class=\"glyphicon glyphicon-floppy-disk\" aria-hidden=\"true\"></span> Save");
        $("#savebutton").css({"border-color" : "#c00"});

        console.log("Removing label from: " + spanid);

        var tokobj = $("#" + spanid);
        var parent = tokobj.parent();
        var origparentid = parent.id;

        var tokid = getnum(spanid);

        // FIXME: needs to be aware of first and last token positions
        var prev = "#tok-" + (tokid - 1);
        var next = "#tok-" + (tokid + 1);

        // really should be... have same parent.
        //if (isLabeled(prev) && isLabeled(next)) {
        if (sameparent(tokobj, prev) && sameparent(tokobj, next)) {
            console.log("interior - don't do anything.");
        } else if (sameparent(tokobj, next)) {
            var p = tokobj.parent();
            tokobj.insertBefore(tokobj.parent());
            p.before(" ");
        } else if (sameparent(tokobj, prev)) {
            console.log(tokobj.parent());
            var p = tokobj.parent();
            tokobj.insertAfter(tokobj.parent());
            p.after(" ");
        } else {
            // just a singleton.
            var p = tokobj.parent();
            tokobj.insertBefore(p);
            p.remove();
        }

        fixspanid(parent);

        // think carefully about what to send to server.
        // we had a constituent, we remove a token on one end.
        $.ajax({
            method: "POST",
            url: "/removetoken",
            data: {tokid: spanid, id: getParameterByName("taid")}
        }).done(function (msg) {
            console.log("successful removal.");
        });
    }


    // fix the spanid of constituent span.
    function fixspanid(parent){
        var kids = $(parent).children("span");

        // if no kids, then this is the span to be removed.
        var newspanid = $(parent).attr("id");
        if(kids.length > 0) {
            var first = $(kids[0]);
            var last = $(kids[kids.length - 1]);
            var firstnum = getnum(first.attr("id"));
            var lastnum = getnum(last.attr("id")) + 1;

            newspanid = "cons-" + firstnum + "-" + lastnum;
            $(parent).attr("id", newspanid);
        }

        // remove all spans with no children.
        $("span.cons").each(function (){
            var kids = $(this).children("span");
            if(kids.length == 0){
                $(this).remove();
            }
        });

        var hh = $(parent).html();
        $(parent).removeText();

        return newspanid;
    }


    // run this function when you click on a token.
    function addlabel(spanid, newclass) {
        console.log("Adding " + newclass + " to " + spanid);

        $('#savebutton').html('<span class="glyphicon glyphicon-floppy-disk" ' +
                              'aria-hidden="true"></span> Save');
        $('#savebutton').css({'border-color' : '#c00'});

        var tokid = getnum(spanid);

        var tokobj = $("#" + spanid);

        // remove label.
        if (isLabeled(spanid)) {
            removelabel(spanid);
        }

        // FIXME: needs to be aware of first and last token positions
        var prev = '#tok-' + (tokid - 1);
        var next = '#tok-' + (tokid + 1);

        if (isLabel(next, newclass)) {
            var cons = $(next).parent();
            $(next).before(" ");
            tokobj.detach();
            cons.prepend(tokobj);
        } else if (isLabel(prev, newclass)) {
            var cons = $(prev).parent();
            $(prev).after(" ");
            tokobj.detach();
            cons.append(tokobj);
        } else {
            // all on your own.
            var prevsib = tokobj.prev();
            var nextsib = tokobj.next();

            tokobj.detach();
            var label = new CompoundLabel();
            primaryClass = label.fromString(newclass)
                .getByName(primaryLabelName);
            var cssClass = primaryLabels[primaryClass] || 'defaultLabel';
            var cons = $('<span>', {
                class: newclass + ' pointer cons ' + cssClass
            });
            cons.append(tokobj);

            // this handles when we click on the first word.
            if (!prevsib.hasClass('pointer')) {
                cons.insertBefore(nextsib);
                nextsib.before(' ');
            } else {
                cons.insertAfter(prevsib);
                prevsib.after(' ');
            }
        }

        // Now fix the label of the constituent span.
        var newspanid = fixspanid(tokobj.parent());

        $.ajax({
            method: 'POST',
            url: '/addtoken',
            data: {
                label: newclass, spanid: newspanid,
                id: getParameterByName('taid')
            }
        }).done(function(msg) {
            console.log('successful label add.');
        });
    };

    function save() {
        var taid = getParameterByName("taid");
        console.log("saving ta: " + taid);

        $.ajax({
            url: "/save",
            data: {taid: taid},
            beforeSend: function() {
                // setting a timeout
                $("#savebutton").html("<span class=\"fa fa-spinner fa-spin\"></span> Saving...");
            }
        }).done(function() {
            console.log("finished saving!");
            $("#savebutton").html("<span class=\"glyphicon glyphicon-ok\" aria-hidden=\"true\"></span> Saved!");
            $('#savebutton').css({'border-color' : ''});
            // Delete the new keys
            rawNewLabels = {};
            $('#new-labels a').remove();
            newLabels = new NewLabelSet('new-labels');
        });

    }
    $('.saveclass').click(save);
    $('#savebutton').click(save);
});
