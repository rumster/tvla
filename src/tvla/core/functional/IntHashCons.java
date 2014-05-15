package tvla.core.functional;

public class IntHashCons {
	private Entry table[];
	private int next;

	public IntHashCons (int tblsize) {
		table = new Entry[tblsize];
		next = 0;
	}

	public int instance(int i, int j) {
	   int hash = (i ^ (j << 1));
		if (hash < 0) hash = -(hash+1);
		hash = hash % table.length;

		for (Entry entry = table[hash]; entry != null; entry = entry.next) {
			if ((entry.fst == i) && (entry.snd == j))
				return entry.val;
		}

		table[hash] = new Entry (i, j, next, table[hash]);
		return next++;
	}

	private static class Entry {
		public int fst, snd, val;
		public Entry next;

		public Entry(int f, int s, int v, Entry n) {
			fst = f;
			snd = s;
			val = v;
			next = n;
		}
	}

	public int generated() { return next; }

	public int size() { return next; }
}

