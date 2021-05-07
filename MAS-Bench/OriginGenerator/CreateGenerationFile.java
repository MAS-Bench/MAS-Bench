
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Scanner;

public class CreateGenerationFile {
	public static void main(String[] args) {
		switch (args[0]){
		case "Observation":
			new Original().main(args);
			break;
		case "Simulation":
			new Simulation().main(args);
			break;
		}
	}
}

class Original extends Simulation {
	public void setCsv(Integer dimension,Integer naiveK, Integer rationalK, Integer rubyK){
		try{
			Double mu[] = new Double[dimension];
			Double sigma[] = new Double[dimension];
			Double pi[] = new Double[dimension];			
			pi[dimension - 1] = -1.0;
			while(true) {
				Double allPi = 0.0;
				Double sumDist = 0.0;
				for(int i = 0;i < dimension;i++){
					sigma[i] = Math.random()*(MAX_SIGMA - MIN_SIGMA) + MIN_SIGMA;
					mu[i] = Math.random()*(MAX_MU - MIN_MU) + MIN_MU;
					pi[i] = Math.random()*(MAX_PI - MIN_PI) + MIN_PI;
					if(i != dimension - 1)allPi += pi[i];
				}
				pi[dimension - 1] = MAX_PI - allPi;
				if(pi[dimension - 1] < MIN_PI)continue;
				for(int t = 0;t < TIME_SEPARATE;t++) {
					sumDist = Math.max(sumDist,NormDist(sigma,mu,pi,dimension,t*1.0));	
				}
				if(sumDist <= MAX_ONE_SEPARATE_PEOPLE_RATE)break;
			}
			
			FileWriter fw = new FileWriter(inputCsvFilePath);
			PrintWriter pw = new PrintWriter(fw);
			
			for(Integer k = 0;k < naiveK;k++)pw.print("sigma_naive"+k+",mu_naive"+k+",pi_naive"+k+",");
			for(Integer k = 0;k < rationalK;k++)pw.print("sigma_rational"+k+",mu_rational"+k+",pi_rational"+k+",");
			for(Integer k = 0;k < rubyK;k++)pw.print("sigma_ruby"+k+",mu_ruby"+k+",pi_ruby"+k+",");
			pw.println();
		
			for(Integer k = 0;k < naiveK;k++)pw.print(sigma[k]+","+mu[k]+","+pi[k]+",");
			for(Integer k = naiveK;k < naiveK + rationalK;k++)pw.print(sigma[k]+","+mu[k]+","+pi[k]+",");
			for(Integer k = naiveK + rationalK;k < naiveK + rationalK + rubyK;k++)pw.print(sigma[k]+","+mu[k]+","+pi[k]+",");
			pw.close();
			
		}catch (IOException e) {
			System.out.println(e);
		}
	}
	
	@Override
	public void main(String[] args) {
		inputCsvFilePath = args[1];
		outputJsonFilePath = args[2];
		outputCsvFilePath = args[3];
		setCsv(AgentSize,naiveAgentSize,rationalAgentSize,rubyAgentSize);
		readCsv();
		Double alpha = correctionValue(MAX_PEOPLE);
		writeJson(alpha);
		writeCsv(alpha);
	}
}

class Simulation {
	final Integer START_HOUR = 19;
	final Integer START_MINUTES = 0;
	final Integer START_SECOND = 0;
	final Integer END_HOUR = 24;
	final Integer END_MINUTES = 0;
	final Integer END_SECOND = 0;
	
	final Integer TIME_SEPARATE = 300;
	final Double MAX_ONE_SEPARATE_PEOPLE_RATE = 0.01;
	
	final Double MAX_SIGMA = 100.0;
	final Double MAX_MU = 300.0;
	final Double MAX_PI = 1.0;
	final Double MIN_SIGMA = 3.0;
	final Double MIN_MU = 0.0;
	final Double MIN_PI = 0.0;
	
	String inputCsvFilePath = "input.csv";
	String outputJsonFilePath = "output.json";
	String outputCsvFilePath = "simulationStartFlow.csv";
	
	ArrayList<NormVar> naiveVarList = new ArrayList<>();
	ArrayList<NormVar> rationalVarList = new ArrayList<>();
	ArrayList<NormVar> rubyVarList = new ArrayList<>();
	
	Integer naiveAgentSize = 1;
	Integer rationalAgentSize = 1;
	Integer rubyAgentSize = 1;
	Integer AgentSize = 3;
        Double MAX_PEOPLE  = 45000.0;
        Integer GPS_SEPARATE = 1;
	
	public void main(String[] args) {
		inputCsvFilePath = args[1];
		outputJsonFilePath = args[2];
                outputCsvFilePath = args[3];
		naiveAgentSize  = Integer.parseInt(args[4]);
		rationalAgentSize  = Integer.parseInt(args[5]);
		rubyAgentSize  = Integer.parseInt(args[6]);
                MAX_PEOPLE  = Double.parseDouble(args[7]);
                GPS_SEPARATE  = Integer.parseInt(args[8]);
		AgentSize = naiveAgentSize + rationalAgentSize + rubyAgentSize;
		readCsv();
		Double alpha = correctionValue(MAX_PEOPLE);
		writeJson(alpha);
		writeCsv(alpha);
	}

