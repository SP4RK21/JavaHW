#!/usr/bin/env bash
SALT=$1
HW_NAME=crawler
HW_NAME_OLD=crawler
HW_SECOND_CLASS=WebCrawler
HW_MAIN_CLASS=WebCrawler
TEST_MODE=easy

HW_PACKAGE_DIR=ru/ifmo/rain/tynyanov/$HW_NAME_OLD
HW_PACKAGE=ru.ifmo.rain.tynyanov.$HW_NAME_OLD
REPOSITORY=../java-advanced-2019
MODULE_PATH=${REPOSITORY}/artifacts/:${REPOSITORY}/modules/:${REPOSITORY}/lib/
TEST_PACKAGE=info.kgeorgiy.java.advanced.$HW_NAME
OUTPUT_DIR=build/
COMPILE_CLASSPATH=${OUTPUT_DIR}:${REPOSITORY}/artifacts/${TEST_PACKAGE}.jar

javac -p ${MODULE_PATH} -d ${OUTPUT_DIR} src/${HW_PACKAGE_DIR}/*.java --add-modules ${TEST_PACKAGE}

java -cp ${COMPILE_CLASSPATH} -p ${MODULE_PATH} -m $TEST_PACKAGE $TEST_MODE $HW_PACKAGE.$HW_MAIN_CLASS $SALT


