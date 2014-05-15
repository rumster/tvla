package tvla.core.functional;

public interface VisitorCombiner extends Visitor, VisitorKleene {
	public abstract void visit(int i, Object o1, Object o2);
}
