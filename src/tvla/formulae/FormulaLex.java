// @author Alexey Loginov
// This file is TVP.lex with minimal modifications.
package tvla.formulae;
import java_cup.runtime.Symbol;
import tvla.util.*;


class FormulaLex implements java_cup.runtime.Scanner {
	private final int YY_BUFFER_SIZE = 512;
	private final int YY_F = -1;
	private final int YY_NO_STATE = -1;
	private final int YY_NOT_ACCEPT = 0;
	private final int YY_START = 1;
	private final int YY_END = 2;
	private final int YY_NO_ANCHOR = 4;
	private final int YY_BOL = 128;
	private final int YY_EOF = 129;

int line_count = 1;

// The silly "#" at the start of Number avoids clashes with 0/1 (FALSE/TRUE).
	private java.io.BufferedReader yy_reader;
	private int yy_buffer_index;
	private int yy_buffer_read;
	private int yy_buffer_start;
	private int yy_buffer_end;
	private char yy_buffer[];
	private boolean yy_at_bol;
	private int yy_lexical_state;

	FormulaLex (java.io.Reader reader) {
		this ();
		if (null == reader) {
			throw (new Error("Error: Bad input stream initializer."));
		}
		yy_reader = new java.io.BufferedReader(reader);
	}

	FormulaLex (java.io.InputStream instream) {
		this ();
		if (null == instream) {
			throw (new Error("Error: Bad input stream initializer."));
		}
		yy_reader = new java.io.BufferedReader(new java.io.InputStreamReader(instream));
	}

	private FormulaLex () {
		yy_buffer = new char[YY_BUFFER_SIZE];
		yy_buffer_read = 0;
		yy_buffer_index = 0;
		yy_buffer_start = 0;
		yy_buffer_end = 0;
		yy_at_bol = true;
		yy_lexical_state = YYINITIAL;
	}

