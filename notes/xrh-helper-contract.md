# `$xrh` Helper Contract fuer Velocity

Diese Datei beschreibt den minimalen Helper-Vertrag, den die aktuellen
Velocity-Templates im Kontext unter dem Namen `$xrh` erwarten.

Geltungsbereich:

- [ubl-pattern-library.vm](../templates/ubl-pattern-library.vm)
- [ubl-invoice-core.vm](../templates/ubl-invoice-core.vm)
- [ubl-invoice-full.vm](../templates/ubl-invoice-full.vm)

Referenzimplementierung:

- [XRechnungVelocityHelper.java](../velocity-runner/src/main/java/local/xrechnung/velocityrunner/XRechnungVelocityHelper.java)

## Benoetigte Methoden

Es werden aktuell genau diese Methoden benoetigt:

- `has(Object value): boolean`
- `text(Object value): String`
- `attr(Object value): String`
- `date(Object value): String`
- `amount(Object value): String`
- `number(Object value): String`

## Minimale Java-Form

Zum Beispiel so:

```java
public interface XRechnungVelocityHelper {
  boolean has(Object value);
  String text(Object value);
  String attr(Object value);
  String date(Object value);
  String amount(Object value);
  String number(Object value);
}
```

Die Signaturen muessen nicht exakt so aussehen, aber fuer Velocity ist ein
einfaches Objekt mit oeffentlichen Methoden und diesen Namen am unkompliziertesten.

## Semantik pro Methode

### `has`

Zweck:

- Presence-Check fuer alle optionalen Werte, Attribute, Listen und Gruppen.

Muss gelten:

- `null` -> `false`
- leerer String -> `false`
- Blank-String wie `"   "` -> `false`
- numerische `0` -> `true`
- `BigDecimal.ZERO` -> `true`
- negative Zahlen -> `true`
- `false` als Boolean sollte, falls jemals verwendet, als vorhandener Wert
  gelten, also `true`
- leere Collection / leeres Array / leere Map -> `false`
- Collection / Array nur dann `true`, wenn rekursiv mindestens ein Eintrag
  wirklich Inhalt hat
- Map nur dann `true`, wenn rekursiv mindestens ein Value wirklich Inhalt hat
- eine Map mit nur `null`, Blank-Strings, leeren Listen oder leeren Maps ist
  also `false`
- komplexe Objekte / Beans ausserhalb von Collection/Map/Array duerfen weiter
  als `non-null` gelten

Wichtiger Punkt:

- Die Templates verlassen sich weiterhin nicht auf tiefe Bean-Inspektion.
- Fuer echte Objekte reicht im Regelfall `value != null`.
- Fuer Maps, Arrays und Collections lohnt sich aber eine rekursive
  Presence-Pruefung, damit null-gefuellte Adapter-Objekte nicht versehentlich
  Wrapper aufmachen.

Explizite Folge fuer die aktuellen Templates:

- `has("")` muss `false` sein, weil z. B. die Core-Vorlage absichtlich `""`
  an `xrTaxTotal(..., "", ...)` uebergibt, damit der zweite `TaxAmount`
  sicher unterdrueckt wird.
- `has({ price: { netAmount: null } })` sollte `false` sein, wenn diese Map
  am Ende nur leere Teilwerte enthaelt.
- `has([{ id: null }, { id: "A1" }])` sollte `true` sein, weil die Liste
  rekursiv einen echten Eintrag enthaelt.

### `text`

Zweck:

- XML-escaped Elementinhalt fuer Text-, Code- und Identifier-Werte.

Muss gelten:

- Rueckgabe ist XML-sicher fuer Elementinhalt
- mindestens `&`, `<` und `>` muessen escaped werden
- sollte defensiv auch mit `"` und `'` sauber umgehen koennen
- keine Business-Defaults bilden
- keine Datums- oder Zahlenformatierung machen
- `null` sollte defensiv zu `""` werden duerfen, auch wenn die Templates das
  normalerweise schon ueber `has` abfangen

### `attr`

Zweck:

- XML-escaped Attributwert, etwa fuer `schemeID`, `currencyID`, `unitCode`,
  `name`, `mimeCode`, `filename`.

Muss gelten:

- Rueckgabe ist XML-sicher fuer Attributkontext
- mindestens `&`, `<`, `>`, `"` und `'` muessen escaped werden
- keine Business-Defaults bilden
- `null` darf defensiv zu `""` werden

### `date`

Zweck:

- Ausgabe von Datumswerten im XML-Format `YYYY-MM-DD`.

Muss gelten:

- Ausgabeformat exakt `yyyy-MM-dd`
- keine lokalisierte Ausgabe
- keine Zeitkomponente
- keine implizite Zeitzonenlogik im stillen Hintergrund
- empfohlen: nur bereits date-artige Werte akzeptieren, z. B. `LocalDate`
  oder einen schon normalisierten String

Empfehlung:

- Falls intern `LocalDateTime`, `Instant` oder `Date` vorkommen, sollte die
  Umwandlung zur reinen Rechnungssicht vorher passieren und nicht heimlich in
  `date()`.

### `amount`

Zweck:

- Formatierte Dezimaldarstellung fuer Geldbetraege.

Muss gelten:

- Dezimalpunkt `.` statt lokaler Kommaschreibweise
- keine Tausendertrennzeichen
- keine wissenschaftliche Notation wie `1E+2`
- kein stilles Runden
- negative Werte muessen moeglich sein
- `0` muss als echter Wert ausgebbar sein

Empfehlung:

- Werte sollten vorher bereits fachlich normalisiert und gerundet sein
- `BigDecimal.toPlainString()` ist die richtige Denkrichtung
- `Double`/`Float` sollten nicht still unpraezise durchrutschen

### `number`

Zweck:

- Formatierte Dezimaldarstellung fuer Mengen, Prozentwerte und sonstige
  numerische Nicht-Geldwerte.

Muss gelten:

- gleiche Formatregeln wie bei `amount`
- Dezimalpunkt `.`
- keine Tausendertrennzeichen
- keine wissenschaftliche Notation
- kein stilles Runden
- `0` muss ausgebbar sein

Empfehlung:

- Implementierung darf technisch dieselbe Formatlogik wie `amount()` nutzen
- der getrennte Name ist trotzdem sinnvoll, weil die Templates damit semantisch
  lesbar bleiben

## Was `$xrh` bewusst nicht tun sollte

- keine Pflichtfeldpruefung
- keine XRechnung-Geschaeftsregeln
- keine Summenbildung
- keine Steueraufschluesselung
- keine Defaultwerte fuer fehlende Fachdaten
- keine tiefe UBL-spezifische Logik

Das alles gehoert in die Voraufbereitung oder spaetere Validierung, nicht in
den kleinen Render-Helper.

## Praktische Robustheit

Sinnvolle Zusatzregeln fuer die Implementierung:

- Optional/Optionals intern auspacken, falls sie doch in den Kontext geraten
- Arrays wie Collections behandeln
- fuer Zahlen moeglichst ueber `BigDecimal` normalisieren
- fuer `text()` und `attr()` eine zentrale XML-Escape-Funktion benutzen

## Kurzfassung

Wenn `$xrh` diese sechs Dinge sauber kann, reichen die aktuellen Templates aus:

- Presence pruefen: `has`
- Elementtext escapen: `text`
- Attributwerte escapen: `attr`
- Datum formatieren: `date`
- Betraege formatieren: `amount`
- sonstige Zahlen formatieren: `number`
