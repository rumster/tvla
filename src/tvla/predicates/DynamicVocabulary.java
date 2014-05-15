package tvla.predicates;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tvla.util.HashConsFactory;
import tvla.util.HashSetFactory;

public class DynamicVocabulary {
	protected static HashConsFactory<DynamicVocabulary, Set<Predicate>> factory = new HashConsFactory<DynamicVocabulary, Set<Predicate>>() {
		@Override
		protected DynamicVocabulary actualCreate(Set<Predicate> predicates) {
			return new DynamicVocabulary(predicates);
		}

	};

	protected static HashConsFactory<DynamicVocabulary, Set<Predicate>>.CommBinOp<DynamicVocabulary> union = factory.new CommBinOp<DynamicVocabulary>() {
		@Override
		protected DynamicVocabulary actualApply(DynamicVocabulary left,
				DynamicVocabulary right) {
			Set<Predicate> result = HashSetFactory.make(left.predicates);
			result.addAll(right.predicates);
			return create(result);
		}
	};

	protected static HashConsFactory<DynamicVocabulary, Set<Predicate>>.CommBinOp<DynamicVocabulary> intersection = factory.new CommBinOp<DynamicVocabulary>() {
		@Override
		protected DynamicVocabulary actualApply(DynamicVocabulary left,
				DynamicVocabulary right) {
			Set<Predicate> result = HashSetFactory.make(left.predicates);
			result.retainAll(right.predicates);
			return create(result);
		}
	};

	protected static HashConsFactory<DynamicVocabulary, Set<Predicate>>.BinOp<DynamicVocabulary> subtract = factory.new BinOp<DynamicVocabulary>() {
		@Override
		protected DynamicVocabulary actualApply(DynamicVocabulary left,
				DynamicVocabulary right) {
			Set<Predicate> result = HashSetFactory.make(left.predicates);
			result.removeAll(right.predicates);
			return create(result);
		}
	};

	protected static HashConsFactory<DynamicVocabulary, Set<Predicate>>.BinOp<Boolean> subsetof = factory.new BinOp<Boolean>() {
		@Override
		protected Boolean actualApply(DynamicVocabulary left,
				DynamicVocabulary right) {
			return right.predicates.containsAll(left.predicates);
		}
	};

	private static DynamicVocabulary full;

	private static DynamicVocabulary empty = create(new HashSet<Predicate>());

	protected final Set<Predicate> predicates;

	protected Set<Predicate> nullary = HashSetFactory.make();

	protected Set<Predicate> nullaryRel = HashSetFactory.make();

	protected Set<Predicate> nullaryNonRel = HashSetFactory.make();

	protected Set<Predicate> unique = HashSetFactory.make();

	protected Set<Predicate> unary = HashSetFactory.make();

	protected Set<Predicate> unaryRel = HashSetFactory.make();

	protected Set<Predicate> unaryNonRel = HashSetFactory.make();

	protected Set<Predicate> binary = HashSetFactory.make();

	protected Set<Predicate> kary = HashSetFactory.make();

	protected Set<Instrumentation> instrumentation = HashSetFactory.make();

	protected Set<Predicate> positiveArity = HashSetFactory.make();

