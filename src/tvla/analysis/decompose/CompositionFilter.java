package tvla.analysis.decompose;

import java.util.Collection;

import tvla.core.TVSSet;
import tvla.core.decompose.DecompositionName;

public interface CompositionFilter {

    TVSSet filter(DecompositionName name, TVSSet beforeSet, boolean singleUse);

    void setTargetNames(Collection<DecompositionName> targetNames);

}
