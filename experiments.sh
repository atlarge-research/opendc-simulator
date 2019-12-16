#!/bin/bash

rm -r results
mkdir results
rm -r data

for trace in pegasus shell askalon ;
do 
    for setup in setup setup-heterogeneous setup-distributed ;
    do
        for scheduler in FCP Lottery DS ;
        do
            if [ "$scheduler" == "Lottery" ]
            then
                ./opendc-model-odc/sc18/build/install/sc18/bin/sc18 --setup ./setups/${setup}.json --schedulers FIFO-LOTTERY --repeat 5 --warmup 5 --parallelism 6 ./traces/${trace}/tasks/schema-1.0/part.0.parquet
            else
                ./opendc-model-odc/sc18/build/install/sc18/bin/sc18 --setup ./setups/${setup}.json --schedulers ${scheduler} --warmup 5 --parallelism 6 ./traces/${trace}/tasks/schema-1.0/part.0.parquet
            fi
            mv -f data ./results/$trace\_$setup\_$scheduler
        done
    done
done