java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 -cp ../bin;../../common/bin;../../groups/bin;../../lib;../../lib/jline-3.2.0.jar;../../lib/jna-4.2.2.jar com.exametrika.impl.groups.simulator.agent.SimAgentMain