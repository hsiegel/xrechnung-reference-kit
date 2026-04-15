package local.xrechnung.velocityrunner;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Runtime helper for the public XRechnung Velocity templates.
 */
public final class XRechnungVelocityHelper {

  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  public boolean has(Object value) {
    return hasContent(value, Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()));
  }

  public String text(Object value) {
    return escapeXml(stringValue(value), false);
  }

  public String attr(Object value) {
    return escapeXml(stringValue(value), true);
  }

  public String date(Object value) {
    Object unwrapped = unwrap(value);
    if (unwrapped == null) {
      return "";
    }
    if (unwrapped instanceof LocalDate) {
      return ISO_DATE.format((LocalDate) unwrapped);
    }
    if (unwrapped instanceof LocalDateTime) {
      return ISO_DATE.format(((LocalDateTime) unwrapped).toLocalDate());
    }
    if (unwrapped instanceof OffsetDateTime) {
      return ISO_DATE.format(((OffsetDateTime) unwrapped).toLocalDate());
    }
    if (unwrapped instanceof ZonedDateTime) {
      return ISO_DATE.format(((ZonedDateTime) unwrapped).toLocalDate());
    }
    if (unwrapped instanceof CharSequence) {
      String normalized = unwrapped.toString().trim();
      if (normalized.isEmpty()) {
        return "";
      }
      try {
        return ISO_DATE.format(LocalDate.parse(normalized, ISO_DATE));
      } catch (DateTimeParseException ex) {
        throw new IllegalArgumentException("Unsupported date value: " + normalized, ex);
      }
    }
    throw new IllegalArgumentException("Unsupported date type: " + unwrapped.getClass().getName());
  }

  public String amount(Object value) {
    return decimal(value);
  }

  public String number(Object value) {
    return decimal(value);
  }

  private String decimal(Object value) {
    Object unwrapped = unwrap(value);
    if (unwrapped == null) {
      return "";
    }
    return toBigDecimal(unwrapped).toPlainString();
  }

  private static Object unwrap(Object value) {
    Object current = value;
    while (current instanceof Optional<?>) {
      current = ((Optional<?>) current).orElse(null);
    }
    if (current instanceof OptionalInt) {
      OptionalInt optional = (OptionalInt) current;
      return optional.isPresent() ? optional.getAsInt() : null;
    }
    if (current instanceof OptionalLong) {
      OptionalLong optional = (OptionalLong) current;
      return optional.isPresent() ? optional.getAsLong() : null;
    }
    if (current instanceof OptionalDouble) {
      OptionalDouble optional = (OptionalDouble) current;
      return optional.isPresent() ? optional.getAsDouble() : null;
    }
    return current;
  }

  private static boolean hasContent(Object value, Set<Object> seen) {
    Object unwrapped = unwrap(value);
    if (unwrapped == null) {
      return false;
    }
    if (unwrapped instanceof CharSequence) {
      return !unwrapped.toString().trim().isEmpty();
    }
    if (unwrapped instanceof Number || unwrapped instanceof Boolean || unwrapped instanceof Character) {
      return true;
    }
    if (shouldTrackIdentity(unwrapped) && !seen.add(unwrapped)) {
      return false;
    }
    if (unwrapped instanceof Collection<?>) {
      for (Object item : (Collection<?>) unwrapped) {
        if (hasContent(item, seen)) {
          return true;
        }
      }
      return false;
    }
    if (unwrapped instanceof Map<?, ?>) {
      for (Object item : ((Map<?, ?>) unwrapped).values()) {
        if (hasContent(item, seen)) {
          return true;
        }
      }
      return false;
    }
    if (unwrapped.getClass().isArray()) {
      int length = Array.getLength(unwrapped);
      for (int i = 0; i < length; i++) {
        if (hasContent(Array.get(unwrapped, i), seen)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  private static boolean shouldTrackIdentity(Object value) {
    return !(value instanceof Number)
        && !(value instanceof Boolean)
        && !(value instanceof Character)
        && !(value instanceof CharSequence)
        && !value.getClass().isEnum();
  }

  private static String stringValue(Object value) {
    Object unwrapped = unwrap(value);
    return unwrapped == null ? "" : String.valueOf(unwrapped);
  }

  private static BigDecimal toBigDecimal(Object value) {
    if (value instanceof BigDecimal) {
      return (BigDecimal) value;
    }
    if (value instanceof BigInteger) {
      return new BigDecimal((BigInteger) value);
    }
    if (value instanceof Byte) {
      return BigDecimal.valueOf(((Byte) value).longValue());
    }
    if (value instanceof Short) {
      return BigDecimal.valueOf(((Short) value).longValue());
    }
    if (value instanceof Integer) {
      return BigDecimal.valueOf(((Integer) value).longValue());
    }
    if (value instanceof Long) {
      return BigDecimal.valueOf(((Long) value).longValue());
    }
    if (value instanceof Float) {
      float number = ((Float) value).floatValue();
      if (!Float.isFinite(number)) {
        throw new IllegalArgumentException("Non-finite float value: " + number);
      }
      return BigDecimal.valueOf(number);
    }
    if (value instanceof Double) {
      double number = ((Double) value).doubleValue();
      if (!Double.isFinite(number)) {
        throw new IllegalArgumentException("Non-finite double value: " + number);
      }
      return BigDecimal.valueOf(number);
    }
    if (value instanceof CharSequence) {
      String normalized = value.toString().trim();
      if (normalized.isEmpty()) {
        throw new IllegalArgumentException("Blank string is not a valid decimal value");
      }
      try {
        return new BigDecimal(normalized);
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("Unsupported decimal value: " + normalized, ex);
      }
    }
    throw new IllegalArgumentException("Unsupported numeric type: " + value.getClass().getName());
  }

  private static String escapeXml(String input, boolean attributeContext) {
    StringBuilder escaped = new StringBuilder(input.length() + 16);
    for (int i = 0; i < input.length(); ) {
      int codePoint = input.codePointAt(i);
      i += Character.charCount(codePoint);

      if (!isValidXml10CodePoint(codePoint)) {
        throw new IllegalArgumentException(String.format("Invalid XML 1.0 character: U+%04X", codePoint));
      }

      switch (codePoint) {
        case '&':
          escaped.append("&amp;");
          break;
        case '<':
          escaped.append("&lt;");
          break;
        case '>':
          escaped.append("&gt;");
          break;
        case '"':
          escaped.append(attributeContext ? "&quot;" : "\"");
          break;
        case '\'':
          escaped.append(attributeContext ? "&apos;" : "'");
          break;
        default:
          escaped.appendCodePoint(codePoint);
          break;
      }
    }
    return escaped.toString();
  }

  private static boolean isValidXml10CodePoint(int codePoint) {
    return codePoint == 0x9
        || codePoint == 0xA
        || codePoint == 0xD
        || (codePoint >= 0x20 && codePoint <= 0xD7FF)
        || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
        || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
  }
}
