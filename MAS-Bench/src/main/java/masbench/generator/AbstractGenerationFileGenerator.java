package masbench.generator;

import masbench.generator.value.NormVar;
import masbench.generator.value.Time;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;

public abstract class AbstractGenerationFileGenerator {
  static final Integer START_HOUR = 19;
  static final Integer START_MINUTES = 0;
  static final Integer START_SECOND = 0;
  static final Integer END_HOUR = 24;
  //final Integer END_MINUTES = 0;
  //final Integer END_SECOND = 0;

  Path inputCsvFilePath; // = "input.csv";
  Path outputJsonFilePath; // = "output.json";
  Path outputCsvFilePath; // = "simulationStartFlow.csv";

  ArrayList<NormVar> naiveVarList = new ArrayList<>();
  ArrayList<NormVar> rationalVarList = new ArrayList<>();
  ArrayList<NormVar> rubyVarList = new ArrayList<>();

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
          NormVar normVar = new NormVar();
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

      for (Time t = new Time(19, 0, 0, 0); t.h < 24; t.step()) {
        String startTime = t.toString();

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
      pw.println(terminalAgent());
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

      for (Time t = new Time(19, 0, 0, 0); t.h < 24; t.step()) {
        Integer normDistNaive = sumNormDist(naiveVarList, naiveVarList.size(), t.c * 1.0, alpha);
        Integer normDistRational = sumNormDist(rationalVarList, rationalVarList.size(), t.c * 1.0, alpha);
        Integer normDistRuby = sumNormDist(rubyVarList, rubyVarList.size(), t.c * 1.0, alpha);

        Integer sumDist = normDistNaive + normDistRational + normDistRuby;
        pw.println(t + "," + normDistNaive + "," + normDistRational + "," + normDistRuby + "," + sumDist);
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

  public Integer sumNormDist(ArrayList<NormVar> varSet, Integer Size, Double x, Double alpha) {
    Integer result = 0;
    Double normDist = 0.0;

    for (Integer i = 0; i < Size; i++) {
      NormVar Var = varSet.get(i);
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

  public String terminalAgent() {
    String result =
      "  {}";
    return result;
  }
}

