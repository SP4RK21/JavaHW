#!/usr/bin/env bash
OUT=out
REPOSITORY=../java-advanced-2019
PACKAGE_DIR_K=info/kgeorgiy/java/advanced/implementor/
PACKAGE_DIR_T=ru/ifmo/rain/tynyanov/implementor/
PACKAGE_T=ru.ifmo.rain.tynyanov.implementor.Implementor
PACKAGE_K=info.kgeorgiy.java.advanced.implementor
MAN=META-INF/MANIFEST.MF

mkdir -p ${OUT}
javac -d ${OUT} -p ${REPOSITORY}/artifacts/:${REPOSITORY}/modules/:${REPOSITORY}/lib/ src/${PACKAGE_DIR_T}*.java
cd ${OUT}
jar xvf ../${REPOSITORY}/artifacts/${PACKAGE_K}.jar ${PACKAGE_DIR_K}Impler.class ${PACKAGE_DIR_K}JarImpler.class ${PACKAGE_DIR_K}ImplerException.class
jar cfm Implementor.jar ../${MAN} ${PACKAGE_DIR_T}Implementor.class ${PACKAGE_DIR_T}*.class ${PACKAGE_DIR_K}*.class
rm -rf info ru