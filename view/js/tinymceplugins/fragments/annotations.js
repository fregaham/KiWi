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
 * A JavaScript library for overlapping annotations.
 * 
 * @author: Marek Schmidt
*/
function Annotations () {

	this.root = null;

	this.dom_events = false;

	// this.styles = {};
	
	this.default_color = [0.2, 0.0, 0.0, 0.0];
	
	this.colors = {}
	
	this.types = {};

	this.ablocks = [];
}

Annotations.prototype.init = function(root) {
	
	this.root = root;
	
	root.addEventListener("DOMNodeInserted", function(t, event) {
		t.onNodeInserted(event);
	}.partial(this), false);
};

Annotations.prototype.createAnnotationAroundSelection = function (id, range, getControlElement) {
	this.insertAnnotation (id, range, getControlElement);
};

Annotations.prototype.disableDOMEvents = function() {
	this.dom_events = false;
};

Annotations.prototype.enableDOMEvents = function() {
	this.dom_events = true;
};

/*
Annotations.prototype.annotation_set_style = function(id, style) {
	this.styles[id] = style;
};

Annotations.prototype.annotation_get_style = function(id) {
	return this.styles[id];
};*/

/**
 * @param color Array of four elements [r,g,b,a] in values in range <0.0,1.0> 
 */
Annotations.prototype.annotation_set_color = function(id, color) {
	this.colors[id] = color;
};

Annotations.prototype.annotation_get_color = function(id) {
	return this.colors[id];
};

/**
 * Get the list of annotation ids encoded in the element
 * @param element
 * @return list of annotation ids
 */
Annotations.prototype.getAnnotationsData = function(element) {
	if (element.getUserData) {
		return element.getUserData("annotations");
	}
	else {
		var annotations = element.getAttribute("annotations");
		if (annotations == null) {
			return null;
		}
		return annotations.split(" ");
	}
};

/**
 * Store the list of annotation ids in the element. It uses the DOM level 3 user
 * data, if available, if so, the onABSpanClone method will be provided to the
 * setUserData call, which enshures the data will be copied when the element is
 * cloned...
 * 
 * @param element
 * @param annotations list of annotation ids
 */
Annotations.prototype.setAnnotationsData = function(element, annotations) {
	if (element.setUserData) {
		element.setUserData("annotations", annotations,
				onABSpanClone.partial(this)); 
	}
	else {
		element.setAttribute("annotations", annotations.join(" ")); 
	}
};



Annotations.prototype.setAnnotationColor = function (id, color) {
	// The styles needs to be combined in the additive way... so we clear the style first
	var textNodes = this.getAnnotationTextNodes (id);
	for (var i = 0; i < textNodes.length; ++i) {
		var abspan = this.getABSpan(textNodes[i]);
		if (abspan != null) {
			var annots = this.getAnnotationsData(abspan);
			this.clear_ab_style(abspan.style, annots);
		}
	}
				
	this.annotation_set_color (id, color);
				
	// ok, and now set the new combined style again
	for (var i = 0; i < textNodes.length; ++i) {
		var abspan = this.getABSpan(textNodes[i]);
		if (abspan != null) {
			var annots = this.getAnnotationsData(abspan);
			this.set_ab_style(abspan.style, annots);
		}
	}
};

/**
 * Generates a CSS style for a AB span
 * 
 * @annots Array of annots
 */
Annotations.prototype.set_ab_style = function(style, annots) {
	
	var a = 0;
	var r = 0;
	var g = 0;
	var b = 0;
	
	for ( var i = 0; i < annots.length; ++i) {
		
		var _color = this.annotation_get_color(annots[i]);
		if (_color == null) {
			_color = this.default_color;
		}
		
		var _a = _color[0];
		var _r = _color[1];
		var _g = _color[2];
		var _b = _color[3];
		
		a = _a + a*(1-_a);
		r = (_r * _a + r * a * (1 - _a)) / a;
		g = (_g * _a + g * a * (1 - _a)) / a;
		b = (_b * _a + b * a * (1 - _a)) / a;
	}
	
	style["backgroundColor"] = "rgba(" + Math.floor(255 * r + 0.5) + "," + Math.floor(255 * g + 0.5) + "," + Math.floor(255 * b + 0.5) + "," + a + ")";
};

