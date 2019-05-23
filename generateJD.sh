#!/usr/bin/env bash
OUT=out
REPOSITORY=../java-advanced-2019
PACKAGE_DIR_K=info/kgeorgiy/java/advanced/implementor/
PACKAGE_DIR_T=ru/ifmo/rain/tynyanov/implementor/
PACKAGE_T=ru.ifmo.rain.tynyanov.implementor
PACKAGE_K=info.kgeorgiy.java.advanced.implementor
LINK=https://docs.oracle.com/en/java/javase/11/docs/api

javadoc -d javadoc -link ${LINK} -cp src/:${REPOSITORY}/modules/:${REPOSITORY}/lib/ -private -author -version ${PACKAGE_T} ${PACKAGE_K}
