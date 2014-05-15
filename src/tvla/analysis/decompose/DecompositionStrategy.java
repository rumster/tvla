package tvla.analysis.decompose;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import tvla.core.Framer;
import tvla.core.HighLevelTVS;
import tvla.core.decompose.CartesianElement;
import tvla.core.decompose.DecompositionName;

/**
 * Interface for decomposition strategy
 * @author tla
 *
 */
public interface DecompositionStrategy {

    public Map<DecompositionName, Set<DecompositionName>> getDecomposition(Collection<DecompositionName> composedNames);

    public void decompose(DecompositionName sourceName, DecompositionName targetName, Iterable<HighLevelTVS> composed, CartesianElement decomposed, Framer framer);
    
    public void verifyDecomposition() throws DecompositionFailedException;

    public void done(DecompositionName targetName, CartesianElement stepDecomposed, CartesianElement decomposed, Framer framer);
}