	class NormVar {
		Double sigma;
		Double mu;
		Double pi;

		public NormVar(Double sigma,Double mu,Double pi) {
			this.sigma = sigma;
			this.mu = mu;
			this.pi = pi;
		}

		public NormVar() {
		}

		public String toString() {
			return "s:" + sigma +", m:" + mu + ", pi:" + pi;
		}
	}
	
	class Time {
		Integer H;
		Integer M;
		Integer S;
		Integer C;

		public Time(Integer H,Integer M,Integer S,Integer C) {
			this.H = H;
			this.M = M;
			this.S = S;
			this.C = C;
		}
	}
	
	public Double correctionValue(Double maxPeople){
		Double alpha = maxPeople;
		int cnt = 0;
		while(true) {
			cnt++;
			Time t = new Time(START_HOUR,START_MINUTES,START_SECOND,0);
			Integer sumDist = 0;
			while(t.H < END_HOUR){
				Integer normDistNaive = sumNormDist(naiveVarList,naiveVarList.size(),t.C*1.0,alpha);
				Integer normDistRational = sumNormDist(rationalVarList,rationalVarList.size(),t.C*1.0,alpha);
				Integer normDistRuby = sumNormDist(rubyVarList,rubyVarList.size(),t.C*1.0,alpha);
				
				sumDist += normDistNaive + normDistRational + normDistRuby;
				
				t = nextTime(t);		
			}
//			System.out.println(sumDist);
			if(maxPeople - sumDist == 0 || cnt > 100000)return alpha;
			else alpha = alpha + (maxPeople - sumDist)*1.0/cnt;
		}
	}
	
	public void readCsv() {
		try{
			FileReader fr = new FileReader(inputCsvFilePath);
			BufferedReader br = new BufferedReader(fr);
			String variablesName[] = br.readLine().split(",");
			String variables[] = br.readLine().split(",");

			br.close();

			ArrayList<String> readColumnList = new ArrayList<>();

			for (Integer i = 0; i < variablesName.length; i++) {
				String sampledHeader = variablesName[i].trim();
				if (! readColumnList.contains(sampledHeader)) {
					NormVar normVar = new NormVar();
					String regex = sampledHeader.replaceAll(
							"^(sigma|mu|pi)_", "^(sigma|mu|pi)_");

					for (Integer i2 = 0; i2 < variablesName.length; i2++) {
						String variableName = variablesName[i2].trim();
						String variable = variables[i2].trim();
						if (! readColumnList.contains(variableName)
							&& variableName.matches(regex)) {

							readColumnList.add(variableName);

							if(variableName.indexOf("sigma_") == 0){
								normVar.sigma = Double.parseDouble(variable);
							}else if(variableName.indexOf("mu_") == 0){
								normVar.mu = Double.parseDouble(variable);
							}else if(variableName.indexOf("pi_") == 0){
								normVar.pi = Double.parseDouble(variable);
							}
						}
					}

					if(sampledHeader.indexOf("_naive") > 0){
						naiveVarList.add(normVar);
					}else if(sampledHeader.indexOf("_rational") > 0){
						rationalVarList.add(normVar);
					}else if(sampledHeader.indexOf("_ruby") > 0){
						rubyVarList.add(normVar);
					}
				}
			}

		}catch (IOException e) {
			System.out.println(e);
		}
	}

