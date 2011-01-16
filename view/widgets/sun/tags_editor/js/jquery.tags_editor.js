/*
 *Require:
 *- jquery.js - 1.2.6 and greater
 *- jquery.template.js
 *- tags_editor_templates.js
 *
 *
 *Usage:
 * jQuery.noConflict();
 * jQuery.listing_table = new jQuery.SimpleListing("listing_table");
 * jQuery.listing_table.deploy();
 *
 *
 * TODOs:
 * - style for a button?
 * - how to fill in selects before "analyze me" service is invoked?
 * - is web service "imune" to adding a duplicate tag?
 * - Q: Not sure about controlled=0:1 in response of GetTags.
 *     Is it that controlled==0 is a "free tag", while controlled==1 implies
 *	   a required category?
 * - Remove all console.log


 */
(function( $ ) {

//    $.SimpleListing = function(container_id){
  $.TagsEditor = function(CS, inlineTagsViewEl, RESOURCE_URI){
//--------CONSTANTS

	this.ENDPOINT_TAGGING = document.location.protocol + "//" + document.location.host
            + "/KiWi/seam/resource/services/widgets/tagging/";
        
	this.FUNC_ADDTAGS = "addTags";
	this.FUNC_GETTAGS = "listTags.json";
	this.FUNC_REMTAGS = "removeTags";
        this.FUNC_GETTAXONOMIES = "getTaxonomies";
	
	this.ENDPOINT_TAG_EXTRACT = document.location.protocol + "//" + document.location.host
            + "/KiWi/seam/resource/services/ie/tagExtraction/";

	this.FUNC_GET_LIST_REQ = "listOfRequiredTags";
	this.FUNC_EXTRACT_TAX_TAGS = "extractTaxonomyTags";

	this.MAX_SCORE				= 300.0;

        // The font size to be used for a tag with maximal score (in "em")
        // This scale also defines proportional size for other tags, 
        // i.e. a tag with score 50% of the max, gets 50% of this size
        // Note: That the font size can be further "clipped" with MAX_TAG_FONT_SIZE
        this.TAG_FONT_SCALE = 5;

	// This is a further threshold on the font-size, clipping too "big" tags
        // This is normally equal to TAG_FONT_SCALE, but may be lower
        this.MAX_TAG_FONT_SIZE = 3;

	// Tags with low score resulting with display size lower than specified
	// below are not shown in the UI (again, in "em")
	this.MIN_TAG_FONT_SIZE		= 0.5;
	
//--------MEMBER VARS
//	this.CS = CS; //container selector
        this.CS = $(document.createElement("DIV"));


        this.inlineTagsViewEl = inlineTagsViewEl;
	this.RESOURCE_URI = RESOURCE_URI;
	// templates
	this.template_widget = $.template(TEMPLATE_TAGS_EDITOR_WIDGET);

	// Map of selected tag objects (URIs to tag objects)
	this.tagsMap = {};
	// Map of "tag" labels to DOM elements representing editable free tags
	this.freeTags = {};
	// Map of "tag" labels to DOM elements representing recommended tags in tag cloud
	this.recommendedTagEls = {};
	// Map of recommended tags in the tag cloud (uri -> tag)
	this.recommendedTags = {};
	// Map of "tag" labels to DOM elements representing required tags
	this.controlledTags = {};
	// Map of a category label to a tag object, may be null
	this.currentCategoryValues = {};
	// Map of all required categories
	this.categoryMap = {};
//------------------------------------------------------------------------------
//-------------------- AJAX queue
//------------------------------------------------------------------------------
	this.ajaxQueue = [];

	/**
	 * Invokes a function in a "concurrency-safe" way; i.e. if there is a pending
	 * asynchronous request, the function is not called but stored in a queue,
	 * until ajaxDone is called in a callback.
	 *
	 * Adding and Removing a tag needs to be done in this way; otherwise, e.g.
	 * a click on a tag in tag cloud followed by immediate click on the same tag
	 * might end up in an inconsistent state on the server (Remove might be performed
	 * before Add on the server)
	 */
	this.safeInvoke = function(f){
		this.showProgress();

		// Put the function into the queue
		this.ajaxQueue.push(f);

		// If function f is the only element in the queue, invoke it immediately
		// (and wait for the callback to remove it from the queue)
		if (this.ajaxQueue.length == 1) {
			f.apply(this);
		} else {
//			console.log("Queuing only");
		}
	}

	/**
	 * Needs to be called in a callback of an AJAX request sent via
	 * safeInfoke. This performs other calls in sequential way until
	 * the queue is empty
	 */
	this.ajaxDone = function(){
		this.hideProgress();
		
		//console.log("Ajax done");
		// Remove current function from the queue; and ...
		this.ajaxQueue.shift();

		// ... if there is something more in the queue, invoke it
		if (this.ajaxQueue.length > 0) {
//			console.log("Invoking more ... deferred");
			var f = this.ajaxQueue[0];
			f.apply(this);
		}
	}

	this.progressSemaphor = 0;

	/*
	 * Shows progress indication on this widget;
	 *
	 * Actually works as a semaphor; i.e. need to call as many times hideProgress
	 * as showProgress to effectively hide the progress indication.
	 *
	 * This is useful as there are potentially two non-coordinated background
	 * efforts. First, the queued Add/Remove invocations; and then
	 * loading data (initial, or recommended tags)
	 */
	this.showProgress = function(){
		$(this.CS).find("div.tagsEditor_loading").show();
		this.progressSemaphor++;

                //console.log("showProgress:" + this.progressSemaphor);
	}

	/**
	 * Hides progress indication on this widget
	 */
	this.hideProgress = function(){
		this.progressSemaphor--;
		if (this.progressSemaphor <= 0) {
                    $(this.CS).find("div.tagsEditor_loading").hide();
                    this.progressSemaphor = 0;
                }
                    //console.log("hideProgress:" + this.progressSemaphor);
	}

//------------------------------------------------------------------------------
//-------------------- TAGS
//------------------------------------------------------------------------------

	/**
	 * Invokes GetTags web service and populates UI with tags
	 */
	this.fetchTags = function(){
		this.showProgress();

		// If we don't have live data, invoke a web service
		var self = this;
		if (!this.testData) {
			$.ajax({
						url: self.ENDPOINT_TAGGING + self.FUNC_GETTAGS,
                                                data: {
                                                    "resource" : self.RESOURCE_URI
                                                },
						type: "GET",
						success: function(data){
							self.onSuccessFetchTags(data);
						},
						error: function(xhr, textStatus, errorThrown){
							self.onFailureFetchTags(xhr, textStatus, errorThrown);
						}
			});
		}
		// Otherwise, simulate invocation with test data; call success callback
		// in a deferred way
		else {
			window.setTimeout(function(){
				self.onSuccessFetchTags(self.testData);
			}, 800);
		}
	}

	/**
	 * Success callback for GetTags web service invocation.
	 * Starts populating UI if we have all necessary data
	 */
	this.onSuccessFetchTags = function(data){
		this.hideProgress();

		this.dataGetTags = data.items;
		
		// Only when we got both - tags assigned to this document and
		// data for the required categories, we can populate the UI
		if (this.dataGetTags && this.requiredCategoryOptions) {
			//this.buildCategoryOptions();
			this.buildTags(this.dataGetTags);

			// After we're done, drop this data, we won't need it anymore
			this.dataGetTags = null;
		}
	}
	//If .categoryTags is empty -> 'none' text is shown.. argument is parent = .categoryTags

	/**
	 * Adjusts DOM class names on category containers and their control elements
	 *
	 */
	this.checkEmptyCategories = function(parent){



		// If category is empty, show "none" label
		if(parent.find("span.tag").length == 0){
			parent.addClass("empty");
			//parent.children(".none").css("display","inline");
			return true;
		}
		// Otherwise, hide everything
		else {
			parent.removeClass("empty");
			//parent.children(".none").css("display","none");
			//parent.children(".add").css("display","none");
			return false;
		}
	}
	
	this.onFailureFetchTags = function(xhr, textStatus, errorThrown){
		this.ajaxDone();
                if (typeof console != "undefined") {
                    console.log("Error getting tags:" + xhr.responseText);
                }
	}

	/**
	 * Returns true, if given tag belongs to a required category
	 *
	 * @param	tag		A tag object, with a property "uri"
	 *
	 */
	this.isRequiredCategoryTag = function(tag) {
		return this.requiredTags[tag.uri] != null;
	}

	/**
	 *
	 */
	this.invokeAddTag = function(tag) {
		var self = this;

		this.safeInvoke(function(){
			// If we are not in test mode, invoke a web service
			if (!self.testAddRemove) { 
				$.ajax({
					url: self.ENDPOINT_TAGGING + self.FUNC_ADDTAGS,
					type: "POST",
                                        data: {
                                            "resource" : self.RESOURCE_URI,
                                            // Note: $.toJSON is broken on arrays
                                            "tags" : tinymce.util.JSON.serialize([{uri:tag.uri}])
                                        },
					success: function(data){
						self.onSuccessAddTag(data.items);
					},
					error: function(xhr, textStatus, errorThrown){
						self.onFailureAddTag(xhr, textStatus, errorThrown);
					}
				});
			}
			// otherwise, just dummy deferred
			else {
				//console.log("TODO:Would invoke AddTag webservice with tags=" + tag.uri);
				window.setTimeout(function(){
					self.onSuccessAddTag();
				},500);
			}
		});
	}

	/**
	 *
	 *
	 */
	this.invokeRemoveTag = function(tagsString) {
		var self = this;
		this.safeInvoke(function(){

			// If we are not in test mode, invoke a web service
			if (!self.testAddRemove) {
				$.ajax({
					url: self.ENDPOINT_TAGGING + self.FUNC_REMTAGS,
                                        data: {
                                            "resource" : self.RESOURCE_URI,
                                            "tagURI" : tagsString
                                        },
					type: "POST",
					success: function(data){
						self.onSuccessRemoveTag(data);
					},
					error: function(xhr, textStatus, errorThrown){
						self.onFailureRemoveTag(xhr, textStatus, errorThrown);
					}
				});
			}
			// otherwise, just dummy deferred
			else {
//				console.log("TODO: Would invoke RemoveTag webservice with tags=" + tagsString);

				window.setTimeout(function(){
					self.onSuccessRemoveTag();
				},500);
			}
		});
	}


//------------------------------------------------------------------------------
//-------------------- FREE TAGS
//------------------------------------------------------------------------------

	/**
	 * Ends editing of tags in a text field; saves the new data in the back-end
	 * and closes the editing UI
	 */
	/*
	this.addTag = function(tagsString){
		//var tagsString = $(this.CS).find(".tagsEditor_freeTagsEditor input[type='text']").attr("value");

		// We are optimistic about the web service call; we'll invoke the
		// service, clear the UI and add the tags to the view immediately;
		// If the web service call fails (rare), we will show an error message
		// (and reloading the page will recover)

		// Populate free tags UI
		var tags = tagsString.split(",");
		var self = this;
		$.each(tags, function(i, tag){
			tag = $.trim(tag);
			self.buildFreeTag({label:tag});

			// If this tag is being added manually and it is also
			// in the tag cloud, we may need to make it "selected"
			if (self.recommendedTagEls[tag]) {
				self.recommendedTagEls[tag].addClass("selectedTag");
			}
		});

		this.invokeAddTag(tagsString);
		
		// Done with text-entry tags; clear the text field; switch the UI state
		// this.cancelFreeTagsEntry();
	}
	*/

	/**
	 * Cancels the free tag key entry and hides the UI (the text field and Add button)
	 */
	/*
	this.cancelFreeTagsEntry = function(){
		$(this.CS).find(".tagsEditor_freeTagsEditor input[type='text']").attr("value", "");
		$(this.CS).find(".tagsEditor_freeTagsEditor").hide();
		$(this.CS).find("A.tagsEditor_addTags").show();
	}
	*/

	/**
	 * Populates UI with free and controlled tags
	 */
	this.buildTags = function(tags) {
		// Add all tags into the UI
		var self = this;
		$.each(tags, function(i, tag){
			self.buildTag(tag);
		});

                this.adjustTagCloudHeight();

                this.adjustInlineTagsView();
	}

        this.adjustInlineTagsView = function(){
            var h = "";
            
            $.each(this.tagsMap, function(uri, tag) {
                if (tag.prefix) {
                    h += "<span class='controlled' title='" + tag.label + " (controlled tag)'"
                        + ">" + tag.prefix + ":" + tag.label;
                } else {
                    h += "<span class='free' title='" + tag.label + " (free tag)'>" + tag.label
                }

                h += "</span> ";
            });

            $(inlineTagsViewEl).html(h);
        }

	/**
	 * Adds a tag to internal models and UI.
	 *
	 */
	this.buildTag = function(tag) {
		// If this tag is already in the model, don't do anything
		if (this.freeTags[tag.uri] || this.controlledTags[tag.uri]){
			return;
		}
		
		// Update internal data models and UI
		this.tagsMap[tag.uri] = tag;

		if(tag.controlled==1) {
                    this.buildCategoryTag(tag);
		}else{
                    this.buildFreeTag(tag);
		}

		// Tell others
		this.suggester.onTagAdded(tag.uri);
		this.suggester2.onTagAdded(tag.uri);
	}

	/**
	 * Builds a free tag into the UI (not category)
	 */
	this.buildFreeTag = function(tag) {
		// Ignore, if we already have this tag in the UI
		if ((this.freeTags[tag.uri]) || (tag.label == ""))
			return;

		// Create element and add it to the DOM
		var tagEl = $(document.createElement("SPAN"));
		tagEl.attr("tagUri", tag.uri);
		tagEl.html(tag.label + "<div class='delete'/> ");

		var freeTagsContainer = $(this.CS).find(".tagsEditor_freeTags");
		freeTagsContainer.append(tagEl);

		// If the just added tag is in the tag cloud, it should get
                // selected
		if(this.recommendedTagEls[tag.uri]){
                    this.recommendedTagEls[tag.uri].addClass("selectedTag");

		} else if(this.recommendedTagEls["label:" + tag.label]){
                    var recTagEl = this.recommendedTagEls["label:" + tag.label];
                    recTagEl.addClass("selectedTag");
                    recTagEl.attr("tagUri", tag.uri);

                    // Since we have URI of this tag now, remap it
                    delete this.recommendedTagEls["label:" + tag.label];
                    this.recommendedTagEls[tag.uri] = recTagEl;
                    delete this.recommendedTags["label:" + tag.label];
                    this.recommendedTags[tag.uri] = tag;
                }

		// Keep reference to the DOM element
		this.freeTags[tag.uri] = tagEl;
	}

	/**
	 * Builds a category tag in the UI
	 */
	this.buildCategoryTag = function(tag) {
		var self = this;

		// Create element and add it to the DOM
		var tagEl = $(document.createElement("SPAN"));
		tagEl.addClass("tag");
		tagEl.attr("tagUri", tag.uri);
		tagEl.html(tag.label + "<div class='delete'/> ");
		var categoryCont = $(this.CS).find(".tagsEditor_requiredTags");

                // HACK: Since tags in Geo categories may have various prefixes, e.g. "cont", "region" and "country",
                // we don't know based on data, that they belong to the Geo taxonomy
                // So here we special-case this and treat all geo taxonomy tags as if they had geo-taxonomy root prefix
                var catPrefix = tag.prefix;
                if (tag.uri.indexOf("Geo_Category") != -1) {
                    // TODO: This should be "geo"
                    catPrefix = "geo";
                }

		//If suggester is in editingMode, we just replace the TAG - we don't even need to
		//check of the category exists
		if(this.suggester2.editingMode && this.suggester2.isSecondary){
			categoryCont.children("."+catPrefix+"CategoryContainer").find(".categoryTags").children(".gold").replaceWith(tagEl);
		}
		//Suggester isn't in editingMode
		else{
			//Checking if TAG's category-list exists (category's 'required' is 'true')
			if(this.categoryMap[catPrefix]){
				//Category-list exists so just put TAG inside
				categoryCont.children("."+catPrefix+"CategoryContainer").find(".categoryTags").append(tagEl);
				tagEl.after(tagEl.siblings("span.add"));
				self.checkEmptyCategories(categoryCont.children("."+catPrefix+"CategoryContainer").find(".categoryTags"));
			}
			//Category-list doesn't exists (category's 'required' is 'false') so we have to make additional room for it
			else{
				//We need label of the category - let's search this.requiredCategoryOptions for desired label
				for(var i = 0;i<this.requiredCategoryOptions.length;i++){
					//We've got a label
					if(this.requiredCategoryOptions[i].prefix == catPrefix){
						//We have to save this non-required category into the required category map because we don't want to make new space for it nextime
						this.categoryMap[catPrefix] = this.requiredCategoryOptions[i];
						//Building new space for this category
						this.buildCategory(catPrefix, this.categoryMap[catPrefix].label);
						//Appending the tag...
						categoryCont.children("."+catPrefix+"CategoryContainer").find(".categoryTags").append(tagEl);
						tagEl.after(tagEl.siblings("span.add"));
						//Checking empty category - in this case category isn't empty so the ADD buton wil show...
						self.checkEmptyCategories(categoryCont.children("."+catPrefix+"CategoryContainer").find(".categoryTags"));
					}
				}
			}
		}
		// Keep reference to the DOM element
		this.controlledTags[tag.uri] = tagEl;
	}

        /**
         * Adjusts the
         */
        this.adjustTagCloudHeight = function(){
            try {
                // Is there a way to achieve this in CSS?
                jQuery(".tagsEditor_tagCloud").height((jQuery(".tagsEditor").height() - jQuery(".tagsEditor_tagCloud").position().top));
            } catch (e) {
                // fail proof; exception can be thrown if the editor is not shown yet
            }
        }

	/**
	 * Adds a tag based on a tag object. Adds the tag in the backend and in the UI
	 *
	 * Is called when user selects a tag in the tag cloud
	 */
	this.addTag = function(tag){
		this.buildTag(tag);
                this.adjustTagCloudHeight();
		this.invokeAddTag(tag);

                this.adjustInlineTagsView();
	}

	/**
	 * Success callback for AddTag; does not need to do anything
	 * (besides working with the AJAX queue)
	 */
	this.onSuccessAddTag = function(){
		this.ajaxDone();
	}

	/**
	 * Failure callback for AddTag web service invocation
	 *
	 */
	this.onFailureAddTag = function(xhr, textStatus, errorThrown){
		this.ajaxDone();
                if (typeof console != "undefined") {
                    console.log("Ooops, we failed to save your new tags:"  + xhr.responseText);
                }
	}

	/**
	 * Removes a tag. Removes it from the UI and in the backend.
	 */
	this.removeTag = function(tagUri) {
		if (this.tagsMap[tagUri]) {
			delete this.tagsMap[tagUri];
		}
		//Checking if tag is 'recommended&controlled' = is represented in both categoryTags and recommendedTags
		if(this.freeTags[tagUri]&&this.recommendedTagEls[tagUri]){
			this.recommendedTagEls[tagUri].removeClass("selectedTag");
			// Remove the free tag from the UI aggressively;
			this.freeTags[tagUri].remove();
			// Also remove the reference to removed DOM element from freeTags
			delete this.freeTags[tagUri];
		}
		//Checking if tag is 'recommended&not-controlled' = is represented in both freeTags and recommendedTags
		else if(this.controlledTags[tagUri]&&this.recommendedTagEls[tagUri]){
			this.recommendedTagEls[tagUri].removeClass("selectedTag");
			// Remove the category tag from the UI aggressively;
			this.controlledTags[tagUri].remove();
			// Also remove the reference to removed DOM element from categoryTags
			delete this.controlledTags[tagUri];
		}
		//Checking if tag is in freeTags
		else if(this.freeTags[tagUri]){
			// Remove the free tag from the UI aggressively;
			this.freeTags[tagUri].remove();
			// Also remove the reference to removed DOM element
			delete this.freeTags[tagUri];
		}
		//Checking if tag is in reuquiredTags
		else if(this.controlledTags[tagUri]){
			// Remove the required tag from the UI aggressively;
			this.controlledTags[tagUri].remove();
			// Also remove the reference to removed DOM element
			delete this.controlledTags[tagUri];
		}

		

		// Tell others
		this.suggester.onTagRemoved(tagUri);
		this.suggester2.onTagRemoved(tagUri);


		// We are optimistic about the webservice not failing;
		// If it fails (rarely), we'll show an error message; reloading the page
		// would fix the problem
		this.invokeRemoveTag(tagUri);

                this.adjustTagCloudHeight();

                this.adjustInlineTagsView();
	}

	/**
	 * Does not need to do much ...
	 *
	 */
	this.onSuccessRemoveTag = function(){
		this.ajaxDone();
	}

	/**
	 * Shows an error message
	 *
	 */
	this.onFailureRemoveTag = function(xhr, textStatus, errorThrown){
		this.ajaxDone();
                if (typeof console != "undefined") {
                    console.log("Ooops, we failed to remove a tag:" + xhr.responseText);
                }
	}

//------------------------------------------------------------------------------
//-------------------- REQUIRED TAGS
//------------------------------------------------------------------------------

	/**
	 * Invokes a web service that returns list of taxonomies
	 * and populates the UI
	 */
	this.fetchRequiredCategoryOptions = function() {
		this.showProgress();

		var self = this;
		// If we don't have test data, invoke a web service'
		if (!this.testDataRequiredCategories) {
			$.ajax({
				url: self.ENDPOINT_TAGGING + self.FUNC_GETTAXONOMIES,
				success: function(data){
					self.onSuccessFetchRequiredCategoryOptions(data);
				},
				error: function(xhr, textStatus, errorThrown){
					self.onFailureFetchRequiredCategoryOptions(xhr, textStatus, errorThrown);
				}
			});
		}
		// Otherwise, push in the test data in a deferred way
		else {
			window.setTimeout(function(){
				self.onSuccessFetchRequiredCategoryOptions(self.testDataRequiredCategories);
			}, 600);
		}
	}

	/**
	 * Success handler for web service call to retrieve possible
	 * values of required categories
	 */
	this.onSuccessFetchRequiredCategoryOptions = function(data) {
		this.hideProgress();
		//Now we have all categories for controlled tags
		this.requiredCategoryOptions = data;

		var requiredTagsContainer = $(this.CS).find(".tagsEditor_requiredTags");
		requiredTagsContainer.empty();
		//Checking all categories, if category is 'required' we build space for it
		for(var i = 0;i<this.requiredCategoryOptions.length;i++){
			if(this.requiredCategoryOptions[i].required == true){
				//Building function for category
				this.buildCategory(this.requiredCategoryOptions[i].prefix,this.requiredCategoryOptions[i].label);
				//Saving required categories into the MAP for further use
				this.categoryMap[this.requiredCategoryOptions[i].prefix] = this.requiredCategoryOptions[i];
			}
		}
		this.bindControlledCategories();

		// Pass the categories on to suggesters; they may need it too
		this.suggester.setTaxonomies(this.requiredCategoryOptions);
		this.suggester2.setTaxonomies(this.requiredCategoryOptions);

		// Only when we got both - tags assigned to this document and
		// data for the required categories, we can populate the UI of tags
		if (this.dataGetTags && this.requiredCategoryOptions) {
			//this.buildCategoryOptions();
			this.buildTags(this.dataGetTags);

			// After we're done, drop this data, we won't need it anymore
			this.dataGetTags = null;
		}
	}

        this.fetchPrefixes = function(){
            var self = this;
            $.ajax({
                    url: this.ENDPOINT_TAGGING + "getPrefixes",
                    success: function(data){
                            self.suggester.setPrefixes(data);
                            self.suggester2.setPrefixes(data);
                    },
                    error: function(xhr, textStatus, errorThrown){
                        if (typeof console != "undefined")
                            console.log(xhr.responseText)
                    }
            });
        }

	this.buildCategory = function(prefix,label){
		var requiredTagsContainer = $(this.CS).find(".tagsEditor_requiredTags");
		requiredTagsContainer.append(
			"<div class=\""+prefix+"CategoryContainer categoryContainer\">"
                                + "<table><tr><td valign='top'>"
                                + "<div class=\"categoryName\">"
				+label+":</div> "
                                + "</td><td>"
                                + "<div class=\"categoryTags empty\" pref=\"" + prefix + "\">  "
				+ "<span class=\"add\"><a href=\"\">add</a> "
				+ "<div class='add'/>"
				+ "</span><span class=\"none\">none</span>  </div> <div class=\"categoryClear\"></div> "
                                + "</td></tr></table>"
                                + "</div>");
	}
	
	this.onFailureFetchRequiredCategoryOptions = function(xhr, textStatus, errorThrown){
		this.ajaxDone();
                if (typeof console != "undefined") {
                    console.log("Error getting list of possible required categories: " + xhr.responseText);
                }
	}

	/**
	 * Builds a set of SELECT compoments based on the required tags.
	 * One select per each required category.
	 *
	 * May be called repeatedly; subsequent calls clear the component and
	 * start over.
	 */
	this.buildCategoryOptions = function(){
		var requiredTagsContainer = $(this.CS).find(".tagsEditor_requiredTags");
		requiredTagsContainer.empty();

		// Reset map of required tag URIs to OPTION elements
		this.controlledTags = {};

		var self = this;
		// For each category, build one select element
		$.each(this.requiredCategoryOptions, function(i, category){
			var selectEl = document.createElement("SELECT");
			selectEl.title = category.label;
			selectEl.id = "SELECT_" + category.label; 

			// Every select has a blank option ... (at least initially)
			selectEl.options[0] = new Option("", "");

			// ... and a set of options with allowed tags
			$.each(category.narrower, function(j, tag){
				var optionEl = new Option(tag.label, tag.uri);
				selectEl.options[selectEl.options.length] = optionEl;
				self.controlledTags[tag.uri] = {
					tag: tag,
					el: optionEl
				}
			});

			// When all is done, attach to the DOM
			requiredTagsContainer.append("<label for=\"" + selectEl.id + "\">" + category.label + "</label>&nbsp;");
			requiredTagsContainer.append(selectEl);
			requiredTagsContainer.append("<br>");
		});
	}

	/**
	 * Sets currently selected values in required categories (pulldowns)
	 * based on data retrieved from GetTags web service
	 */
	/*
	this.setRequiredCategoryValues = function() {
		// Go through all tags belonging to this document;
		var self = this;
		$.each(this.tags.items, function(i, tag){
			// If a tag is in a required category, find the respective
			// OPTION element and set it as currently selected in its parent SELECT
			if (self.isRequiredCategoryTag(tag)) {
				var optionEl = self.controlledTags[tag.uri].el;
				var selectEl = optionEl.parentNode;
				selectEl.value = optionEl.value;

				// Keep reference to the currently selected tag object per each category
				self.currentCategoryValues[selectEl.title] = tag;

				// Also, since this required category already has a value
				// remove the blank option - i.e. don't let user select
				// a blank option again
				if (selectEl.options[0].value == "") {
					selectEl.remove(0);
				}
			}
		});
	}
	*/

	/**
	 * Removes the old tag and adds the newly selected tag in given SELECT
	 * element
	 */
	this.saveSelectedValue = function(selectEl) {
		// Lookup the previously selected tag for this category
		var oldTag = this.currentCategoryValues[selectEl.title];

		// Lookup the newly selected tag (by URI)
		var newTag = null;
		var newTagPairObject = this.controlledTags[selectEl.value];
		if (newTagPairObject) {
			newTag = newTagPairObject.tag;
		}

		// Keep reference to the newly selected tag in the map
		this.currentCategoryValues[selectEl.title] = newTag;

		// Proceed with backend changes only if new tag is different from
		// old tag; this is needed as this method may be called multiple times
		// in case of keypress

		// Old tag may be null -> need to save if new tag is not null
		if (!(
			((oldTag == null) && (newTag != null))
			|| ((oldTag != null) && (newTag != null) && (oldTag.uri != newTag.uri))
			))
		{
			return;
		}


		if (oldTag)
			this.invokeRemoveTag(oldTag.uri);

                // TODO: Is this right?
		if (newTag)
			this.invokeAddTag(newTag.label);

		// Also, if we have just selected a value remove the blank value
		// from options
		if (selectEl.options[0].value == "") {
			selectEl.remove(0);
		}
	}

//------------------------------------------------------------------------------
//-------------------- RECOMMENDED TAGS
//------------------------------------------------------------------------------

	/**
	 * Invokes a web service and populates the tag cloud with recommended tags
	 * in a callback;
	 *
	 * also sets tags into required categories if they had a blank value.
	 *
	 */
	this.fetchRecommendedTags = function(){
		this.showProgress();

		var self = this;

		// If we don't have test data, invoke a web service ...
		if (!this.testDataTagExtract) {
			$.ajax({
				url: self.ENDPOINT_TAG_EXTRACT + self.FUNC_EXTRACT_TAX_TAGS,
                                data: {
                                    "uri" : self.RESOURCE_URI
                                },
				success: function(data){
					self.onSuccessFetchRecommendedTags(data);
				},
				error: function(xhr, textStatus, errorThrown){
					self.onFailureFetchRecommendedTags(xhr, textStatus, errorThrown);
				}
			});
		}
		// ... otherwise set the test data in
		else {
			self.onSuccessFetchRecommendedTags(self.testDataTagExtract);
		}
	}

	/**
	 * Web service success callback
	 *
	 */
	this.onSuccessFetchRecommendedTags = function(data){
		this.hideProgress();
                $(this.CS).find(".tagsEditor_tagCloud").removeClass("noTags");
		$(this.CS).find(".tagsEditor_tagCloud").show();

		// Option 1: Values in pulldowns are automatically adjusted after recommendation
		this.adjustRequiredCategoryValues(data);

		// Option 2: Recommended categories are only shown above tag cloud
		//this.buildRecommendedCategories(data);

                this.adjustTagCloudHeight();

		this.buildRecommendedTags(data.optional);
	}

	this.onFailureFetchRecommendedTags = function(xhr, textStatus, errorThrown){
		this.ajaxDone();

                $(this.CS).find(".tagsEditor_tagCloud").addClass("noTags");
                $(this.CS).find(".tagsEditor_tagCloud").show();

                this.adjustTagCloudHeight();

                if (typeof console != "undefined") {
                    console.log("Error extracting tags from the document: " + xhr.responseText);
                }
	}

        this.hideRecommendedTags = function(){
            $(this.CS).find(".tagsEditor_tagCloud").hide();
            $(this.CS).find("A.tagsEditor_recommendTags").show();
        }

	/**
	 * Adjusts tags in required categories, if they are blank,
	 * based on the recommendations.
	 *
	 * If user has previously selected a value, that is different from
	 * the recommended one, a hint is shown.
	 */
	this.adjustRequiredCategoryValues = function(data){
		var self = this;
		// For each category, find SELECT with matching title
		// and possibly adjust its value
		$.each(data.required, function(i, category){

			var selectEl = $(this.CS).find(".tagsEditor_requiredTags")
				.find("SELECT[title='" + category.label + "']");

			// There should be exactly one SELECT element per each category;
			// if not, it's a system-error; not much to do about it on the UI
			// level
			if (selectEl.length == 1) {
				selectEl = selectEl[0];

				// Find the recommended tag for this category
				// i.e. the one with highest score
				var maxScore = -1;
				var maxScoreValue = null;
				$.each(category.narrower, function(j, tag){
					if (maxScore < tag.score) {
						maxScore = tag.score;
						maxScoreValue = tag.uri;
					}
				});
				//The recommended value is selected everytime + fadeOut/set/fadeIn effect.
				if (maxScoreValue) {
					$(selectEl).animate({opacity: 0.1},500,function(){
						selectEl.value = maxScoreValue;
						self.saveSelectedValue(selectEl);						
						$(selectEl).animate({opacity: 1},500);
					});
				};			
			}
		});
	}

	this.buildRecommendedCategories = function(data) {
		// Clear the UI
		var requiredTagsContainer = $(this.CS).find(".tagsEditor_tagCloud div.tagsEditor_required div");
		requiredTagsContainer.empty();

		var tagsCount = 0;
		var self = this;
		// For each category, find the one tag with highest score (recommended)
		$.each(data.required, function(i, category){

			var selectEl = $(self.CS).find(".tagsEditor_requiredTags")
				.find("SELECT[title='" + category.label + "']");

			// There should be exactly one SELECT element per each category;
			// if not, it's a system-error; not much to do about it on the UI
			// level
			if (selectEl.length == 1) {
				selectEl = selectEl[0];


				// Find the recommended tag for this category
				// i.e. the one with highest score
				var maxScore = -1;
				var maxScoreTag = null;
				$.each(category.narrower, function(j, tag){
					if (maxScore < tag.score) {
						maxScore = tag.score;
						maxScoreTag = tag;
					}
				});

				// If the current value in the category is different from
				// the recommended one, show a hint
				if (selectEl.value != maxScoreTag.uri) {
					var tagEl = $(document.createElement("A"));
					tagEl.html(category.label + ": " + maxScoreTag.label);
					tagEl.attr("title", maxScoreTag.uri);
					requiredTagsContainer.append(tagEl);
					tagsCount++;
				}

			}
		});

		if (tagsCount > 0)
			$(this.CS).find(".tagsEditor_tagCloud div.tagsEditor_required").show();

	}

	/*
	 * Builds a tag cloud based on optional tags.
	 *
	 * May be called repeatedly; subsequent calls clear the component and
	 * start over.
	 */
	this.buildRecommendedTags = function(recommendedTags){

		// Clear the UI and the internal maps
		var recommendedTagsContainer = $(this.CS).find(".tagsEditor_tagCloud div.tagsEditor_optional div");
		recommendedTagsContainer.empty();
		this.recommendedTags = {};
		this.recommendedTagEls = {};

		// Compute average score and max score
		var totalScore = 0;
		var avgScore = 0;
		var maxScore = 0;
		$.each(recommendedTags, function(i, tag){
			totalScore += tag.score;

			if (tag.score > maxScore)
				maxScore = tag.score;
		});
		avgScore = totalScore / recommendedTags.length;
		
		// Compute average deviation of tags higher than average
		var sumDeviation = 0;
		var countDeviation = 0;
		$.each(recommendedTags, function(i, tag){
			if (tag.score > avgScore) {
				sumDeviation += tag.score - avgScore;
				countDeviation++;
			}
		});
		var avgDeviation = sumDeviation / countDeviation;

		// If the maximal score is 4x higher than average score + average deviation,
		// let the maximal score be average score + 4x avg_deviation
		if (maxScore > avgScore + 4*avgDeviation) {
			maxScore = avgScore + 4*avgDeviation;
		}

		var tagsCount = 0;
		var self = this;
		// For each tag, build one "optional" tag UI
		$.each(recommendedTags, function(i, tag){
			// Compute the font size for this tag;
			// effectively only show tags with sizes above threshold
			var fontSize = 0;

			// If the tag score is lower than the above computed and limited max
			// compute proportional size;
			if (tag.score < maxScore) {
				fontSize = tag.score / maxScore * self.TAG_FONT_SCALE;
			} 
			// otherwise, just use max size
			else {
				fontSize = self.TAG_FONT_SCALE;
			}

                        // Ceiling threshold
                        if (fontSize > self.MAX_TAG_FONT_SIZE) {
                            fontSize = self.MAX_TAG_FONT_SIZE;
                        }

                        // Floor threshold
			if (fontSize >= self.MIN_TAG_FONT_SIZE) {
				var tagEl = $(document.createElement("A"));
                                if (tag.uri) {
                                    tagEl.attr("tagUri", tag.uri);
                                } else {
                                    tagEl.attr("tagLabel", tag.label);
                                }
				
				if (tag.controlled == 1) {
					tagEl.addClass("tagsEditor_controlled");
                                        tagEl.attr("title", tag.prefix + ":" + tag.label + " (controlled tag)");
				} else {
                                    tagEl.attr("title", tag.label + " (free tag)");
                                }
				if (self.freeTags[tag.uri] || self.controlledTags[tag.uri]){
					tagEl.addClass("selectedTag");
				}

				tagEl.html(tag.label);
				tagEl.css("font-size", fontSize + "em");
				recommendedTagsContainer.append(tagEl);
				recommendedTagsContainer.append(" ");

                                // If this is a preexisting tag, keep a reference to the element
                                // and the tag by URI
                                if (tag.uri) {
                                    // Keep reference to the DOM element in a map (if it has URI
                                    self.recommendedTagEls[tag.uri] = tagEl;

                                    // Also keep reference to the tag object itself
                                    self.recommendedTags[tag.uri] = tag;
                                } 
                                // But if this is a non-existent tag, keep a reference to the element
                                // and the tag by URI of kind "label:$label"
                                // This is a bit of a hack, but will work
                                else {
                                    // Keep reference to the DOM element in a map (if it has URI
                                    self.recommendedTagEls["label:" + tag.label] = tagEl;

                                    // Also keep reference to the tag object itself
                                    self.recommendedTags["label:" + tag.label] = tag;
                                }


				
				tagsCount++;
			}
		});

		if (tagsCount > 0)
			$(this.CS).find(".tagsEditor_tagCloud div.tagsEditor_optional").show();
	}

	//Combine all click-handling functions for tags
	this.bindTagsHandlers = function(){
		var self = this;

		//1st freeTagsHandler
		// Hookup event handlers for free tags removal
		var freeTagsContainer = $(this.CS).find(".tagsEditor_freeTags");
		freeTagsContainer.click(function(event){
			var el = $(event.target);
			if ((el.attr("tagName") == "DIV") && (el.attr("className") == "delete")) {
				var tagUri = $(el.parent()[0]).attr("taguri");
				//var tag = el.parent()[0].firstChild.textContent;
				self.removeTag(tagUri);
				/*
				self.removeTag(tagUri);
				self.suggester.selectSuggestion(tagUri);
				*/
				event.stopPropagation();
			}
		});

		//2nd recommendedTagsHandlers - tag cloud
		var recommendedTagsContainer = $(this.CS).find(".tagsEditor_tagCloud div.tagsEditor_optional div");
		recommendedTagsContainer.click(function(event){
			var el = $(event.target);
			//Checking if we are clicking on a tag (represented by link)
			if (el.attr("tagName") == "A") {
				//Checking if tag isn't already selected => already in free tag container
				if (el.hasClass("selectedTag")) {
					//If is tag already selected = is already in freeTags map and container => remove him
					el.removeClass("selectedTag");
					self.removeTag(el.attr("tagUri"));
				} else {
					//Tag hasn't been selected yet => build up new free or controlled tag
					el.addClass("selectedTag");

                                        if (el.attr("tagUri")) {
                                            var uri = el.attr("tagUri");
                                            var tag = self.recommendedTags[uri];

                                            self.addTag(tag);
                                        } else {
                                            var label = el.attr("tagLabel");
                                            var tag = {
                                                label : label
                                            }

                                            self.invokeAddTagWithLabel(label, null);
                                        }
					
				}
			}
		});

		//3rd controlledTagsHandlers
		var requiredTagsContainer = $(this.CS).find(".tagsEditor_requiredTags");
		requiredTagsContainer.click($.proxy(function(event){
			var el = $(event.target);
			//Separating X-icon from PLUS-icon - parent of PLUS-icon doesn't have a tagUri attribute
			if ((el.attr("tagName") == "DIV") && (el.attr("className") == "delete") && el.parent().attr("tagUri")) {
				var tagUri = $(el.parent()[0]).attr("taguri");
				var categoryContainer = el.parents(".categoryContainer");
				var prefix = this.tagsMap[tagUri].prefix;
				self.removeTag(tagUri);
				//Checking if the category is empty
				if(self.checkEmptyCategories(categoryContainer.find("div.categoryTags")) == true){
					//If category is empty - check if it's non-required and eventually remove it
					this.removeEmptyCategory(prefix,categoryContainer.parent());
				}
				event.stopPropagation();
			} else if ((el.attr("tagName") == "SPAN") && el.hasClass("tag")) {
				var tagUri = $(el).attr("taguri");
				var tag = self.tagsMap[tagUri];
				//Removing highlight from all controlled TAGS
				requiredTagsContainer.find("div.categoryContainer").find("div.categoryTags").children().removeClass("gold");
				//Adding highlight to the editing tag
				el.addClass("gold");
				//Showing suggester
				self.suggester2.show({below:el, edit: tag, parent:$(self.CS).find("div.tagsEditor")});
				//Hiding textfield - we don't need it in a tag editing
				self.suggester2.elTxt.css("display","none");
				//Hiding ADD button aswell
				self.suggester2.elTxt.siblings("input[type='button']").css("display","none");
				//Moving container with results little higher
				self.suggester2.elTxt.siblings("div.resultsContainer").css("top","-4px");
				//Focusing hint-list via div.invis
				self.suggester2.elTxt.siblings(".invis").focus();
			} else {
				var parent = el.parent();
				if (parent.attr("tagName") == "SPAN" && parent.hasClass("add")) {
					var prefix = parent.parent().attr("pref");
					self.suggester2.show({below:parent, add: prefix, parent:$(self.CS).find("div.tagsEditor")});
					//Showing back the textfield - we don't need it in a tag editing
					self.suggester2.elTxt.css("display","inline");
					//Showing back the ADD button aswell
					self.suggester2.elTxt.siblings("input[type='button']").css("display","inline");
					//Moving container with results little higher
					self.suggester2.elTxt.siblings("div.resultsContainer").css("top","20px");
					//Focusing hint-list via div.invis
					self.suggester2.elTxt.focus();
					//Moving the suggester the tag's ADD button
					self.suggester2.el.css("top",self.suggester2.el.position().top-22+"px");
					event.preventDefault();
				}
			}
		},this));

	}

	//This function checks if empty category is non-required and eventually removes it via URI of the last tag and the categoty element itself
	this.removeEmptyCategory = function(prefix,category){
                var catPrefix = prefix;
                if ((prefix == "region") || (prefix == "cont") || (prefix == "country"))  {
                    prefix = "geo";
                }

		if (this.categoryMap[prefix].required == false){
			delete this.categoryMap[prefix];
			category.remove();
		}
	};

	this.bindControlledCategories = function(){
		var requiredTagsContainer = $(this.CS).find(".tagsEditor_requiredTags");

		/*
		//Adding some hover action to the NONE text
		requiredTagsContainer.children(".categoryContainer")
			.mouseover(function(){
				if($(this).children(".categoryTags").children().length<3){
					$(this).css("background-color","lightGrey");
					$(this).children(".categoryTags").children(".none").css("display","none");
					$(this).children(".categoryTags").children(".add").css("display","inline");
				}

			})
			.mouseout(function(){
				if($(this).children(".categoryTags").children().length<3){
					$(this).css("background-color","white");
					$(this).children(".categoryTags").children(".add").css("display","none")
					$(this).children(".categoryTags").children(".none").css("display","inline");
				}
			});
		*/
	}

	/**
	 * Invokes AddTag web service, while only label is provided
	 *
	 */
	this.invokeAddTagWithLabel = function(label, suggester){
		var tag = {
			label: label
		}
                var self = this;
                this.safeInvoke(function(){
                    $.ajax({
                            url: this.ENDPOINT_TAGGING + this.FUNC_ADDTAGS,
                            data : {
                                    "resource" : this.RESOURCE_URI,
                                    // Note: $.toJSON is broken on arrays
                                    "tags"	   : tinymce.util.JSON.serialize([tag])
                            },
                            type:"POST",
                            success: function(data){
                                    self.onSuccessAddTagWithLabel(data, suggester);
                            },
                            error: function(xhr, textStatus, errorThrown){
                                    self.onFailureAddTagWithLabel(textStatus, suggester);
                            }
                    });
                });
	}

	this.onSuccessAddTagWithLabel = function(data, suggester) {
		this.ajaxDone();
		this.buildTags(data.items);

                if (suggester)
                    suggester.onCustomTagAdded();
	}

	this.onFailureAddTagWithLabel = function(error, suggester) {
		this.ajaxDone();

                if (suggester)
                    suggester.onCustomTagFailed(error);
	}


//------------------------------------------------------------------------------
//-------------------- DEPLOY
//------------------------------------------------------------------------------


	/**
	 * Disables ability to select and drag text in a DOM element.
	 * This is useful to disable in tag cloud where a subsequent click
	 * on a tag may result in unwanted text selection
	 *
	 * Courtesy of:
	 * http://www.dynamicdrive.com/dynamicindex9/noselect.htm
	 */
	this.disableSelection = function(target){
		if (typeof target.onselectstart!="undefined") //IE route
			target.onselectstart=function(){return false}
		else if (typeof target.style.MozUserSelect!="undefined") //Firefox route
			target.style.MozUserSelect="none"
		else //All other route (ie: Opera)
			target.onmousedown=function(){return false}
			target.style.cursor = "default"
	}

	/**
	 * Builds the DOM of this component and initiates data fetching
	 *
	 */
	this.deploy = function(){
		// TODO: This was breaking selection in the text field; need a fix
		//this.disableSelection($(this.CS)[0]);


		// Create basic DOM skeleton
		$(this.CS).append(this.template_widget, {});

		// Hook up handlers for Add Tags and Recommend Tags links
		var self = this;
		$(this.CS).find("A.tagsEditor_addTags").click(function(event){
			$(self.CS).find("A.tagsEditor_addTags").hide();
			$(self.CS).find(".tagsEditor_freeTagsEditor").show();

			// Activate the suggester component
			self.suggester.show({parent:$(self.CS).find("div.suggesterDiv")});
			
		});
		$(this.CS).find("A.tagsEditor_recommendTags").click(function(event){
			$(self.CS).find("A.tagsEditor_recommendTags").hide();
			self.fetchRecommendedTags();
		});

		// Initialize suggester
		this.suggester = new $.Suggester(false);
		this.suggester.setSelectedTags(this.tagsMap);
		// TODO: Remove this? Not used?
		this.suggester.addTag = this.addTag;

		this.suggester.onRemoveTag = $.proxy(function(uri){
			this.removeTag(uri);
		},this);

		this.suggester.onAddTag = $.proxy(function(tag){
			this.addTag(tag);
		},this);

		//Adding new TAG with label from suggester's textField - triggered by ENTER key
		this.suggester.addCustomTag = $.proxy(function(label, suggester){
			//We need to get new special URI from the server
			this.invokeAddTagWithLabel(label, suggester);
		},this);




		// Initialize secondary suggester
		this.suggester2 = new $.Suggester(true);
		//Receiving already selected tags
		this.suggester2.setSelectedTags(this.tagsMap);

		this.suggester2.onRemoveTag = $.proxy(function(uri){
			this.removeTag(uri);
		},this);

		this.suggester2.onAddTag = $.proxy(function(tag){
			this.addTag(tag);

                        // After tag is added, need to reposition the suggester below the newly added tag?
                        var tagEl = this.suggester2.positionAnchorEl.parent().find("span.tag").last();
                        var offset = tagEl.position();
                        offset.left = offset.left + tagEl.width() +5;
                        offset.top = offset.top - 3;
                        this.suggester2.reposition(offset);
		},this);

		//Adding new TAG with label from suggester's textField - triggered by ENTER key
		this.suggester2.addCustomTag = $.proxy(function(label, suggester){
			//We need to get new special URI from the server
			this.invokeAddTagWithLabel(label, suggester);
		},this);

		// Hookup some more DOM event handlers
		this.bindTagsHandlers();

		// Start fetching initial data
		this.fetchRequiredCategoryOptions();
		this.fetchTags(this.RESOURCE_URI);

                this.fetchPrefixes();
	},

        this.attach = function(parent){
            $(parent).append($(this.CS));
        }

        this.detach = function(){
            $(this.CS).remove();
        }
	

  };
})(jQuery);
