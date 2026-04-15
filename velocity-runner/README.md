# Velocity Runner

Kleines lokales Test-Harness fuer die UBL-`Invoice`-Templates unter `templates/`.

Ziel:

- `Velocity 1.6.4` mit genau den fuer unsere Templates relevanten Einstellungen starten
- ein oeffentliches Beispiel-`$xr` plus `$xrh` in den Kontext legen
- ein `.vm` aus `templates/` direkt rendern

## Build

Vom Projektwurzelverzeichnis aus:

```bash
mvn -f velocity-runner/pom.xml package
```

Das erzeugt ein lauffaehiges Fat-JAR:

```bash
velocity-runner/target/velocity-runner.jar
```

## Start

Vollsicht rendern:

```bash
java -jar velocity-runner/target/velocity-runner.jar \
  --template templates/ubl-invoice-full.vm \
  --out /tmp/invoice-full.xml
```

Core rendern:

```bash
java -jar velocity-runner/target/velocity-runner.jar \
  --template templates/ubl-invoice-core.vm \
  --out /tmp/invoice-core.xml
```

Ohne `--out` wird nach `stdout` geschrieben.

## Wichtige Velocity-Einstellungen

Das Harness setzt bewusst:

- `velocimacro.permissions.allow.inline = true`
- `velocimacro.permissions.allow.inline.local.scope = true`
- `velocimacro.context.localscope = true`
- `runtime.references.strict = false`

Damit passen Engine-Verhalten und unsere null-toleranten Single-File-Templates
zusammen.
