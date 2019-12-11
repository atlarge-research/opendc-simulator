#!/bin/bash

./gradlew installDist

if [ $# -lt 3 ]
then
    echo "Please supply the setup, the trace file and a list of schedulers separated by spaces as arguments"
else
    scheds=""
    argc=$#
    argv=("$@")
    for ((i = 2; i < argc; i++)); do
        scheds=$scheds" --schedulers "${argv[i]}
    done

    ./opendc-model-odc/sc18/build/install/sc18/bin/sc18 --setup $1 $scheds $2
fi