	public void writeJson(Double alpha) {
		Time t = new Time(19,0,0,0);
	
		try{
			FileWriter fw = new FileWriter(outputJsonFilePath, false);
			PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

			pw.println("#{ \"version\" : 2}");
			pw.println("[");
			
			while(t.H < 24){
				String startTime = t.H+":"+inFrontZero(t.M)+":"+inFrontZero(t.S);
				
				Integer normDistNaive = sumNormDist(naiveVarList,naiveVarList.size(),t.C*1.0,alpha);
				Integer normDistRational = sumNormDist(rationalVarList,rationalVarList.size(),t.C*1.0,alpha);
				Integer normDistRuby = sumNormDist(rubyVarList,rubyVarList.size(),t.C*1.0,alpha);
				
				if(normDistNaive > 0)pw.println(naiveAgent(startTime,normDistNaive));
				if(normDistRational > 0)pw.println(rationalAgent(startTime,normDistRational));
				if(normDistRuby > 0)pw.println(rubyAgent(startTime,normDistRuby));
					
				for(int i = 1;i <= 3;i++){
					if(t.C % GPS_SEPARATE == 0) {
						pw.println(naiveAgentOneMinutes(startTime,i,t.C));
					}
				}
				
				t = nextTime(t);		
			}
			pw.println(finishAgent());
			pw.println("]");
			pw.close();
		}catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public void writeCsv(Double alpha) {
		Time t = new Time(19,0,0,0);
	
		try{
			FileWriter fw = new FileWriter(outputCsvFilePath, false);
			PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
			
			pw.println("currentTime,naiveAgent,rationalAgent,rubyAgent,allAgent");
			
			while(t.H < 24){
				String startTime = t.H+":"+inFrontZero(t.M)+":"+inFrontZero(t.S);
				
				Integer normDistNaive = sumNormDist(naiveVarList,naiveVarList.size(),t.C*1.0,alpha);
				Integer normDistRational = sumNormDist(rationalVarList,rationalVarList.size(),t.C*1.0,alpha);
				Integer normDistRuby = sumNormDist(rubyVarList,rubyVarList.size(),t.C*1.0,alpha);
				
				Integer sumDist = normDistNaive + normDistRational + normDistRuby;
				pw.println(startTime+","+normDistNaive+","+normDistRational+","+normDistRuby+","+sumDist);
				
				t = nextTime(t);		
			}
			pw.close();
		}catch (IOException e) {
			System.out.println(e);
		}
	}

	
	public Time nextTime(Time t) {
		t.C++;
		t.H = 19 + (t.C / 60);
		t.M = t.C % 60;
		return t;
	}

	public String inFrontZero(Integer num){
		if(num < 10)return "0"+num;
		return ""+num;
	}

	public Integer sumNormDist(ArrayList<NormVar> varSet,Integer Size,Double x,Double alpha){
		Integer result = 0;
		Double normDist = 0.0;
		
		for(Integer i = 0;i < Size;i++){
			NormVar Var = varSet.get(i);
			normDist += Math.floor(Math.exp(-(x-Var.mu)*(x-Var.mu)/(2.0*Var.sigma*Var.sigma)) / Math.sqrt(2.0*Math.PI*Var.sigma*Var.sigma) * Var.pi * alpha);
		}
		result = normDist.intValue();
		return result;
	}
	
	public Double NormDist(Double sigma[],Double mu[],Double pi[],Integer Size,Double x){
		Double normDist = 0.0;
		
		for(Integer i = 0;i < Size;i++){
			normDist += Math.exp(-(x-mu[i])*(x-mu[i])/(2.0*sigma[i]*sigma[i])) / Math.sqrt(2.0*Math.PI*sigma[i]*sigma[i]) * pi[i];
		}
		return normDist;
	}
	
	public String naiveAgentOneMinutes(String startTime,Integer indexa,Integer cnt){
		String result = "";
		if(indexa == 1){
			result = 
					"  {\"rule\":\"EACH\""+
					",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_Route1\"}"+
					",\"startTime\":\""+startTime+"\""+
					",\"total\":1,"+
					"\"duration\":1,"+
					"\"startPlace\":\"SL_GPS\""+
					",\"goal\":\"EXIT_GPS\""+
					",\"conditions\":[\"INDEXA_R"+indexa+"_"+cnt+"\"]},";
		}
		else if(indexa == 2){
			result = 
					"  {\"rule\":\"EACH\""+
					",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_Route2\"}"+
					",\"startTime\":\""+startTime+"\""+
					",\"total\":1,"+
					"\"duration\":1,"+
					"\"startPlace\":\"SL_GPS\""+
					",\"goal\":\"EXIT_GPS\""+
					",\"conditions\":[\"INDEXA_R"+indexa+"_"+cnt+"\"]},";
		}
		else if(indexa == 3){
			result = 
					"  {\"rule\":\"EACH\""+
					",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_Route3\"}"+
					",\"startTime\":\""+startTime+"\""+
					",\"total\":1,"+
					"\"duration\":1,"+
					"\"startPlace\":\"SL_GPS\""+
					",\"goal\":\"EXIT_GPS\""+
					",\"conditions\":[\"INDEXA_R"+indexa+"_"+cnt+"\"]},";
		}
		return result;
	}

	public String naiveAgent(String startTime,Integer total){
		String result = 
			"  {\"rule\":\"EACH\""+
			",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_Busy\"}"+
			",\"startTime\":\""+startTime+"\""+
			",\"total\":"+total+","+
			"\"duration\":60,"+
			"\"startPlace\":\"SL_SWM\""+
			",\"goal\":\"EXIT_STATION\"},";
		return result;
	}
	public String rubyAgent(String startTime,Integer total){
		String result = 
			"  {\"rule\":\"EACH\""+
			",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_SignalFollow\"}"+
			",\"startTime\":\""+startTime+"\""+
			",\"total\":"+total+","+
			"\"duration\":60,"+
			"\"startPlace\":\"SL_SWM\""+
			",\"goal\":\"EXIT_STATION\"},";
		return result;
	}
	public String rationalAgent(String startTime,Integer total){
		String result = 
			"  {\"rule\":\"EACH\""+
			",\"agentType\":{\"className\":\"RubyAgent\",\"rubyAgentClass\":\"SampleAgent_StallIdle\"}"+
			",\"startTime\":\""+startTime+"\""+
			",\"total\":"+total+","+
				"\"duration\":60,"+
			"\"startPlace\":\"SL_SWM\""+
			",\"goal\":\"EXIT_STATION\"},";
		return result;
	}
	public String finishAgent() {
		String result = 
				"  {}";
		return result;
	}
}
