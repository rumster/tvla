package tvla.core.functional;

import tvla.logic.Kleene;
// import tvla.core.Node;
// import java.util.*;

// class KleenePacking:
// Utility class for packing kleene values into integers.

public class KleenePacking {
   public static final int kleenesPerInt = 16;

	// Kleene values are encoded into two bits. The encoding is intended
	// to enable a simple bitwise operation (&) can implement "join":
	// 	join (x,y) = if (x == y) then x else Unknown.
	// Using "01" for false and "10" for true and "00" for unknown
	// allows us to use bitwise-and for implementing join.

	static Kleene decode(int i) {
		switch(i) {
		case 0: return Kleene.unknownKleene;
		case 1: return Kleene.falseKleene;
		case 2: return Kleene.trueKleene;
		default: throw new RuntimeException("Wrong kleene value.");
		}
	}

	static int encode(Kleene k) {
		if (k == Kleene.unknownKleene) return 0;
		if (k == Kleene.falseKleene) return 1;
		if (k == Kleene.trueKleene) return 2;
		throw new RuntimeException("Kleene assumption violated.");
	}

   static Kleene lookup (int word, int pos) {
      if (pos < kleenesPerInt) {
         int i = (word >> (2*pos)) & 3;
         return decode(i);
      } else
         return Kleene.falseKleene;
   }
   
   static public void filter(int word, Visitor results, int bound) {
	   switch (bound) {
	   case 16:
		   results.visit(15, decode((word >> 30) & 3));
	   case 15:
		   results.visit(14, decode((word >> 28) & 3));
	   case 14:
		   results.visit(13, decode((word >> 26) & 3));
	   case 13:
		   results.visit(12, decode((word >> 24) & 3));
	   case 12:
		   results.visit(11, decode((word >> 22) & 3));
	   case 11:
		   results.visit(10, decode((word >> 20) & 3));
	   case 10:
		   results.visit(9, decode((word >> 18) & 3));
	   case 9:
		   results.visit(8, decode((word >> 16) & 3));
	   case 8:
		   results.visit(7, decode((word >> 14) & 3));
	   case 7:
		   results.visit(6, decode((word >> 12) & 3));
	   case 6:
		   results.visit(5, decode((word >> 10) & 3));
	   case 5:
		   results.visit(4, decode((word >> 8) & 3));
	   case 4:
		   results.visit(3, decode((word >> 6) & 3));
	   case 3:
		   results.visit(2, decode((word >> 4) & 3));
	   case 2:
		   results.visit(1, decode((word >> 2) & 3));
	   case 1:
		   results.visit(0, decode(word & 3));
	   }
   }
   
   static public void combine(int i, int j, int k, Visitor results) {
	   if (i == j)
		   return;
	   switch(i) {
	   case 0: 
		   results.visit(k, Kleene.unknownKleene);
		   return;
	   case 1: 
		   results.visit(k, Kleene.falseKleene);
		   return;
	   case 2:
		   results.visit(k, Kleene.trueKleene);
		   return;
	   }
   }

   static public void combine(int word1, int word2, Visitor results, int bound) {
	   int j = bound;
	   for (j = bound; j > 0; ) {
		   --j;
		   combine((word1 >> (2 * j)) & 3, (word2 >> (2 * j)) & 3, j, results);
	   }
   }

   static int update (int word, int pos, Kleene k) {
      if (pos < kleenesPerInt) {
         int clearpos = ~(3 << (2*pos));
	 		int newval = encode(k) << (2*pos);
         return (word & clearpos) | newval;
      } else
         throw new RuntimeException("Invalid argument");
   }

	static int join (int word1, int word2) {
		return (word1 & word2);
	}

   private static int[] constMap;

   static {
      constMap = new int[3];
      for (int i = 0; i < 3; i++) {
         Kleene k = Kleene.kleene((byte) i);
	 		int word = 0;
	 		for (int pos = 0; pos < kleenesPerInt; pos++) {
	    		word = update (word, pos, k);
	 		}
	 		constMap[i] = word;
      }
   }

   public static int constant(Kleene k) {
      return constMap[k.kleene()];
   }

   static void main(String[] args) {
      int word = 0;
      word = update(word, 16, Kleene.kleene((byte)2));
      System.out.println(word);
      System.out.println(lookup(word,16).toString());
      word = update(word, 16, Kleene.kleene((byte)0));
      System.out.println(word);
      System.out.println(lookup(word,16).toString());
      word = update(word, 16, Kleene.kleene((byte)1));
      System.out.println(word);
      System.out.println(lookup(word,16).toString());
   }

}