	private boolean yy_eof_done = false;
	private final int YYINITIAL = 0;
	private final int COMMENT = 1;
	private final int LINECOMMENT = 2;
	private final int yy_state_dtrans[] = {
		0,
		86,
		87
	};
	private void yybegin (int state) {
		yy_lexical_state = state;
	}
	private int yy_advance ()
		throws java.io.IOException {
		int next_read;
		int i;
		int j;

		if (yy_buffer_index < yy_buffer_read) {
			return yy_buffer[yy_buffer_index++];
		}

		if (0 != yy_buffer_start) {
			i = yy_buffer_start;
			j = 0;
			while (i < yy_buffer_read) {
				yy_buffer[j] = yy_buffer[i];
				++i;
				++j;
			}
			yy_buffer_end = yy_buffer_end - yy_buffer_start;
			yy_buffer_start = 0;
			yy_buffer_read = j;
			yy_buffer_index = j;
			next_read = yy_reader.read(yy_buffer,
					yy_buffer_read,
					yy_buffer.length - yy_buffer_read);
			if (-1 == next_read) {
				return YY_EOF;
			}
			yy_buffer_read = yy_buffer_read + next_read;
		}

		while (yy_buffer_index >= yy_buffer_read) {
			if (yy_buffer_index >= yy_buffer.length) {
				yy_buffer = yy_double(yy_buffer);
			}
			next_read = yy_reader.read(yy_buffer,
					yy_buffer_read,
					yy_buffer.length - yy_buffer_read);
			if (-1 == next_read) {
				return YY_EOF;
			}
			yy_buffer_read = yy_buffer_read + next_read;
		}
		return yy_buffer[yy_buffer_index++];
	}
	private void yy_move_end () {
		if (yy_buffer_end > yy_buffer_start &&
		    '\n' == yy_buffer[yy_buffer_end-1])
			yy_buffer_end--;
		if (yy_buffer_end > yy_buffer_start &&
		    '\r' == yy_buffer[yy_buffer_end-1])
			yy_buffer_end--;
	}
	private boolean yy_last_was_cr=false;
	private void yy_mark_start () {
		yy_buffer_start = yy_buffer_index;
	}
	private void yy_mark_end () {
		yy_buffer_end = yy_buffer_index;
	}
	private void yy_to_mark () {
		yy_buffer_index = yy_buffer_end;
		yy_at_bol = (yy_buffer_end > yy_buffer_start) &&
		            ('\r' == yy_buffer[yy_buffer_end-1] ||
		             '\n' == yy_buffer[yy_buffer_end-1] ||
		             2028/*LS*/ == yy_buffer[yy_buffer_end-1] ||
		             2029/*PS*/ == yy_buffer[yy_buffer_end-1]);
	}
	private java.lang.String yytext () {
		return (new java.lang.String(yy_buffer,
			yy_buffer_start,
			yy_buffer_end - yy_buffer_start));
	}
	private int yylength () {
		return yy_buffer_end - yy_buffer_start;
	}
	private char[] yy_double (char buf[]) {
		int i;
		char newbuf[];
		newbuf = new char[2*buf.length];
		for (i = 0; i < buf.length; ++i) {
			newbuf[i] = buf[i];
		}
		return newbuf;
	}
	private final int YY_E_INTERNAL = 0;
	private final int YY_E_MATCH = 1;
	private java.lang.String yy_error_string[] = {
		"Error: Internal error.\n",
		"Error: Unmatched input.\n"
	};
	private void yy_error (int code,boolean fatal) {
		java.lang.System.out.print(yy_error_string[code]);
		java.lang.System.out.flush();
		if (fatal) {
			throw new Error("Fatal Error.\n");
		}
	}
	private int[][] unpackFromString(int size1, int size2, String st) {
		int colonIndex = -1;
		String lengthString;
		int sequenceLength = 0;
		int sequenceInteger = 0;

		int commaIndex;
		String workString;

		int res[][] = new int[size1][size2];
		for (int i= 0; i < size1; i++) {
			for (int j= 0; j < size2; j++) {
				if (sequenceLength != 0) {
					res[i][j] = sequenceInteger;
					sequenceLength--;
					continue;
				}
				commaIndex = st.indexOf(',');
				workString = (commaIndex==-1) ? st :
					st.substring(0, commaIndex);
				st = st.substring(commaIndex+1);
				colonIndex = workString.indexOf(':');
				if (colonIndex == -1) {
					res[i][j]=Integer.parseInt(workString);
					continue;
				}
				lengthString =
					workString.substring(colonIndex+1);
				sequenceLength=Integer.parseInt(lengthString);
				workString=workString.substring(0,colonIndex);
				sequenceInteger=Integer.parseInt(workString);
				res[i][j] = sequenceInteger;
				sequenceLength--;
			}
		}
		return res;
	}
	private int yy_acpt[] = {
		/* 0 */ YY_NOT_ACCEPT,
		/* 1 */ YY_NO_ANCHOR,
		/* 2 */ YY_NO_ANCHOR,
		/* 3 */ YY_NO_ANCHOR,
		/* 4 */ YY_NO_ANCHOR,
		/* 5 */ YY_NO_ANCHOR,
		/* 6 */ YY_NO_ANCHOR,
		/* 7 */ YY_NO_ANCHOR,
		/* 8 */ YY_NO_ANCHOR,
		/* 9 */ YY_NO_ANCHOR,
		/* 10 */ YY_NO_ANCHOR,
		/* 11 */ YY_NO_ANCHOR,
		/* 12 */ YY_NO_ANCHOR,
		/* 13 */ YY_NO_ANCHOR,
		/* 14 */ YY_NO_ANCHOR,
		/* 15 */ YY_NO_ANCHOR,
		/* 16 */ YY_NO_ANCHOR,
		/* 17 */ YY_NO_ANCHOR,
		/* 18 */ YY_NO_ANCHOR,
		/* 19 */ YY_NO_ANCHOR,
		/* 20 */ YY_NO_ANCHOR,
		/* 21 */ YY_NO_ANCHOR,
		/* 22 */ YY_NO_ANCHOR,
		/* 23 */ YY_NO_ANCHOR,
		/* 24 */ YY_NO_ANCHOR,
		/* 25 */ YY_NO_ANCHOR,
		/* 26 */ YY_NO_ANCHOR,
		/* 27 */ YY_NO_ANCHOR,
		/* 28 */ YY_NO_ANCHOR,
		/* 29 */ YY_NO_ANCHOR,
		/* 30 */ YY_NO_ANCHOR,
		/* 31 */ YY_NO_ANCHOR,
		/* 32 */ YY_NO_ANCHOR,
		/* 33 */ YY_NO_ANCHOR,
		/* 34 */ YY_NO_ANCHOR,
		/* 35 */ YY_NO_ANCHOR,
		/* 36 */ YY_NO_ANCHOR,
		/* 37 */ YY_NO_ANCHOR,
		/* 38 */ YY_NO_ANCHOR,
		/* 39 */ YY_NO_ANCHOR,
		/* 40 */ YY_NO_ANCHOR,
		/* 41 */ YY_NO_ANCHOR,
		/* 42 */ YY_NO_ANCHOR,
		/* 43 */ YY_NO_ANCHOR,
		/* 44 */ YY_NO_ANCHOR,
		/* 45 */ YY_NO_ANCHOR,
		/* 46 */ YY_NO_ANCHOR,
		/* 47 */ YY_NO_ANCHOR,
		/* 48 */ YY_NO_ANCHOR,
		/* 49 */ YY_NO_ANCHOR,
		/* 50 */ YY_NO_ANCHOR,
		/* 51 */ YY_NO_ANCHOR,
		/* 52 */ YY_NO_ANCHOR,
		/* 53 */ YY_NO_ANCHOR,
		/* 54 */ YY_NO_ANCHOR,
		/* 55 */ YY_NO_ANCHOR,
		/* 56 */ YY_NO_ANCHOR,
		/* 57 */ YY_NO_ANCHOR,
		/* 58 */ YY_NOT_ACCEPT,
		/* 59 */ YY_NO_ANCHOR,
		/* 60 */ YY_NO_ANCHOR,
		/* 61 */ YY_NO_ANCHOR,
		/* 62 */ YY_NOT_ACCEPT,
		/* 63 */ YY_NO_ANCHOR,
		/* 64 */ YY_NO_ANCHOR,
		/* 65 */ YY_NOT_ACCEPT,
		/* 66 */ YY_NO_ANCHOR,
		/* 67 */ YY_NOT_ACCEPT,
		/* 68 */ YY_NOT_ACCEPT,
		/* 69 */ YY_NOT_ACCEPT,
		/* 70 */ YY_NOT_ACCEPT,
		/* 71 */ YY_NOT_ACCEPT,
		/* 72 */ YY_NOT_ACCEPT,
		/* 73 */ YY_NOT_ACCEPT,
		/* 74 */ YY_NOT_ACCEPT,
		/* 75 */ YY_NOT_ACCEPT,
		/* 76 */ YY_NOT_ACCEPT,
		/* 77 */ YY_NOT_ACCEPT,
		/* 78 */ YY_NOT_ACCEPT,
		/* 79 */ YY_NOT_ACCEPT,
		/* 80 */ YY_NOT_ACCEPT,
		/* 81 */ YY_NOT_ACCEPT,
		/* 82 */ YY_NOT_ACCEPT,
		/* 83 */ YY_NOT_ACCEPT,
		/* 84 */ YY_NOT_ACCEPT,
		/* 85 */ YY_NOT_ACCEPT,
		/* 86 */ YY_NOT_ACCEPT,
		/* 87 */ YY_NOT_ACCEPT,
		/* 88 */ YY_NO_ANCHOR,
		/* 89 */ YY_NOT_ACCEPT,
		/* 90 */ YY_NOT_ACCEPT,
		/* 91 */ YY_NOT_ACCEPT,
		/* 92 */ YY_NOT_ACCEPT,
		/* 93 */ YY_NO_ANCHOR,
		/* 94 */ YY_NO_ANCHOR,
		/* 95 */ YY_NO_ANCHOR,
		/* 96 */ YY_NO_ANCHOR
	};
	private int yy_cmap[] = unpackFromString(1,130,
"39:9,32,52,39:2,51,39:12,53,39:5,26,30,38,36,40,5,29,39,44,45,2,31,49,27,41" +
",1,34,33,35,37:7,50,39,25,3,4,48,39,22,40,24,40,21,40:14,23,40:6,42,39,43,3" +
"9,40,39,13,40,18,40,11,9,14,20,7,40:3,10,16,19,6,40,8,12,15,40:2,17,40:3,46" +
",28,47,39:2,0:2")[0];

