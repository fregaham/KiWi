var TEMPLATE_TAGS_EDITOR_WIDGET = '\n\
<div class="tagsEditor">\n\
	<div class="tagsEditor_loading main" style="display:none"></div>\n\
	<h4>Apply Tags</h4>\n\
        <h3>Controlled Tags</h3>\n\
	<div class="tagsEditor_requiredTags"></div>\n\
        <h3>Free Tags</h3>\n\
	<div class="tagsEditor_freeTags"></div>\n\
	\n\
	<div class="tagsEditor_freeTagsEditor">\n\
		<div class="suggesterDiv"></div>\n\
		<div class=""></div>\n\
	</div>\n\
	<div class="tagsEditor_tagCloud">\n\
                <div class="hint">\n\
                    <p>KiWi can recommend tags based on this page\'s contents</p>\n\
                    <a>Recommend Tags</a>\n\
                    <div class="tagsEditor_loading inline" style="display:none"></div>\n\
                </div>\n\
		<div class="tagsEditor_required" style="display:none">\n\
			Recommended tags (click to select):<br/>\n\
			<div></div>\n\
		</div>\n\
		<div class="tagsEditor_optional" style="display:none">\n\
                        <span style="color: rgb(238, 101, 35);">Recommended tags</span> for this page (click to add):<br>'
                        /*Recommended <span class="freetags_label">free tags</span> and <span class="categorytags_label">controlled tags</span> for this page (click to add):<br/>*/
                        + '\n\
			<div></div>\n\
		</div>\n\
                <div class="noTags">Sorry, we couldn\'t recommend any tags for this page.\n\
                </div>\n\
	</div>\n\
</div>\n\
<div class="tagsEditorHelp" style="display:none">\n\
<h4>Tag Editor Help</h4>\n\
<p>\n\
In this pop up window, you may add tags to this wiki page. Tagging is a useful means of categorizing pages, tags can also be used to find pages.\n\
</p>\n\
<h3>Controlled tags and free tags</h3>\n\
<p>\n\
There are two kinds of tags: free tags and controlled tags. Free tag is just any textual label attached to the document. New free tags can be created in this pop up window by any content author. Controlled tags are managed by taxonomy administrators. You may only attach a preexisting controlled tag to a page, however you cannot create a new controlled tag here.\n\
</p>\n\
<h3>Hierarchies</h3>\n\
<p>\n\
Controlled tags form hierarchies, i.e. a tag may relate to a more general tag (e.g. <i>Switzerland</i> relates to a more general tag <i>Europe</i>). In this pop up window, you may browse the hierachies and attach one or more tags by using mouse or keyboard.\n\
</p>\n\
<table>\n\
\n\
\n\
<tr>\n\
<td class="control">\n\
Up and down arrow keys \n\
</td>\n\
<td>\n\
Select previous or next controlled tag\n\
</td>\n\
</tr>\n\
<tr>\n\
<td class="control">\n\
Left arrow key or <span class="left_arrow"></span>\n\
</td>\n\
<td>\n\
Browse one level up in the hierarchy\n\
</td>\n\
</tr>\n\
\n\
<tr>\n\
<td class="control">\n\
Right arrow key or <span class="right_arrow"></span>\n\
</td>\n\
<td>\n\
Browse one level down in the hierarchy\n\
</td>\n\
</tr>\n\
\n\
\n\
<tr>\n\
<td class="control">\n\
Space key\n\
</td>\n\
<td>\n\
Attach currently selected tag to the document\n\
</td>\n\
</tr>\n\
\n\
<tr>\n\
<td class="control">\n\
Enter\n\
</td>\n\
<td>\n\
Attach currently selected tag to the document and close the hierarchy browser\n\
</td>\n\
</tr>\n\
\n\
\n\
</table>\n\
</div>\n\
<div class="helpIcon"></div>\n\
';

