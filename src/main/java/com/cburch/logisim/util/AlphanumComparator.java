/*
 * The Alphanum Algorithm is an improved sorting algorithm for strings
 * containing numbers.  Instead of sorting numbers in ASCII order like
 * a standard sort, this algorithm sorts numbers in numeric order.
 *
 * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
 *
 * Released under the MIT License - https://opensource.org/licenses/MIT
 *
 * Copyright 2007-2017 David Koelle
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.cburch.logisim.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * This is an updated version with enhancements made by Daniel Migowski, Andre Bogus, and David
 * Koelle. Updated by David Koelle in 2017.
 *
 * <p>To use this class: Use the static "sort" method from the java.util.Collections class:
 * Collections.sort(your list, new AlphanumComparator());
 */
public class AlphanumComparator implements Comparator<String> {
  private boolean isDigit(char ch) {
    return ((ch >= 48) && (ch <= 57));
  }

  /** Length of string is passed in for improved efficiency (only need to calculate it once). */
  private String getChunk(String s, int slength, int marker) {
    final var chunk = new StringBuilder();
    var c = s.charAt(marker);
    chunk.append(c);
    marker++;
    if (isDigit(c)) {
      while (marker < slength) {
        c = s.charAt(marker);
        if (!isDigit(c)) break;
        chunk.append(c);
        marker++;
      }
    } else {
      while (marker < slength) {
        c = s.charAt(marker);
        if (isDigit(c)) break;
        chunk.append(c);
        marker++;
      }
    }
    return chunk.toString();
  }

  public int compare(String s1, String s2) {
    if ((s1 == null) || (s2 == null)) {
      return 0;
    }

    var thisMarker = 0;
    var thatMarker = 0;
    var s1Length = s1.length();
    var s2Length = s2.length();

    while (thisMarker < s1Length && thatMarker < s2Length) {
      final var thisChunk = getChunk(s1, s1Length, thisMarker);
      thisMarker += thisChunk.length();

      final var thatChunk = getChunk(s2, s2Length, thatMarker);
      thatMarker += thatChunk.length();

      // If both chunks contain numeric characters, sort them numerically
      var result = 0;
      if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
        // Simple chunk comparison by length.
        var thisChunkLength = thisChunk.length();
        result = thisChunkLength - thatChunk.length();
        // If equal, the first different number counts
        if (result == 0) {
          for (var i = 0; i < thisChunkLength; i++) {
            result = thisChunk.charAt(i) - thatChunk.charAt(i);
            if (result != 0) {
              return result;
            }
          }
        }
      } else {
        result = thisChunk.compareTo(thatChunk);
      }

      if (result != 0) return result;
    }

    return s1Length - s2Length;
  }

  /** Shows an example of how the comparator works. Feel free to delete this in your own code! */
  public static void main(String[] args) {
    final var values =
        Arrays.asList(
            "dazzle2",
            "dazzle10",
            "dazzle1",
            "dazzle2.7",
            "dazzle2.10",
            "2",
            "10",
            "1",
            "EctoMorph6",
            "EctoMorph62",
            "EctoMorph7");
    System.out.println(
        values.stream().sorted(new AlphanumComparator()).collect(Collectors.joining(" ")));
  }
}