	private int yy_rmap[] = unpackFromString(1,97,
"0,1,2,1,3,1,4,5:3,1,6,1:2,7,1,8,1:15,9,1:2,10,1:3,5,1:2,11,1:8,5,1:6,12,13," +
"14,15,16,11,17,18,19,20,21,22,23,24,19,25,26,27,28,29,30,31,32,33,34,35,36," +
"37,38,39,40,41,42,43,44,45,46,47,48")[0];

	private int yy_nxt[][] = unpackFromString(49,54,
"1,2,3,4,5,6,7:3,96,7:11,8,9,60,7,59,10,11,12,13,14,15,10,16,17,5,63,5,66,5," +
"7,5,18,19,20,21,22,23,24,25,26,27,28,29,-1:55,30,31,-1:54,32,-1:56,33,34,35" +
",36,58,-1,37,62,-1,38,89,-1:43,7:19,-1:8,7:3,-1,7,-1:2,7:2,-1:16,40,-1:21,6" +
"8,69,-1:29,41,-1:51,70,-1:24,71,-1:31,44,-1:60,73,-1:75,42:3,-1,42,-1:27,74" +
",-1:68,65,67,-1:32,7:18,39,-1:8,7:3,-1,7,-1:2,7:2,-1:13,55,-1:70,90,-1:41,7" +
":14,51,7:4,-1:8,7:3,-1,7,-1:2,7:2,-1:39,67,-1:27,72:37,43,72:15,-1:4,45,-1:" +
"21,76,-1:31,40,-1:53,46,-1:75,77,-1:8,47,-1:19,70,-1:67,78,-1:50,91,-1:58,4" +
"8,-1:40,45,-1:84,47,-1:31,92,-1:47,81,-1:59,83,-1:59,84,-1:50,49,-1:51,85,-" +
"1:55,50,-1:48,52,-1:42,1,53,61,53:48,-1,54,53,1,56:50,-1,57,56,-1:6,7:12,64" +
",7:6,-1:8,7:3,-1,7,-1:2,7:2,-1:23,75,-1:57,79,-1:50,80,-1:48,82,-1:52,7:7,8" +
"8,7:11,-1:8,7:3,-1,7,-1:2,7:2,-1:18,7:5,93,7:13,-1:8,7:3,-1,7,-1:2,7:2,-1:1" +
"8,7:2,94,7:16,-1:8,7:3,-1,7,-1:2,7:2,-1:18,7:13,95,7:5,-1:8,7:3,-1,7,-1:2,7" +
":2,-1:12");

