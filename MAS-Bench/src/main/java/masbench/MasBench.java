package masbench;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import masbench.evaluator.PerformanceEvaluator;
import masbench.generator.AbstractGenerationFileGenerator;
import masbench.generator.GenerationFileGenerator;
import masbench.generator.PropertiesFileGenerator;
import masbench.generator.SimulationGenerationFileGenerator;
import nodagumi.ananPJ.CrowdWalkLauncher;
import org.apache.pdfbox.io.IOUtils;

public class MasBench {
  public static final String MASBENCH_RESOURCES = "masbench-resources";
  public static final String SIMULATOR_SCENARIO = "SimulatorScenario";

  public static void main(String[] args) throws IOException {
    if (args.length == 1) {
      if (args[0].equals("init")) {
        initializeResources();
      } else {
        printHelp();
      }
      return;
    }

    if (args.length < 3) {
      printHelp();
      return;
    }

    if (!getResourcesDirectoryPath().toFile().exists()) {
      initializeResources();
    }

    final ModelProperty modelProperty = new ModelProperty(args[0]);
    final Path workingDirectoryPath = Paths.get(args[1]);
    final Path parameterCsvPath = Paths.get(args[2]);

    final Path propertyDirectoryPath = workingDirectoryPath.resolve("property");
    final Path analyzeDirectoryPath = workingDirectoryPath.resolve("analyze");
    final Path logDirectoryPath = workingDirectoryPath.resolve("log");

    try {
      Files.createDirectories(propertyDirectoryPath);
      Files.createDirectories(analyzeDirectoryPath);
      Files.createDirectories(logDirectoryPath);
    } catch (IOException e) {
      e.printStackTrace();
    }

    final Path generationJsonPath = propertyDirectoryPath.resolve("gen.json");
    final Path propertyJsonPath = propertyDirectoryPath.resolve("prop.json");
    final Path simulationStartFlowCsvPath = propertyDirectoryPath.resolve("simulationStartFlow.csv");

    AbstractGenerationFileGenerator generationFileGenerator = GenerationFileGenerator.getGenerator(parameterCsvPath, generationJsonPath, simulationStartFlowCsvPath, modelProperty);
    generationFileGenerator.generate();
    PropertiesFileGenerator propertiesFileGenerator = new PropertiesFileGenerator(propertyJsonPath, getResourcesDirectoryPath().resolve(SIMULATOR_SCENARIO), modelProperty);
    propertiesFileGenerator.generate();

    CrowdWalkLauncher.main(new String[]{
      propertyJsonPath.toAbsolutePath().toString(),
        "-g", "-lError"
    });

    if (true) {
      return;
    }

    PerformanceEvaluator.SimulationResultEvaluator evaluator = new PerformanceEvaluator.SimulationResultEvaluator(logDirectoryPath, analyzeDirectoryPath, modelProperty);
    evaluator.outputResults();
  }

  static void printHelp() {
    System.err.println("The arguments are not enough.");
    System.err.println("" + "init");
    System.err.println("" + "[MODEL NAME] [WORKING DIR] [PARAMETER CSV FILE]");
  }

  public static Path getResourcesDirectoryPath() {
    File currentJar = null;
    try {
      currentJar = new File(MasBench.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      if (!currentJar.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
        throw new Exception();
      }
    } catch (Exception e) {
      System.err.println("run form jar file.");
      System.exit(1);
    }
    return currentJar.toPath().toAbsolutePath().getParent().resolve(MASBENCH_RESOURCES);
  }

  static void initializeResources() {
    if (!getResourcesDirectoryPath().toFile().exists()) {
      Path destPath = getResourcesDirectoryPath();

      try (ZipInputStream zipInputStream = new ZipInputStream((new MasBench()).getClass().getResourceAsStream("/" + MASBENCH_RESOURCES + ".zip"))) {
        ZipEntry entry = null;
        while ((entry = zipInputStream.getNextEntry()) != null) {
          Path entryPath = destPath.resolve(entry.getName());
          if (entry.isDirectory()) {
            Files.createDirectories(entryPath);
          } else {
            Files.createDirectories(entryPath.getParent());
            try (OutputStream out = new FileOutputStream(entryPath.toFile())) {
              IOUtils.copy(zipInputStream, out);
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
