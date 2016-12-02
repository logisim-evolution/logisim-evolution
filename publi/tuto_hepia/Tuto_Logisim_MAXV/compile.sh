#!/bin/bash

pdflatex tutoLogisim.tex; bibtex tutoLogisim.aux; pdflatex tutoLogisim.tex; pdflatex tutoLogisim.tex;
