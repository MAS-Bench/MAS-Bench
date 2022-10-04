package masbench.generator;

import masbench.ModelProperty;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class GenerationFileGenerator {
  public static void main(String[] args) throws IOException {
    switch (args[0]) {
      case "Observation":
        OriginalGenerationFileGenerator.main(args);
        break;
      case "Simulation":
        SimulationGenerationFileGenerator.main(args);
        break;
    }
  }

  public static AbstractGenerationFileGenerator getGenerator(Path parameterCsvPath, Path generationJsonPath, Path simulationStartFlowCsvPath, ModelProperty modelProperty){
    try {
      if (Files.readString(parameterCsvPath).toUpperCase().startsWith("#SIMPLE")) {
        return new SimpleGenerationFileGenerator(parameterCsvPath, generationJsonPath, simulationStartFlowCsvPath, modelProperty);
      } else {
        return new SimulationGenerationFileGenerator(parameterCsvPath, generationJsonPath, simulationStartFlowCsvPath, modelProperty);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
