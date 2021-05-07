#!/bin/sh

cd $(dirname $0)
cp -r sample masbench-sample
cd MAS-Bench
gradle
cp build/libs/MAS-Bench-all.jar ../masbench-sample/masbench.jar
cd ..
cp README.md masbench-sample/
tar zcf masbench-sample.tar.gz masbench-sample

