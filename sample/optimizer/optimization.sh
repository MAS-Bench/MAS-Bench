#!/bin/sh
JAVA="java"
PYTHON="python"

ENTRY_DIR=$(pwd)
OPT_DIR=$(dirname $0)
MASBENCH_JAR=$1
BASE_DIR=$2
MODEL=$3
TYPE=$4
STARTCYCLE=$5
ENDCYCLE=$6
POPSIZE=$7
SUFFIX=$8
PARALLEL=$9

OPTIMIZER="${OPT_DIR}/${TYPE}.py"

$JAVA -jar $MASBENCH_JAR init
source $(dirname $MASBENCH_JAR)/masbench-resources/Dataset/$MODEL/agent_size.sh
WORK_DIR="$ENTRY_DIR/$BASE_DIR/$MODEL/${TYPE}_${ENDCYCLE}_${POPSIZE}_${SUFFIX}"

for i in `seq $STARTCYCLE $(($ENDCYCLE -1))`; do
  cd $ENTRY_DIR

  #1. make directory
  mkdir -p "$WORK_DIR/$i/output_error"
  mkdir -p "$WORK_DIR/$i/input_parameter/current_parameter"

  if [ $i -gt 0 ]; then
    cp -rf "$WORK_DIR/$(($i -1))/input_parameter/current_parameter" "$WORK_DIR/$i/input_parameter/previous_parameter"
    cp "$WORK_DIR/$(($i -1))/output_error/FitnessList.csv" "$WORK_DIR/$i/input_parameter/previous_parameter/FitnessList.csv"
  fi

  #2. optimizar
  $PYTHON $OPTIMIZER $i $(($i +1)) $POPSIZE $NAIVE_AGENT $RATIONAL_AGENT $RUBY_AGENT "$WORK_DIR/$i/input_parameter/"

  #3. origin generator
  #3. crowd simulator
  #4. performance evaluator
  seq 0 $(($POPSIZE -1)) | xargs -n 1 -P $PARALLEL -I {} ${JAVA} -jar $MASBENCH_JAR $MODEL "$WORK_DIR/$i/{}" "$WORK_DIR/$i/input_parameter/population_{}.csv"

  #5. save result data
  cd "$WORK_DIR/$i"
  echo "AllError,FlowError,PosError,StartFlowError,FollowFlowError,IdleFlowError,BusyFlowError" > output_error/FitnessList.csv
  for j in `seq 0 $(($POPSIZE -1))`; do
    cat "$j/analyze/Fitness.csv" | tail -1 >> output_error/FitnessList.csv
    echo >> output_error/FitnessList.csv
    tar -zcf $j.tar.gz $j
  done
  if [ $i -eq 0 ]; then
    echo "cycle,test,AllError,FlowError,PosError,StartFlowError,FollowFlowError,IdleFlowError,BusyFlowError" > ../FitnessAll.csv
  fi
  for j in `seq 2 $(($POPSIZE +1))`; do
    echo -n "$i,$(($j -2))," >> ../FitnessAll.csv
    cat "output_error/FitnessList.csv" | head -$j | tail -1 >> ../FitnessAll.csv
  done
  seq 0 $(($POPSIZE -1))| xargs -n 1 -P $PARALLEL -I {} rm -r {}
done