	public Symbol next_token ()
		throws java.io.IOException {
		int yy_lookahead;
		int yy_anchor = YY_NO_ANCHOR;
		int yy_state = yy_state_dtrans[yy_lexical_state];
		int yy_next_state = YY_NO_STATE;
		int yy_last_accept_state = YY_NO_STATE;
		boolean yy_initial = true;
		int yy_this_accept;

		yy_mark_start();
		yy_this_accept = yy_acpt[yy_state];
		if (YY_NOT_ACCEPT != yy_this_accept) {
			yy_last_accept_state = yy_state;
			yy_mark_end();
		}
		while (true) {
			if (yy_initial && yy_at_bol) yy_lookahead = YY_BOL;
			else yy_lookahead = yy_advance();
			yy_next_state = YY_F;
			yy_next_state = yy_nxt[yy_rmap[yy_state]][yy_cmap[yy_lookahead]];
			if (YY_EOF == yy_lookahead && true == yy_initial) {
 
return (new Symbol(sym.EOF)); 
			}
			if (YY_F != yy_next_state) {
				yy_state = yy_next_state;
				yy_initial = false;
				yy_this_accept = yy_acpt[yy_state];
				if (YY_NOT_ACCEPT != yy_this_accept) {
					yy_last_accept_state = yy_state;
					yy_mark_end();
				}
			}
			else {
				if (YY_NO_STATE == yy_last_accept_state) {
					throw (new Error("Lexical Error: Unmatched Input."));
				}
				else {
					yy_anchor = yy_acpt[yy_last_accept_state];
					if (0 != (YY_END & yy_anchor)) {
						yy_move_end();
					}
					yy_to_mark();
					switch (yy_last_accept_state) {
					case 1:
						
					case -2:
						break;
					case 2:
						{return new Symbol(sym.COMBINE);}
					case -3:
						break;
					case 3:
						{return new Symbol(sym.STAR);}
					case -4:
						break;
					case 4:
						{return new Symbol(sym.ASSIGN);}
					case -5:
						break;
					case 5:
						{ Logger.println("Illegal character: "+yytext()); }
					case -6:
						break;
					case 6:
						{return new Symbol(sym.PERCENT);}
					case -7:
						break;
					case 7:
						{return new Symbol(sym.ID, yytext()); }
					case -8:
						break;
					case 8:
						{return new Symbol(sym.EXISTS);}
					case -9:
						break;
					case 9:
						{return new Symbol(sym.FORALL);}
					case -10:
						break;
					case 10:
						{}
					case -11:
						break;
					case 11:
						{return new Symbol(sym.MINUS);}
					case -12:
						break;
					case 12:
						{return new Symbol(sym.OR);}
					case -13:
						break;
					case 13:
						{return new Symbol(sym.AND);}
					case -14:
						break;
					case 14:
						{return new Symbol(sym.NOT);}
					case -15:
						break;
					case 15:
						{return new Symbol(sym.PLUS);}
					case -16:
						break;
					case 16:
						{return new Symbol(sym.TRUE);}
					case -17:
						break;
					case 17:
						{return new Symbol(sym.FALSE);}
					case -18:
						break;
					case 18:
						{return new Symbol(sym.LBR); }
					case -19:
						break;
					case 19:
						{return new Symbol(sym.RBR); }
					case -20:
						break;
					case 20:
						{return new Symbol(sym.LP); }
					case -21:
						break;
					case 21:
						{return new Symbol(sym.RP); }
					case -22:
						break;
					case 22:
						{return new Symbol(sym.LCBR); }
					case -23:
						break;
					case 23:
						{return new Symbol(sym.RCBR); }
					case -24:
						break;
					case 24:
						{return new Symbol(sym.QMARK);}
					case -25:
						break;
					case 25:
						{return new Symbol(sym.COMMA);}
					case -26:
						break;
					case 26:
						{return new Symbol(sym.COLON);}
					case -27:
						break;
					case 27:
						{}
					case -28:
						break;
					case 28:
						{ line_count++;}
					case -29:
						break;
					case 29:
						{}
					case -30:
						break;
					case 30:
						{yybegin(LINECOMMENT) ;}
					case -31:
						break;
					case 31:
						{yybegin(COMMENT) ;}
					case -32:
						break;
					case 32:
						{return new Symbol(sym.EQ);}
					case -33:
						break;
					case 33:
						{return new Symbol(sym.PRED); }
					case -34:
						break;
					case 34:
						{return new Symbol(sym.INS_PRED); }
					case -35:
						break;
					case 35:
						{return new Symbol(sym.CONSISTENCY_RULE );}
					case -36:
						break;
					case 36:
						{return new Symbol(sym.FOCUS );}
					case -37:
						break;
					case 37:
						{return new Symbol(sym.SET);}
					case -38:
						break;
					case 38:
						{return new Symbol(sym.TITLE);}
					case -39:
						break;
					case 39:
						{return new Symbol(sym.TC);}
					case -40:
						break;
					case 40:
						{return new Symbol(sym.IMPLIES);}
					case -41:
						break;
					case 41:
						{return new Symbol(sym.NEQ);}
					case -42:
						break;
					case 42:
						{return new Symbol(sym.NUMBER, new Integer(yytext().substring(1)));}
					case -43:
						break;
					case 43:
						{return new Symbol(sym.STRING, yytext().substring(1, yytext().length()-1));}
					case -44:
						break;
					case 44:
						{return new Symbol(sym.IMPLIES_T);}
					case -45:
						break;
					case 45:
						{return new Symbol(sym.IFF);}
					case -46:
						break;
					case 46:
						{return new Symbol(sym.ARROW);}
					case -47:
						break;
					case 47:
						{return new Symbol(sym.UNKNOWN );}
					case -48:
						break;
					case 48:
						{return new Symbol(sym.NEW );}
					case -49:
						break;
					case 49:
						{return new Symbol(sym.RETAIN );}
					case -50:
						break;
					case 50:
						{return new Symbol(sym.ACTION );}
					case -51:
						break;
					case 51:
						{return new Symbol(sym.FOREACH );}
					case -52:
						break;
					case 52:
						{return new Symbol(sym.MESSAGE );}
					case -53:
						break;
					case 53:
						{}
					case -54:
						break;
					case 54:
						{line_count++;}
					case -55:
						break;
					case 55:
						{ yybegin(YYINITIAL) ;}
					case -56:
						break;
					case 56:
						{}
					case -57:
						break;
					case 57:
						{line_count++; yybegin(YYINITIAL); }
					case -58:
						break;
					case 59:
						{ Logger.println("Illegal character: "+yytext()); }
					case -59:
						break;
					case 60:
						{return new Symbol(sym.ID, yytext()); }
					case -60:
						break;
					case 61:
						{}
					case -61:
						break;
					case 63:
						{ Logger.println("Illegal character: "+yytext()); }
					case -62:
						break;
					case 64:
						{return new Symbol(sym.ID, yytext()); }
					case -63:
						break;
					case 66:
						{ Logger.println("Illegal character: "+yytext()); }
					case -64:
						break;
					case 88:
						{return new Symbol(sym.ID, yytext()); }
					case -65:
						break;
					case 93:
						{return new Symbol(sym.ID, yytext()); }
					case -66:
						break;
					case 94:
						{return new Symbol(sym.ID, yytext()); }
					case -67:
						break;
					case 95:
						{return new Symbol(sym.ID, yytext()); }
					case -68:
						break;
					case 96:
						{return new Symbol(sym.ID, yytext()); }
					case -69:
						break;
					default:
						yy_error(YY_E_INTERNAL,false);
					case -1:
					}
					yy_initial = true;
					yy_state = yy_state_dtrans[yy_lexical_state];
					yy_next_state = YY_NO_STATE;
					yy_last_accept_state = YY_NO_STATE;
					yy_mark_start();
					yy_this_accept = yy_acpt[yy_state];
					if (YY_NOT_ACCEPT != yy_this_accept) {
						yy_last_accept_state = yy_state;
						yy_mark_end();
					}
				}
			}
		}
	}
}