Annotations.prototype.clear_ab_style = function(style, annots) {
	
	style["backgroundColor"] = null;
	
	/*for ( var i = 0; i < annots.length; ++i) {
		var s = this.annotation_get_style(annots[i]);
		for ( var key in s) {
			style[key] = null;
		}
	}*/
};

Annotations.prototype.getRoot = function() {
	return this.root;
};

/**
 * utility function for cloning arrays
 */
Annotations.prototype.array_clone = function(a) {
	var ret = [];
	for ( var i = 0; i < a.length; ++i) {
		ret.push(a[i]);
	}
	return ret;
};

/**
 * returns a previous text node, or null if outside the subtree defined by "root"
 */
Annotations.prototype.previousTextNode = function(root, node) {

	if (node == null) {
		alert("null");
	}

	if (root.isSameNode(node))
		return null;

	while (true) {
		while (node.previousSibling == null) {
			node = node.parentNode;
			if (node == null || node.isSameNode(root))
				return null;
		}

		node = node.previousSibling;

		while (node.nodeType != Node.TEXT_NODE
				&& node.lastChild != null) {
			node = node.lastChild;
		}

		if (node.nodeType == Node.TEXT_NODE)
			return node;
	}
};

/**
 * returns a first text node below the "root" element, or null if none exists
 */
Annotations.prototype.firstTextNode = function(root) {

	var node = root;

	while (true) {
		while (node.nodeType != Node.TEXT_NODE
				&& node.firstChild != null) {
			node = node.firstChild;
		}

		if (node.nodeType == Node.TEXT_NODE)
			return node;

		if (node.isSameNode(root))
			return null;

		while (node.nextSibling == null) {
			node = node.parentNode;
			if (node.isSameNode(root))
				return null;
		}

		node = node.nextSibling;
	}
};

/**
 * returns the last text node below some "root" element or null if none exists
 */
Annotations.prototype.lastTextNode = function(root) {
	var node = root;

	while (true) {
		while (node.nodeType != Node.TEXT_NODE
				&& node.lastChild != null) {
			node = node.lastChild;
		}

		if (node.nodeType == Node.TEXT_NODE)
			return node;

		if (node.isSameNode(root))
			return null;

		while (node.previousSibling == null) {
			node = node.parentNode;
			if (node.isSameNode(root))
				return null;
		}

		node = node.previousSibling;
	}
};

/**
 * same as previousTextNode, but different 
 */
Annotations.prototype.nextTextNode = function(root, node) {
	
	if (root.isSameNode(node))
		return null;
	while (true) {
		while (node.nextSibling == null) {
			node = node.parentNode;
			if (node == null || node.isSameNode(root))
				return null;
		}

		node = node.nextSibling;

		while (node.nodeType != Node.TEXT_NODE
				&& node.firstChild != null) {
			node = node.firstChild;
		}

		if (node.nodeType == Node.TEXT_NODE)
			return node;
	}
};

/**
 * get the ABSpan for the text node, or null if the text node has no abspan (therefore no annotations)
 */
Annotations.prototype.getABSpan = function(n) {

	if (n == null) return null;

	var p = n.parentNode;
	if (p == null) return null;

	if (this.getAnnotationsData(p) != null) {
		return p;
	}
	return null;
};

Annotations.prototype.renameAnnotation = function (oldid, newid) {
	var nodes = this.getAnnotationTextNodes(oldid);
	for (var i = 0; i < nodes.length; ++i) {
		var node = nodes[i];
		var abspan = this.getABSpan (node);
		if (abspan != null) {
			var annots = this.getAnnotationsData(abspan);
			if (annots != null) {
				var newannots = [];
				for ( var j = 0; j < annots.length; ++j) {
					if (annots[j] == oldid) {
						newannots.push (newid);
					}
					else {
						newannots.push(annots[j]);
					}
				}
			
				this.setAnnotationsData(abspan, newannots);
			}
		}
	}
	
	this.styles[newid] = this.styles[oldid];
};

