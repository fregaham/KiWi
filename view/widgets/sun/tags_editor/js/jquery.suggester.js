(function( $ ) {
	$.Suggester = function(isSecondary){
                // Initiate the WS endpoint ref
                this.ENDPOINT_TAGGING = document.location.protocol + "//" + document.location.host
                    + "/KiWi/seam/resource/services/widgets/tagging/";

                // Initiate variables
		var selectedResult = null;
		var selectedSuggestions = null;

		this.pendingRequest = false;
		this.suggestions = [];
		this.isSecondary = isSecondary;

		//Length of prefix - used for editing controlled tag in 2nd suggester
		this.fieldLength = null;
		//Parent tag object - used to keep information about parent while loading children
		this.parent = {};
		///////////////
		// Private functions
		///////////////

		/**
		 * Prepares the UI and models for loading of suggestions
		 *
		 * Returns false, if a request should not me made 
		 *
		 */
		var startLoading = $.proxy(function(){
			// TODO: Show "suggestions" box,
			// show progress indication
			if(this.pendingRequest){
				return false;
			}
			this.pendingRequest = true;

			// Reset the model
			selectedResult = null;

			//Empty div with results
			this.resultsContainer.empty();
			this.resultsContainer.css("visibility","visible");

			//Show progress indication
			showProgressIndication();
			
			return true;
		},this);

		/**
		 * Starts showing a progress indication
		 *
		 */
		var showProgressIndication = $.proxy(function(){
			this.resultsContainer.append("<span class=\"progress_bar\">Please wait... <img src=\"img/wait.gif\" ></span> ");
			this.resultsContainer.css("visiblity", "visible");
		}, this);

		/**
		 * Hides progress indication
		 */
		var hideProgressIndication = $.proxy(function(){
			this.resultsContainer.children("span.progress_bar").remove();
			this.resultsContainer.css("visibility","hidden");
			this.resultsContainer.empty();
		}, this);

		/**
		 * Starts loading suggestions
		 */
		var startLoadingSuggestions = $.proxy(function(){
			if (startLoading()) {
				this.loadingTimer = this.loadSuggestions(this.elTxt.attr("value"));
			}
		},this);

		/**
		 * Cancels loading suggestions (or cancels the timeout which would
		 * trigger loading suggestions)
		 */
		var cancelLoadingSuggestions = $.proxy(function(){
			if(this.loadingTimer != null){
				// If request is already in progress, loadingTimer is an object
				// with abort function; call it
				if (this.loadingTimer.abort !== undefined) {
					this.loadingTimer.abort();
					this.pendingRequest = false;
				}
				// Otherwise, just clear internal timeout
				else {
					window.clearTimeout(this.loadingTimer);
				}

				// If we cancelled something, hide progress indication
				hideProgressIndication();
			}

			if (this.catTimer) {
				window.clearTimeout(this.catTimer);
				this.catTimer = null;
			}
		},this);

		/**
		 * Starts loading siblings - this is needed when a controlled
		 * tag is being edited.
		 */
		var startLoadingSiblings = $.proxy(function(uri){
			if (startLoading()) {
				this.loadSiblings(uri);
			}
		},this);


		///////////////
		// Public interface
		///////////////

		/**
	 * Shows the suggester positioned below the given element
	 *
	 */
		this.show = function(options) {
			// If the component hasn't been initialized yet, do it
			if (this.el == null)
				this.init(options);

			// If "below" property was specified, position the component below
			// the specified element
			if (options && options.below) {
                            this.positionAnchorEl = options.below;

                            var offset = $(this.positionAnchorEl).position();
                            offset.top += 19;

                            this.reposition(offset);
			}


			// Reset
			this.editingMode = false;

			// Look at incoming data and setup the component accordingly
			// If we are editing a tag, populate the UI
			if (options.edit) {
				var tag = options.edit;
				this.setEditMode(true, tag);
				
				startLoadingSiblings(tag.uri);
			}
			// Or, if we are in "adding" mode, populate the UI
			else if (options.add) {
				this.elTxt.attr("value", options.add + ":");
				this.fieldLength = this.elTxt.attr("value").length;
				startLoadingSuggestions();
			}

			// And show it
			this.el.show();

			// Put focus into text field
			this.elTxt.focus();
		}

                /**
                 *
                 */
                this.reposition = function(offset){
                    this.el.css("left", offset.left + "px");
                    this.el.css("top", offset.top + "px");
                    this.el.css("position", "absolute");
                }

		/**
		* Initialize DOM elements
		*/
		this.init = function(options){
			// Create basic structure, set references to some elements
			this.el = $(document.createElement("DIV"));
			this.el.addClass("suggester");
			this.el.html("<input type='text'>"
				+ "<input type='button' value='Add'>"
				+ "<div tabindex=\"100\" class=\"invis\"></div>"
				+ "<div class=\"resultsContainer\"></div>");
			this.elTxt = $(this.el.children("input[type='text']")[0]);

			// Hookup keydown handler for the text field
			this.bindTextFieldHandler();
			//Selecting results container
			this.resultsContainer = this.el.children("div.resultsContainer");
			this.invisDiv = this.el.children("div.invis");

			// Hookup keydown handler for the results container
			this.bindInvisHandler();
			//Adding some mouse&keyboard handlers to the resultContainer
			this.bindSuggestionHandlers();

			this.bindFocusHandlers();

			// Initially hidden
			this.el.hide();
			

			// Append to the document; or to a parent element if specified
			if (options && options.parent) {
				$(options.parent).append(this.el);
			} else {
				$(document.body).append(this.el);
			}
		}
		
		this.setEditMode = function(mode, tag) {
			this.editingMode = mode;
			
			if (this.editingMode) {
				// TODO: Need to disable txtfield
				// Keep reference to the tag being edited
				this.editedTag = tag;

				// Prefill the text field
				this.elTxt.attr("value", tag.prefix + ":" + tag.label);
				
			} else {
				// TODO: Need to enable txtfield
			}
		}

		/**
		 *reultsContainer keyDown handler
		 */
		this.bindInvisHandler = function(){
			var self = this;
			
			this.invisDiv.keypress(function(e){
				// TODO: Move these where they're needed (e.g. handling keydown/up)
				//Getting number of results
				var childrenNumber = self.suggestions.length;

				//True HEIGHT of the each result. Top/bottom border + top/bottom margin = 2.
				//We have to include them there, otherwise scolling would screw up.
				var trueResultHeight = parseInt(self.resultsContainer.children(1).css("height"))+2;

				//Number of result per container height - used for scrolling stuff.
				var resultsPerPage = Math.ceil( parseInt(self.el.children("div.resultsContainer").css("height")) / trueResultHeight );

				//40 = down arrow, 38 = up arrow, 37 = left arrow, 39 = right arrow


				// key down
				if (e.keyCode == 40){
					if (selectedResult == null){
						selectedResult = 0;
						self.resultsContainer.children("div").eq(selectedResult).addClass("highlighted");
					}else if (selectedResult < childrenNumber-1) {
						// Deselect previously selected node, move and
						// select new one
						self.resultsContainer.children("div").eq(selectedResult).removeClass("highlighted");
						selectedResult++;
						self.resultsContainer.children("div").eq(selectedResult).addClass("highlighted");
						// Scrolling of result container: we will start scrolling
						// half of the page from the top of the container div
						if(selectedResult+1 > resultsPerPage/2 || self.resultsContainer.scrollTop()>0){
							//console.log(trueResultHeight *(selectedResult-Math.floor(resultsPerPage/2)));
							self.resultsContainer.scrollTop(
							trueResultHeight *(selectedResult-Math.floor(resultsPerPage/2)));
						}
					}
                                        e.preventDefault();
				}
				// key up
				else if(e.keyCode==38){
					if(selectedResult>0){
						self.resultsContainer.children("div").eq(selectedResult).removeClass("highlighted");
						selectedResult--;
						self.resultsContainer.children("div").eq(selectedResult).addClass("highlighted");

						//Scrolling of result container, we will start scrolling half of the page from the end of the container div
						if(selectedResult+1 < (childrenNumber-resultsPerPage/2) || self.resultsContainer.scrollTop()<(self.resultsContainer.children().length * self.resultsContainer.children(1).height())){
							self.el.children("div").scrollTop(trueResultHeight *(selectedResult-Math.floor(resultsPerPage/2)));
						}
					}
                                        e.preventDefault();
				}
				// key left
				else if(e.keyCode==37 && selectedResult!=null && self.suggestions[selectedResult].controlled==1
                                    && (self.resultsContainer.children("div").eq(selectedResult).hasClass("hasParent")))
                                {
					e.preventDefault();
					startLoadingParents(selectedResult);
					selectedResult = null;
				}
				// key right
				else if(e.keyCode==39 && selectedResult!=null && self.suggestions[selectedResult].hasChildren==true){
					e.preventDefault();
					startLoadingChildren(selectedResult);
					//Keeping info about the parent - we will need it when loading parets back
					this.parent = self.suggestions[selectedResult];
					selectedResult = null;
				}
				//SPACE
				else if(e.charCode==32){
					//Selecting suggestion
					if(selectedResult != null){
						var uri = self.suggestions[selectedResult].uri;
						if (uri) {
							self.selectSuggestion(uri);
						} else {
							self.elTxt.attr("value", self.suggestions[selectedResult].prefix + ":");
							startLoadingSuggestions();
						}

						// If this is a secondary instance and we are editing,
						// hide the component after space
						if ((self.isSecondary) && (self.editingMode)) {
							self.hide();
						}
					}
				}
				//ENTER
				else if(e.keyCode == 13){
					if(selectedResult != null){

						var uri = self.suggestions[selectedResult].uri;
						if (uri) {
							self.selectSuggestion(uri);
							$(self.elTxt).attr("value", "");
						} else {
							self.elTxt.attr("value", self.suggestions[selectedResult].prefix + ":");
							startLoadingSuggestions();
						}
						self.hide(true);
						selectedResult = null;
						self.elTxt.focus();
					}
				}
				//ESC key
				else if(e.keyCode==27){
					//Hiding resultsContainer and clearing him
					self.hideSuggestions(true);
					selectedResult = null;
				}
			});
		}

		//Checking if string contains semicolon
		this.hasSemicolon = function(string){
			// first ":"
			var i = string.indexOf(":");
			if (i != -1) {
				return true;
			}
			return false;
		}


		/**
		 *Suggester keyDown handler
		 */
		this.bindTextFieldHandler = function() {
			var self = this;

			this.elTxt.keypress(function(e){

				// Need to cancel loading suggestions, because:
				// - either entry will be confirmed (hence no suggestions needed)
				// - or entry is invalid, and fresher suggestions will be loaded
				cancelLoadingSuggestions();


				//input value before anyKeyDown
				var textFieldValue = self.elTxt.attr("value");
				
				//Checking not-allowed characters
				// 188 = ,
				if(e.keyCode == 188){
					e.preventDefault();
				}

				// ":"
				if (e.charCode == 58) {
					self.loadingTimer = window.setTimeout(function(){
						startLoadingSuggestions();
					}, 500);
				}

				//            A       -      Z  or         a          -      z          or   space     or               ;                ENTER               ESC                  DOWN             BACKSPACE
				if(e.charCode>=65 && e.charCode<=90 || e.charCode>=97 && e.charCode<=122 ||  e.charCode == 32 || e.charCode == 59 || e.charCode == 13 || e.keyCode == 27 || e.keyCode == 40 || e.keyCode == 8){
					//console.log($(e.currentTarget).parent());
					//59 = ";" - few things can happen:

					if((e.charCode == 59)){
						//Textfield is empty -> load all prefixes
						if(self.elTxt.attr("value")==""){
//							console.log("Only one \":\" => loading prefixes - temporary disabled");
//							self.loadingTimer = window.setTimeout(function(){
//								startLoadingSuggestions();
//							}, 2000);
						}
						//Textfield isn't empty -> more checking
						else{
							//Checking is user doesn't try to write second semi-colon
							// Note: This does not work as e.charCode == 0 (on Mac)
							if(!self.hasSemicolon(textFieldValue)){
								self.loadingTimer = window.setTimeout(function(){
									startLoadingSuggestions();
								}, 500);
							}else{
								//console.log("Second semicolon - gotcha!");
								e.preventDefault();
							}
						}
					}
					
					//KEYCODES:
					//65-90 = A-Z
					//32 = SPACE
					//188 = ,
					//186 = :
					//13 = ENTER
					//27 = ESC
					//40 = DOWN
					//8 = BACKSPACE
					//              a         -        z     or A  - Z or          space
					if(e.charCode >= 65 && e.charCode <= 90 || e.charCode>=97 && e.charCode<=122 || e.charCode == 32){
						//Checking if there isn't any semicolon on the first place
						if(self.hasSemicolon(textFieldValue) && textFieldValue.indexOf(":")==0){
//							console.log("Already has semicolon on first place - loading prefixes, stop bothering me!")
							e.preventDefault();
						}else{
							// Starts "sugestions loading timer". If there was a timer set before,
							// cancel it. If another keystroke does not come within the
							// period, we will start loading suggestions
							self.loadingTimer = window.setTimeout(function(){
								startLoadingSuggestions();
							}, 500);
						}
					}
					// key down
					else if (e.keyCode == 40 /*&& self.suggestions.length != 0*/){
						//console.log("selectedResult: "+selectedResult+", "+self.resultsContainer.children().length);
						//Suggestions are ready - but nothing hasn't been ighligted yet.
						if (selectedResult==null && self.resultsContainer.children().length > 0 ) {
							//Unfocus
							self.elTxt.blur();
							//Focus newly fed results container
							self.invisDiv.focus();
							//Selecting very first child
							self.resultsContainer.children("div").eq(0).addClass("highlighted");
							selectedResult = 0;
							self.resultsContainer.scrollTop(0);
                                                        e.preventDefault();
						} else if(self.resultsContainer.children().length == 0){
							//self.invisDiv.focus();
                                                        if (textFieldValue.empty()) {
                                                            self.onLoadSuggestions(self.allPrefixes);
                                                        } else {
                                                            startLoadingSuggestions();
                                                        }
						}else if(selectedResult > 0){
							self.invisDiv.focus();
							selectedResult++;
							self.resultsContainer.children().removeClass("highlighted");
							self.resultsContainer.children("div").eq(selectedResult).addClass("highlighted");
						}
					}
					//ESC key
					else if(e.keyCode==27){
						//Hiding resultsContainer and clearing it
						// Note: This is a hotfix; shouldn't be necessary
						// once we bind to correct event?
						window.setTimeout(function(){
							$(self.elTxt).attr("value", "");
						},0);
						
						self.hide(true);
						selectedResult = null;
					}
					//ENTER key
					else if(e.keyCode==13){
						//Adding customTag
						if(self.elTxt.attr("value") != ""){
							self.hideSuggestions(true);
							self.setEnabled(false);
							self.addCustomTag(self.elTxt.attr("value"), self);
						}

						if(self.isSecondary == true){
							self.hideSuggester();
						}
					}
					//BACKSPACE
					else if(e.keyCode==8){
						if  ((self.isSecondary == true) && (self.elTxt.attr("value").length < self.fieldLength+1)) {
							e.preventDefault();
						} else {
							if (self.elTxt.attr("value").length > 1) {
								self.loadingTimer = window.setTimeout(function(){
									startLoadingSuggestions();
								}, 500);
							} else {
								self.hideSuggestions(true);
							}
						}
					}
				}
			});
		}

		////////////////////
		// UI state management
		////////////////////

		/**
		 * Hides the whole suggester component
		 */
		this.hideSuggester = function(){
			this.el.hide();
		}

		/**
		 * Hides suggestions list
		 */
		this.hideSuggestions = function(keepFocus){
			this.resultsContainer.empty();
			this.resultsContainer.css("visibility","hidden");

			if (keepFocus) {
				this.elTxt.focus();
			}
		}

		/**
		 * Hides suggestions list or the whole component, based on
		 * whether this is primary or secondary component.
		 *
		 * If this component is primary, this function may be instructed
		 * to keep focus (i.e. move it from suggestions list
		 * to textfield).
		 */
		this.hide = function(keepFocus){
			if (!this.isSecondary) {
				this.hideSuggestions(keepFocus);
			} else {
				this.hideSuggester();
                                this.editingMode = false;
			}
		}

		/**
		 * Switches the suggester into enabled or disabled mode.
		 *
		 */
		this.setEnabled = function(enabled) {
			// Set the instance property
			this.enabled = enabled;

			// Enable/disable the text field
			this.elTxt.attr("readOnly", !this.enabled);

			// If we just enabled place the focus into text field
			if (this.enabled) {
				this.elTxt.focus();
			}
		}

		/*
		 * Sets up focus handlers to hide suggestion list or the whole
		 * suggester, if focus is lost from the component
		 */
		this.bindFocusHandlers = function(){
			var self = this;
			this.focusLostTimer = null;
			// If focus is going away ...
			this.el.focusout(function(e){
				// ... it may still come back (in a different part of the component)
				// (but if it won't, close the suggester)
				self.focusLostTimer = window.setTimeout(function(){
					self.hide(false);
				},100);
			});

			this.el.focusin(function(e){
				// ... so if it did, don't close the suggester
				if (self.focusLostTimer)
					window.clearTimeout(self.focusLostTimer);
			});

			var hasFocus = false;
			this.elTxt.focus(function(e){
				// If the text field receives focus, and is empty,
				// wait 3 secs and show all prefixes
				if (($(self.elTxt).attr("value") == "") && (!hasFocus)){
					self.catTimer = window.setTimeout(function(){
						// TODO: This was problematic, so excluding...
						self.onLoadSuggestions(self.allPrefixes);
					},3000);
					// in keyhandler: if(this.catTimer) window.cancelTimeout(this.catTimer)
				}
				hasFocus = true;
			});

			this.elTxt.blur(function(e){
				hasFocus = false;
				if (self.catTimer) {
					window.clearTimeout(self.catTimer);
					self.catTimer = null;
				}
			});
		}

		///////////////
		// Public interface - to be overriden by implementors
		///////////////

		/**
		 * This method is called by the compoment when it needs to load suggestions;
		 * integrators may need to override this method
		*
		*/
		this.loadSuggestions = function(txt){
                    var self = this;
                    return $.ajax({
                        url: this.ENDPOINT_TAGGING + "searchTags",
                        type : "GET",
                        data : {
                            q : txt
                        },
                        success: function(data){
                            self.onLoadSuggestions(data.items);
                        },
                        error: function(xhr, textStatus, errorThrown){
                            // Not completely right, but let's assume now
                            // the WS will never fail
                            self.onLoadSuggestions([]);
                        }
                    });
		}

		/**
		* This method should be called when the suggestions are ready
		*
		*/
		this.onLoadSuggestions = function(suggestions){
			this.pendingRequest = false;
			this.loadingTimer = null;
			
			//Hiding progress indication
			this.el.children("div").empty();
			this.suggestions = suggestions;
			this.fillTheDiv("suggestions");
			this.resultsContainer.css("visibility","visible");
		}

		/**
		 * Initial implementation, integrators may override this
		 */
		this.loadSiblings = function(uri){
                    var self = this;
                    $.ajax({
                        url: this.ENDPOINT_TAGGING + "getSiblings",
                        type : "GET",
                        data : {
                            uri : uri
                        },
                        success: function(data){
                            self.onLoadChildren(data.items);
                        },
                        error: function(xhr, textStatus, errorThrown){
                            // Not completely right, but let's assume now
                            // the WS will never fail
                            self.onLoadChildren([]);
                        }
                    });
		}



		var startLoadingChildren = $.proxy(function(selRes){
			if(this.pendingRequest){
				return;
			}
			this.pendingRequest = true;
			//Hide partents - EDIT:newly hiding parents in loadChildren function
			//var _child = $(this.el).find("div");
			//_child.empty();
			//Showing hidden waitin spinner
			this.resultsContainer.children("div").eq(selRes).children(".result_wait_icon").css("visibility","visible");
			this.loadChildren(this.suggestions[selRes].uri);
		},this);

                /*
                 * Initial implementation, integrators may override this
                 */
                this.loadChildren = function(uri) {
                    var self = this;
                    $.ajax({
                        url: this.ENDPOINT_TAGGING + "getChildren",
                        type : "GET",
                        data : {
                            uri : uri
                        },
                        success: function(data){
                            self.onLoadChildren(data.items);
                        },
                        error: function(xhr, textStatus, errorThrown){
                            // Not completely right, but let's assume now
                            // the WS will never fail
                            self.onLoadChildren([]);
                        }
                    });
                }


		this.onLoadChildren = function(children){
			//After simulated 2s hide the parents
			this.el.find("div.resultsContainer").empty();
			//Load children into the divs
			this.suggestions = children;
			this.fillTheDiv("children");
		}

		var startLoadingParents = $.proxy(function(selRes){
			if(this.pendingRequest){
				return;
			}
			this.pendingRequest = true;
			//Hide children - EDIT:newly hiding children in loadParents function
			//var _child = $(this.el).find("div");
			//_child.empty();

			//Showing hidden waitin spinner
			//$("#result_wait_icon"+selRes).css("visibility","visible");
			this.resultsContainer.children("div").eq(selRes).children(".result_wait_icon").css("visibility","visible");
			this.loadParents(this.suggestions[selRes].parent);
		},this);

                /*
                 * 
                 */
                this.loadParents = function(uri) {
                    var self = this;
                    $.ajax({
                        url: this.ENDPOINT_TAGGING + "getSiblings",
                        type : "GET",
                        data : {
                            uri : uri
                        },
                        success: function(data){
                            self.onLoadParents(data.items);
                        },
                        error: function(xhr, textStatus, errorThrown){
                            // Not completely right, but let's assume now
                            // the WS will never fail
                            self.onLoadParents([]);
                        }
                    });
                }

		this.onLoadParents = function(parents){
			//After simulated 2s hide the parents
			this.el.find("div.resultsContainer").empty();
			//Load children into the divs
			this.suggestions = parents;
			this.fillTheDiv("parents");
		}

                /**
                 * Returns true if given URI is root of a taxonomy
                 */
                var isTaxonomyRoot = $.proxy(function(uri){
                    if (!this.categories)
                        return false;

                    for (var i = 0; i < this.categories.length; i++) {
                        if (this.categories[i].uri == uri)
                            return true;
                    }

                    return false;
                },this);

                this.setTaxonomies = function(taxonomies) {
                    this.categories = taxonomies;
                    this.allPrefixes = [];

                    // Collect prefixes from all taxonomies
                    for (var i = 0; i < taxonomies.length; i++) {
                        this.allPrefixes.push({
                            prefix: taxonomies[i].prefix
                        });
                    }
                }

                this.setPrefixes = function(prefixes) {
                    // This is a more precise WS response; use this one
                    this.allPrefixes = prefixes;
                }

		/**
		 * Function to be overriden by integrators;
		 * called by the component when a tag needs to be added
		 */
		this.addTag = function(){

		}

		/**
		 * Function to be overriden by integrators;
		 * called by the component when a tag needs to be removed
		 */
		this.removeTag = function(tagURI){

		}

		this.fillTheDiv = function(type){
			if(this.suggestions.length == 0){
				this.resultsContainer.append("<div style=\"text-align:center\">Your query does not match any tags</div>");
				return;
			}
			//Feeding up resultsContainer
			for(var i=0;i<this.suggestions.length;i++){

				// Create label first
				// If suggestion is controlled we also have to show 'prefix' along with 'label'
				var label;
				if (this.suggestions[i].controlled == 1) {
					label = this.suggestions[i].prefix + ":" + this.suggestions[i].label;
				} else if (this.suggestions[i].uri) {
					label = this.suggestions[i].label;
				}
				// If it is not controlled, and does not have URI, it's a prefix'
				else {
					label = this.suggestions[i].prefix + ":";
				}

				// Figure out necessary class names based on tag type
				var classNames = "";
				// Checking if the suggestion isn't already selected
				// If the suggestion is already selected, make it selected in the UI
				if(selectedSuggestions[this.suggestions[i].uri]){
					classNames += "selected ";
				}

				// Controlled suggestion may need extra classes to show navigation arrows
				if(this.suggestions[i].controlled==1){
					// They're all controlled'
					classNames += "isControlled ";

					//If suggestion has a children, right arrow get visible condition via class
					if(this.suggestions[i].hasChildren==true){
						classNames += "hasChildren ";
						//Checking for parent - selectin is little lower
						if(type == "parents" && this.suggestions[i].parent == this.parent.parent ){
							selectedResult = i;
						}
					}

					//If suggestion has a parent, left arrow get visible condition via class
					if((this.suggestions[i].parent!=null) && (!isTaxonomyRoot(this.suggestions[i].parent))) {
						classNames += "hasParent ";
					}
				}
				// Free tags may need something too
				else{
					classNames += "notControlled";
				}

				// And create it!
				this.resultsContainer.append(
					"<div class=\"result_item " + classNames + " \">   <span class=\"left_arrow\"></span>   <span class=\"label\">"
						+ label
						+"</span>   <span class=\"result_wait_icon\"></span>   <span class=\"right_arrow\"></span>   </div>");

			}

			//Loading suggestions - first state
			if(type == "suggestions"){
				selectedResult = null;
				this.elTxt.focus();
			//Loading parents - second state
			}else if(type == "parents"){
				//Checking is little higher but selecting here (after puting tag into UI)
				this.resultsContainer.children().eq(selectedResult).addClass("highlighted");
			//Loading children - third state
			}else if(type == "children"){
				//Always first suggestion-children is selected
				selectedResult = 0;
				this.resultsContainer.children().eq(selectedResult).addClass("highlighted");
			}

			this.pendingRequest = false;
		}

		/**
		 *Keyboard and mouse events handler
		 */
		this.bindSuggestionHandlers = function(){
			var self = this;
			//Mouseclick action on suggestion's parts (label and arrows)
			this.resultsContainer.click(function(event){
				// Click on list shouldn't be considered as focus lost; cancel hideout timer
				if (self.focusLostTimer)
					window.clearTimeout(self.focusLostTimer);

				var el = $(event.target);
				//Clicking on the suggestion label makes it selected
				if (el.hasClass("label")) {
					//Removing highlight from all suggestions
					el.parent().parent().children().removeClass("highlighted");
					//Adding highlight to the newly selected suggestion
					el.parent().addClass("highlighted");
					selectedResult = el.parent().parent().children("div").index(el.parent());
					labelClicked();
					$(this).parent().children("div.invis").focus();
				}
				//Clicking on the LEFT-ARROW loads the PARENTS of the hovered suggestion
				else if(el.hasClass("left_arrow")) {
					el.parent().parent().children("div").removeClass("highlighted");
					selectedResult = el.parent().parent().children("div").index(el.parent());
					el.parent().addClass("highlighted");

					startLoadingParents(selectedResult);
				}
				//Clicking on the RIGHT-ARROW loads the CHILDREN of the hovered suggestion
				else if(el.hasClass("right_arrow")) {
					el.parent().parent().children("div").removeClass("highlighted");
					selectedResult = el.parent().parent().children("div").index(el.parent());
					el.parent().addClass("highlighted");
					startLoadingChildren(selectedResult);
				}
			});

			this.el.children("input").click(function(event){
				var el = $(event.target)
				if(el.attr("type") == "button" && self.elTxt.attr("value") != ""){
					self.hideSuggestions(true);
					self.setEnabled(false);
					self.addCustomTag(self.elTxt.attr("value"), self);
				}
			})
		}

		/**
		 * Selects or deselects currently focused suggestion
		 */
		this.selectSuggestion = function(uri){
			if (!this.editingMode) {
				if(selectedSuggestions[uri]){
					this.onRemoveTag(uri);
					this.onTagRemoved(uri);
				}else{
					this.onAddTag(this.getTagByUri(uri));
					this.onTagAdded(uri);
				}
			}
			// If we are in editing mode, we will add the highlighted tag
			// (if it's not already added) and remove the "edited" tag
			// - i.e. replace the edited tag with new suggestion
			else {
				if (uri != this.editedTag.uri) {
					if(!selectedSuggestions[uri]) {
						this.onAddTag(this.getTagByUri(uri));
						this.onTagAdded(uri);
					}
					this.onRemoveTag(this.editedTag.uri);
					this.onTagRemoved(this.editedTag.uri);
				}

				// TODO: hide suggestions, hide the component
                                this.hide(true);
                                selectedResult = null;
			}
		}

		this.setSelectedTags = function(tags){
			selectedSuggestions = tags;
		}

		this.onRemoveTag = function(uri){
			//Dummy implementation
		}

		this.onAddTag = function(label,uri){
			//Dummy implementation
		}

		//Function called by ENTER keydown in textfield - adding new Tag to the tagEditor freeTag category - overrided by tagEditor
		this.addCustomTag = function(label){
			//Dummy implementation
		}

		/**
		 * This callback should be called when custom tag with label
		 * is successfully added.
		 *
		 * We enable the component and reset the text field
		 */
		this.onCustomTagAdded = function() {
			this.elTxt.attr("value", "");
			this.setEnabled(true);
		}

		/**
		 * This callback should be called when custom tag with label
		 * is not successfully added.
		 *
		 * We enable the component and show the error
		 */
		this.onCustomTagFailed = function(error) {
			this.setEnabled(true);
			this.resultsContainer.html(
				"<span class='error'>" + error + "</span>");
			this.resultsContainer.css("visibility","visible");
		}

		this.onTagRemoved = function(uri){
			// Deselect the tag in the UI (if UI is shown)
			if (this.resultsContainer) {
				for(var i = 0;i<this.suggestions.length;i++){
					if(this.suggestions[i].uri == uri){
						this.resultsContainer.children("div").eq(i).removeClass("selected");
						return;
					}
				}
			}
		}

		this.onTagAdded = function(uri){
			// Select that tag in the UI (if UI is shown)
			if (this.resultsContainer) {
				var i = this.getIndexByUri(uri);
				if (i != -1)
					this.resultsContainer.children("div").eq(i).addClass("selected");
			}
		}

		/**
		 * Returns index of suggestion in the UI by URI;
		 *
		 * Returns -1 if this URI is not one of the suggestions
		 */
		this.getIndexByUri = function(uri){
			for(var i = 0;i<this.suggestions.length;i++){
				if(this.suggestions[i].uri == uri){
					return i;
				}
			}

			return -1;
		}
		/**
		 * Returns whole TAG object of suggestion in the UI by URI
		 */
		this.getTagByUri = function(uri){
			for(var j = 0;j<this.suggestions.length;j++){
				if(this.suggestions[j].uri == uri){
					return this.suggestions[j];
				}
			}
		}

		var labelClicked = $.proxy(function(){
			var uri = this.suggestions[selectedResult].uri;
			if (uri) {
				this.selectSuggestion(uri);
			} else {
				$(this.elTxt).attr("value", this.suggestions[selectedResult].prefix + ":");
				this.hide(true);
				$(this.elTxt).focus();
				startLoadingSuggestions();
			}
		},this);
	};
})(jQuery);
