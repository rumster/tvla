package tvla.analysis.interproc.api;

import tvla.api.ITVLAKleene;
import tvla.logic.Kleene;

public class TVLAKleeneImpl implements ITVLAKleene {
	private static TVLAKleeneImpl theInstance = new TVLAKleeneImpl();
	public static final TVLAKleeneImpl getInstance() {
		return theInstance;
	}
	
	
	public ITVLAKleeneValue trueVal() {
		return TVLAKleeneValueImpl.wrapKleene(Kleene.trueKleene);
	}
	
	public ITVLAKleeneValue falseVal() {
		return TVLAKleeneValueImpl.wrapKleene(Kleene.falseKleene);
	}
	
	public ITVLAKleeneValue unknownVal() {
		return TVLAKleeneValueImpl.wrapKleene(Kleene.unknownKleene);			
	}

	public ITVLAKleeneValue join(ITVLAKleeneValue val1, ITVLAKleeneValue val2) {
		if (val1 == null || val2 == null)
			return null;

		TVLAKleeneValueImpl kleene1 = (TVLAKleeneValueImpl) val1;
		TVLAKleeneValueImpl kleene2 = (TVLAKleeneValueImpl) val2;
		
		Kleene res = Kleene.join(kleene1.val, kleene2.val);
	
		// TODO Auto-generated method stub
		return TVLAKleeneValueImpl.wrapKleene(res);
	}

	public ITVLAKleeneValue meet(ITVLAKleeneValue val1, ITVLAKleeneValue val2) {
		if (val1 == null || val2 == null)
			return null;

		TVLAKleeneValueImpl kleene1 = (TVLAKleeneValueImpl) val1;
		TVLAKleeneValueImpl kleene2 = (TVLAKleeneValueImpl) val2;
		
		Kleene res = Kleene.meet(kleene1.val, kleene2.val);
	
		// TODO Auto-generated method stub
		return TVLAKleeneValueImpl.wrapKleene(res);
	}
	
	
	public static class TVLAKleeneValueImpl implements ITVLAKleeneValue {		
		Kleene val;
		
		public final static TVLAKleeneValueImpl trueKleene = new TVLAKleeneValueImpl(Kleene.trueKleene); 
        public final static TVLAKleeneValueImpl falseKleene = new TVLAKleeneValueImpl(Kleene.falseKleene); 
        public final static TVLAKleeneValueImpl unknownKleene = new TVLAKleeneValueImpl(Kleene.unknownKleene); 
		
		public TVLAKleeneValueImpl(Kleene val) {
			this.val = val;
		}
		
		public boolean isTrue() {
			return this == trueKleene;
		}

		public boolean isFalse() {
			return this == falseKleene;
		}

		public boolean isUnknown() {
			return this == unknownKleene;
		}

		public Kleene val() {
			return val;
		}
		
		public static TVLAKleeneValueImpl wrapKleene(Kleene val) {
			if (val == Kleene.falseKleene)
				return falseKleene;
			if (val == Kleene.trueKleene)
				return trueKleene;

			assert(val == Kleene.unknownKleene);
			
			return unknownKleene;
		}
	}
}