/**
 * Returns a list of textnodes for this annotation
 */
Annotations.prototype.getAnnotationTextNodes = function (id) {
	var root = this.getRoot();
	var n = this.firstTextNode(root);
	var ret = [];
	while (n != null) {
		var abspan = this.getABSpan (n);
		if (abspan != null) {
			var annots = this.getAnnotationsData(abspan);
			if (annots != null) {
				if (this.obj_in_array (id, annots)) {
					ret.push (n);
				}
			}
		}
		n = this.nextTextNode(root, n);
	}

	return ret;
};

Annotations.prototype.getAnnotationText = function(id) {
	var textNodes = this.getAnnotationTextNodes(id);
	ret = "";
	
	for (var i = 0; i < textNodes.length; ++i) {
		ret += textNodes[i].nodeValue;
	}
	
	return ret;
};

/**
 * get a list of annotation IDs for this text node
 */
Annotations.prototype.getAnnotations = function(n) {
	var ab = this.getABSpan(n);
	if (ab == null) {
		return [];
	}
	var ret = this.getAnnotationsData(ab);
	if (ret == null) {
		return [];
	}
	return ret;
};

/**
 * removes the abspan, inserts all abspan descendants back to the tree
 * (there should be only one descendant of the abspan)
 */
Annotations.prototype.deleteABSpan = function(abspan) {
	var p = abspan.parentNode;

	var child = abspan.childNodes;
	while (abspan.firstChild != null) {
		p.insertBefore(abspan.firstChild, abspan);
	}

	p.removeChild(abspan);
};

/**
 * Splits the text node at offset
 * returns an array of two newly created text nodes
 */
Annotations.prototype.splitTextNode = function(n, offset) {
	var abspan = this.getABSpan(n);
	if (abspan == null) {
		var p = n.parentNode;
		var text = n.nodeValue;
		var n1 = document.createTextNode(text.substring(0, offset));
		var n2 = document.createTextNode(text.substring(offset,
				n.length));

		p.replaceChild(n2, n);
		p.insertBefore(n1, n2);

		var ret = [];
		ret.push(n1);
		ret.push(n2);
		return ret;
	} else {
		var abspan1 = document.createElement("span");
		var abspan2 = document.createElement("span");

		var annotations = this.getAnnotationsData(abspan);
		this.setAnnotationsData(abspan1, annotations);
		this.set_ab_style(abspan1.style, annotations);

		this.setAnnotationsData(abspan2, this.array_clone(annotations));
		this.set_ab_style(abspan2.style, annotations);

		var p = abspan.parentNode;

		var text = n.nodeValue;
		var n1 = document.createTextNode(text.substring(0, offset));
		var n2 = document.createTextNode(text.substring(offset,
				n.length));

		p.replaceChild(abspan2, abspan);
		p.insertBefore(abspan1, abspan2);

		abspan1.appendChild(n1);
		abspan2.appendChild(n2);

		var ret = [];
		ret.push(n1);
		ret.push(n2);
		return ret;
	}
};

/**
 * inserts an annotation to the text node
 * "annotation" should be the ID
 */
Annotations.prototype.insertAnnotationToTextNode = function(annotation, n) {
	var abspan = this.getABSpan(n);

	if (abspan == null) {
		abspan = document.createElement("span");
		n.parentNode.replaceChild(abspan, n);
		abspan.appendChild(n);

		this.setAnnotationsData(abspan, [ annotation ]);
		this.set_ab_style(abspan.style, [ annotation ]);
	} else {
		
		var alist = this.getAnnotationsData(abspan);
		alist.push(annotation);
		this.set_ab_style(abspan.style, alist);
	}
};

