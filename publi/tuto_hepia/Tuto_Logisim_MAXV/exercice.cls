% Classe exercice.cls - Version 1.0
% Y. Thoma dec. 2005
%
\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{exercice}[2006/10/17 v1.1 Classe exercice]

\LoadClass[a4paper,oneside,onecolumn,11pt,reversemp,asymmetric]{article}
%\LoadClass[12pt]{article}

%\RequirePackage{vhdl}
\RequirePackage{graphicx}
\RequirePackage[francais]{layout}
\RequirePackage{alltt}
\RequirePackage{xcolor}
\RequirePackage{fancybox}
\RequirePackage{booktabs}


\RequirePackage{palatino}
\RequirePackage[all]{xy}
\RequirePackage[wide, margincaption, leftbody, raggedleft]{sidecap}
\RequirePackage[bindingoffset=10mm, marginparwidth=35mm, 
  right=15mm, left=15mm, top=15mm, bottom=20mm]{geometry}
\RequirePackage[french]{babel}
\RequirePackage[T1]{fontenc}
\RequirePackage[labelfont={sf,bf}, textfont=it, labelsep=period]{caption}
\RequirePackage{graphicx}
\RequirePackage{fancyhdr}
\RequirePackage{fancyvrb}
\RequirePackage{ifthen}
\RequirePackage{titlesec}
\RequirePackage{picins}
\RequirePackage{multirow}
\RequirePackage{booktabs}
\RequirePackage{enumerate}
\RequirePackage{pifont}
\RequirePackage{amsmath,amssymb,mathrsfs}

\newcounter{questioncounter}
\setcounter{questioncounter}{1}



\setlength{\oddsidemargin}{0pt}
\setlength{\textwidth}{16cm}
\setlength{\parindent}{0pt}

\newcommand{\extitle}[1]{\begin{center}{\Huge #1}\end{center}\vspace{1cm}}


%\doublebox{
%\begin{minipage}{\textwidth}
%\begin{center}
%\end{center}\end{minipage}}

%\newenvironment{exercice}[1][]{\extitle{#1}}{}
%\newenvironment{exercice}[1][]{\section{#1}}{}
\newenvironment{exercice}[1][]{{%
  \noindent%
  \begin{minipage}[l]{\linewidth}%
    \vspace{8mm}%
    \rule{\linewidth}{0.2mm} \\%
    %%\vspace{1mm}%
    \large\textbf{Exercice \thequestioncounter :} #1\\ %
    \rule{\linewidth}{0.2mm}%
    \vspace{5mm}%
    \addtocounter{questioncounter}{1}%
  \end{minipage}}}{}
  
  
\newif\if@enonce \@enoncetrue
\DeclareOption{onlysolution}{\@enoncefalse
\newenvironment{solution}{{%
  \noindent%
  \begin{minipage}[l]{\linewidth}%
    \large\textbf{Solution:}\\ %
 \end{minipage}}}{}}


\DeclareOption{nosolution}{\input{solutioncomments.clo}}
\DeclareOption{solution}{\newenvironment{solution}{{%
  \noindent%
  \begin{minipage}[l]{\linewidth}%
    \vspace{8mm}%
    %%\rule{\linewidth}{0.2mm} \\%
    %%\vspace{1mm}%
    \large\textbf{Solution:}\\ %
    \rule{\linewidth}{0.2mm}%
    \vspace{5mm}%
 \end{minipage}}}{}}
 
 \newcommand\entete[2]{%
  \noindent%
  \begin{minipage}[l]{\linewidth}%
    {\large #1} \\%
    \rule{\linewidth}{0.2mm} \\%
    %
    {\vspace{-5mm}%
      \parpic[r]{\includegraphics[width=4cm]{HESSO_logo}}%
      \hspace{-3.5mm}\includegraphics[width=6cm]{HEPIA_logo}\vspace{-3mm} \\%
  %    \textbf{Haute école du paysage, d'ingénierie et d'architecture. } \\%
      \textbf{} \\%
  %    \textbf{Laboratoire de systèmes numériques} \\%
      #2}\vspace{8mm}%
    %
    \vspace{5mm}%
  \end{minipage}}


\def\baa #1{\leavevmode %
   #1 %
   \kern-.55em \raise 1.1em \hbox{\_}\kern-.1em %
}
\newcommand{\ba}[1]{\baa #1}
\newcommand{\gand}{\cdot}
\newcommand{\gor}{+}
\newcommand{\gnor}{\downarrow}
\newcommand{\gnand}{\uparrow}
\newcommand{\gxor}{\oplus}

\DeclareOption{nonumber}{\newcounter{thecounter}
\setcounter{thecounter}{0}\renewenvironment{exercice}[2][]{
\addtocounter{thecounter}{1}\section*{#2 \arabic{thecounter} #1}}{}}

\ProcessOptions


\if@enonce
\newenvironment{enonce}{}{}
\else
\input{enoncecomments.clo}
\fi
