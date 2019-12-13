#!/bin/bash

REPO_PATH=$(dirname $(realpath -s $0)) 

if [ $# -lt 1 ]
then
	echo "Please supply a scheduler"
else
	rm -rf $REPO_PATH/data2
	$REPO_PATH/scheduler-test.sh $1
	mv $REPO_PATH/data $REPO_PATH/data2
	$REPO_PATH/scheduler-test.sh $1
	diff -r data data2 && echo "Outputs are the same!"
fi