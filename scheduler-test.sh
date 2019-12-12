#!/bin/bash

REPO_PATH=$(dirname $(realpath -s $0)) 

if [ $# -lt 1 ]
then
	echo "Please supply one or more schedulers"
else
	$REPO_PATH/test.sh $REPO_PATH/setups/setup.json $REPO_PATH/traces/shell/tasks/schema-1.0/part.0.parquet $@
fi