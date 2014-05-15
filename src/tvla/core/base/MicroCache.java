package tvla.core.base;

import java.util.Map;
import tvla.predicates.Predicate;

final class MicroCache {
	final private Map predicates;
	final private Cell cell;
	
	final private Cell cell2;
	final private Cell cell3;
	private Cell cur;
	
	public MicroCache(Map predicates) {
		this.predicates = predicates;
		cell = new Cell();
		
		cell2 = new Cell();
		cell3 = new Cell();
		cell.next = cell2;
		cell2.next = cell3;
		cell3.next = cell;
		cur = cell;
	}
	
	final Object get(Predicate p) {
		//return cell.get(p);
		Cell _cur = cur;
		if (_cur.contains(p)) {
			return _cur.concrete();
		}
		_cur = _cur.next;
		if (_cur.contains(p)) {
			cur = _cur;
			return _cur.concrete();
		}
		_cur = _cur.next;
		cur = _cur;
		if (_cur.contains(p)) {
			return _cur.concrete();
		}
		return _cur.get(p);
	}
	
	final void putCurrent(Object o) {
		cur.putConcrete(o);
	}

	final void put(Predicate p, Object o) {
		//cell.put(p, o);
		
		if (cur.contains(p)) {
			cur.putConcrete(o);
			return;
		}
		cur = cur.next;
		if (cur.contains(p)) {
			cur.putConcrete(o);
			return;
		}
		cur = cur.next;
		cur.put(p, o);
	}
	
	final void remove(Predicate p) {
		//cell.remove(p);
		
		if (cell.contains(p)) {
			cell.putConcrete(null);
			return;
		}
		if (cell2.contains(p)) {
			cell2.putConcrete(null);
			return;
		}
		if (cell3.contains(p)) {
			cell3.putConcrete(null);
		}
	}
	
	final void clear() {
		//cell.clear();
		
		cell.clear();
		cell2.clear();
		cell3.clear();
	}
	
	class Cell {
		private Predicate predicate = null;
		private Object concrete = null;
		private Cell next;
		
		final boolean contains(Predicate p) {
			return predicate == p;
		}

		final Object concrete() {
			return concrete;
		}

		final void putConcrete(Object o) {
			concrete = o;
		}

		Object get(Predicate p) {
			if (predicate == p) {
				return concrete;
			}
			else {
				Object o = predicates.get(p);
				concrete = o;
				predicate = p;
				return o;
			}
		}

		void put(Predicate p, Object o) {
			predicate = p;
			concrete = o;
		}
		
		void remove(Predicate p) {
			if (predicate == p)
				concrete = null;
		}

		void clear() {
			predicate = null;
		}
	}
}

