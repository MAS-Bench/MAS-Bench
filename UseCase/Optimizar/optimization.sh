#!/bin/sh
JAVAC="javac"
JAVA="java"
PYTHON="python"

HOMEPATH=${1}
OPTPATH=${2}
BENCHPATH=${3}
RESULTPATH=${4}
CWPATH=${5}
BENCH=${6}
MAPSIZE=${7}
TYPE=${8}
STARTCYCLE=${9}
ENDCYCLE=${10}
POPSIZE=${11}
LOOPNUMBER=${12}
PARALLEL=${13}

NAIVE_AGENT=0
RATIONAL_AGENT=0
RUBY_AGENT=1
AGENT_SIZE=4500
GPS_SIZE=10

${JAVAC} $BENCHPATH/OriginGenerator/CreateGenerationFile.java
${JAVAC} $BENCHPATH/OriginGenerator/CreatePropertiesFile.java
${JAVAC} $BENCHPATH/PerformanceEvaluator/DataAssimilation.java

#0. function
if [ $FUNCTIONNUMBER -eq 1 ]; then
	NAIVE_AGENT=0
        RATIONAL_AGENT=0
        RUBY_AGENT=1 
elif [ $FUNCTIONNUMBER -eq 2 ]; then
	NAIVE_AGENT=1
	RATIONAL_AGENT=0
	RUBY_AGENT=1	
elif [ $FUNCTIONNUMBER -eq 3 ]; then
	NAIVE_AGENT=1
        RATIONAL_AGENT=1
        RUBY_AGENT=1
elif [ $FUNCTIONNUMBER -eq 4 ]; then
	NAIVE_AGENT=2
        RATIONAL_AGENT=2
        RUBY_AGENT=2
fi

if [ ${MAPSIZE} = "Small" ]; then
	AGENT_SIZE=4500
	GPS_SIZE=10
elif [ ${MAPSIZE} = "Large" ]; then
        AGENT_SIZE=45000
        GPS_SIZE=1
fi


SAVEPATH="$RESULTPATH/$BENCH/${TYPE}_${ENDCYCLE}_${POPSIZE}_${LOOPNUMBER}"
mkdir "$RESULTPATH/$BENCH"
mkdir "$RESULTPATH/$BENCH/${TYPE}_${ENDCYCLE}_${POPSIZE}_${LOOPNUMBER}"

for i in `seq $STARTCYCLE $(($ENDCYCLE -1))`; do
	#1. make directory
	cd $HOMEPATH
	echo "make directory"
	mkdir "$SAVEPATH/$i"
	mkdir "$SAVEPATH/$i/input_parameter"
	mkdir "$SAVEPATH/$i/output_error"
	mkdir "$SAVEPATH/$i/input_parameter/current_parameter"

	if [ $i -gt 0 ]; then
		echo "$SAVEPATH/$(($i -1))/input_parameter/current_parameter"
		cp -rf "$SAVEPATH/$(($i -1))/input_parameter/current_parameter" "$SAVEPATH/$i/input_parameter/previous_parameter"
		cp "$SAVEPATH/$(($i -1))/output_error/FitnessList.csv" "$SAVEPATH/$i/input_parameter/previous_parameter/FitnessList.csv"
	fi

	for j in `seq 0 $(($POPSIZE -1))`; do
		mkdir $SAVEPATH"/$i/$j"
		mkdir $SAVEPATH"/$i/$j/property"
        	mkdir $SAVEPATH"/$i/$j/analyze"
        	mkdir $SAVEPATH"/$i/$j/log"
	done

	set -e
	#2. optimizar
	echo "optimizar"
	echo $PYTHON ${OPTPATH}"/"${TYPE}.py $i $(($i +1)) $POPSIZE $NAIVE_AGENT $RATIONAL_AGENT $RUBY_AGENT "$SAVEPATH/$i/input_parameter/"
	if [ ${TYPE} = "TPE" ]; then
		cd "${OPTPATH}/tpe"
		if [ $i = 0 ]; then
			rm -r history
		fi
		bash mainLoop.sh $POPSIZE $NAIVE_AGENT $RATIONAL_AGENT $RUBY_AGENT "${OPTPATH}/tpe" "$SAVEPATH/$i/input_parameter/"
	else
		$PYTHON ${OPTPATH}"/"${TYPE}.py $i $(($i +1)) $POPSIZE $NAIVE_AGENT $RATIONAL_AGENT $RUBY_AGENT "$SAVEPATH/$i/input_parameter/"
	fi

	#3. origin generator
	echo "origin generator"
	cd $BENCHPATH/OriginGenerator
	seq 0 $(($POPSIZE -1)) | xargs -n 1 -P $PARALLEL -I {} ${JAVA} CreateGenerationFile "Simulation" $SAVEPATH"/$i/input_parameter/population_"{}".csv" $SAVEPATH"/$i/"{}"/property/gen.json" $SAVEPATH"/$i/"{}"/analyze/simulationStartFlow.csv" $NAIVE_AGENT $RATIONAL_AGENT $RUBY_AGENT $AGENT_SIZE $GPS_SIZE
	seq 0 $(($POPSIZE -1)) | xargs -n 1 -P $PARALLEL -I {} ${JAVA} CreatePropertiesFile $SAVEPATH"/$i/"{}"/property/prop.json" {} $BENCH "../../../../../../MAS-Bench/CrowdSimulator/" $MAPSIZE
	
	#3. crowd simulator
	echo "crowd simulator"
	cd $BENCHPATH/CrowdSimulator
	seq 0 $(($POPSIZE -1))| xargs -n 1 -P $PARALLEL -I {} bash $CWPATH "$SAVEPATH/$i/"{}"/property/prop.json" -c -lError 2>&1 | grep -v "WAR" | grep -v "ITK" 
	
	#4. performance evaluator
	echo "performance evaluator"
	cd $BENCHPATH/PerformanceEvaluator
	seq 0 $(($POPSIZE -1)) | xargs -n 1 -P $PARALLEL -I {} ${JAVA} DataAssimilation "Simulation" $SAVEPATH"/$i/"{}"/log/" $SAVEPATH"/$i/"{}"/analyze/" $BENCHPATH"/Dataset/$BENCH/" $GPS_SIZE
	
	#5. save result data
	cd "$SAVEPATH/$i"
	echo "AllError,FlowError,PosError,StartFlowError,FollowFlowError,IdleFlowError,BusyFlowError" > "output_error/"FitnessList.csv
	for j in `seq 0 $(($POPSIZE -1))`; do
		cat "$j/analyze/Fitness.csv" | tail -1 >> "output_error/"FitnessList.csv
		echo "" >> "output_error/"FitnessList.csv
		tar -zcf $j.tar.gz $j
	done
	if [ ${TYPE} = "TPE" ]; then
		cat output_error/FitnessList.csv | tail -${POPSIZE} | cut -d "," -f 1 > "${OPTPATH}/tpe/Fitness.csv"
	fi
	if [ $i -eq 0 ]; then
		echo "cycle,test,AllError,FlowError,PosError,StartFlowError,FollowFlowError,IdleFlowError,BusyFlowError" > "../"FitnessAll.csv
	fi
	for j in `seq 2 $(($POPSIZE +1))`; do
		echo -n "$i,$(($j -2))," >> "../"FitnessAll.csv
		cat "output_error/FitnessList.csv" | head -$j | tail -1 >> "../"FitnessAll.csv
	done
	seq 0 $(($POPSIZE -1))| xargs -n 1 -P $PARALLEL -I {} rm -r {}
done
