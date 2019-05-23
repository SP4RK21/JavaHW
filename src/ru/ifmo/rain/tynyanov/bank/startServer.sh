#!/bin/bash
export CLASSPATH=..

rmiregistry &
java examples.rmi.Server
