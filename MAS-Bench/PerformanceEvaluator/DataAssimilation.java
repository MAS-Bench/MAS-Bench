
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class DataAssimilation {
	public static void main(String[] args) {
		switch (args[0]){
		case "Observation":
			new Observation_Result().main(args);
			break;
		case "Simulation":
			new Simulation_Result().main(args);
			break;
		}
	}
}

class Observation_Result extends Simulation_Result {
	@Override
	public void main(String args[]){
		Simpos = args[1];
		Anapos = args[2];
		
		ArrayList<Integer> flowList = new ArrayList<Integer>();
		ArrayList<Integer> timeList = new ArrayList<Integer>();
		ArrayList<Double> filterFlowList = new ArrayList<Double>();
		HashMap<String,Double> agentPosition = new HashMap<String,Double>();
		HashMap<String,ArrayList<TravelAgent>> agentPositionData = new HashMap<String,ArrayList<TravelAgent>>();

		System.out.println("Check : getObservationGoalFlow");
		getGoalFlow(flowList,timeList,filterFlowList,"observation");
		System.out.println("Check : getObservationAgent");
		getAgentPosition(agentPosition,agentPositionData,"observation");
	}
}

class Simulation_Result {
	static Scanner sc = new Scanner(System.in);
	static final Integer startTime = Integer.parseInt("190000");
	static final Integer CAMERA_CYCLE = 30;
	static final Integer GPS_CYCLE = 1;
	static final Integer SIGMA_L = 700;
	static final Double SIGMA = 4.0;

	static final Integer NUMBER_OF_ROUTE = 3;
	static final Integer NUMBER_OF_NAIVEAGENT = 300;
	static final Double TRAVEL_DISTANCE[] = {0.0,327.0,577.0,755.0};
	static Integer GPS_SEPARATE = 10;

	static String Simpos = "";
	static String Anapos = "";
	static String Obspos = "";
	
	public void main(String args[]){
		Simpos = args[1];
		Anapos = args[2];
		Obspos = args[3];
		GPS_SEPARATE = Integer.parseInt(args[4]);

		ArrayList<Integer> flowList = new ArrayList<Integer>();
		ArrayList<Integer> timeList = new ArrayList<Integer>();
		ArrayList<Double> filterFlowList = new ArrayList<Double>();
		HashMap<String,Double> agentPosition = new HashMap<String,Double>();
		HashMap<String,ArrayList<TravelAgent>> agentPositionData = new HashMap<String,ArrayList<TravelAgent>>();
		ArrayList<Double> observationList = new ArrayList<Double>();
		ArrayList<Double> simulationList = new ArrayList<Double>();

		System.out.println("Check : getSimulationGoalFlow");
		getGoalFlow(flowList,timeList,filterFlowList,"simulation");
		System.out.println("Check : getSimulationAgent");
		getAgentPosition(agentPosition,agentPositionData,"simulation");
		System.out.println("Check : getErrorData");
		getErrorData(observationList,simulationList);
	}

