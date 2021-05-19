package masbench.generator;

import masbench.ModelProperty;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class GenerationFileGenerator {
  public static void main(String[] args) throws IOException {
    switch (args[0]) {
      case "Observation":
        Original.main(args);
        break;
      case "Simulation":
        Simulation.main(args);
        break;
    }
  }

  static abstract class AbstractGenerationFileGenerator {
    static final Integer START_HOUR = 19;
    static final Integer START_MINUTES = 0;
    static final Integer START_SECOND = 0;
    static final Integer END_HOUR = 24;
    //final Integer END_MINUTES = 0;
    //final Integer END_SECOND = 0;

    Path inputCsvFilePath; // = "input.csv";
    Path outputJsonFilePath; // = "output.json";
    Path outputCsvFilePath; // = "simulationStartFlow.csv";

    ArrayList<Simulation.NormVar> naiveVarList = new ArrayList<>();
    ArrayList<Simulation.NormVar> rationalVarList = new ArrayList<>();
    ArrayList<Simulation.NormVar> rubyVarList = new ArrayList<>();

    Integer gpsSeparating = 1;
    Double maxPeople;
    Double alpha;

    public AbstractGenerationFileGenerator(Path inputCsvFilePath, Path outputJsonFilePath, Path outputCsvFilePath, Double maxPeople, int gpsSeparating) {
      this.inputCsvFilePath = inputCsvFilePath;
      this.outputJsonFilePath = outputJsonFilePath;
      this.outputCsvFilePath = outputCsvFilePath;
      this.maxPeople = maxPeople;
      this.gpsSeparating = gpsSeparating;
    }

    void readCsv() {
      try {
        FileReader fr = new FileReader(inputCsvFilePath.toFile());
        BufferedReader br = new BufferedReader(fr);
        String variablesName[] = br.readLine().split(",");
        String variables[] = br.readLine().split(",");

        br.close();

        ArrayList<String> readColumnList = new ArrayList<>();

        for (Integer i = 0; i < variablesName.length; i++) {
          String sampledHeader = variablesName[i].trim();
          if (!readColumnList.contains(sampledHeader)) {
            Simulation.NormVar normVar = new Simulation.NormVar();
            String regex = sampledHeader.replaceAll(
                "^(sigma|mu|pi)_", "^(sigma|mu|pi)_");

            for (Integer i2 = 0; i2 < variablesName.length; i2++) {
              String variableName = variablesName[i2].trim();
              String variable = variables[i2].trim();
              if (!readColumnList.contains(variableName)
                  && variableName.matches(regex)) {

                readColumnList.add(variableName);

                if (variableName.indexOf("sigma_") == 0) {
                  normVar.sigma = Double.parseDouble(variable);
                } else if (variableName.indexOf("mu_") == 0) {
                  normVar.mu = Double.parseDouble(variable);
                } else if (variableName.indexOf("pi_") == 0) {
                  normVar.pi = Double.parseDouble(variable);
                }
                  }
            }

            if (sampledHeader.indexOf("_naive") > 0) {
              naiveVarList.add(normVar);
            } else if (sampledHeader.indexOf("_rational") > 0) {
              rationalVarList.add(normVar);
            } else if (sampledHeader.indexOf("_ruby") > 0) {
              rubyVarList.add(normVar);
            }
          }
        }

      } catch (IOException e) {
        e.printStackTrace();
      }

      alpha = correctionValue(maxPeople);
    }

    void writeJson() {
      try {
        FileWriter fw = new FileWriter(outputJsonFilePath.toFile(), false);
        PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

        pw.println("#{ \"version\" : 2}");
        pw.println("[");

        for (Simulation.Time t = new Simulation.Time(19, 0, 0, 0); t.h < 24; t.step()) {
          String startTime = t.h + ":" + inFrontZero(t.m) + ":" + inFrontZero(t.s);

          Integer normDistNaive = sumNormDist(naiveVarList, naiveVarList.size(), t.c * 1.0, alpha);
          Integer normDistRational = sumNormDist(rationalVarList, rationalVarList.size(), t.c * 1.0, alpha);
          Integer normDistRuby = sumNormDist(rubyVarList, rubyVarList.size(), t.c * 1.0, alpha);

          if (normDistNaive > 0) pw.println(naiveAgent(startTime, normDistNaive));
          if (normDistRational > 0) pw.println(rationalAgent(startTime, normDistRational));
          if (normDistRuby > 0) pw.println(rubyAgent(startTime, normDistRuby));

          for (int i = 1; i <= 3; i++) {
            if (t.c % gpsSeparating == 0) {
              pw.println(naiveAgentOneMinutes(startTime, i, t.c));
            }
          }
        }
        pw.println(finishAgent());
        pw.println("]");
        pw.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    void writeCsv() {
      try {
        FileWriter fw = new FileWriter(outputCsvFilePath.toFile(), false);
        PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

        pw.println("currentTime,naiveAgent,rationalAgent,rubyAgent,allAgent");

        for (Simulation.Time t = new Simulation.Time(19, 0, 0, 0); t.h < 24; t.step()) {
          String startTime = t.h + ":" + inFrontZero(t.m) + ":" + inFrontZero(t.s);

          Integer normDistNaive = sumNormDist(naiveVarList, naiveVarList.size(), t.c * 1.0, alpha);
          Integer normDistRational = sumNormDist(rationalVarList, rationalVarList.size(), t.c * 1.0, alpha);
          Integer normDistRuby = sumNormDist(rubyVarList, rubyVarList.size(), t.c * 1.0, alpha);

          Integer sumDist = normDistNaive + normDistRational + normDistRuby;
          pw.println(startTime + "," + normDistNaive + "," + normDistRational + "," + normDistRuby + "," + sumDist);
        }
        pw.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public void generate() {
      writeJson();
      writeCsv();
    }

    static class NormVar {
      Double sigma;
      Double mu;
      Double pi;

      public NormVar(Double sigma, Double mu, Double pi) {
        this.sigma = sigma;
        this.mu = mu;
        this.pi = pi;
      }

      public NormVar() {
      }

      public String toString() {
        return "s:" + sigma + ", m:" + mu + ", pi:" + pi;
      }
    }

    static class Time {
      Integer h;
      Integer m;
      Integer s;
      Integer c;

      public Time(Integer h, Integer m, Integer s, Integer c) {
        this.h = h;
        this.m = m;
        this.s = s;
        this.c = c;
      }

      public Time step() {
        c += 1;
        h = 19 + (c / 60);
        m = c % 60;
        return this;
      }
    }

    public Double correctionValue(Double maxPeople) {
      Double alpha = maxPeople;
      int cnt = 0;
      while (true) {
        cnt += 1;
        Integer sumDist = 0;
        for (Time t = new Time(START_HOUR, START_MINUTES, START_SECOND, 0); t.h < END_HOUR; t.step()) {
          Integer normDistNaive = sumNormDist(naiveVarList, naiveVarList.size(), t.c * 1.0, alpha);
          Integer normDistRational = sumNormDist(rationalVarList, rationalVarList.size(), t.c * 1.0, alpha);
          Integer normDistRuby = sumNormDist(rubyVarList, rubyVarList.size(), t.c * 1.0, alpha);

          sumDist += normDistNaive + normDistRational + normDistRuby;
        }
        //			System.out.println(sumDist);
        if (maxPeople - sumDist == 0 || cnt > 100000) {
          break;
        }
        else {
          alpha = alpha + (maxPeople - sumDist) * 1.0 / cnt;
        }
      }
      return alpha;
    }

    public String inFrontZero(Integer num) {
      if (num < 10) return "0" + num;
      return "" + num;
    }

    public Integer sumNormDist(ArrayList<Simulation.NormVar> varSet, Integer Size, Double x, Double alpha) {
      Integer result = 0;
      Double normDist = 0.0;

      for (Integer i = 0; i < Size; i++) {
        Simulation.NormVar Var = varSet.get(i);
        normDist += Math.floor(Math.exp(-(x - Var.mu) * (x - Var.mu) / (2.0 * Var.sigma * Var.sigma)) / Math.sqrt(2.0 * Math.PI * Var.sigma * Var.sigma) * Var.pi * alpha);
      }
      result = normDist.intValue();
      return result;
    }

    public String naiveAgentOneMinutes(String startTime, Integer indexa, Integer cnt) {
      String result = "";
      if (indexa == 1) {
        result =
          "  {\"rule\":\"EACH\"" +
          ",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_Route1\"}" +
          ",\"startTime\":\"" + startTime + "\"" +
          ",\"total\":1," +
          "\"duration\":1," +
          "\"startPlace\":\"SL_GPS\"" +
          ",\"goal\":\"EXIT_GPS\"" +
          ",\"conditions\":[\"INDEXA_R" + indexa + "_" + cnt + "\"]},";
      } else if (indexa == 2) {
        result =
          "  {\"rule\":\"EACH\"" +
          ",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_Route2\"}" +
          ",\"startTime\":\"" + startTime + "\"" +
          ",\"total\":1," +
          "\"duration\":1," +
          "\"startPlace\":\"SL_GPS\"" +
          ",\"goal\":\"EXIT_GPS\"" +
          ",\"conditions\":[\"INDEXA_R" + indexa + "_" + cnt + "\"]},";
      } else if (indexa == 3) {
        result =
          "  {\"rule\":\"EACH\"" +
          ",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_Route3\"}" +
          ",\"startTime\":\"" + startTime + "\"" +
          ",\"total\":1," +
          "\"duration\":1," +
          "\"startPlace\":\"SL_GPS\"" +
          ",\"goal\":\"EXIT_GPS\"" +
          ",\"conditions\":[\"INDEXA_R" + indexa + "_" + cnt + "\"]},";
      }
      return result;
    }

    public String naiveAgent(String startTime, Integer total) {
      String result =
        "  {\"rule\":\"EACH\"" +
        ",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_Busy\"}" +
        ",\"startTime\":\"" + startTime + "\"" +
        ",\"total\":" + total + "," +
        "\"duration\":60," +
        "\"startPlace\":\"SL_SWM\"" +
        ",\"goal\":\"EXIT_STATION\"},";
      return result;
    }

    public String rubyAgent(String startTime, Integer total) {
      String result =
        "  {\"rule\":\"EACH\"" +
        ",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_SignalFollow\"}" +
        ",\"startTime\":\"" + startTime + "\"" +
        ",\"total\":" + total + "," +
        "\"duration\":60," +
        "\"startPlace\":\"SL_SWM\"" +
        ",\"goal\":\"EXIT_STATION\"},";
      return result;
    }

    public String rationalAgent(String startTime, Integer total) {
      String result =
        "  {\"rule\":\"EACH\"" +
        ",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_StallIdle\"}" +
        ",\"startTime\":\"" + startTime + "\"" +
        ",\"total\":" + total + "," +
        "\"duration\":60," +
        "\"startPlace\":\"SL_SWM\"" +
        ",\"goal\":\"EXIT_STATION\"},";
      return result;
    }

    public String finishAgent() {
      String result =
        "  {}";
      return result;
    }
  }

  public static class Original extends AbstractGenerationFileGenerator {
    static final Integer TIME_SEPARATE = 300;
    static final Double MAX_ONE_SEPARATE_PEOPLE_RATE = 0.01;

    static final Double MAX_SIGMA = 100.0;
    static final Double MAX_MU = 300.0;
    static final Double MAX_PI = 1.0;
    static final Double MIN_SIGMA = 3.0;
    static final Double MIN_MU = 0.0;
    static final Double MIN_PI = 0.0;

    public Double NormDist(Double sigma[], Double mu[], Double pi[], Integer Size, Double x) {
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
            sumDist = Math.max(sumDist, NormDist(sigma, mu, pi, dimension, t * 1.0));
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

    public static void main(String[] args) {
      Original generator = new Original(Paths.get(args[1]), Paths.get(args[2]), Paths.get(args[3]));
      generator.generate();
    }

    public Original(Path inputCsvFilePath, Path outputJsonFilePath, Path outputCsvFilePath) {
      super(inputCsvFilePath, outputJsonFilePath, outputCsvFilePath, 45000.0, 1);
      Integer naiveAgentSize = 1;
      Integer rationalAgentSize = 1;
      Integer rubyAgentSize = 1;
      Integer agentSize = 3;

      setCsv(agentSize, naiveAgentSize, rationalAgentSize, rubyAgentSize);
      readCsv();
    }
  }

  public static class Simulation extends AbstractGenerationFileGenerator{

    public static void main(String[] args) throws IOException {
      //Simulation generator = new Simulation(Paths.get(args[1]), Paths.get(args[2]), Paths.get(args[3]),
      //		Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]));
      Simulation generator = new Simulation(Paths.get(args[1]), Paths.get(args[2]), Paths.get(args[3]), new ModelProperty(args[4]));
      generator.generate();
    }

    public Simulation(Path inputCsvFilePath, Path outputJsonFilePath, Path outputCsvFilePath, ModelProperty modelProperty) {
      super(inputCsvFilePath, outputJsonFilePath, outputCsvFilePath, new Double(modelProperty.getNumberOfPeople()), modelProperty.getGpsSeparating());
      readCsv();
    }
  }
}
