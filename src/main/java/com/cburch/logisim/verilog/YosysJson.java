/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

/** Dependency-free JSON codec. Keeps integer net IDs distinct from string constants. */
final class YosysJson {
  private final Reader reader;
  private int current;
  private long position;

  private YosysJson(Reader reader) throws IOException {
    this.reader = reader;
    advance();
  }

  static Map<String, Object> read(Path path) throws IOException {
    try (var reader = Files.newBufferedReader(path)) {
      return object(parse(reader));
    }
  }

  static Object parse(Reader reader) throws IOException {
    var parser = new YosysJson(reader);
    var value = parser.value(0);
    parser.space();
    if (parser.current != -1) {
      throw parser.error("Trailing content");
    }
    return value;
  }

  private void advance() throws IOException {
    if ((position++ & 4095) == 0) {
      checkCancelled();
    }
    current = reader.read();
  }

  static void checkCancelled() {
    if (Thread.currentThread().isInterrupted()) {
      throw new CancellationException();
    }
  }

  private void space() throws IOException {
    while (current == ' ' || current == '\n' || current == '\r' || current == '\t') {
      advance();
    }
  }

  private ImportException error(String message) {
    return new ImportException(message + " at JSON character " + position);
  }

  private Object value(int depth) throws IOException {
    if (depth > 128) {
      throw error("JSON nesting exceeds 128 levels");
    }
    space();
    if (current == '"') {
      return string();
    }
    if (current == '{') {
      advance();
      space();
      var map = new LinkedHashMap<String, Object>();
      if (current != '}') {
        while (true) {
          space();
          if (current != '"') {
            throw error("Expected object key");
          }
          var key = string();
          space();
          if (current != ':') {
            throw error("Expected colon");
          }
          advance();
          if (map.containsKey(key)) {
            throw error("Duplicate key " + key);
          }
          map.put(key, value(depth + 1));
          space();
          if (current != ',') {
            break;
          }
          advance();
        }
      }
      if (current != '}') {
        throw error("Expected closing brace");
      }
      advance();
      return map;
    }
    if (current == '[') {
      advance();
      space();
      var list = new ArrayList<Object>();
      if (current != ']') {
        while (true) {
          list.add(value(depth + 1));
          space();
          if (current != ',') {
            break;
          }
          advance();
        }
      }
      if (current != ']') {
        throw error("Expected closing bracket");
      }
      advance();
      return list;
    }
    if (current == 't' || current == 'f' || current == 'n') {
      var literal = current == 't' ? "true" : current == 'f' ? "false" : "null";
      for (char c : literal.toCharArray()) {
        if (current != c) {
          throw error("Invalid literal");
        }
        advance();
      }
      return literal.equals("null") ? null : literal.equals("true");
    }
    var token = new StringBuilder();
    while (current >= 0 && "-+0123456789.eE".indexOf(current) >= 0) {
      token.append((char) current);
      advance();
    }
    var number = token.toString();
    if (!number.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) {
      throw error("Invalid JSON value");
    }
    return number.matches("-?[0-9]+") ? new BigInteger(number) : new BigDecimal(number);
  }

  private String string() throws IOException {
    advance();
    var result = new StringBuilder();
    while (current != '"') {
      if (current < 32) {
        throw error("Unterminated string or control character");
      }
      if (current == '\\') {
        advance();
        if (current == 'u') {
          int code = 0;
          for (int i = 0; i < 4; i++) {
            advance();
            int digit = Character.digit(current, 16);
            if (digit < 0) {
              throw error("Invalid Unicode escape");
            }
            code = code * 16 + digit;
          }
          result.append((char) code);
        } else {
          result.append(switch (current) {
            case '"', '\\', '/' -> (char) current;
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> throw error("Invalid escape");
          });
        }
      } else {
        result.append((char) current);
      }
      advance();
    }
    advance();
    return result.toString();
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> object(Object value) {
    if (!(value instanceof Map<?, ?>)) {
      throw new ImportException("Expected JSON object");
    }
    return (Map<String, Object>) value;
  }

  static Map<String, Object> map(Map<String, Object> value, String key) {
    return object(value.getOrDefault(key, Map.of()));
  }

  static List<?> array(Object value) {
    if (!(value instanceof List<?> list)) {
      throw new ImportException("Expected JSON array");
    }
    return list;
  }

  static BigInteger parameter(Object value) {
    if (value instanceof BigInteger integer) {
      return integer;
    }
    if (value instanceof Number number) {
      return BigInteger.valueOf(number.longValue());
    }
    if (value instanceof String text && text.matches("[01]+")) {
      return new BigInteger(text, 2);
    }
    throw new ImportException("Expected integer or binary parameter, got " + value);
  }

  static boolean flag(Object value) {
    return parameter(value).signum() != 0;
  }

  static String encode(Object value) {
    checkCancelled();
    if (value == null) {
      return "null";
    }
    if (value instanceof String text) {
      var out = new StringBuilder("\"");
      for (char c : text.toCharArray()) {
        switch (c) {
          case '"', '\\' -> out.append('\\').append(c);
          case '\n' -> out.append("\\n");
          case '\r' -> out.append("\\r");
          case '\t' -> out.append("\\t");
          default -> {
            if (c < 32) {
              out.append(String.format("\\u%04x", (int) c));
            } else {
              out.append(c);
            }
          }
        }
      }
      return out.append('"').toString();
    }
    if (value instanceof Map<?, ?> map) {
      var entries = new ArrayList<String>();
      map.forEach((k, v) -> entries.add(encode(k.toString()) + ":" + encode(v)));
      return "{" + String.join(",", entries) + "}";
    }
    if (value instanceof List<?> list) {
      return "[" + String.join(",", list.stream().map(YosysJson::encode).toList()) + "]";
    }
    return value.toString();
  }
}
