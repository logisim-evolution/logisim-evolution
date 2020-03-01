#include <stdio.h>
#include <stdlib.h>
#include <string.h>

char lines[10000][256];

int removeRemark( FILE *ifile ) {
   int notEnded = 0;
   char line[256];
   while (!feof(ifile) && notEnded == 0) {
      int nrOfChars = fscanf(ifile,"%[^\n]", line);
      fgetc(ifile);
      if (strstr(line,"*/")!=NULL && nrOfChars >0)
         notEnded = 1;
   }
   if (feof(ifile))
      return -1;
   else
      return 1;
}

void printHeader( FILE *ifile ) {
   const char* const HEADER = 
      "/**\n"
      "* This file is part of logisim-evolution.\n"
      "*\n"
      "* Logisim-evolution is free software: you can redistribute it and/or modify\n"
      "* it under the terms of the GNU General Public License as published by the\n"
      "* Free Software Foundation, either version 3 of the License, or (at your\n"
      "* option *\n"
      "* Logisim-evolution is distributed in the hope that it will be useful, but\n"
      "* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY\n"
      "* or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License\n"
      "* for more details.\n"
      "*\n"
      "* You should have received a copy of the GNU General Public License along \n"
      "* with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.\n"
      "*\n"
      "* Original code by Carl Burch (http://www.cburch.com * Subsequent modifications by:\n"
      "*      + College of the Holy Cross\n"
      "*        http://www.holycross.edu\n"
      "*      + Haute École Spécialisée Bernoise/Berner Fachhochschule\n"
      "*        http://www.bfh.ch\n"
      "*      + Haute École du paysage, d'ingénierie et d'architecture de Genève\n"
      "*        http://hepia.hesge.ch/\n"
      "*      + Haute École d'Ingénierie et de Gestion du Canton de Vaud\n"
      "*        http://www.heig-vd.ch/\n"
      "*/\n";
   fprintf( ifile , HEADER );
}


int main( int argc, char *argv[] ) {
   FILE *ifile;
   char line[256];
   ifile = fopen(argv[1],"r");
   if (ifile==NULL) {
       exit(1);
   }
   int index = 0;
   int remarkRemoved = 0;
   
   while (!feof(ifile)&& (index<10000)) {
      size_t nrOfChars = fscanf(ifile,"%[^\n]", line);
      fgetc(ifile);
      if (strstr(line,"/**")!=NULL && nrOfChars >0 && remarkRemoved == 0) {
         remarkRemoved = removeRemark(ifile);
         index = 0;
      } else {
        if (nrOfChars == 0) {
           lines[index][0] = 0;
        } else if (nrOfChars > 0) {
           strcpy(lines[index],line);
        } 
      }
   }
   fclose(ifile);
   ifile = fopen(argv[1],"w");
   printHeader(ifile);
   for (int i = 0 ; i < index ; i++)
      fprintf(ifile, "%s\n", lines[i]);
   fclose(ifile);
}
