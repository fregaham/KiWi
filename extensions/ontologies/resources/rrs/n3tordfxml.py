#!/usr/bin/env python

import sys
from rdflib import ConjunctiveGraph, Graph
from rdflib import BNode, Literal, Namespace, RDF, RDFS
from rdflib.util import first, uniq

if __name__ == "__main__":
    g = Graph()
    g.parse(sys.stdin, format="n3")

    g.serialize(destination=sys.stdout, format="xml")

