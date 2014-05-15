package tvla.core.decompose;

import java.util.Set;
import tvla.core.HighLevelTVS;
import tvla.formulae.Formula;

/**
 * Interface for a decomposition name. Immutable.
 * 
 * @author tla
 * 
 */
public interface DecompositionName {
    /**
     * Compose this name with the given name
     * 
     * @return The composed name
     */
    public DecompositionName compose(DecompositionName name);

    /**
     * Can this structure be decomposed according to this name?
     * 
     * @param ignoreOutside
     *            Should the outside node by ignored when checking.
     */
    public boolean canDecompose(HighLevelTVS structure, boolean ignoreOutside);

    /**
     * Does the current name contain the given subname
     */
    public boolean contains(DecompositionName subname);
    /**
     * Can we decompose from the given name
     */
    public boolean canDecomposeFrom(DecompositionName other);

    public Formula getFormula();

    public boolean isAbstraction();

    public Set<DecompositionName> getBase();
}
