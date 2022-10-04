package masbench.generator;

import masbench.ModelProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SimulationGenerationFileGenerator extends AbstractGenerationFileGenerator{

  public static void main(String[] args) throws IOException {
    SimulationGenerationFileGenerator generator = new SimulationGenerationFileGenerator(Paths.get(args[1]), Paths.get(args[2]), Paths.get(args[3]), new ModelProperty(args[4]));
    generator.generate();
  }

  public SimulationGenerationFileGenerator(Path inputCsvFilePath, Path outputJsonFilePath, Path outputCsvFilePath, ModelProperty modelProperty) {
    super(inputCsvFilePath, outputJsonFilePath, outputCsvFilePath, (double) modelProperty.getNumberOfPeople(), modelProperty.getGpsSeparating());
    readCsv();
  }
}