	public static void getGoalFlow(ArrayList<Integer> flowList,ArrayList<Integer> timeList,ArrayList<Double> filterFlowList,String csvFile){
		try {
			Integer time = startTime;
			String file = "evacuatedAgent.csv";
			FileReader fr = new FileReader(Simpos+file);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			
			Integer currentFlow = 0;
			while ((line = br.readLine()) != null) {
				String subString[] = line.split(",");
				Integer nextFlow = Integer.parseInt(subString[1])+Integer.parseInt(subString[2])+Integer.parseInt(subString[3]);
				if(time % 100 == 0 || time % 100 == CAMERA_CYCLE){
					flowList.add(nextFlow-currentFlow);
					timeList.add(time);
					currentFlow = nextFlow;
					filterFlowList.add(0.0);
				}
				time = nextTime(time, GPS_CYCLE);
			}
			br.close();
			
			file = Anapos+csvFile+"GoalFlow.csv";
			FileWriter fw = new FileWriter(file, false);
			PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
			pw.println("time30sec,countTime,inflowStation,gaussianFilter");
			for(Integer countTime = 0;countTime < timeList.size();countTime++){
				for(Integer filterTime = -1 * SIGMA_L;filterTime <= SIGMA_L;filterTime += 1){
					Integer currentTime = countTime + filterTime;
					if(currentTime < 0 || currentTime >= timeList.size())continue;
					filterFlowList.set(countTime,filterFlowList.get(countTime) + (1.0 * flowList.get(currentTime) * Normalization(filterTime*1.0)));
				}
				pw.println(timeList.get(countTime)+","+(countTime*CAMERA_CYCLE)+","+flowList.get(countTime)+","+filterFlowList.get(countTime));
			}
			pw.close();
			
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Error : getSimulationGoalFlow");
		}
	}
	public static void getAgentPosition(HashMap<String,Double> agentPosition,HashMap<String,ArrayList<TravelAgent>> agentPositionData,String csvFile){
		try {
			int time = startTime;
			createAgentCSV(agentPosition,agentPositionData,csvFile);
			
			String file = "log_individual_pedestrians.csv";
			FileReader fr = new FileReader(Simpos+file);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			int cnt = 0;
			int cost = 0;
			
			while ((line = br.readLine()) != null) {
				cost++;
				String subString[] = line.split(",");
				String indexa = subString[2].substring(8,subString[2].length()-1);
				if(Integer.parseInt(subString[0]) > cnt){
					time = nextTime(time, 1);
					cnt++;
				}
				
				agentPosition.put(indexa,agentPosition.get(indexa) + Double.parseDouble(subString[1]));
				agentPositionData.get(indexa).add(new TravelAgent(time,cnt,agentPosition.get(indexa)));
				
				if(cost > 100000){
					outputAgentPosition(agentPositionData,csvFile);
					cost = 0;
				}
			}
			outputAgentPosition(agentPositionData,csvFile);
			br.close();
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Error : getSimulationAgent");
		}
	}
	public static void getErrorData(ArrayList<Double> observationList,ArrayList<Double> simulationList){
		Integer size1 = 0;
		Integer size10 = 0;
		ArrayList<Double>flowError = new ArrayList<Double>();
		ArrayList<Double>agentPositionError1 = new ArrayList<Double>();
		ArrayList<Double>agentPositionError10 = new ArrayList<Double>();
		
		for(Integer i = 0;i < 11;i++){
			if(GPS_SEPARATE == 1){
				agentPositionError1.add(0.0);
			}else if(GPS_SEPARATE == 10){
				agentPositionError10.add(0.0);
			}
		}
		String fileO = "observationGoalFlow.csv";
		String fileS = "simulationGoalFlow.csv";
		readCSV(Obspos+fileO,observationList);
		readCSV(Anapos+fileS,simulationList);
		sizeControl(observationList,simulationList,0.0);
		Double AverageO = Average(observationList);
		Double AverageS = Average(simulationList);
		
		flowError.add(MAE(observationList,simulationList,AverageO,AverageS));
		flowError.add(RMSE(observationList,simulationList,AverageO,AverageS));
		flowError.add(MSE(observationList,simulationList,AverageO,AverageS));
		flowError.add(squared_NSE(observationList,simulationList,AverageO,AverageS));
		flowError.add(absolute_NSE(observationList,simulationList,AverageO,AverageS));
		flowError.add(squared_R(observationList,simulationList,AverageO,AverageS));
		flowError.add(absolute_R(observationList,simulationList,AverageO,AverageS));
		flowError.add(squared_d(observationList,simulationList,AverageO,AverageS));
		flowError.add(absolute_d(observationList,simulationList,AverageO,AverageS));
		flowError.add(Watterson_M(observationList,simulationList,AverageO,AverageS));
		flowError.add(universal_R(observationList,simulationList,AverageO,AverageS));
		outputFlowError(flowError);
		
		for(Integer i = 1;i <= NUMBER_OF_ROUTE;i++){
			for(Integer j = 0;j < NUMBER_OF_NAIVEAGENT;j++){
				if(j % GPS_SEPARATE != 0)continue;
				observationList.clear();
				simulationList.clear();
				fileO = "observationAgent_R"+i+"_"+j+".csv";
				fileS = "simulationAgent_R"+i+"_"+j+".csv";
				readCSV(Obspos+fileO,observationList);
				readCSV(Anapos+fileS,simulationList);
				maxDistanceControl(observationList,TRAVEL_DISTANCE[i]);
				maxDistanceControl(simulationList,TRAVEL_DISTANCE[i]);
				sizeControl(observationList,simulationList,TRAVEL_DISTANCE[i]);
				AverageO = Average(observationList);
				AverageS = Average(simulationList);
				if(GPS_SEPARATE == 1) {
					size1 = size1 + countAgentPositionError(observationList,simulationList,AverageO,AverageS,j,1,agentPositionError1);
				}else if(GPS_SEPARATE == 10) {
					size10 = size10 + countAgentPositionError(observationList,simulationList,AverageO,AverageS,j,10,agentPositionError10);
				}
			}
		}
		
		if(GPS_SEPARATE == 1) {
			outputAgentPositionError(1,size1*1.0,agentPositionError1);
			outputAllError(size1*1.0,agentPositionError1,flowError);
		}else if(GPS_SEPARATE == 10) {
			outputAgentPositionError(10,size10*1.0,agentPositionError10);
			outputAllError(size10*1.0,agentPositionError10,flowError);			
		}
	}
	public static void outputFlowError(ArrayList<Double> flowError){
		try{
			String fileF = "flowError.csv";
			FileWriter fw = new FileWriter(Anapos + fileF, false);
			PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
			pw.println("MAE,RMSE,MSE,squared_NSE,absolute_NSE,squared_R,absolute_R,squared_d,absolute_d,Watterson_M,universal_R,");
			for(Integer i = 0;i < 11;i++){
				pw.print(flowError.get(i)+",");
			}
			pw.close();
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Error : getErrorData");
		}
	}
	public static void outputAgentPositionError(Integer index,Double size,ArrayList<Double>agentPositionError){
		try{
			String fileF = index+"minAgentPositionError.csv";
			FileWriter fw = new FileWriter(Anapos + fileF, false);
			PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
			pw.println("MAE,RMSE,MSE,squared_NSE,absolute_NSE,squared_R,absolute_R,squared_d,absolute_d,Watterson_M,universal_R,");
			for(Integer i = 0;i < 11;i++){
				pw.print((agentPositionError.get(i) / size)+",");
			}
			pw.close();
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Error : getErrorData");
		}
	}
	public static void outputAllError(Double size,ArrayList<Double>agentPositionError,ArrayList<Double> flowError){
		try{
			String fileF = "Fitness.csv";
			FileWriter fw = new FileWriter(Anapos + fileF, false);
			PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
			pw.println("AllError,FlowError,PosError,StartFlowError,FollowFlowError,IdleFlowError,BusyFlowError");
			double allerror = ((flowError.get(1) * GPS_SEPARATE)+(agentPositionError.get(1) / size))/2;
			double flowerror = flowError.get(1) * GPS_SEPARATE;
			double poserror = (agentPositionError.get(1) / size);
			pw.print(allerror+","+flowerror+","+poserror+","+getStartFlowError(1)+","+getStartFlowError(2)+","+getStartFlowError(3)+","+getStartFlowError(4));
			pw.close();
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Error : getAllData");
		}
	}
	public static Double getStartFlowError(Integer T){
		try {
			ArrayList<Double> Type = new ArrayList<Double>();
			ArrayList<Double> Type2 = new ArrayList<Double>();
			
			String file = "simulationStartFlow.csv";
			FileReader fr = new FileReader(Anapos+file);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			while ((line = br.readLine()) != null) {
				String subString[] = line.split(",");
				Type.add(Double.parseDouble(subString[subString.length - T]));
			}
			br.close();
			
			file = "observationStartFlow.csv";
			fr = new FileReader(Obspos+file);
			br = new BufferedReader(fr);
			line = br.readLine();
			while ((line = br.readLine()) != null) {
				String subString[] = line.split(",");
				Type2.add(Double.parseDouble(subString[subString.length - T]));
			}
			br.close();
			
			Double RMSEs = 0.0;
			Integer length = Math.max(Type.size(),Type2.size());
			for(Integer i = 0;i < length;i++){
				Double T1 = 0.0;
				Double T2 = 0.0;
				if(Type.size() - i > 0){
					T1 = Type.get(i);
				}else{
					T1 = 0.0;
				}
				if(Type2.size() - i > 0){
					T2 = Type2.get(i);
				}else{
					T2 = 0.0;
				}
				RMSEs = RMSEs + ((T1 - T2)*(T1 - T2));
			}
			RMSEs = Math.sqrt(RMSEs/length);
			return RMSEs;
		} catch (IOException e) {
		}
		return 100000.0;
	}
	public static Integer countAgentPositionError(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS,Integer index,Integer cycle,ArrayList<Double>agentPositionError){
		if(index % cycle == 0){
			agentPositionError.set(0,agentPositionError.get(0) + MAE(observationList,simulationList,AverageO,AverageS));
			agentPositionError.set(1,agentPositionError.get(1) + RMSE(observationList,simulationList,AverageO,AverageS));
			agentPositionError.set(2,agentPositionError.get(2) + MSE(observationList,simulationList,AverageO,AverageS));
			agentPositionError.set(3,agentPositionError.get(3) + squared_NSE(observationList,simulationList,AverageO,AverageS));
			agentPositionError.set(4,agentPositionError.get(4) + absolute_NSE(observationList,simulationList,AverageO,AverageS));
			agentPositionError.set(5,agentPositionError.get(5) + squared_R(observationList,simulationList,AverageO,AverageS));
			agentPositionError.set(6,agentPositionError.get(6) + absolute_R(observationList,simulationList,AverageO,AverageS));
			agentPositionError.set(7,agentPositionError.get(7) + squared_d(observationList,simulationList,AverageO,AverageS));
			agentPositionError.set(8,agentPositionError.get(8) + absolute_d(observationList,simulationList,AverageO,AverageS));
			agentPositionError.set(9,agentPositionError.get(9) + Watterson_M(observationList,simulationList,AverageO,AverageS));
			agentPositionError.set(10,agentPositionError.get(10) + universal_R(observationList,simulationList,AverageO,AverageS));
			return 1;
		}
		return 0;
	}
	public static void maxDistanceControl(ArrayList<Double> agentList,Double maxDistance){
		Integer length = agentList.size();
		Double control = maxDistance / agentList.get(length - 1);
		for(Integer i = 0;i < length;i++){
			agentList.set(i, agentList.get(i)*control);
		}
	}
	public static void sizeControl(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double addNumber){
		Integer length = Math.abs(simulationList.size() - observationList.size());
		if(observationList.size() < simulationList.size()){
			for(Integer i = 0;i < length;i++){
				observationList.add(addNumber);
			}
		}else{
			for(Integer i = 0;i < length;i++){
				simulationList.add(addNumber);
			}
		}
	}
	public static Double Average(ArrayList<Double> flowList){
		Double result = 0.0;
		for(Integer i = 0;i < flowList.size();i++){
			result = result + flowList.get(i);
		}
		result = result / flowList.size();
		return result;
	}
	public static void readCSV(String filepath,ArrayList<Double> agentList){
		try {
			FileReader fr = new FileReader(filepath);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			while ((line = br.readLine()) != null) {
				String agent[] = line.split(",");
				agentList.add(Double.parseDouble(agent[agent.length - 1]));
			}
			br.close();
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Error : readFlowCSV");
		}
	}

