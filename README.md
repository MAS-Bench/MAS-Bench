# MAS-Bench

## Requirements
- OpenJDK 11

## Requirements for sample-code
- python3.8 (3.8.3)
- numpy 1.19.4
- scipy 1.5.4
- pandas 1.1.4

```
pip install numpy
pip install scipy
pip install pandas
```

## How to run sample-code
```
wget https://github.com/MAS-Bench/MAS-Bench/releases/download/v0.4/masbench-sample.tar.gz
tar xf masbench-sample.tar.gz
cd masbench-sample
sh main.sh
```
or
```
git clone https://github.com/MAS-Bench/MAS-Bench.git
cd MAS-Bench
sh ./release-archive-build.sh
cd masbench-sample
sh main.sh
```

## MAS-Bench commands
- `java -jar MAS-Bench-all.jar init`
- `java -jar MAS-Bench-all.jar <Model name> <Working dir> <Parameter CSV file>`

### Model name
- FS1-1
- FS1-2
- FS1-3
- FS1-4
- FL1-1
- FL1-2
- FL1-3
- FL1-4

### Parameter CSV file format

### Output file format

## Publication
We would appreciate if you cite following paper when you publish your research.

https://link.springer.com/article/10.1007/s42001-024-00302-6
https://dl.acm.org/doi/10.5555/3463952.3464190
- BibTeX
```
@article{10.1007/s42001-024-00302-6,
author = {Shigenaka, Shusuke and Takami, Shunki and Tanigaki, Yuki and Watanabe, Shuhei and Onishi, Masaki},
title = {MAS-Bench: a benchmarking for parameter calibration of multi-agent crowd simulation},
year = {2024},
publisher = {Multi-agent simulation (MAS) has attracted significant attention for the prevention of pedestrian accidents and the spread of infectious diseases caused by overcrowding in recent years. In the MAS paradigm, each pedestrian is represented by a single agent. Control parameters for each agent need to be calibrated based on pedestrian traffic data to reproduce phenomena of interest accurately. Furthermore, observing all pedestrian traffic at large-scale events such as festivals and sports games is difficult. In such cases, parameter optimization is essential so that the appropriate parameters can be determined by solving an error minimization problem between the simulation results and incomplete observed pedestrian traffic data. We propose a benchmark problem, namely MAS-Bench, to discuss the performance of MAS parameter calibration methods uniformly. Numerical experiments demonstrate the baseline performance of four well-known optimization methods on six different error minimization problems that are defined on MAS-Bench. Moreover, we investigate the validity of the error function in the calibration by evaluating the correlation between the calibration and estimation scores. These scores are error functions relating to the available and unavailable observations, respectively.},
booktitle = {Journal of Computational Social Science},
keywords = {multi-agent crowd simulation, parameter calibration, meta-heuristic optimization benchmarking}
}


@inproceedings{10.5555/3463952.3464190,
author = {Shigenaka, Shusuke and Takami, Shunki and Watanabe, Shuhei and Tanigaki, Yuki and Ozaki, Yoshihiko and Onishi, Masaki},
title = {MAS-Bench: Parameter Optimization Benchmark for Multi-Agent Crowd Simulation},
year = {2021},
isbn = {9781450383073},
publisher = {International Foundation for Autonomous Agents and Multiagent Systems},
address = {Richland, SC},
abstract = {Multi-agent crowd simulation is generally used to construct an environment suitable for reality by parameter estimation. This estimation is to modify a model defined by human movements and behaviors to fit real-world data by means of statistics or optimization [2, 11]. In the field of traffic engineering, for example, parameter estimation is used to modify speed models and collision models of pedestrians and vehicles [10, 13]. For different simulation settings, it is not easy to obtain original data of each real-world environment. Few existing estimation methods have been fairly evaluated. Therefore, there is a need for a benchmark to discuss the applicability of certain estimation methods to other use cases.},
booktitle = {Proceedings of the 20th International Conference on Autonomous Agents and MultiAgent Systems},
pages = {1652–1654},
numpages = {3},
keywords = {parameter estimation, evaluation tool, pedestrian simulator},
location = {Virtual Event, United Kingdom},
series = {AAMAS '21}
}
```
