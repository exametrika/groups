#!/bin/sh
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 -cp ./bin:../common/bin:../lib:../lib/jline-3.2.0.jar:../lib/jna-4.2.2.jar Test