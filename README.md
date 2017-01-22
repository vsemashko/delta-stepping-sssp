#Implementation of single source shortest path problem in java
Program finds strongest path in graph from source to all of specified target files.

It uses delta stepping algorithm to find strongest path, which is based on Dijkstra algorithm.
Algorithm description: https://arxiv.org/pdf/1604.02113v1
.pdf

#Build
to build program from sources execute

mvn clean install

and in target folder sssp-0.1-jar-with-dependencies.jar could be executed

#Run
Program require three arguments:
path_to_graph_file path_to_source_target_file path_to_output_file

To run program from command line you can use:

java -jar sssp-0.1-jar-with-dependencies.jar graph_medium.txt source_target_medium.txt output.txt

#Requirements

To run program JRE 1.8 should be installed

TODO: fully implement delta stepping algorithm (currently main cycle doesn't allow parallel execution)