Annotations.prototype.removeAnnotationFromTextNode = function (annotation, n) {
	var abspan = this.getABSpan(n);
	if (abspan != null) {
		/* build a new array without the annotation */
		var annots = this.getAnnotationsData(abspan);
		var newannots = [];

		for (var i = 0; i < annots.length; ++i) {
			if (annots[i] == annotation) {
			}
			else {
				newannots.push (annots[i]);
			}
		}

		if (annots.length != newannots.length) {
			if (newannots.length == 0) {
				/* no annotations left, remove the whole ABSpan */
				while (abspan.firstChild != null) {
					var n = abspan.firstChild;
					abspan.removeChild (n);
					abspan.parentNode.insertBefore (n, abspan);
				}
				abspan.parentNode.removeChild (abspan);
			}
			else {
				this.setAnnotationsData(abspan, newannots);
				this.clear_ab_style(abspan.style, [annotation]);
				this.set_ab_style(abspan.style, newannots);
			}
		}
	}
};

Annotations.prototype.removeAllAnnotationsFromTextNode = function (n) {
	var abspan = this.getABSpan(n);
	if (abspan != null) {
		while (abspan.firstChild != null) {
			var n = abspan.firstChild;
			abspan.removeChild (n);
			abspan.parentNode.insertBefore (n, abspan);
		}
		abspan.parentNode.removeChild (abspan);
	}
};

Annotations.prototype.insertAnnotation = function(annotation, sel, getControlElement) {
	this.disableDOMEvents();

	var f = this.firstTextNode(sel.startContainer);
	var e = this.lastTextNode(sel.endContainer);

	if (f != e) {
		if (sel.startOffset != 0) {
			f = this.splitTextNode(f, sel.startOffset)[1];
		}

		if (sel.endOffset != 0) {
			e = this.splitTextNode(e, sel.endOffset)[0];
		} else {
			e = this.previousTextNode(this.getRoot(), e);
		}
	} else {
		var diff = sel.endOffset - sel.startOffset;
		rest = this.splitTextNode(f, sel.startOffset)[1];
		e = f = this.splitTextNode(rest, diff)[0];
	}

	while (f != null) {
		this.insertAnnotationToTextNode(annotation, f);
		if (f == e)
			break;

		f = this.nextTextNode(this.getRoot(), f);
	}
	
	

	this.enableDOMEvents();
};

Annotations.prototype.removeAnnotation = function (id) {
	
	var root = this.getRoot();
	
	this.removeControlElement(root, id);
	
	/* go through all the DOM and remove the annots 
	 * or whole AB spans */
	var n = this.firstTextNode(root);
	while (n != null) {
		this.removeAnnotationFromTextNode (id, n);
		n = this.nextTextNode(root, n);
	}
};

Annotations.prototype.removeAllAnnotations = function (root) {
	var n = this.firstTextNode(root);
	while (n != null) {
		this.removeAllAnnotationsFromTextNode (n);
		n = this.nextTextNode(root, n);
	}
};

Annotations.prototype.removeControlElement = function (root, id) {
	var containers = root.ownerDocument.evaluate("//span[@annotations_control_container_id='" + id +"']", root, null, XPathResult.ANY_TYPE, null);
	var container = containers.iterateNext();

	var lst = [];
	
	while(container) {
		lst.push (container);
		container = containers.iterateNext();
	}
	
	for (var i = 0; i < lst.length; ++i) {
		var e = lst[i];
		e.parentNode.removeChild(e);
	}
};

Annotations.prototype.removeAllControlElements = function (root) {
	var containers = root.ownerDocument.evaluate("//span[@annotations_control_container='true']", root, null, XPathResult.ANY_TYPE, null);
	var container = containers.iterateNext();

	var lst = [];
	
	while(container) {
		lst.push (container);
		container = containers.iterateNext();
	}
	
	for (var i = 0; i < lst.length; ++i) {
		var e = lst[i];
		e.parentNode.removeChild(e);
	}
};

Annotations.prototype.getAllAnnotations = function(root) {
	var n = this.firstTextNode(root);
	var ret = [];
	while (n != null) {
		var annots = this.getAnnotations (n);
		for (var i = 0; i < annots.length; ++i) {
			if (!this.obj_in_array(annots[i], ret)) {
				ret.push (annots[i]);
			}
		}
		n = this.nextTextNode(root, n);
	}

	return ret;
};

