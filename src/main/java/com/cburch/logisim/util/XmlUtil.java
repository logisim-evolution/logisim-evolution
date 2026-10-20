/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class XmlUtil {

  /**
   * Returns instance of DocumentBuilderFactory configured to mitigate potential XXE (XML External
   * Entity) attacks.
   *
   * <ul>
   *   <li>https://cwe.mitre.org/data/definitions/611.html
   *   <li>https://owasp.org/www-community/vulnerabilities/XML_External_Entity_(XXE)_Processing
   *   <li>https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
   * </ul>
   */
  public static DocumentBuilderFactory getHardenedBuilderFactory() {
    var dbf = DocumentBuilderFactory.newInstance();

    String feature = null;
    try {
      // This is the PRIMARY defense. If DTDs (doctypes) are disallowed, almost all
      // XML entity attacks are prevented
      feature = "http://apache.org/xml/features/disallow-doctype-decl";
      dbf.setFeature(feature, true);

      // If you can't completely disable DTDs, then at least do the following:
      // JDK7+ - http://xml.org/sax/features/external-general-entities
      // This feature has to be used together with the following one, otherwise it will not protect
      // you from XXE for sure
      feature = "http://xml.org/sax/features/external-general-entities";
      dbf.setFeature(feature, false);

      // JDK7+ - http://xml.org/sax/features/external-parameter-entities
      // This feature has to be used together with the previous one, otherwise it will not protect
      // you from XXE for sure
      feature = "http://xml.org/sax/features/external-parameter-entities";
      dbf.setFeature(feature, false);

      // Disable external DTDs as well
      feature = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
      dbf.setFeature(feature, false);

      // and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
      dbf.setXIncludeAware(false);
      dbf.setExpandEntityReferences(false);

      // And, per Timothy Morgan: "If for some reason support for inline DOCTYPEs are a requirement,
      // then ensure the entity settings are disabled (as shown above) and beware that SSRF attacks
      // (http://cwe.mitre.org/data/definitions/918.html) and denial of service attacks (such as
      // billion laughs or decompression bombs via "jar:") are a risk."
    } catch (ParserConfigurationException e) {
      // This should catch a failed setFeature feature
      // FIXME: hardcoded string. Maybe we can just return exception message instead?
      System.err.println(
          String.format(
              "Error: ParserConfigurationException was thrown for feature '%s'.", feature));
      dbf = null;
    }

    return dbf;
  }
}
