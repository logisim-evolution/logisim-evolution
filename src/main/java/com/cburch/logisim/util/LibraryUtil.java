/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Marcin Orlowski (http://MarcinOrlowski.com), 2021
 */

package com.cburch.logisim.util;

import java.lang.reflect.Field;
import java.util.NoSuchElementException;

public class LibraryUtil {

  /**
   * Tries to read unique identifier from object (mainly Library or Tool)
   * stored in object's class static field named as set in idFieldName.
   *
   * As we want to have static _ID per library, generic implementation must
   * look for it in the current instance using Reflection.
   *
   * Throws NoSuchElementException if no ID field is found.
   * Throws NullPointerException if ID filed is present but its value is NULL
   *
   * @param cls Class of the object to obtain ID/Name from.
   *
   * @return ID of the object
   */
  public static String getName(Class cls) {
    final String idFieldName = "_ID";
    try {
      Field[] fields = cls.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].getName().equals(idFieldName)) {
          String id = (String)fields[i].get(null);
          if (id != null) return id;
          throw new NullPointerException(
                  "The " + idFieldName + " for " + cls + " cannot be NULL");
        }
      }
    } catch (Exception ex) {
      try {
        throw ex;
      } catch (IllegalAccessException iaeEx) {
        iaeEx.printStackTrace();
      }
    }

    throw new NoSuchElementException(
            "Missing " + idFieldName + " static const field for " + cls);
  }

}
