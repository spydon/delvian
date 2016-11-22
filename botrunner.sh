#!/usr/bin/env bash
cd ~/repos/delvian/
echo run
while true; do
    echo loop
    timeout 1h sbt clean run
done
