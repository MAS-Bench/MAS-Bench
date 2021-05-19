#!/bin/sh

cd $(dirname $0)
rm -rf masbench-sample
cp -r sample masbench-sample
cd MAS-Bench
./gradlew
cp build/libs/MAS-Bench.jar ../masbench-sample/
cd ..
cp README.md masbench-sample/
tar zcf masbench-sample.tar.gz masbench-sample

