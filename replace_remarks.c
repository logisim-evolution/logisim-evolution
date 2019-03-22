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
   fprintf( ifile , "/**\n" );
   fprintf( ifile , " * This file is part of logisim-evolution.\n" );
   fprintf( ifile , " *\n" );
   fprintf( ifile , " * Logisim-evolution is free software: you can redistribute it and/or modify\n" );
   fprintf( ifile , " * it under the terms of the GNU General Public License as published by the\n" );
   fprintf( ifile , " * Free Software Foundation, either version 3 of the License, or (at your\n" );
   fprintf( ifile , " * option) any later version.\n" );
   fprintf( ifile , " *\n" );
   fprintf( ifile , " * Logisim-evolution is distributed in the hope that it will be useful, but\n" );
   fprintf( ifile , " * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY\n" );
   fprintf( ifile , " * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License\n" );
   fprintf( ifile , " * for more details.\n" );
   fprintf( ifile , " *\n" );
   fprintf( ifile , " * You should have received a copy of the GNU General Public License along \n" );
   fprintf( ifile , " * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.\n" );
   fprintf( ifile , " *\n" );
   fprintf( ifile , " * Original code by Carl Burch (http://www.cburch.com), 2011.\n" );
   fprintf( ifile , " * Subsequent modifications by:\n" );
   fprintf( ifile , " *   + College of the Holy Cross\n" );
   fprintf( ifile , " *     http://www.holycross.edu\n" );
   fprintf( ifile , " *   + Haute École Spécialisée Bernoise/Berner Fachhochschule\n" );
   fprintf( ifile , " *     http://www.bfh.ch\n" );
   fprintf( ifile , " *   + Haute École du paysage, d'ingénierie et d'architecture de Genève\n" );
   fprintf( ifile , " *     http://hepia.hesge.ch/\n" );
   fprintf( ifile , " *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud\n" );
   fprintf( ifile , " *     http://www.heig-vd.ch/\n" );
   fprintf( ifile , " */\n" );
}


int main( int argc, char *argv[] ) {
   FILE *ifile;
   char line[256];
   ifile = fopen(argv[1],"r");
   int index = 0;
   int remarkRemoved = 0;
   
   while (!feof(ifile)&& (index<10000)) {
      int nrOfChars = fscanf(ifile,"%[^\n]", line);
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