Annotations.prototype.getText = function(root) {
	var ret = "";
	var n = this.firstTextNode(root);
	while (n != null) {
		ret += n.nodeValue;
		n = this.nextTextNode(root, n);
	}

	return ret;
};

Annotations.prototype.obj_in_array = function(obj, array) {
	for ( var i = 0; i < array.length; ++i) {
		if (obj == array[i])
			return true;
	}		
	return false;
};

Annotations.prototype.createSpanFromAnnotation = function(id) {
	//this.disableDOMEvents();
	
	var ret = null;
	var lst = this.getAnnotationTextNodes(id);

	if (lst.length > 0) {
		var firstABSpan = this.getABSpan(lst[0]);
		
		var doc = firstABSpan.ownerDocument;
		ret = doc.createElement("span");
		
		var parent = firstABSpan.parentNode;
		parent.insertBefore(ret, firstABSpan);
		for (var i = 0; i < lst.length; ++i) {
			ret.appendChild(this.getABSpan(lst[i]));
		}
	}
	
	this.removeAnnotation(id);
	
	//this.enableDOMEvents();
	
	return ret;
};

/**
 * Returns the 'p' or 'div' element that contains the specified node, or null if there is no such node up the DOM tree.
 * @param n
 * @returns
 */
Annotations.prototype.getNearestParagraphOrDiv = function(n) {
	while(n != null) {
		if (n.nodeType == Node.ELEMENT_NODE) {
			if (n.localName.toLowerCase() == "p" || n.localName.toLowerCase() == "div") {
				return n;
			}
		}
		
		n = n.parentNode;
	}
	
	return null;
};

/**
 * Creates a div element, not breaking any paragraphs, (that is, if there is
 * any annotation in a paragraph, the whole paragraph gets inserted into the div.
 * @param id
 */
Annotations.prototype.createDivFromAnnotation = function(id) {	
	var ret = null;
	var lst = this.getAnnotationTextNodes(id);

	if (lst.length > 0) {
		var n = lst[0];
		var firstBlock = this.getNearestParagraphOrDiv(n);
		if (firstBlock == null) {
			firstBlock = this.getABSpan(n);
		}
		
		var doc = firstBlock.ownerDocument;
		ret = doc.createElement("div");
		
		var parent = firstBlock.parentNode;
		parent.insertBefore(ret, firstBlock);
		for (var i = 0; i < lst.length; ++i) {
			var block = this.getNearestParagraphOrDiv(lst[i]);
			if (block == null) {
				block = this.getABSpan(lst[i]);
			}
			
			ret.appendChild(block);
		}
	}
	
	this.removeAnnotation(id);
	
	return ret;
};

/**
 * Go through all the tree from beginNode to endNode and fixes all
 * annotations
 */
Annotations.prototype.fixAnnotations = function(beginNode, endNode) {

	this.disableDOMEvents();

	var lr = {};
	var rl = {};

	var beginNodeText = this.lastTextNode(beginNode);
	var endNodeText = this.firstTextNode(endNode);

	var e = beginNodeText;
	while (true) {
		var annots = this.getAnnotations(e);
		for ( var i = 0; i < annots.length; ++i) {
			var a = annots[i];
			if (lr[a] == null) {
				lr[a] = e;
			}
		}

		if (e == endNodeText)
			break;
		e = this.nextTextNode(this.getRoot(), e);
	}

	var e = endNodeText;
	while (true) {
		var annots = this.getAnnotations(e);
		for ( var i = 0; i < annots.length; ++i) {
			var a = annots[i];
			if (rl[a] == null) {
				rl[a] = e;
			}
		}

		if (e == beginNodeText)
			break;
		e = this.previousTextNode(this.getRoot(), e);
	}

	var open = {};

	var e = beginNodeText;
	while (true) {
		var annots = this.getAnnotations(e);

		for ( var a in open) {
			if (open[a] == true) {
				if (this.obj_in_array(a, annots)) {
				} else {
					this.insertAnnotationToTextNode(a, e);
				}
			}
		}

		for ( var i = 0; i < annots.length; ++i) {
			var a = annots[i];
			if (open[a] == null) {
				open[a] = true;
			}

			if (rl[a] == e) {
				open[a] = null;
			}
		}

		if (e == endNodeText)
			break;
		e = this.nextTextNode(this.getRoot(), e);
	}

	this.enableDOMEvents();
};

