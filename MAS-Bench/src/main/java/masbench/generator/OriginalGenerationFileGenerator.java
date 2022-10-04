package masbench.generator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OriginalGenerationFileGenerator extends AbstractGenerationFileGenerator {
  static final Integer TIME_SEPARATE = 300;
  static final Double MAX_ONE_SEPARATE_PEOPLE_RATE = 0.01;

  static final Double MAX_SIGMA = 100.0;
  static final Double MAX_MU = 300.0;
  static final Double MAX_PI = 1.0;
  static final Double MIN_SIGMA = 3.0;
  static final Double MIN_MU = 0.0;
  static final Double MIN_PI = 0.0;


  public static void main(String[] args) {
    OriginalGenerationFileGenerator generator = new OriginalGenerationFileGenerator(Paths.get(args[1]), Paths.get(args[2]), Paths.get(args[3]));
    generator.generate();
  }

  public OriginalGenerationFileGenerator(Path inputCsvFilePath, Path outputJsonFilePath, Path outputCsvFilePath) {
    super(inputCsvFilePath, outputJsonFilePath, outputCsvFilePath, 45000.0, 1);
    Integer naiveAgentSize = 1;
    Integer rationalAgentSize = 1;
    Integer rubyAgentSize = 1;
    Integer agentSize = 3;

    setCsv(agentSize, naiveAgentSize, rationalAgentSize, rubyAgentSize);
    readCsv();
  }
  public Double calcNormDist(Double sigma[], Double mu[], Double pi[], Integer Size, Double x) {
    Double normDist = 0.0;

    for (Integer i = 0; i < Size; i++) {
      normDist += Math.exp(-(x - mu[i]) * (x - mu[i]) / (2.0 * sigma[i] * sigma[i])) / Math.sqrt(2.0 * Math.PI * sigma[i] * sigma[i]) * pi[i];
    }
    return normDist;
  }

  public void setCsv(Integer dimension, Integer naiveK, Integer rationalK, Integer rubyK) {
    try {
      Double mu[] = new Double[dimension];
      Double sigma[] = new Double[dimension];
      Double pi[] = new Double[dimension];
      pi[dimension - 1] = -1.0;
      while (true) {
        Double allPi = 0.0;
        Double sumDist = 0.0;
        for (int i = 0; i < dimension; i++) {
          sigma[i] = Math.random() * (MAX_SIGMA - MIN_SIGMA) + MIN_SIGMA;
          mu[i] = Math.random() * (MAX_MU - MIN_MU) + MIN_MU;
          pi[i] = Math.random() * (MAX_PI - MIN_PI) + MIN_PI;
          if (i != dimension - 1) allPi += pi[i];
        }
        pi[dimension - 1] = MAX_PI - allPi;
        if (pi[dimension - 1] < MIN_PI) continue;
        for (int t = 0; t < TIME_SEPARATE; t++) {
          sumDist = Math.max(sumDist, calcNormDist(sigma, mu, pi, dimension, t * 1.0));
        }
        if (sumDist <= MAX_ONE_SEPARATE_PEOPLE_RATE) break;
      }

      FileWriter fw = new FileWriter(inputCsvFilePath.toFile());
      PrintWriter pw = new PrintWriter(fw);

      for (Integer k = 0; k < naiveK; k++)
        pw.print("sigma_naive" + k + ",mu_naive" + k + ",pi_naive" + k + ",");
      for (Integer k = 0; k < rationalK; k++)
        pw.print("sigma_rational" + k + ",mu_rational" + k + ",pi_rational" + k + ",");
      for (Integer k = 0; k < rubyK; k++) pw.print("sigma_ruby" + k + ",mu_ruby" + k + ",pi_ruby" + k + ",");
      pw.println();

      for (Integer k = 0; k < naiveK; k++) pw.print(sigma[k] + "," + mu[k] + "," + pi[k] + ",");
      for (Integer k = naiveK; k < naiveK + rationalK; k++)
        pw.print(sigma[k] + "," + mu[k] + "," + pi[k] + ",");
      for (Integer k = naiveK + rationalK; k < naiveK + rationalK + rubyK; k++)
        pw.print(sigma[k] + "," + mu[k] + "," + pi[k] + ",");
      pw.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}