	public static Double absoluteSum(ArrayList<Double> observationList,ArrayList<Double> simulationList){
		Double result = 0.0;
		Integer length = observationList.size();
		for(Integer i = 0;i < length;i++){
			for(Integer j = 0;j < length;j++){
				result = result + Math.abs(simulationList.get(j) - observationList.get(i));
			}
		}
		return result;
	}
	
	public static Double C(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		Integer length = observationList.size();
		for(Integer i = 0;i < length;i++){
			result = result + ((observationList.get(i) - AverageO)*(simulationList.get(i) - AverageS));
		}
		return result;
	}
	
	public static Double OS_AD(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		Integer length = observationList.size();
		for(Integer i = 0;i < length;i++){
			result = result + Math.abs(observationList.get(i) - AverageO) + Math.abs(simulationList.get(i) - AverageO);
		}
		return result;
	}
	public static Double OS_SD(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		Integer length = observationList.size();
		for(Integer i = 0;i < length;i++){
			result = result + (Math.abs(observationList.get(i) - AverageO) + Math.abs(simulationList.get(i) - AverageO))*(Math.abs(observationList.get(i) - AverageO) + Math.abs(simulationList.get(i) - AverageO));
		}
		return result;
	}
	public static Double AD(ArrayList<Double> agentList,Double agentAverage){
		Double result = 0.0;
		Integer length = agentList.size();
		for(Integer i = 0;i < length;i++){
			result = result + Math.abs(agentList.get(i) - agentAverage);
		}
		return result;
	}
	public static Double SD(ArrayList<Double> agentList,Double agentAverage){
		Double result = 0.0;
		Integer length = agentList.size();
		for(Integer i = 0;i < length;i++){
			result = result + ((agentList.get(i) - agentAverage)*(agentList.get(i) - agentAverage));
		}
		return result;
	}
	
