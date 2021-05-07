package masbench.evaluator;

import masbench.ModelProperty;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class PerformanceEvaluator {
  public static void main(String[] args) {
    switch (args[0]) {
      case "Observation":
        ObservationResultEvaluator.main(args);
        break;
      case "Simulation":
        SimulationResultEvaluator.main(args);
        break;
    }
  }

  static class AbstractEvaluator {
    static final Integer START_TIME = Integer.parseInt("190000");
    static final Integer CAMERA_CYCLE = 30;
    static final Integer GPS_CYCLE = 1;
    static final Integer SIGMA_L = 700;
    static final Double SIGMA = 4.0;

    static final Integer NUMBER_OF_ROUTE = 3;
    static final Integer NUMBER_OF_NAIVEAGENT = 300;

    Path simDirectoryPath;
    Path anaDirectoryPath;

    Integer gpsSeparating;

    public AbstractEvaluator(Path simDirectoryPath, Path anaDirectoryPath, int gpsSeparating) {
      this.simDirectoryPath = simDirectoryPath;
      this.anaDirectoryPath = anaDirectoryPath;
      this.gpsSeparating = gpsSeparating;
    }

    public void writeGoalFlowData(String csvNamePrefix) {
      ArrayList<Integer> flowList = new ArrayList<Integer>();
      ArrayList<Integer> timeList = new ArrayList<Integer>();
      ArrayList<Double> filterFlowList = new ArrayList<Double>();

      try {
        Integer time = START_TIME;
        FileReader fr = new FileReader(simDirectoryPath.resolve("evacuatedAgent.csv").toFile());
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();

        Integer currentFlow = 0;
        while ((line = br.readLine()) != null) {
          String subString[] = line.split(",");
          Integer nextFlow = Integer.parseInt(subString[1]) + Integer.parseInt(subString[2]) + Integer.parseInt(subString[3]);
          if (time % 100 == 0 || time % 100 == CAMERA_CYCLE) {
            flowList.add(nextFlow - currentFlow);
            timeList.add(time);
            currentFlow = nextFlow;
            filterFlowList.add(0.0);
          }
          time = nextTime(time, GPS_CYCLE);
        }
        br.close();

        FileWriter fw = new FileWriter(anaDirectoryPath.resolve(csvNamePrefix + "GoalFlow.csv").toFile(), false);
        PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
        pw.println("time30sec,countTime,inflowStation,gaussianFilter");
        for (Integer countTime = 0; countTime < timeList.size(); countTime++) {
          for (Integer filterTime = -1 * SIGMA_L; filterTime <= SIGMA_L; filterTime += 1) {
            Integer currentTime = countTime + filterTime;
            if (currentTime < 0 || currentTime >= timeList.size()) continue;
            filterFlowList.set(countTime, filterFlowList.get(countTime) + (1.0 * flowList.get(currentTime) * normalization(filterTime * 1.0)));
          }
          pw.println(timeList.get(countTime) + "," + (countTime * CAMERA_CYCLE) + "," + flowList.get(countTime) + "," + filterFlowList.get(countTime));
        }
        pw.close();

      } catch (IOException e) {
        System.err.println("Error : getSimulationGoalFlow");
        e.printStackTrace();
      }
    }

    public void writeAgentPositionData(String csvFile) {
      HashMap<String, Double> agentPosition = new HashMap<String, Double>();
      HashMap<String, ArrayList<TravelAgent>> agentPositionData = new HashMap<String, ArrayList<TravelAgent>>();

      try {
        int time = START_TIME;
        createAgentCsv(agentPosition, agentPositionData, csvFile);

        FileReader fr = new FileReader(simDirectoryPath.resolve("log_individual_pedestrians.csv").toFile());
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        int cnt = 0;
        int cost = 0;

        while ((line = br.readLine()) != null) {
          cost++;
          String subString[] = line.split(",");
          String indexa = subString[2].substring(8, subString[2].length() - 1);
          if (Integer.parseInt(subString[0]) > cnt) {
            time = nextTime(time, 1);
            cnt++;
          }

          agentPosition.put(indexa, agentPosition.get(indexa) + Double.parseDouble(subString[1]));
          agentPositionData.get(indexa).add(new SimulationResultEvaluator.TravelAgent(time, cnt, agentPosition.get(indexa)));

          if (cost > 100000) {
            outputAgentPosition(agentPositionData, csvFile);
            cost = 0;
          }
        }
        outputAgentPosition(agentPositionData, csvFile);
        br.close();
      } catch (IOException e) {
        System.err.println("Error : getSimulationAgent");
        e.printStackTrace();
      }
    }

    public static int nextTime(int currentTime, int cycleTime) {
      currentTime = currentTime + cycleTime;
      if (currentTime % 100 == 60) {
        currentTime = currentTime + 100 - 60;
      }
      if (currentTime % 10000 == 6000) {
        currentTime = currentTime + 10000 - 6000;
      }
      return currentTime;
    }

    public static double normalization(double x) {
      return Math.exp(-1.0 * x * x / (SIGMA * SIGMA * SIGMA)) / Math.sqrt(SIGMA * SIGMA * SIGMA * Math.PI);
    }

    public void createAgentCsv(HashMap<String, Double> agentPosition, HashMap<String, ArrayList<SimulationResultEvaluator.TravelAgent>> agentPositionData, String csvNamePrefix) {
      for (Integer i = 1; i <= NUMBER_OF_ROUTE; i++) {
        for (Integer j = 0; j < NUMBER_OF_NAIVEAGENT; j++) {
          if (j % gpsSeparating != 0) continue;
          String indexa = "R" + i + "_" + j;
          agentPosition.put(indexa, 0.0);
          agentPositionData.put(indexa, new ArrayList<SimulationResultEvaluator.TravelAgent>());
          File file = anaDirectoryPath.resolve(csvNamePrefix + "Agent_" + indexa + ".csv").toFile();
          try {
            FileWriter fw = new FileWriter(file, false);
            PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
            pw.println("time1sec,countTime,totalDistance");
            pw.close();
          } catch (IOException e) {
            System.out.println(e);
            System.out.println("Error : createAgentCSV " + indexa + ".csv");
          }
        }
      }
    }

    public void outputAgentPosition(HashMap<String, ArrayList<SimulationResultEvaluator.TravelAgent>> agentPositionData, String csvNamePrefix) {
      for (Integer i = 1; i <= NUMBER_OF_ROUTE; i++) {
        for (Integer j = 0; j < NUMBER_OF_NAIVEAGENT; j++) {
          if (j % gpsSeparating != 0) continue;
          String indexa = "R" + i + "_" + j;
          File file = anaDirectoryPath.resolve(csvNamePrefix + "Agent_" + indexa + ".csv").toFile();
          try {
            FileWriter fw = new FileWriter(file, true);
            PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
            Integer size = agentPositionData.get(indexa).size();
            for (int k = 0; k < size; k++) {
              SimulationResultEvaluator.TravelAgent TA = agentPositionData.get(indexa).remove(0);
              pw.println(TA.time1sec + "," + TA.countTime + "," + TA.totalDistance);
            }
            pw.close();
          } catch (IOException e) {
            System.out.println(e);
            System.out.println("Error : outputAgentPosition " + indexa + ".csv");
          }
        }
      }
    }

    static class TravelAgent {
      Integer time1sec;
      Integer countTime;
      Double totalDistance;

      public TravelAgent(Integer time1sec, Integer countTime, Double totalDistance) {
        this.time1sec = time1sec;
        this.countTime = countTime;
        this.totalDistance = totalDistance;
      }
    }
  }

  public static class ObservationResultEvaluator extends AbstractEvaluator {
    public static void main(String args[]) {
      ObservationResultEvaluator evaluator = new ObservationResultEvaluator(Paths.get(args[1]), Paths.get(args[2]), 10);

      System.out.println("Check : getObservationGoalFlow");
      evaluator.writeGoalFlowData("observation");
      System.out.println("Check : getObservationAgent");
      evaluator.writeAgentPositionData("observation");
    }

    public ObservationResultEvaluator(Path simDirectoryPath, Path anaDirectoryPath, int gpsSeparating) {
      super(simDirectoryPath, anaDirectoryPath, gpsSeparating);
    }
  }

  public static class SimulationResultEvaluator extends AbstractEvaluator {
    static final Double TRAVEL_DISTANCE[] = {0.0, 327.0, 577.0, 755.0};

    Path obsDirectoryPath;

    public static void main(String args[]) {
      //SimulationResultEvaluator evaluator = new SimulationResultEvaluator(Paths.get(args[1]), Paths.get(args[2]), Paths.get(args[3]), Integer.parseInt(args[4]));
      SimulationResultEvaluator evaluator = null;
      try {
        evaluator = new SimulationResultEvaluator(Paths.get(args[1]), Paths.get(args[2]), new ModelProperty(args[4]));
      } catch (IOException e) {
        e.printStackTrace();
      }
      evaluator.outputResults();
    }

    public SimulationResultEvaluator(Path simDirectoryPath, Path anaDirectoryPath, ModelProperty modelProperty) {
      super(simDirectoryPath, anaDirectoryPath, modelProperty.getGpsSeparating());
      this.obsDirectoryPath = modelProperty.getModelPath();
    }

    public void outputResults() {
      System.out.println("Check : getSimulationGoalFlow");
      writeGoalFlowData("simulation");
      System.out.println("Check : getSimulationAgent");
      writeAgentPositionData("simulation");
      System.out.println("Check : getErrorData");
      writeErrorData();
    }

    public void writeErrorData() {
      ArrayList<Double> observationList;
      ArrayList<Double> simulationList;

      Integer size1 = 0;
      Integer size10 = 0;
      ArrayList<Double> flowError = new ArrayList<Double>();
      ArrayList<Double> agentPositionError1 = new ArrayList<Double>();
      ArrayList<Double> agentPositionError10 = new ArrayList<Double>();

      for (Integer i = 0; i < 11; i++) {
        if (gpsSeparating == 1) {
          agentPositionError1.add(0.0);
        } else if (gpsSeparating == 10) {
          agentPositionError10.add(0.0);
        }
      }
      observationList = readFlowCsv(obsDirectoryPath.resolve("observationGoalFlow.csv"));
      simulationList = readFlowCsv(anaDirectoryPath.resolve("simulationGoalFlow.csv"));
      sizeControl(observationList, simulationList, 0.0);
      Double AverageO = average(observationList);
      Double AverageS = average(simulationList);

      flowError.add(calcMAE(observationList, simulationList, AverageO, AverageS));
      flowError.add(calcRMSE(observationList, simulationList, AverageO, AverageS));
      flowError.add(calcMSE(observationList, simulationList, AverageO, AverageS));
      flowError.add(squaredNSE(observationList, simulationList, AverageO, AverageS));
      flowError.add(absoluteNSE(observationList, simulationList, AverageO, AverageS));
      flowError.add(squaredR(observationList, simulationList, AverageO, AverageS));
      flowError.add(absoluteR(observationList, simulationList, AverageO, AverageS));
      flowError.add(squaredD(observationList, simulationList, AverageO, AverageS));
      flowError.add(absoluteD(observationList, simulationList, AverageO, AverageS));
      flowError.add(wattersonM(observationList, simulationList, AverageO, AverageS));
      flowError.add(universalR(observationList, simulationList, AverageO, AverageS));
      outputFlowError(flowError);

      for (Integer i = 1; i <= NUMBER_OF_ROUTE; i++) {
        for (Integer j = 0; j < NUMBER_OF_NAIVEAGENT; j++) {
          if (j % gpsSeparating != 0) continue;
          observationList = readFlowCsv(obsDirectoryPath.resolve("observationAgent_R" + i + "_" + j + ".csv"));
          simulationList = readFlowCsv(anaDirectoryPath.resolve("simulationAgent_R" + i + "_" + j + ".csv"));
          maxDistanceControl(observationList, TRAVEL_DISTANCE[i]);
          maxDistanceControl(simulationList, TRAVEL_DISTANCE[i]);
          sizeControl(observationList, simulationList, TRAVEL_DISTANCE[i]);
          AverageO = average(observationList);
          AverageS = average(simulationList);
          if (gpsSeparating == 1) {
            size1 = size1 + countAgentPositionError(observationList, simulationList, AverageO, AverageS, j, 1, agentPositionError1);
          } else if (gpsSeparating == 10) {
            size10 = size10 + countAgentPositionError(observationList, simulationList, AverageO, AverageS, j, 10, agentPositionError10);
          }
        }
      }

      if (gpsSeparating == 1) {
        outputAgentPositionError(1, size1 * 1.0, agentPositionError1);
        outputAllError(size1 * 1.0, agentPositionError1, flowError);
      } else if (gpsSeparating == 10) {
        outputAgentPositionError(10, size10 * 1.0, agentPositionError10);
        outputAllError(size10 * 1.0, agentPositionError10, flowError);
      }
    }

    public void outputFlowError(ArrayList<Double> flowError) {
      try {
        FileWriter fw = new FileWriter(anaDirectoryPath.resolve("flowError.csv").toFile(), false);
        PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
        pw.println("MAE,RMSE,MSE,squared_NSE,absolute_NSE,squared_R,absolute_R,squared_d,absolute_d,Watterson_M,universal_R,");
        for (Integer i = 0; i < 11; i++) {
          pw.print(flowError.get(i) + ",");
        }
        pw.close();
      } catch (IOException e) {
        System.err.println("Error : getErrorData");
        e.printStackTrace();
      }
    }

    public void outputAgentPositionError(Integer index, Double size, ArrayList<Double> agentPositionError) {
      try {
        FileWriter fw = new FileWriter(anaDirectoryPath.resolve(index + "minAgentPositionError.csv").toFile(), false);
        PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
        pw.println("MAE,RMSE,MSE,squared_NSE,absolute_NSE,squared_R,absolute_R,squared_d,absolute_d,Watterson_M,universal_R,");
        for (Integer i = 0; i < 11; i++) {
          pw.print((agentPositionError.get(i) / size) + ",");
        }
        pw.close();
      } catch (IOException e) {
        System.err.println("Error : getErrorData");
        e.printStackTrace();
      }
    }

    public void outputAllError(Double size, ArrayList<Double> agentPositionError, ArrayList<Double> flowError) {
      try {
        FileWriter fw = new FileWriter(anaDirectoryPath.resolve("Fitness.csv").toFile(), false);
        PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
        pw.println("AllError,FlowError,PosError,StartFlowError,FollowFlowError,IdleFlowError,BusyFlowError");
        double allerror = ((flowError.get(1) * gpsSeparating) + (agentPositionError.get(1) / size)) / 2;
        double flowerror = flowError.get(1) * gpsSeparating;
        double poserror = (agentPositionError.get(1) / size);
        pw.print(allerror + "," + flowerror + "," + poserror + "," + getStartFlowError(1) + "," + getStartFlowError(2) + "," + getStartFlowError(3) + "," + getStartFlowError(4));
        pw.close();
      } catch (IOException e) {
        System.err.println("Error : getAllData");
        e.printStackTrace();
      }
    }

    public Double getStartFlowError(Integer T) {
      try {
        ArrayList<Double> Type = new ArrayList<Double>();
        ArrayList<Double> Type2 = new ArrayList<Double>();

        FileReader fr = new FileReader(anaDirectoryPath.resolve("simulationStartFlow.csv").toFile());
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        while ((line = br.readLine()) != null) {
          String subString[] = line.split(",");
          Type.add(Double.parseDouble(subString[subString.length - T]));
        }
        br.close();

        fr = new FileReader(obsDirectoryPath.resolve("observationStartFlow.csv").toFile());
        br = new BufferedReader(fr);
        line = br.readLine();
        while ((line = br.readLine()) != null) {
          String subString[] = line.split(",");
          Type2.add(Double.parseDouble(subString[subString.length - T]));
        }
        br.close();

        Double RMSEs = 0.0;
        Integer length = Math.max(Type.size(), Type2.size());
        for (Integer i = 0; i < length; i++) {
          Double T1 = 0.0;
          Double T2 = 0.0;
          if (Type.size() - i > 0) {
            T1 = Type.get(i);
          } else {
            T1 = 0.0;
          }
          if (Type2.size() - i > 0) {
            T2 = Type2.get(i);
          } else {
            T2 = 0.0;
          }
          RMSEs = RMSEs + ((T1 - T2) * (T1 - T2));
        }
        RMSEs = Math.sqrt(RMSEs / length);
        return RMSEs;
      } catch (IOException e) {
      }
      return 100000.0;
    }

    public static Integer countAgentPositionError(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS, Integer index, Integer cycle, ArrayList<Double> agentPositionError) {
      if (index % cycle == 0) {
        agentPositionError.set(0, agentPositionError.get(0) + calcMAE(observationList, simulationList, AverageO, AverageS));
        agentPositionError.set(1, agentPositionError.get(1) + calcRMSE(observationList, simulationList, AverageO, AverageS));
        agentPositionError.set(2, agentPositionError.get(2) + calcMSE(observationList, simulationList, AverageO, AverageS));
        agentPositionError.set(3, agentPositionError.get(3) + squaredNSE(observationList, simulationList, AverageO, AverageS));
        agentPositionError.set(4, agentPositionError.get(4) + absoluteNSE(observationList, simulationList, AverageO, AverageS));
        agentPositionError.set(5, agentPositionError.get(5) + squaredR(observationList, simulationList, AverageO, AverageS));
        agentPositionError.set(6, agentPositionError.get(6) + absoluteR(observationList, simulationList, AverageO, AverageS));
        agentPositionError.set(7, agentPositionError.get(7) + squaredD(observationList, simulationList, AverageO, AverageS));
        agentPositionError.set(8, agentPositionError.get(8) + absoluteD(observationList, simulationList, AverageO, AverageS));
        agentPositionError.set(9, agentPositionError.get(9) + wattersonM(observationList, simulationList, AverageO, AverageS));
        agentPositionError.set(10, agentPositionError.get(10) + universalR(observationList, simulationList, AverageO, AverageS));
        return 1;
      }
      return 0;
    }

    public static void maxDistanceControl(ArrayList<Double> agentList, Double maxDistance) {
      Integer length = agentList.size();
      Double control = maxDistance / agentList.get(length - 1);
      for (Integer i = 0; i < length; i++) {
        agentList.set(i, agentList.get(i) * control);
      }
    }

    public static void sizeControl(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double addNumber) {
      Integer length = Math.abs(simulationList.size() - observationList.size());
      if (observationList.size() < simulationList.size()) {
        for (Integer i = 0; i < length; i++) {
          observationList.add(addNumber);
        }
      } else {
        for (Integer i = 0; i < length; i++) {
          simulationList.add(addNumber);
        }
      }
    }

    public static Double average(ArrayList<Double> flowList) {
      Double result = 0.0;
      for (Integer i = 0; i < flowList.size(); i++) {
        result = result + flowList.get(i);
      }
      result = result / flowList.size();
      return result;
    }

    public ArrayList<Double> readFlowCsv(Path filePath) {
      ArrayList<Double> agentList = new ArrayList<>();
      try {
        FileReader fr = new FileReader(filePath.toFile());
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        while ((line = br.readLine()) != null) {
          String agent[] = line.split(",");
          agentList.add(Double.parseDouble(agent[agent.length - 1]));
        }
        br.close();
      } catch (IOException e) {
        System.err.println("Error : readFlowCSV");
        e.printStackTrace();
      }
      return agentList;
    }

    public static Double absoluteSum(ArrayList<Double> observationList, ArrayList<Double> simulationList) {
      Double result = 0.0;
      Integer length = observationList.size();
      for (Integer i = 0; i < length; i++) {
        for (Integer j = 0; j < length; j++) {
          result = result + Math.abs(simulationList.get(j) - observationList.get(i));
        }
      }
      return result;
    }

    public static Double calcC(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      Integer length = observationList.size();
      for (Integer i = 0; i < length; i++) {
        result = result + ((observationList.get(i) - AverageO) * (simulationList.get(i) - AverageS));
      }
      return result;
    }

    public static Double calcOsAD(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      Integer length = observationList.size();
      for (Integer i = 0; i < length; i++) {
        result = result + Math.abs(observationList.get(i) - AverageO) + Math.abs(simulationList.get(i) - AverageO);
      }
      return result;
    }

    public static Double calcOsSD(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      Integer length = observationList.size();
      for (Integer i = 0; i < length; i++) {
        result = result + (Math.abs(observationList.get(i) - AverageO) + Math.abs(simulationList.get(i) - AverageO)) * (Math.abs(observationList.get(i) - AverageO) + Math.abs(simulationList.get(i) - AverageO));
      }
      return result;
    }

    public static Double calcAD(ArrayList<Double> agentList, Double agentAverage) {
      Double result = 0.0;
      Integer length = agentList.size();
      for (Integer i = 0; i < length; i++) {
        result = result + Math.abs(agentList.get(i) - agentAverage);
      }
      return result;
    }

    public static Double calcSD(ArrayList<Double> agentList, Double agentAverage) {
      Double result = 0.0;
      Integer length = agentList.size();
      for (Integer i = 0; i < length; i++) {
        result = result + ((agentList.get(i) - agentAverage) * (agentList.get(i) - agentAverage));
      }
      return result;
    }

    public static Double calcAE(ArrayList<Double> observationList, ArrayList<Double> simulationList) {
      Double result = 0.0;
      Integer length = observationList.size();
      for (Integer i = 0; i < length; i++) {
        result = result + Math.abs(observationList.get(i) - simulationList.get(i));
      }
      return result;
    }

    public static Double calcSE(ArrayList<Double> observationList, ArrayList<Double> simulationList) {
      Double result = 0.0;
      Integer length = observationList.size();
      for (Integer i = 0; i < length; i++) {
        result = result + ((observationList.get(i) - simulationList.get(i)) * (observationList.get(i) - simulationList.get(i)));
      }
      return result;
    }

    public static Double calcMAE(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      Integer length = observationList.size();
      result = calcAE(observationList, simulationList) / length;
      return result;
    }

    public static Double calcRMSE(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      Integer length = observationList.size();
      result = Math.sqrt(calcSE(observationList, simulationList) / length);
      return result;
    }

    public static Double calcMSE(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      Integer length = observationList.size();
      result = calcSE(observationList, simulationList) / length;
      return result;
    }

    public static Double squaredNSE(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      result = 1 - (calcSE(observationList, simulationList) / calcSD(observationList, AverageO));
      return result;
    }

    public static Double absoluteNSE(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      result = 1 - (calcAE(observationList, simulationList) / calcAD(observationList, AverageO));
      return result;
    }

    public static Double squaredR(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      result = calcC(observationList, simulationList, AverageO, AverageS) / (Math.sqrt(calcSD(observationList, AverageO)) * Math.sqrt(calcSD(simulationList, AverageS)));
      result = result * result;
      return result;
    }

    public static Double absoluteR(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      result = calcC(observationList, simulationList, AverageO, AverageS) / (Math.sqrt(calcSD(observationList, AverageO)) * Math.sqrt(calcSD(simulationList, AverageS)));
      result = (result + 1.0) / 2.0;
      return result;
    }

    public static Double squaredD(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      result = 1 - (calcSE(observationList, simulationList) / calcOsSD(observationList, simulationList, AverageO, AverageS));
      return result;
    }

    public static Double absoluteD(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      result = 1 - (calcAE(observationList, simulationList) / calcOsAD(observationList, simulationList, AverageO, AverageS));
      return result;
    }

    public static Double wattersonM(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      Double length = observationList.size() * 1.0;
      result = (2.0 / Math.PI) * Math.asin(1 - (calcMSE(observationList, simulationList, AverageO, AverageS) / ((calcSD(observationList, AverageO) / length) + (calcSD(simulationList, AverageS) / length) + (Math.abs(AverageO - AverageS) * Math.abs(AverageO - AverageS)))));
      return result;
    }

    public static Double universalR(ArrayList<Double> observationList, ArrayList<Double> simulationList, Double AverageO, Double AverageS) {
      Double result = 0.0;
      Double length = observationList.size() * 1.0;
      result = 1 - (calcMAE(observationList, simulationList, AverageO, AverageS) / (Math.pow(length, -2.0) * absoluteSum(observationList, simulationList)));
      return result;
    }

    /*
       public static Double dicimalCurrentTime(Integer currentTime) {
       Integer h = currentTime / 10000;
       Integer m = (currentTime / 100) % 100;
       Integer s = currentTime % 100;
       return h * 100 + m * 60 * 100.0 / 3600 + ((s % 100) * 100.0) / 3600;
       }
       */
  }
}
