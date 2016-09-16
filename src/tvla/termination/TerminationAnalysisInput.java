package tvla.termination;

import tvla.util.graph.Graph;
import java.util.List;

/**
 * Created by BorisD on 11/16/2015.
 */
public final class TerminationAnalysisInput {

  public final Graph        RegionTransitionGraph;
  public final List<RTNode> EntryNodes;
  public final int          NestingDepth;
  public final String       OutputDir;

  public TerminationAnalysisInput(Graph regionTransitionGraph, List<RTNode> entryNodes, int nestingDepth, String outputDir) {
    RegionTransitionGraph = regionTransitionGraph;
    EntryNodes = entryNodes;

    NestingDepth = nestingDepth;
    OutputDir = outputDir;
  }
}