	public static Double AE(ArrayList<Double> observationList,ArrayList<Double> simulationList){
		Double result = 0.0;
		Integer length = observationList.size();
		for(Integer i = 0;i < length;i++){
			result = result + Math.abs(observationList.get(i) - simulationList.get(i));
		}
		return result;
	}
	public static Double SE(ArrayList<Double> observationList,ArrayList<Double> simulationList){
		Double result = 0.0;
		Integer length = observationList.size();
		for(Integer i = 0;i < length;i++){
			result = result + ((observationList.get(i) - simulationList.get(i)) * (observationList.get(i) - simulationList.get(i)));
		}
		return result;
	}
	
	public static Double MAE(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		Integer length = observationList.size();
		result = AE(observationList,simulationList) / length;
		return result;
	}
	public static Double RMSE(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		Integer length = observationList.size();
		result = Math.sqrt(SE(observationList,simulationList) / length);
		return result;
	}
	public static Double MSE(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		Integer length = observationList.size();
		result = SE(observationList,simulationList) / length;
		return result;
	}
	public static Double squared_NSE(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		result = 1 - (SE(observationList,simulationList) / SD(observationList,AverageO));
		return result;
	}
	public static Double absolute_NSE(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		result = 1 - (AE(observationList,simulationList) / AD(observationList,AverageO));
		return result;
	}
	public static Double squared_R(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		result = C(observationList,simulationList,AverageO,AverageS) / (Math.sqrt(SD(observationList,AverageO)) * Math.sqrt(SD(simulationList,AverageS)));
		result = result * result;
		return result;
	}
	public static Double absolute_R(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		result = C(observationList,simulationList,AverageO,AverageS) / (Math.sqrt(SD(observationList,AverageO)) * Math.sqrt(SD(simulationList,AverageS)));
		result = (result + 1.0) / 2.0;
		return result;
	}
	public static Double squared_d(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		result = 1 - (SE(observationList,simulationList) / OS_SD(observationList,simulationList,AverageO,AverageS));
		return result;
	}
	public static Double absolute_d(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		result = 1 - (AE(observationList,simulationList) / OS_AD(observationList,simulationList,AverageO,AverageS));
		return result;
	}
	public static Double Watterson_M(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		Double length = observationList.size() * 1.0;
		result = (2.0/Math.PI)*Math.asin(1 - (MSE(observationList,simulationList,AverageO,AverageS)/((SD(observationList,AverageO)/length) +( SD(simulationList,AverageS)/length) + (Math.abs(AverageO - AverageS)*Math.abs(AverageO - AverageS)))));
		return result;
	}
	public static Double universal_R(ArrayList<Double> observationList,ArrayList<Double> simulationList,Double AverageO,Double AverageS){
		Double result = 0.0;
		Double length = observationList.size() * 1.0;
		result = 1 - (MAE(observationList,simulationList,AverageO,AverageS) / (Math.pow(length, -2.0)*absoluteSum(observationList,simulationList)));
		return result;
	}
	