	public static void reset() {
		factory = new HashConsFactory<DynamicVocabulary, Set<Predicate>>() {
			@Override
			protected DynamicVocabulary actualCreate(Set<Predicate> predicates) {
				return new DynamicVocabulary(predicates);
			}

		};

		union = factory.new CommBinOp<DynamicVocabulary>() {
			@Override
			protected DynamicVocabulary actualApply(DynamicVocabulary left,
					DynamicVocabulary right) {
				Set<Predicate> result = HashSetFactory.make(left.predicates);
				result.addAll(right.predicates);
				return create(result);
			}
		};

		intersection = factory.new CommBinOp<DynamicVocabulary>() {
			@Override
			protected DynamicVocabulary actualApply(DynamicVocabulary left,
					DynamicVocabulary right) {
				Set<Predicate> result = HashSetFactory.make(left.predicates);
				result.retainAll(right.predicates);
				return create(result);
			}
		};

		subtract = factory.new BinOp<DynamicVocabulary>() {
			@Override
			protected DynamicVocabulary actualApply(DynamicVocabulary left,
					DynamicVocabulary right) {
				Set<Predicate> result = HashSetFactory.make(left.predicates);
				result.removeAll(right.predicates);
				return create(result);
			}
		};

		subsetof = factory.new BinOp<Boolean>() {
			@Override
			protected Boolean actualApply(DynamicVocabulary left,
					DynamicVocabulary right) {
				return right.predicates.containsAll(left.predicates);
			}
		};

		DynamicVocabulary full = null;

		empty = create(new HashSet<Predicate>());
	}

	protected DynamicVocabulary(Set<Predicate> predicates) {
		this.predicates = predicates;
		for (Predicate predicate : predicates) {
			switch (predicate.arity()) {
			case 0:
				nullary.add(predicate);
				if (predicate.abstraction())
					nullaryRel.add(predicate);
				else
					nullaryNonRel.add(predicate);
				break;
			case 1:
				unary.add(predicate);
				if (predicate.unique) {
					unique.add(predicate);
				}
				if (predicate.abstraction())
					unaryRel.add(predicate);
				else
					unaryNonRel.add(predicate);
				break;
			case 2:
				binary.add(predicate);
				break;
			default:
				kary.add(predicate);
				break;
			}

			if (predicate instanceof Instrumentation)
				instrumentation.add((Instrumentation) predicate);
			if (predicate.arity() > 0)
				positiveArity.add(predicate);
		}
	}

	public static DynamicVocabulary create(Set<Predicate> predicates) {
		return factory.create(predicates);
	}

	public DynamicVocabulary union(DynamicVocabulary other) {
		return union.apply(this, other);
	}

	public DynamicVocabulary intersection(DynamicVocabulary other) {
		return intersection.apply(this, other);
	}

	public DynamicVocabulary subtract(DynamicVocabulary other) {
		return subtract.apply(this, other);
	}

	public boolean subsetof(DynamicVocabulary other) {
		return subsetof.apply(this, other);
	}

	public Set<Predicate> all() {
		return predicates;
	}

	public Set<Predicate> unique() {
		return unique;
	}

	public Set<Predicate> unary() {
		return unary;
	}

	public Set<Predicate> unaryNonRel() {
		return unaryNonRel;
	}

	public Set<Predicate> unaryRel() {
		return unaryRel;
	}

	public Set<Predicate> nullary() {
		return nullary;
	}

	public Set<Predicate> nullaryNonRel() {
		return nullaryNonRel;
	}

	public Set<Predicate> nullaryRel() {
		return nullaryRel;
	}

	public Set<Predicate> positiveArity() {
		return positiveArity;
	}

	public Set<Predicate> binary() {
		return binary;
	}

	public Set<Predicate> kary() {
		return kary;
	}

	public Set<Instrumentation> instrumentation() {
		return instrumentation;
	}

	public static DynamicVocabulary full() {
		if (full == null
				|| full.predicates.size() != Vocabulary.allPredicates().size()) {
			full = DynamicVocabulary.create(HashSetFactory.make(Vocabulary
					.allPredicates()));
		}
		return full;
	}

	public boolean contains(Predicate predicate) {
		return predicates.contains(predicate);
	}

	public String toString() {
		return predicates.toString();
	}

	public static DynamicVocabulary empty() {
		return empty;
	}

	public int size() {
		return predicates.size();
	}

	public DynamicVocabulary permute(Map<Predicate, Predicate> mapping) {
		Set<Predicate> result = HashSetFactory.make();
		for (Predicate predicate : predicates) {
			if (mapping.containsKey(predicate))
				predicate = mapping.get(predicate);
			result.add(predicate);
		}
		return create(result);
	}
}
