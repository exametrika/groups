#!/bin/sh
java -Djava.net.PreferIPv4Stack=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8001 -cp ../bin:../../common/bin:../../groups/bin:../../lib:../../lib/jline-3.2.0.jar:../../lib/jna-4.2.2.jar:../../lib/trove-3.0.3.jar com.exametrika.impl.groups.simulator.coordinator.SimCoordinatorMain