Annotations.prototype.onNodeInserted = function(event) {

	if (!this.dom_events) {
		return;
	}

	switch (event.target.nodeType) {
		case Node.TEXT_NODE:
		case Node.ELEMENT_NODE: {
			/* We need to fix possibly splitted annotations */

			var et = event.target;
			var fc = et.firstChild;

			var tf = this.firstTextNode(event.target);
			var tl = this.lastTextNode(event.target);

			/*
			 * node has no text nodes inside... if only one is null, that is
			 * some error...
			 */
			if (tf == null && tl == null) break;

			var f = this.previousTextNode(this.getRoot(), tf);
			var t = this.nextTextNode(this.getRoot(), tl);

			/* if at the beginning or end, no possible splits... */
			if (f == null || t == null) break;

			this.fixAnnotations(f, t);
			break;
		}
	}
};

Annotations.prototype.scrollToAnnotation = function(root, id) {
	var textNodes = this.getAnnotationTextNodes (id);
	if (textNodes.length >= 1) {
		var textNode = textNodes[0];
		var abspan = this.getABSpan(textNode);

		// non-standard assumption that defaultView is a window
		var window = abspan.ownerDocument.defaultView;

		// non-standard assumption that window has the scroll method
		window.scroll(0, abspan.offsetTop);
	}
};

Annotations.prototype.serializeAnnotations = function(root) {
	this.disableDOMEvents();
	var lr = {};
	var rl = {};
	var as = [];

	var beginNodeText = this.firstTextNode(root);
	var endNodeText = this.lastTextNode(root);

	var e = beginNodeText;
	while (true) {
		var annots = this.getAnnotations(e);
		for ( var i = 0; i < annots.length; ++i) {
			var a = annots[i];
			if (lr[a] == null) {
				lr[a] = e;
				as.push (a);
			}
		}

		if (e == endNodeText)
			break;
		e = this.nextTextNode(root, e);
	}

	var e = endNodeText;
	while (true) {
		var annots = this.getAnnotations(e);
		for ( var i = 0; i < annots.length; ++i) {
			var a = annots[i];
			if (rl[a] == null) {
				rl[a] = e;
			}
		}

		if (e == beginNodeText)
			break;
		e = this.previousTextNode(root, e);
	}

	for (var i = 0; i < as.length; ++i) {
		var a = as[i];
		var begin_e = lr[a];
		var end_e = rl[a];

		if (this.types[a] == null) {
			begin_e.nodeValue = "[[BookmarkStart:" + a + "]]" + begin_e.nodeValue;
		}
		else {
			begin_e.nodeValue = "[[BookmarkStart:" + a + "|" + this.types[a] + "]]" + begin_e.nodeValue;
		}
	
		end_e.nodeValue = end_e.nodeValue + "[[BookmarkEnd:" + a + "]]";
	}

	this.removeAllAnnotations (root);
	this.removeAllControlElements (root);
	this.enableDOMEvents();
};

