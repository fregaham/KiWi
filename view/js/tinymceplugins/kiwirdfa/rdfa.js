/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2008-2009, The KiWi Project (http://www.kiwi-project.eu)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of the KiWi Project nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contributor(s):
 * 
 * 
 */


/** 
 * Links, RDFa and component visualization and manipulation JavaScript Library.  
 * 
 * @author Marek Schmidt
*/


function RDFa(root) {
	this.root = root;
	this.document = root.ownerDocument;

	this.selectedPropertyElement = null;
	this.selectedBlockContainerElement = null;
}

RDFa.prototype.setSelectedLinkElement = function (element) {
	if (this.selectedLinkElement != null) {
		var cls = this.selectedLinkElement.getAttribute("class");
		if (cls != null) {
			this.selectedLinkElement.setAttribute("class", cls.replace(" selected", ""));
		}
	}
	
	this.selectedLinkElement = element;
	
	if (this.selectedLinkElement != null) {
		var cls = this.selectedLinkElement.getAttribute("class");
		if (cls != null) {
			this.selectedLinkElement.setAttribute("class", cls + " selected");
		}
	}
};

RDFa.prototype.getControlElementByBlockContainerElement = function (element) {
	var e = element.firstChild;
	while(e != null && !e.hasAttribute("rdfa_block_control")) {
		e = e.nextSibling;
	}
	
	return e;
};

/* either block or iteration item */
RDFa.prototype.getAboutControlElement = function (element) {
	var e = element.firstChild;
	while(e != null && !e.hasAttribute("rdfa_block_control") && !e.hasAttribute("rdfa_iteration_item_control")) {
		e = e.nextSibling;
	}
	
	return e;
};

RDFa.prototype.getContentElementByBlockContainerElement = function (element) {
	var e = element.firstChild;
	while(e != null && !e.hasAttribute("rdfa_block_content")) {
		e = e.nextSibling;
	}
	
	return e;
};

RDFa.prototype.getIterationContentElement = function (element) {
	var e = element.firstChild;
	while(e != null && !e.hasAttribute("rdfa_iteration_content")) {
		e = e.nextSibling;
	}
	
	return e;
};

RDFa.prototype.getIterationItemIterationElement = function (item) {
	/* iteration div -> iteration content div -> iteration item div */
	return item.parentNode.parentNode;
};

RDFa.prototype.getIterationItemControlElement = function (element) {
	var e = element.firstChild;
	while(e != null && !e.hasAttribute("rdfa_iteration_item_control")) {
		e = e.nextSibling;
	}
	
	return e;
};

RDFa.prototype.getIterationItemContentElement = function (element) {
	var e = element.firstChild;
	while(e != null && !e.hasAttribute("rdfa_iteration_item_content")) {
		e = e.nextSibling;
	}
	
	return e;
};

/* about element is can either be "block container" or "iteration item" */
RDFa.prototype.setSelectedAboutElement = function (element) {
	
	if (this.selectedAboutElement != null) {
		var cls = this.selectedAboutElement.getAttribute("class");
		if (cls != null) {
			this.selectedAboutElement.setAttribute("class", cls.replace(" selected", ""));
		}
	}
	
	this.selectedAboutElement = element;
	
	if (this.selectedAboutElement != null) {
		var cls = this.selectedAboutElement.getAttribute("class");
		if (cls != null) {
			this.selectedAboutElement.setAttribute("class", cls + " selected");
		}
	}
	
	/*if (this.selectedAboutElement != null) {
		for (var p in this.block_css_style) {
			this.selectedAboutElement.style[p] = this.block_css_style[p];
		}
		
		var selectedAboutControlElement = this.getAboutControlElement (this.selectedAboutElement);
		if (selectedAboutControlElement != null) {
			for (var p in this.block_control_css_style) {
				selectedAboutControlElement.style[p] = this.block_control_css_style[p];
			}
		}
	}
	
	this.selectedAboutElement = element;
	
	if (this.selectedAboutElement != null) {
		for (var p in this.block_css_style) {
			this.selectedAboutElement.style[p] = this.block_css_style_selected[p];
		}
		
		var selectedAboutControlElement = this.getAboutControlElement (this.selectedAboutElement);
		if (selectedAboutControlElement != null) {
			for (var p in this.block_control_css_style_selected) {
				selectedAboutControlElement.style[p] = this.block_control_css_style_selected[p];
			}
		}
	}*/
};

RDFa.prototype.getRoot = function() {
	return this.root;
};

/*
 * Set the property of the selected span
 */
/*RDFa.prototype.setProperty = function (element, property) {
	 element.setAttribute("property", property);
}
 
RDFa.prototype.getProperty = function (element) {
	 return element.getAttribute ("property");
}*/
 
RDFa.prototype.setAboutUri = function (element, uri) {
	element.setAttribute("about", uri);
};

RDFa.prototype.getAboutUri = function (element) {
	return element.getAttribute ("about");
};


RDFa.prototype.setAboutTypes = function (element, type) {
	element.setAttribute("typeof", type);
};

/**
 * Get the value of the typeof attribute, or an empty string, if it doesn't exist. 
 * @param element
 * @returns
 */
RDFa.prototype.getAboutTypes = function (element) {
	var ret = element.getAttribute ("typeof");
	if (ret == null) {
		return "";
	}
	
	return ret;
};

RDFa.prototype.setAboutRelation = function (element, relation) {
	element.setAttribute("rel", relation);
};

RDFa.prototype.getAboutRelation = function (element) {
	return element.getAttribute ("rel");
};

RDFa.prototype.clearElement = function(element) {
	while (element.firstChild != null) {
		element.removeChild(element.firstChild);
	}
};

RDFa.prototype.setAboutTitle = function (element, title) {
	/* title span is the first child of the control element. */
	var controlDiv = this.getControlElementByBlockContainerElement (element);
	var titleSpan = controlDiv.childNodes.item(0);
	
	this.clearElement (titleSpan);
	titleSpan.appendChild(this.document.createTextNode(title));
};

RDFa.prototype.getLinkResource = function (element) {
	return element.getAttribute("resource");
};

RDFa.prototype.getLinkRel = function (element) {
	return element.getAttribute("rel");
};

RDFa.prototype.getLinkProperty = function (element) {
	return element.getAttribute("property");
};

RDFa.prototype.getLinkContent = function (element) {
	return element.getAttribute("content");
};

RDFa.prototype.getLinkText = function (element) {
	return this.getText(element);
};

RDFa.prototype.setLink = function(element, resource, rel, property, content, text) {
	if (resource == null || resource == "") {
		element.removeAttribute("resource");
	}
	else {
		element.setAttribute("resource", resource);
	}
	
	if (rel == null || rel == "") {
		element.removeAttribute("rel");
	}
	else {
		element.setAttribute("rel", rel);
	}
	
	if (property == null || property == "") {
		element.removeAttribute("property");
	}
	else {
		element.setAttribute("property", property);
	}
	
	if (content == null || content == "") {
		element.removeAttribute("content");
	}
	else {
		element.setAttribute("content", content);
	}
	
	if (text != null && text != "") {
		this.clearElement(element);
		element.appendChild(element.ownerDocument.createTextNode(text));		
	}
	
	this.createLinkInternal(element);
};

RDFa.prototype.isRDFaLinkElement = function (element) {
	// IEBUG: elmenet.ELEMENT_NODE is not defined in IE
    var ELEMENT_NODE = 1;	
	if (element.nodeType != ELEMENT_NODE) return false;
	
	// alert("element " + element.nodeName + " typeof: " + element.hasAttribute("typeof") + " rel:" + element.hasAttribute("rel"));
	
	if (element.nodeName.toLowerCase() == "span" && (element.hasAttribute("resource") || element.hasAttribute("rel") || element.hasAttribute("property"))) {
		return true;
	}
	
	return false;
};

RDFa.prototype.isRDFaBlockContainerElement = function (element) {
	// IEBUG: elmenet.ELEMENT_NODE is not defined in IE
    var ELEMENT_NODE = 1;	
	if (element.nodeType != ELEMENT_NODE) return false;
	
	if (element.hasAttribute("rdfa_block_container")) {
		return true;
	}
	
	return false;
};

/*
 * Get the nearest RDFa property element on the way up the tree
 */
RDFa.prototype.getLeastRDFaLinkElement = function (element) {
	while (element != null && element != this.getRoot() && !this.isRDFaLinkElement(element)) {
		element = element.parentNode;
	}
	
	if (element == this.getRoot()) return null;
	
	return element;
};
 
 /*
  * Get the nearest RDFa block container element on the way up the tree
  */
 RDFa.prototype.getLeastRDFaAboutElement = function (element) {
 	while (element != null && element != this.getRoot() && !this.isRDFaBlockContainerElement(element) /*&& !this.isRDFaIterationItemContainerElement(element)*/) {
 		element = element.parentNode;
 	}
 	
 	if (element == this.getRoot()) return null;
 	
 	return element;
 };

RDFa.prototype.isSameNode = function (n1, n2) {
    if (n1.isSameNode) {
        return n1.isSameNode(n2);
    }
    else {
        return n1 == n2;
    }
};

/* 
* returns a previous text node, or null if outside the subtree defined by "root"
*/
RDFa.prototype.previousTextNode = function(root, node) {

    
	if (this.isSameNode(root, node))
		return null;

    // IEBUG: IE does not support Node.TEXT_NODE
    var TEXT_NODE = 3; 

	while (true) {
		while (!node.previousSibling) {
			node = node.parentNode;
			if (!node || this.isSameNode(root, node))
				return null;
		}

		node = node.previousSibling;

		while (node.nodeType != TEXT_NODE
				&& node.lastChild != null) {
			node = node.lastChild;
		}

		if (node.nodeType == TEXT_NODE)
			return node;
	}
};

/**
* returns a first text node below the "root" element, or null if none exists
*/
RDFa.prototype.firstTextNode = function(root) {

	var node = root;
	
	// IEBUG: IE does not support Node.TEXT_NODE
	var TEXT_NODE = 3;

	while (true) {
		while (node.nodeType != TEXT_NODE
				&& node.firstChild != null) {
			node = node.firstChild;
		}

		if (node.nodeType == TEXT_NODE)
			return node;

		if (this.isSameNode(node, root))
			return null;

		while (node.nextSibling == null) {
			node = node.parentNode;
			if (this.isSameNode(node, root))
				return null;
		}

		node = node.nextSibling;
	}
};

/*
* returns the last text node below some "root" element or null if none exists
*/
RDFa.prototype.lastTextNode = function(root) {
	var node = root;
	
	// IEBUG: IE does not support Node.TEXT_NODE
	var TEXT_NODE = 3;

	while (true) {
		while (node.nodeType != TEXT_NODE
				&& node.lastChild != null) {
			node = node.lastChild;
		}

		if (node.nodeType == TEXT_NODE)
			return node;

		if (this.isSameNode(node, root))
			return null;

		while (node.previousSibling == null) {
			node = node.parentNode;
			if (this.isSameNode(node, root))
				return null;
		}

		node = node.previousSibling;
	}
};

/*
* same as previousTextNode, but different 
*/
RDFa.prototype.nextTextNode = function(root, node) {

	// IEBUG: IE does not support Node.TEXT_NODE
	var TEXT_NODE = 3;
	
	if (this.isSameNode(root, node))
		return null;
	while (true) {
		while (node.nextSibling == null) {
			node = node.parentNode;
			if (node == null || this.isSameNode(node, root))
				return null;
		}

		node = node.nextSibling;

		while (node.nodeType != TEXT_NODE
				&& node.firstChild != null) {
			node = node.firstChild;
		}

		if (node.nodeType == TEXT_NODE)
			return node;
	}
};

RDFa.prototype.getText = function(root) {
	var node = this.firstTextNode(root);
	var ret = "";
	while (node != null) {
		ret += node.nodeValue;
		node = this.nextTextNode(root, node);
	}
	
	return ret;
};

/*
* Splits the text node at offset
* returns an array of two newly created text nodes
*/
RDFa.prototype.splitTextNode = function(n, offset) {
	var p = n.parentNode;
	var text = n.nodeValue;

    var doc = n.ownerDocument;
	var n1 = doc.createTextNode(text.substring(0, offset));
	var n2 = doc.createTextNode(text.substring(offset,
			n.length));

	p.replaceChild(n2, n);
	p.insertBefore(n1, n2);

	var ret = [];
	ret.push(n1);
	ret.push(n2);
	return ret;
};

RDFa.prototype.getLCP = function (n1, n2) {
	return null;
};

RDFa.prototype.deleteSpan = function (span) {
	var p = span.parentNode;
	while (span.firstChild != null) {
		p.insertBefore (span.firstChild, span);
	}
	p.removeChild (span);
};

RDFa.prototype.deleteAbout = function (div) {
	var p = div.parentNode;
	p.removeChild (div);
};

RDFa.prototype.deleteLink = function (span) {
	var p = span.parentNode;
	while(span.firstChild != null) {
		var c = span.firstChild;
		p.insertBefore(c, span);
	}
	
	p.removeChild (span);
};

RDFa.prototype.deleteIteration = function (div) {
	var p = div.parentNode;
	p.removeChild(div);
};

RDFa.prototype.deleteIterationItem = function (item) {
	var p = item.parentNode;
	p.removeChild(item);
};


RDFa.prototype.createAboutInternal = function (containerDiv, controlDiv, contentDiv) {
	var doc = this.root.ownerDocument;
	var titleSpan = doc.createElement("span");
	
	controlDiv.appendChild(titleSpan);
	
	var controlModify = doc.createElement("span");
	controlModify.setAttribute("class", "rdfa_block_modify");
	controlModify.setAttribute("title", "Modify");
	controlDiv.appendChild(controlModify);
	
	var controlDelete = doc.createElement("span");
	controlDelete.setAttribute("class", "rdfa_block_delete");
	controlDelete.setAttribute("title", "Delete");
	controlDiv.appendChild(controlDelete);
	
	var controlEnter = doc.createElement("span");
	controlEnter.setAttribute("class", "rdfa_block_enter");
	controlEnter.setAttribute("title", "Enter");
	controlDiv.appendChild(controlEnter);
	
	controlModify.addEventListener("click", function(t, container, event) {
		t.onAboutModify(container);
	}.partial(this, containerDiv), false);
	
	controlDelete.addEventListener("click", function(t, container, event) {
		// t.onAboutDelete(container);
		var p = container.parentNode;
		p.removeChild (container);
	}.partial(this, containerDiv), false);
	
	controlEnter.addEventListener("click", function(t, container, event) {
		var doc = containerDiv.ownerDocument;
		var p = doc.createElement("p");
		containerDiv.parentNode.insertBefore(p, containerDiv.nextSibling);
		
		if (containerDiv.ownerDocument.defaultView) {
			var view = containerDiv.ownerDocument.defaultView;
			if (view.getSelection) {
				var sel = view.getSelection();
				var rng = doc.createRange();
				rng.setStart(p, 0);
				rng.setEnd(p, 0);
				
				sel.removeAllRanges();
				sel.addRange(rng);
			}
			window.getSelection();
		}
	}.partial(this, containerDiv), false);
	
	containerDiv.setAttribute("class", "rdfa_block_container");
	controlDiv.setAttribute("class", "mceNonEditable rdfa_block_control");
	contentDiv.setAttribute("class", "rdfa_block_content");
	
	// HTML5 contenteditable attributge
	//containerDiv.contentEditable = "false";
	//contentDiv.contentEditable = "true";

	containerDiv.setAttribute("rdfa_block_container", "true");
	controlDiv.setAttribute("rdfa_block_control", "true");
	contentDiv.setAttribute("rdfa_block_content", "true");
	
	containerDiv.addEventListener("DOMSubtreeModified", function(t, container, event) {
		/* Check consistency */
		var contentDiv = t.getContentElementByBlockContainerElement (container);
		if (contentDiv == null) {
			// contentDiv was deleted... delete the whole damn thing
			var p = container.parentNode;
			p.removeChild (container);
		}
	}.partial(this, containerDiv), false);
	
};

RDFa.prototype.createAbout = function (sel) {
	var doc = this.root.ownerDocument;	
	var contentDiv = doc.createElement("div");
	var contentDivWrapper = doc.createElement("div");
	var controlDiv = doc.createElement("div");
	var containerDiv = doc.createElement("div");
	
	
	contentDiv = this.createElementFromSelection (sel, contentDiv);
	
	/* Add a br element to the beginning, so the user have to do one more backspace to delete the whole content... */
	contentDiv.appendChild(doc.createElement("br"));
	
	var p = contentDiv.parentNode;
	p.replaceChild(containerDiv, contentDiv);
	containerDiv.appendChild(controlDiv);
	containerDiv.appendChild(contentDivWrapper);
	contentDivWrapper.appendChild (contentDiv);
	
	this.createAboutInternal (containerDiv, controlDiv, contentDivWrapper);
	
	return containerDiv;
};

RDFa.prototype.createAboutFromAboutNode = function (containerDiv) {
	var doc = this.root.ownerDocument;	
	var contentDivWrapper = doc.createElement("div");
	var controlDiv = doc.createElement("div");
	// var containerDiv = doc.createElement("div");
	
	while(containerDiv.firstChild != null) {
		contentDivWrapper.appendChild(containerDiv.firstChild);
	}
	
	containerDiv.appendChild(controlDiv);
	containerDiv.appendChild(contentDivWrapper);
	
	this.createAboutInternal (containerDiv, controlDiv, contentDivWrapper);
	
	return containerDiv;
};

/* 
 * do whatever DOM manipulations necessary to convert true RDFa to internal representation (for visualizations, etc...) 
 */
RDFa.prototype.postLoad = function (o) {
	/* Add CSS style for visualization */
	
	setStyleRecursive = function(t, e) {
	
		if (t.isRDFaLinkElement(e)) {
			t.createLinkInternal(e);
		}
		
		/* Construct about or iteration item */
		if(e.nodeType == 1 && e.hasAttribute("about")) {
			var doc = t.root.ownerDocument;
			var containerDiv = e;
			var contentDiv = doc.createElement("div");
			var controlDiv = doc.createElement("div");
			
			while (e.firstChild != null) {
				var c = e.firstChild;
				e.removeChild(c);
				contentDiv.appendChild(c);
			}
			
			containerDiv.appendChild(controlDiv);
			containerDiv.appendChild(contentDiv);
			
			t.createAboutInternal(containerDiv, controlDiv, contentDiv);
			e = contentDiv;
		}

		var children = e.childNodes;
		for (var i = 0; i < children.length; ++i) {
			var child = children.item(i);
			setStyleRecursive (t, child);
		}
	};
	
	setStyleRecursive (this, o);
};

/*
 * Do whatever DOM manipulations to convert the editor representation into a true RDFa format
 */
RDFa.prototype.preStore = function (o) {
	 clearStyleRecursive = function (t, e) {
		 
		if (t.isRDFaLinkElement(e)) {
			var cls = e.getAttribute("class");
			if (cls != null) {
				cls = cls.replace(" selected", "");
				cls = cls.replace(" link", "");
				cls = cls.replace(" object", "");
				cls = cls.replace(" datatype", "");
				cls = cls.replace(" nonsemantic", "");
				e.setAttribute("class", cls);
			}
		}
		
		/* Delete the control elements in components and move the children of the content element... */
		if (t.isRDFaBlockContainerElement(e)) {
			var content = t.getContentElementByBlockContainerElement(e);
			t.clearElement(e);
			
			while (content.firstChild != null) {
				var c = content.firstChild;
				content.removeChild(c);
				e.appendChild(c);
			}
			
			e.removeAttribute("rdfa_block_container");
			e.setAttribute("class", "");
		}
			
		var children = e.childNodes;
		for (var i = 0; i < children.length; ++i) {
			var child = children.item(i);
			clearStyleRecursive (t, child);
		}
	 };
	 
	 clearStyleRecursive (this, o);
};

RDFa.prototype.createElementFromSelectionIE = function (sel, element) {
    var d = sel.duplicate();
    
    var p = sel.parentElement();
    d.moveToElementText (p);
    
    var f = this.firstTextNode(p);
    var e = this.lastTextNode(p);
    
    var fi = 0;
    while (true) {
        if (d.compareEndPoints ("StartToStart", sel) == 0) {
            break;
        }

        fi += 1;
        d.moveStart("character");

        if (fi >= f.length) {
            f = this.nextTextNode(p, f);
            fi = 0;

            if (f == null) break;
        }
    }

    if (f == null) {
        alert("f is null");
        return null;
    }

    var ei = e.length;
    while (true) {
        if (d.compareEndPoints ("EndToEnd", sel) == 0) {
            break;
        }

        ei -= 1;
        d.moveEnd("character", -1);

        if (ei <= 0) {
            e = this.previousTextNode(p, e);
            ei = e.length;

            if (e == null) break;
        }
    }

    if (e == null) {
        alert ("e is null!");
        return null;
    }

    
    if (f != e) {
		if (fi != 0) {
			f = this.splitTextNode(f, fi)[1];
		}

		if (ei != 0) {
			e = this.splitTextNode(e, ei)[0];
		} else {
			e = this.previousTextNode(this.getRoot(), e);
		}
	} else {
		var diff = ei - fi;
		rest = this.splitTextNode(f, fi)[1];
		e = f = this.splitTextNode(rest, diff)[0];
	}

	var lst = [];
	while (f != null) {
		
		lst.push (f);
		
		if (f == e)
			break;

		f = this.nextTextNode(this.getRoot(), f);
	}
	
	f.parentNode.replaceChild(element, f);
	
	/* Now import all of those to the span */
	for (var i = 0; i < lst.length; ++i) {
		var node = lst[i];
		element.appendChild (node);
	}
	
	return element;

};

RDFa.prototype.createElementFromSelection = function (sel, element) {
	// IEBUG: IE does not support w3c Range
    if (!window.getSelection && document.selection) {
        return this.createElementFromSelectionIE (sel, element);
    }
    

    var ret = null;
    
    var startOffset = sel.startOffset;
    var endOffset = sel.endOffset;
    
	var f = this.firstTextNode(sel.startContainer);
	var e = this.lastTextNode(sel.endContainer);
	
	if (sel.startContainer.nodeType == 1) {
		// element, offsets doesn't have valid values
		startOffset = 0;
	}
	if (sel.endContainer.nodeType == 1) {
		endOffset = 0;
		if (e != null) {
			// actually, as and end offset, it needs to be the length of the last text content
			endOffset = e.nodeValue.length;
		}
	}
		
	if (f == null && e == null && sel.startContainer == sel.endContainer) {
		// Special case: empty selection on empty element
		sel.startContainer.appendChild(element);
		ret = element;
	}
	else 
	{
		// selections in between of elements (comtainer is empty element), for f and e nodes...
		if (f == null) {
			f = this.nextTextNode(this.getRoot(), f);
		}
		
		if (e == null) {
			e = this.previousTextNode(this.getRoot(), e);
		}

		if (f != e) {
			if (startOffset != 0) {
				f = this.splitTextNode(f, startOffset)[1];
			}

			if (endOffset != 0) {
				e = this.splitTextNode(e, endOffset)[0];
			} else {
				e = this.previousTextNode(this.getRoot(), e);
			}
		} else {
			var diff = endOffset - startOffset;
			rest = this.splitTextNode(f, startOffset)[1];
			e = f = this.splitTextNode(rest, diff)[0];
		}
	
		var lst = [];
		while (f != null) {
			lst.push (f);
			if (f == e)
				break;

			f = this.nextTextNode(this.getRoot(), f);
		}
	
		f.parentNode.replaceChild(element, f);
	
		/* Now import all of those to the span */
		for (var i = 0; i < lst.length; ++i) {
			var node = lst[i];
			element.appendChild (node);
		}
	
		ret = element;
	}

	return ret;
};

function kiwirdfaUserDataHandler (t, operation, key, data, src, dst) {
	if (operation == 1) { // CLONE
		// Clear the selected style and selected..
		if (t.selectedLinkElement == src) {
			t.selectedLinkElement = null;
			if (src.hasAttribute("class")) {
				src.setAttribute("class", src.getAttribute("class").replace(" selected", ""));
			}
			
			if (dst.hasAttribute("class")) {
				dst.setAttribute("class", dst.getAttribute("class").replace(" selected", ""));
			}
		}
		
		dst.setUserData("kiwirdfalink", true, kiwirdfaUserDataHandler.partial(t));
	}
};

RDFa.prototype.createLinkInternal = function (span) {
	var doc = this.root.ownerDocument;

	var cls = " link";
	
	if (span.hasAttribute("rel")) {
		cls = cls + " object";
	}
	else if (span.hasAttribute("property")) {
		cls = cls + " datatype";
	}
	else {
		cls = cls + " nonsemantic";
	}
	
	if (span == this.selectedLinkElement) {
		cls = cls + " selected";
	}
	span.setAttribute("class", cls);
	
	if (span.setUserData) {
		span.setUserData("kiwirdfalink", true, kiwirdfaUserDataHandler.partial(this));
	}
};

RDFa.prototype.createLinkSpan = function (sel) {
	var doc = this.root.ownerDocument;	
	var span = doc.createElement("span");
	
	var ret = this.createElementFromSelection (sel, span);
	
	this.createLinkInternal(ret);
	
	return ret;
};