	public static Double Normalization(Double x){
		Double f = Math.exp(-1.0 * x * x / (SIGMA * SIGMA * SIGMA)) / Math.sqrt(SIGMA * SIGMA * SIGMA * Math.PI);
		return f;
	}
	public static void createAgentCSV(HashMap<String,Double> agentPosition,HashMap<String,ArrayList<TravelAgent>> agentPositionData,String csvFile){
		for(Integer i = 1;i <= NUMBER_OF_ROUTE;i++){
			for(Integer j = 0;j < NUMBER_OF_NAIVEAGENT;j++){
				if(j % GPS_SEPARATE != 0)continue;
				String indexa = "R"+i+"_"+j;
				agentPosition.put(indexa,0.0);
				agentPositionData.put(indexa,new ArrayList<TravelAgent>());
				String file = Anapos+csvFile+"Agent_"+indexa+".csv";
				try {
					FileWriter fw = new FileWriter(file, false);
					PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
					pw.println("time1sec,countTime,totalDistance");
					pw.close();
				}catch (IOException e) {
					System.out.println(e);
					System.out.println("Error : createAgentCSV "+indexa+".csv");
				}
			}
		}
	}
	public static void outputAgentPosition(HashMap<String,ArrayList<TravelAgent>> agentPositionData,String csvFile){
		for(Integer i = 1;i <= NUMBER_OF_ROUTE;i++){
			for(Integer j = 0;j < NUMBER_OF_NAIVEAGENT;j++){
				if(j % GPS_SEPARATE != 0)continue;
				String indexa = "R"+i+"_"+j;
				String file = Anapos+csvFile+"Agent_"+indexa+".csv";
				try {
					FileWriter fw = new FileWriter(file, true);
					PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
					Integer size = agentPositionData.get(indexa).size();
					for(int k = 0;k < size;k++){
						TravelAgent TA = agentPositionData.get(indexa).remove(0);
						pw.println(TA.time1sec+","+TA.countTime+","+TA.totalDistance);
					}
					pw.close();
				}catch (IOException e) {
					System.out.println(e);
					System.out.println("Error : outputAgentPosition "+indexa+".csv");
				}
			}
		}
	}

	public static Integer nextTime(Integer currentTime,Integer cycleTime){
		currentTime = currentTime + cycleTime;
		if(currentTime % 100 == 60)currentTime = currentTime + 100 - 60;
		if(currentTime % 10000 == 6000)currentTime = currentTime + 10000 - 6000;
		return currentTime;
	}
	public static Double dicimalCurrentTime(Integer currentTime){
		Integer h = currentTime/10000;
		Integer m = (currentTime / 100) % 100;
		Integer s = currentTime % 100;
		return h*100 + m*60*100.0/3600 + ((s % 100) * 100.0)/3600;
	}
	static class TravelAgent{
		Integer time1sec;
		Integer countTime;
		Double totalDistance;
		public TravelAgent(Integer time1sec,Integer countTime,Double totalDistance){
			this.time1sec = time1sec;
			this.countTime = countTime;
			this.totalDistance = totalDistance;
		}
	}
}
