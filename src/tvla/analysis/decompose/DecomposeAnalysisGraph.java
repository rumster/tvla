package tvla.analysis.decompose;

import tvla.core.HighLevelTVS;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.Location;

public class DecomposeAnalysisGraph extends AnalysisGraph {

    @Override
    public void storeStructures(Location location, Iterable<HighLevelTVS> structures) {
        DecomposeLocation dloc = ((DecomposeLocation) location);
        for (HighLevelTVS structure : structures) {
            dloc.getElement().join(structure);
        }
        dloc.getElement().permuteBack();
    }
    
    @Override
    public void addAction(String source, Action action, String target) {
        if (backwardAnalysis) { // swap source and target
            String tmpLabel = source;
            source = target;
            target = tmpLabel;
        }

        if (!program.containsKey(source))
            program.put(source, new DecomposeLocation(source, true));
        if (!program.containsKey(target))
            program.put(target, new DecomposeLocation(target, true));

        if (!inOrder.contains(source))
            inOrder.add(source);
        if (!inOrder.contains(target))
            inOrder.add(target);

        Location sourceLocation = program.get(source);
        sourceLocation.addAction(action, target);

        ++numberOfActions;
    }
 
    
}
