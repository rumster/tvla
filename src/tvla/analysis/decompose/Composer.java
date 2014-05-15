package tvla.analysis.decompose;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import tvla.core.HighLevelTVS;
import tvla.core.decompose.CartesianElement;
import tvla.core.decompose.DecompositionName;

public interface Composer {
    public void init(Set<? extends DecompositionName> currentNames);    
    public Collection<DecompositionName> getComposedNames();
    public boolean compose(DecompositionName toCompose);
    public Set<DecompositionName> getIntermediates(DecompositionName toCompose);
    public Map<DecompositionName, Iterable<HighLevelTVS>> init(
            CartesianElement beforeOld, CartesianElement beforeDelta, CartesianElement beforeNew 
    );
    public Collection<DecompositionName> getSources(DecompositionName toCompose);
	public void done();
	public void removeOld(DecompositionName name);
	public void removeDelta(DecompositionName name);
	public void removeNew(DecompositionName name);
}