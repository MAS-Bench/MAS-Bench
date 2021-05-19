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
wget [masbench-sample.tar.gz url]
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

https://dl.acm.org/doi/10.5555/3463952.3464190
- BibTeX
```
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
