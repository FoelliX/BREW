#!/bin/bash

cd target/build
mkdir answers
mkdir storage
mkdir output

zip -u $1 tool.properties
rm tool.properties