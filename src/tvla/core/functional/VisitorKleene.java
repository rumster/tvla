package tvla.core.functional;

public interface VisitorKleene extends Visitor {
	public abstract void visitNonZero(int i, Object o);
	public abstract void visitSetDefault(int i, Object o);
}