Annotations.prototype.deserializeAnnotations = function (root, getControlElement) {

	this.disableDOMEvents();

	var re_bookmark_start = /\[\[\s*BookmarkStart:([^\]]+?)\s*\]\]/;
	var re_bookmark_start_1 = /\[\[\s*BookmarkStart:([^\]\|]+?)\s*\|\s*([^\]]+?)\s*\]\]/;
	var re_bookmark_end = /\[\[\s*BookmarkEnd:([^\]]+?)\s*\]\]/;

	var begins = {};
	var ends = {};
	var annots = [];
	var id2type = {};

	var beginNodeText = this.firstTextNode(root);

	var e = beginNodeText;

	if (e == null) return;

	while (e != null) {	

		while(true) {		
			var m_bm_start_1 = re_bookmark_start_1.exec (e.nodeValue);
			var m_bm_start = re_bookmark_start.exec (e.nodeValue);
			var m_bm_end = re_bookmark_end.exec (e.nodeValue);

			var bm_nearest = null;
			var bm_nearest_index = null;

			if (m_bm_start_1) {
				if (bm_nearest_index == null || bm_nearest_index > m_bm_start_1.index) {
					bm_nearest = m_bm_start_1;
					bm_nearest_index = m_bm_start_1.index;
				}
			}
			if (m_bm_start) {
				if (bm_nearest_index == null || bm_nearest_index > m_bm_start.index) {
					bm_nearest = m_bm_start;
					bm_nearest_index = m_bm_start.index;
				}
			}
			if (m_bm_end) {
				if (bm_nearest_index == null || bm_nearest_index > m_bm_end.index) {
					bm_nearest = m_bm_end;
					bm_nearest_index = m_bm_end.index;
				}
			}

			if (bm_nearest == null) break;

			var isBegin = null;
			var id = bm_nearest[1];
			//var id = parseInt(bm_nearest[1]);
			//var type = null; 
			if (bm_nearest == m_bm_start_1) {
				isBegin = true;
				annots.push(id);
				type = bm_nearest[2];
				id2type[id] = type;
			}
			if (bm_nearest == m_bm_start) {
				isBegin = true;
				annots.push(id);
				type = null;
				id2type[id] = type;
			}
			else if (bm_nearest == m_bm_end) {
				isBegin = false;
			}



			var ret = this.splitTextNode (e, bm_nearest_index);
			var pre = ret[0];
			var tag_rest = ret[1];

			ret = this.splitTextNode (tag_rest, bm_nearest[0].length);
			var tag = ret[0];
			var rest = ret[1];

			/* remove the tag */
			this.removeAllAnnotationsFromTextNode (tag);
			tag.parentNode.removeChild (tag);

			if (isBegin) {
				begins[id] = pre;
			}
			else {
				ends[id] = pre;
			}

			e = rest;        			
		}


		e = this.nextTextNode(root, e);
	}

	for (var i = 0; i < annots.length; ++i) {
		var id = annots[i];
		var type = id2type[id];
		this.types[id] = type; 

		if (begins[id] != null && ends[id] != null) {
			var begin_e = this.nextTextNode(root, begins[id]);
			var end_e = ends[id];

			//this.annotation_set_style (id, {backgroundColor:"#E0E0F0"});
			this.annotation_set_color(id, this.default_color);

			this.insertAnnotationToTextNode (id, begin_e);
			if (begin_e != end_e) {
				this.insertAnnotationToTextNode (id, end_e);
			}
			
			// Insert the control element to the end
			if (getControlElement != null) {
				var controlElement = getControlElement(root, id, type);
				if (controlElement) {
					var controlContainer = root.ownerDocument.createElement("span");
					controlContainer.appendChild(controlElement);
					controlContainer.contentEditable = "false";
					controlContainer.setAttribute("annotations_control_container", "true");
					controlContainer.setAttribute("annotations_control_container_id", id);
					controlContainer.setAttribute("class", "mceNonEditable");
					
					end_e.parentNode.insertBefore(controlContainer, end_e.nextSibling);
				}
			}
		}
	}

	// fix annotations
	this.fixAnnotations (this.firstTextNode(root), this.lastTextNode(root));
	this.enableDOMEvents();
};




/**
 * onUserData handler on AB spans if annotation is cloned (eg. copy-paste),
 * then also create the annotation for the copy
 */
function onABSpanClone (t, operation, key, data, src, dst) {
	if (operation == 1) {
		/* NODE_CLONED */
		dst.setUserData("annotations", t.array_clone(src
				.getUserData("annotations")), onABSpanClone.partial(t));
	}
	else if (operation == 3) {
		/* NODE_DELETED */		
	}
};

