package tvla.analysis.decompose;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import tvla.analysis.AnalysisStatus;
import tvla.core.FrameManager;
import tvla.core.Framer;
import tvla.core.HighLevelTVS;
import tvla.core.decompose.CartesianElement;
import tvla.core.decompose.DecompositionName;
import tvla.core.generic.ConcreteTVSSet;
import tvla.formulae.Formula;
import tvla.formulae.ValueFormula;
import tvla.logic.Kleene;
import tvla.transitionSystem.Action;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.Pair;
import tvla.util.SimpleIterator;

public class BasicCompositionStrategy extends CompositionStrategy {

    protected Action action;

    protected Composer composer;
    protected DecompositionStrategy decomposer;

    protected CartesianElement decomposed;
    protected Map<DecompositionName, Iterable<HighLevelTVS>> composed;

    protected List<Step> steps = new LinkedList<Step>();

    protected FrameManager framerPre;

    protected FrameManager framer;

    private CompositionFilter filter;

    private CartesianElement stepDecomposed;

    private Step currentStep = null;
    
    public BasicCompositionStrategy(Action action, Set<? extends DecompositionName> currentNames, boolean incremental,
            DecomposeLocation nextLocation) {
        super();
        this.action = action;
        Formula filterFormula = action.getPrecondition() == null ? new ValueFormula(Kleene.trueKleene) : action
                .getPrecondition();
        framerPre = new FrameManager(action.getFramePre(), action);
        framer = new FrameManager(action.getFrame(), action);
        filter = new BasicCompositionFilter(filterFormula, action, nextLocation);
        this.composer = new BasicComposer(action.getComposeFormula(), incremental, framerPre, filter);
        this.decomposer = new MeetDecompositionStrategy(action.getDecomposeFormula(), incremental);

        this.composer.init(currentNames);

        Collection<DecompositionName> composedNames = composer.getComposedNames();
        Map<DecompositionName, Set<DecompositionName>> decomposition = decomposer.getDecomposition(composedNames);

        Map<DecompositionName, Step> nameToStep = HashMapFactory.make();

        // Create steps
        for (DecompositionName dname : decomposition.keySet()) {
            Set<DecompositionName> required = decomposition.get(dname);
            for (DecompositionName cname : required) {
                Step step = nameToStep.get(cname);
                if (step == null) {
                    step = new Step(cname);
                    nameToStep.put(cname, step);
                    steps.add(step);
                }
                if (!dname.isAbstraction()) {
                    step.toDecompose.add(dname);
                }
            }
        }
        // TODO maybe optimize order

        // Now decide when to remove
        Set<DecompositionName> futureNeeded = HashSetFactory.make();
        for (ListIterator<Step> stepIt = steps.listIterator(steps.size()); stepIt.hasPrevious();) {
            Step step = stepIt.previous();
            Set<DecompositionName> subs = composer.getIntermediates(step.toCompose);
            for (DecompositionName sub : subs) {
                if (futureNeeded.add(sub)) {
                    // Last time this is needed, add it to removal list
                    step.toRemove.add(sub);
                }
            }
        }
        // System.err.println(action + ": " + decomposition + ": " + steps);
    }

    @Override
    public CartesianElement getDecomposed() {
        return decomposed;
    }

    protected class Step {
        protected final DecompositionName toCompose;
        protected final Collection<DecompositionName> toDecompose = new ArrayList<DecompositionName>();
        protected final Collection<DecompositionName> toRemove = new ArrayList<DecompositionName>();

        public Step(DecompositionName toCompose) {
            this.toCompose = toCompose;
        }

        public String toString() {
            return "{" + toCompose + "=>" + toDecompose + " -" + toRemove + "}";
        }

        public Pair<DecompositionName, Iterable<HighLevelTVS>> executeBefore() {
            AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.COMPOSE_TIME);
            filter.setTargetNames(toDecompose);
            boolean success = composer.compose(toCompose);
            AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.COMPOSE_TIME);
            Pair<DecompositionName, Iterable<HighLevelTVS>> result = null;
            if (success) {
                Iterable<HighLevelTVS> composedSet = composed.get(toCompose);
                // Apply post-composition framer
                Framer cFramer = framer.getFramer(toCompose);
                if (cFramer != null) {
                    composedSet = new ConcreteTVSSet(cFramer.frame(composedSet, null));
                }
                result = Pair.create(toCompose, (Iterable<HighLevelTVS>) composed.get(toCompose));
            }
            stepDecomposed = new CartesianElement();
            return result;
        }

        public void executeAfter() {
            for (DecompositionName name : toRemove) {
                composer.removeOld(name);
                composer.removeDelta(name);
                composer.removeNew(name);
            }
            if (stepDecomposed != null) {
                for (DecompositionName name : toDecompose) {
                    Framer fullFramer = Framer.compose(framer.getFramer(name), framerPre.getFramer(name));
                    decomposer.done(name, stepDecomposed, decomposed, fullFramer);
                }
            }
        }

    }

    public Iterator<Pair<DecompositionName, Iterable<HighLevelTVS>>> iterator() {
        assert currentStep == null;
        return new SimpleIterator<Pair<DecompositionName, Iterable<HighLevelTVS>>>() {
            protected Iterator<Step> stepIterator = steps.iterator();

            @Override
            protected Pair<DecompositionName, Iterable<HighLevelTVS>> advance() {
                if (currentStep != null) {
                    currentStep.executeAfter();
                    // System.err.println("After " + lastStep);
                    currentStep = null;
                }
                while (stepIterator.hasNext()) {
                    currentStep = stepIterator.next();
                    Pair<DecompositionName, Iterable<HighLevelTVS>> result = currentStep.executeBefore();
                    // System.err.println("Before " + lastStep + " got " +
                    // name);
                    if (result != null) {
                        return result;
                    }
                    currentStep.executeAfter();
                    currentStep = null;
                }
                return null;
            }
        };
    }

    @Override
    public void verify() throws DecompositionFailedException {
        decomposer.verifyDecomposition();
    }

    @Override
    public void done() {
        this.composer.done();
        this.composed = null;
        this.decomposed = null;
        this.stepDecomposed = null;
    }

    @Override
    public void init(CartesianElement beforeOld, CartesianElement beforeDelta, CartesianElement beforeNew) {
        this.composed = this.composer.init(beforeOld, beforeDelta, beforeNew);

        this.decomposed = new CartesianElement();
    }

    @Override
    public void after(DecompositionName composedName, Iterable<HighLevelTVS> results) {
        AnalysisStatus.getActiveStatus().startTimer(AnalysisStatus.DECOMPOSE_TIME);
        for (DecompositionName name : currentStep.toDecompose) {
            // Compute full framer as possible composition of framers
            // matching this component name.
            Framer fullFramer = Framer.compose(framer.getFramer(name), framerPre.getFramer(name));
            decomposer.decompose(composedName, name, results, stepDecomposed, fullFramer);
        }
        AnalysisStatus.getActiveStatus().stopTimer(AnalysisStatus.DECOMPOSE_TIME);
    }
}
