(function() {
	tinymce.create('tinymce.plugins.FragmentsPlugin', {
		/**
		 * Initializes the plugin, this will be executed after the plugin has been created.
		 * This call is done before the editor instance has finished it's initialization so use the onInit event
		 * of the editor instance to intercept that event.
		 *
		 * @param {tinymce.Editor} ed Editor instance that the plugin is initialized in.
		 * @param {string} url Absolute URL to where the plugin is located.
		 */
		init : function(ed, url) {
		
			var t = this;
			t.ed = ed;
			t.selectedFragmentId = null;
			t.creating = false;
			t.editingFragmentId = null;
			t.suggestions = false;
			
			fragmentJSLib._setFragment = function (t, fragmentId) {
				if (t.creating) {
					t.fragments.renameAnnotation ("__tmp__", fragmentId);
					t.fragments.setAnnotationColor (fragmentId, [0.2, 0.0, 0.0, 0.0]); /*{backgroundColor:"#E0E0F0"});*/
				}
			}.partial(t);
			
			fragmentJSLib._cancel = function (t) {
				if (t.creating) {
					t.fragments.removeAnnotation("__tmp__");
				}
			}.partial(t);
			
			fragmentJSLib._delete = function(t) {
				if (t.creating) {
					t.fragments.removeAnnotation("__tmp__");
				}
				else {
					t.fragments.removeAnnotation(t.editingFragmentId);
				}
			}.partial(t);
			
			fragmentJSLib._deleteFragment = function(t, id) {
				t.fragments.removeAnnotation(id);
			}.partial(t);
			
			/* Transforms a fragment into a span element, deleting the fragment */
			fragmentJSLib._createSpanFromFragment = function(t, id) {
				var ret = t.fragments.createSpanFromAnnotation(id);
				// t.fragments.removeAnnotation(id);
				return ret;
			}.partial(t);
			
			fragmentJSLib._createDivFromFragment = function(t, id) {
				var ret = t.fragments.createDivFromAnnotation(id);
				return ret;
			}.partial(t);
			
			// Register the command so that it can be invoked by using tinyMCE.activeEditor.execCommand('mceKiwibookmark');
			ed.addCommand('mceFragment', function(t) {

				if (t.selectedFragmentId != null) {
					t.creating = false;
					t.editingFragmentId = t.selectedFragmentId;
				}
				else {
					t.fragments.setAnnotationColor ("__tmp__", [0.2, 0.0, 0.0, 0.0]);//{backgroundColor:"#E0E0F0"});
					t.fragments.createAnnotationAroundSelection ("__tmp__",  t.ed.selection.getRng());
					t.editingFragmentId = null;
					t.creating = true;
				}
				
				fragmentJSLib.showFragmentsPanel(null, t.editingFragmentId);
				
			}.partial(t));
			
			ed.addCommand('mceSuggestions', function(t) {
				if (fragmentJSLib.suggestionsDisplayed) {
					fragmentJSLib.hideSuggestions();
				}
				else {
					fragmentJSLib.showSuggestions();
				}
			}.partial(t));

			// Register kiwibookmark button
			ed.addButton('fragments', {
				title : 'Create/edit fragment',
				cmd : 'mceFragment',
				image : url + '/bookmark.png'
			});
			
			ed.addButton('suggestions', {
				title : 'Show/hide suggestions',
				cmd : 'mceSuggestions',
				image : url + '/suggest.png'
			});
			
			ed.onInit.add( function(t, ed) {
				t.fragments = new Annotations();
				t.fragments.init(ed.getDoc().body);
				
				t.fragments.deserializeAnnotations (ed.getDoc().body, function (t, root, id, type) {
					// We will generate the suggestion control elements for the suggestion fragments.
					if (type == "link_suggestion" || type == "item_suggestion") {
						var ret = root.ownerDocument.createElement("span");
						
						ret.setAttribute("class", "mceNonEditable");
						
						var accept = root.ownerDocument.createElement("span");
						accept.setAttribute("class", "suggestion_accept");
			
						var reject = root.ownerDocument.createElement("span");
						reject.setAttribute("class", "suggestion_reject");
					
						if (type == "link_suggestion") {
							accept.addEventListener("click", function(t, id, control, event) {
								fragmentJSLib.acceptLinkSuggestion(id, t.fragments.getAnnotationText(id), control);
							}.partial(t, id, ret), false);
						}
						else if (type == "item_suggestion") {
							accept.addEventListener("click", function(t, id, control, event) {
								fragmentJSLib.acceptItemSuggestion(id, t.fragments.getAnnotationText(id), control);
							}.partial(t, id, ret), false);
						}
						
						reject.addEventListener("click", function(t, id, control, event) {
							fragmentJSLib.rejectSuggestion(id);
						}.partial(t, id, ret), false);
						
						ret.appendChild(accept);
						ret.appendChild(reject);
						
						return ret;
					}
					else return null;
				}.partial(t));
			}.partial(t));
			
			ed.onPreProcess.add ( function(t, ed, o) {
				// clear the temporary fragment before saving
				// t.fragments.removeAnnotation("__tmp__");
				t.fragments.serializeAnnotations(o.node);
			}.partial(t));	
			
			ed.onNodeChange.add ( function(t, ed, cm, e) {
				
				var oldSelectedFragmentId = t.selectedFragmentId;
				
				t.selectedFragmentId = null;
				var n = t.fragments.firstTextNode(e);
				if (n != null) {
					var fragmentIds = t.fragments.getAnnotations(n);
					// cycle through available annotations.
					if (fragmentIds.length > 0) {
						
						t.selectedFragmentId = fragmentIds[0];
						
						var i = 0;
						for (var i = 0; i < fragmentIds.length - 1; ++i) {
							if (oldSelectedFragmentId == fragmentIds[i]) {
								t.selectedFragmentId = fragmentIds[i + 1];
							}
						}
					}
				}
				
				// If we have selected a (non-suggestion) fragment, set the fragment button active. 
				if (oldSelectedFragmentId != t.selectedFragmentId) {
					if (t.selectedFragmentId != null && t.fragments.types[t.selectedFragmentId] == null) {
						cm.setActive('fragments', true);
					}
					else {
						cm.setActive('fragments', false);
					}
					
					if (oldSelectedFragmentId != null) {
						t.fragments.setAnnotationColor (oldSelectedFragmentId, [0.2, 0.0, 0.0, 0.0]);
					}
					
					if (t.selectedFragmentId != null) {
						t.fragments.setAnnotationColor (t.selectedFragmentId, [0.2, 0.0, 1.0, 0.0]);
					}
				}
				
				cm.setActive('suggestions', fragmentJSLib.suggestionsDisplayed);
				
			}.partial(t));
		}
	});

	// Register plugin
	tinymce.PluginManager.add('fragments', tinymce.plugins.FragmentsPlugin);
})();
