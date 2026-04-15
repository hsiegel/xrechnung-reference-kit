# DTO-to-Public Mapping Matrix Guide

Diese Vorlage beschreibt nur den neutralen Schritt

`interne DTOs -> oeffentliches $xr-Modell`

und gerade nicht den Schritt `Public -> UBL`.

Die eigentliche Vorlage steht in
[dto-to-public-mapping-matrix-template.tsv](./dto-to-public-mapping-matrix-template.tsv).

## Sinn der Matrix

Die Matrix trennt sauber:

- interne Herkunft eines Wertes
- oeffentlichen Zielpfad im `$xr`-Modell
- notwendige Voraufbereitung vor Velocity

Damit kann sich die `.vm` spaeter aendern, ohne dass ihr jedes Mal wieder die
interne DTO-Zuordnung neu denken muesst.

## Wie ihr sie verwenden koennt

1. Diese TSV-Datei in einen privaten Arbeitsbereich kopieren.
2. Die Spalte `source_internal` nur dort mit internen DTO-Pfaden oder
   Erzeugungsregeln fuellen.
3. In `transform_kind` notieren, welche Aufbereitung vor dem Befuellen des
   Public Models passiert.
4. In `status` markieren, was nur roh gemappt, was fachlich vorbereitet und was
   schon verifiziert ist.

## Spalten

| Spalte | Bedeutung |
|---|---|
| `public_path` | Zielpfad im oeffentlichen `$xr`-Modell |
| `node_kind` | `value`, `object` oder `list` |
| `term_hint` | oeffentliche XRechnung-Referenz oder Hinweis auf den Block |
| `action_hint` | grobe Arbeitsform: `map_value`, `compose_object`, `repeat_or_group` |
| `source_internal` | privat zu fuellender DTO-Pfad, Query oder Berechnungsquelle |
| `transform_kind` | privat zu fuellende Voraufbereitung |
| `null_policy` | was bei fehlendem oder leerem Quellwert passieren soll |
| `status` | z. B. `todo`, `mapped`, `verified`, `blocked` |
| `notes` | freie Bemerkungen, offene Fragen, Sonderlogik |

## Empfohlene Werte fuer `transform_kind`

- `direct`
- `normalize_blank_to_null`
- `format_date`
- `normalize_decimal`
- `constant`
- `conditional`
- `choose_one`
- `lookup_code`
- `split_identifier`
- `join_text`
- `aggregate_totals`
- `build_vat_breakdown`
- `repeat_from_source_list`
- `group_by_business_key`

Ihr koennt diese Werte frei erweitern. Wichtig ist nur, dass sie bei euch
einheitlich verwendet werden.

## Empfohlene Werte fuer `null_policy`

- `omit_if_missing`
- `derive_or_null`
- `empty_list_if_missing`
- `drop_list_item_if_empty`
- `must_be_constant`
- `diagnose_if_missing`

## Wie man Container-Zeilen nutzt

- `object`-Zeilen beschreiben nicht einen einzelnen Wert, sondern wie ein
  zusammengesetzter Block gebaut wird.
- `list`-Zeilen beschreiben, aus welcher internen Liste oder Gruppierung mehrere
  Public-Objekte entstehen.

Beispiele:

- `xr.vatBreakdowns[]`:
  `transform_kind = build_vat_breakdown`
- `xr.lines[]`:
  `transform_kind = repeat_from_source_list`
- `xr.totals`:
  `transform_kind = aggregate_totals`

## Empfohlene Bearbeitungsreihenfolge

1. Kopfwerte und einfache Referenzen
2. Parteien und Adressen
3. Zahlungsdaten und Lieferdaten
4. Linienlisten
5. Abgeleitete Summen und Steueraufschluesselung
6. Sonderfaelle wie `BT-18`, `BT-82`, `BT-90`, `BT-111`

## Wichtige Sonderfaelle

- `BT-8` liegt im Public Model unter `xr.invoicePeriod.descriptionCode`.
- `BT-18` und `BT-128` sind im Public Model Objektpfade mit `id` und
  `schemeId`, obwohl sie spaeter als UBL-Referenzcontainer gerendert werden.
- `BT-82` ist Public-seitig `payment.meansText`, auch wenn es in UBL als
  Attribut rendert.
- `BT-90` wird im Public Model bei Seller oder Payee als `sepaCreditorId`
  gefuehrt.
- `BT-111` ist getrennt als `xr.totals.taxAmountInTaxCurrency`.

## Praktischer Hinweis

Die oeffentliche Vorlage kann hier im Projekt liegen bleiben.
Die befuellte Matrix mit echten `source_internal`-Eintraegen sollte in euren
privaten Bereich oder in euer internes Repo wandern.
