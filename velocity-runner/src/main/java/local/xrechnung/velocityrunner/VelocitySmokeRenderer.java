package local.xrechnung.velocityrunner;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * Minimal local runner for the public XRechnung Velocity templates.
 */
public final class VelocitySmokeRenderer {

  private VelocitySmokeRenderer() {
  }

  public static void main(String[] args) throws Exception {
    CliOptions options = CliOptions.parse(args);

    VelocityEngine engine = createEngine(options.projectRoot);
    Template template = engine.getTemplate(options.templatePath, StandardCharsets.UTF_8.name());

    VelocityContext context = new VelocityContext();
    context.put("xr", SamplePublicInvoiceFactory.fullInvoice());
    context.put("xrh", new XRechnungVelocityHelper());

    StringWriter writer = new StringWriter();
    template.merge(context, writer);
    String xml = writer.toString();

    if (options.outputPath == null) {
      System.out.print(xml);
      return;
    }

    writeFile(options.outputPath, xml);
    System.out.println("Rendered " + options.templatePath + " -> " + options.outputPath.toAbsolutePath());
  }

  private static VelocityEngine createEngine(Path projectRoot) throws Exception {
    Properties props = new Properties();
    props.setProperty("resource.loader", "file");
    props.setProperty(
        "file.resource.loader.class",
        "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
    props.setProperty("file.resource.loader.path", projectRoot.toAbsolutePath().toString());
    props.setProperty("input.encoding", StandardCharsets.UTF_8.name());
    props.setProperty("output.encoding", StandardCharsets.UTF_8.name());

    // Our templates define inline macros and rely on null-tolerant omission.
    props.setProperty("velocimacro.permissions.allow.inline", "true");
    props.setProperty("velocimacro.permissions.allow.inline.local.scope", "true");
    props.setProperty("velocimacro.context.localscope", "true");
    props.setProperty("runtime.references.strict", "false");

    VelocityEngine engine = new VelocityEngine();
    engine.init(props);
    return engine;
  }

  private static void writeFile(Path outputPath, String xml) throws IOException {
    Path absolute = outputPath.toAbsolutePath();
    Path parent = absolute.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.write(absolute, xml.getBytes(StandardCharsets.UTF_8));
  }

  private static final class CliOptions {
    private static final String DEFAULT_TEMPLATE = "templates/ubl-invoice-full.vm";

    private final Path projectRoot;
    private final String templatePath;
    private final Path outputPath;

    private CliOptions(Path projectRoot, String templatePath, Path outputPath) {
      this.projectRoot = projectRoot;
      this.templatePath = templatePath;
      this.outputPath = outputPath;
    }

    private static CliOptions parse(String[] args) {
      Path projectRoot = Paths.get("").toAbsolutePath();
      String templatePath = DEFAULT_TEMPLATE;
      Path outputPath = null;

      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        if ("--help".equals(arg) || "-h".equals(arg)) {
          printUsageAndExit(0);
        } else if ("--project-root".equals(arg)) {
          projectRoot = Paths.get(requireValue(args, ++i, "--project-root"));
        } else if ("--template".equals(arg)) {
          templatePath = requireValue(args, ++i, "--template");
        } else if ("--out".equals(arg)) {
          outputPath = Paths.get(requireValue(args, ++i, "--out"));
        } else if (arg.startsWith("--")) {
          System.err.println("Unknown option: " + arg);
          printUsageAndExit(2);
        } else {
          templatePath = arg;
        }
      }

      return new CliOptions(projectRoot, templatePath, outputPath);
    }

    private static String requireValue(String[] args, int index, String option) {
      if (index >= args.length) {
        System.err.println("Missing value for " + option);
        printUsageAndExit(2);
      }
      return args[index];
    }

    private static void printUsageAndExit(int statusCode) {
      System.out.println(
          "Usage: java -jar velocity-runner.jar [--project-root DIR] [--template PATH] [--out FILE]\n"
              + "Defaults:\n"
              + "  --project-root  current working directory\n"
              + "  --template      " + DEFAULT_TEMPLATE + "\n"
              + "  --out           stdout");
      System.exit(statusCode);
    }
  }
}
