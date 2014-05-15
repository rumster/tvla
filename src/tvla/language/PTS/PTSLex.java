// @author Noam Rinetzky.
// (based on Eran Yahav's TVM.lex).
// @since 9.5.2001 Added the characters '$' and '.' to identifiers (Roman).
//
// To generate: java JLex.Main PTS.lex 
//              move PTS.lex.java PTSLex.java
package tvla.language.PTS;
import java_cup.runtime.Symbol;
import tvla.util.*;


class PTSLex implements java_cup.runtime.Scanner {
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
	private java.io.BufferedReader yy_reader;
	private int yy_buffer_index;
	private int yy_buffer_read;
	private int yy_buffer_start;
	private int yy_buffer_end;
	private char yy_buffer[];
	private boolean yy_at_bol;
	private int yy_lexical_state;

	PTSLex (java.io.Reader reader) {
		this ();
		if (null == reader) {
			throw (new Error("Error: Bad input stream initializer."));
		}
		yy_reader = new java.io.BufferedReader(reader);
	}

	PTSLex (java.io.InputStream instream) {
		this ();
		if (null == instream) {
			throw (new Error("Error: Bad input stream initializer."));
		}
		yy_reader = new java.io.BufferedReader(new java.io.InputStreamReader(instream));
	}

	private PTSLex () {
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
		141,
		142
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
		/* 58 */ YY_NO_ANCHOR,
		/* 59 */ YY_NO_ANCHOR,
		/* 60 */ YY_NO_ANCHOR,
		/* 61 */ YY_NO_ANCHOR,
		/* 62 */ YY_NO_ANCHOR,
		/* 63 */ YY_NO_ANCHOR,
		/* 64 */ YY_NO_ANCHOR,
		/* 65 */ YY_NO_ANCHOR,
		/* 66 */ YY_NO_ANCHOR,
		/* 67 */ YY_NO_ANCHOR,
		/* 68 */ YY_NO_ANCHOR,
		/* 69 */ YY_NO_ANCHOR,
		/* 70 */ YY_NO_ANCHOR,
		/* 71 */ YY_NO_ANCHOR,
		/* 72 */ YY_NOT_ACCEPT,
		/* 73 */ YY_NO_ANCHOR,
		/* 74 */ YY_NO_ANCHOR,
		/* 75 */ YY_NO_ANCHOR,
		/* 76 */ YY_NOT_ACCEPT,
		/* 77 */ YY_NO_ANCHOR,
		/* 78 */ YY_NO_ANCHOR,
		/* 79 */ YY_NOT_ACCEPT,
		/* 80 */ YY_NO_ANCHOR,
		/* 81 */ YY_NOT_ACCEPT,
		/* 82 */ YY_NO_ANCHOR,
		/* 83 */ YY_NOT_ACCEPT,
		/* 84 */ YY_NO_ANCHOR,
		/* 85 */ YY_NOT_ACCEPT,
		/* 86 */ YY_NOT_ACCEPT,
		/* 87 */ YY_NOT_ACCEPT,
		/* 88 */ YY_NOT_ACCEPT,
		/* 89 */ YY_NOT_ACCEPT,
		/* 90 */ YY_NOT_ACCEPT,
		/* 91 */ YY_NOT_ACCEPT,
		/* 92 */ YY_NOT_ACCEPT,
		/* 93 */ YY_NOT_ACCEPT,
		/* 94 */ YY_NOT_ACCEPT,
		/* 95 */ YY_NOT_ACCEPT,
		/* 96 */ YY_NOT_ACCEPT,
		/* 97 */ YY_NOT_ACCEPT,
		/* 98 */ YY_NOT_ACCEPT,
		/* 99 */ YY_NOT_ACCEPT,
		/* 100 */ YY_NOT_ACCEPT,
		/* 101 */ YY_NOT_ACCEPT,
		/* 102 */ YY_NOT_ACCEPT,
		/* 103 */ YY_NOT_ACCEPT,
		/* 104 */ YY_NOT_ACCEPT,
		/* 105 */ YY_NOT_ACCEPT,
		/* 106 */ YY_NOT_ACCEPT,
		/* 107 */ YY_NOT_ACCEPT,
		/* 108 */ YY_NOT_ACCEPT,
		/* 109 */ YY_NOT_ACCEPT,
		/* 110 */ YY_NOT_ACCEPT,
		/* 111 */ YY_NOT_ACCEPT,
		/* 112 */ YY_NOT_ACCEPT,
		/* 113 */ YY_NOT_ACCEPT,
		/* 114 */ YY_NOT_ACCEPT,
		/* 115 */ YY_NOT_ACCEPT,
		/* 116 */ YY_NOT_ACCEPT,
		/* 117 */ YY_NOT_ACCEPT,
		/* 118 */ YY_NOT_ACCEPT,
		/* 119 */ YY_NOT_ACCEPT,
		/* 120 */ YY_NOT_ACCEPT,
		/* 121 */ YY_NOT_ACCEPT,
		/* 122 */ YY_NOT_ACCEPT,
		/* 123 */ YY_NOT_ACCEPT,
		/* 124 */ YY_NOT_ACCEPT,
		/* 125 */ YY_NOT_ACCEPT,
		/* 126 */ YY_NOT_ACCEPT,
		/* 127 */ YY_NOT_ACCEPT,
		/* 128 */ YY_NOT_ACCEPT,
		/* 129 */ YY_NOT_ACCEPT,
		/* 130 */ YY_NOT_ACCEPT,
		/* 131 */ YY_NOT_ACCEPT,
		/* 132 */ YY_NOT_ACCEPT,
		/* 133 */ YY_NOT_ACCEPT,
		/* 134 */ YY_NOT_ACCEPT,
		/* 135 */ YY_NOT_ACCEPT,
		/* 136 */ YY_NOT_ACCEPT,
		/* 137 */ YY_NOT_ACCEPT,
		/* 138 */ YY_NOT_ACCEPT,
		/* 139 */ YY_NOT_ACCEPT,
		/* 140 */ YY_NOT_ACCEPT,
		/* 141 */ YY_NOT_ACCEPT,
		/* 142 */ YY_NOT_ACCEPT,
		/* 143 */ YY_NO_ANCHOR,
		/* 144 */ YY_NOT_ACCEPT,
		/* 145 */ YY_NOT_ACCEPT,
		/* 146 */ YY_NOT_ACCEPT,
		/* 147 */ YY_NOT_ACCEPT,
		/* 148 */ YY_NOT_ACCEPT,
		/* 149 */ YY_NOT_ACCEPT,
		/* 150 */ YY_NOT_ACCEPT,
		/* 151 */ YY_NOT_ACCEPT,
		/* 152 */ YY_NOT_ACCEPT,
		/* 153 */ YY_NOT_ACCEPT,
		/* 154 */ YY_NOT_ACCEPT,
		/* 155 */ YY_NOT_ACCEPT,
		/* 156 */ YY_NOT_ACCEPT,
		/* 157 */ YY_NO_ANCHOR,
		/* 158 */ YY_NOT_ACCEPT,
		/* 159 */ YY_NOT_ACCEPT,
		/* 160 */ YY_NOT_ACCEPT,
		/* 161 */ YY_NO_ANCHOR,
		/* 162 */ YY_NOT_ACCEPT,
		/* 163 */ YY_NOT_ACCEPT,
		/* 164 */ YY_NO_ANCHOR,
		/* 165 */ YY_NO_ANCHOR,
		/* 166 */ YY_NOT_ACCEPT,
		/* 167 */ YY_NOT_ACCEPT,
		/* 168 */ YY_NOT_ACCEPT,
		/* 169 */ YY_NO_ANCHOR,
		/* 170 */ YY_NO_ANCHOR,
		/* 171 */ YY_NO_ANCHOR,
		/* 172 */ YY_NO_ANCHOR,
		/* 173 */ YY_NO_ANCHOR,
		/* 174 */ YY_NO_ANCHOR,
		/* 175 */ YY_NO_ANCHOR,
		/* 176 */ YY_NO_ANCHOR,
		/* 177 */ YY_NO_ANCHOR
	};
	private int yy_cmap[] = unpackFromString(1,130,
"41:9,36,57,41:2,56,41:12,58,41:5,30,34,40,41,42,5,33,41,47,48,2,35,52,31,44" +
",1,38,37,39,43:7,53,54,29,3,4,51,41,26,42,28,42,25,42:14,27,42:6,45,41,46,4" +
"1,42,41,13,42,18,23,11,9,14,21,7,42:2,19,10,16,20,6,42,8,12,15,24,22,17,55," +
"42:2,49,32,50,41:2,0:2")[0];

	private int yy_rmap[] = unpackFromString(1,178,
"0,1,2,1,3,1,4,5:3,1,6,1:2,7,1,8,9:2,1:15,10,1,11,12,1,13,1,5,1:9,5:3,1:4,5," +
"1:13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,19,32,33,34,35,3" +
"6,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,6" +
"1,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,8" +
"6,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,10" +
"8,109,110,111,112,113,114,115,116,117,118")[0];

	private int yy_nxt[][] = unpackFromString(119,59,
"1,2,3,4,5,6,7:3,177,7:2,172,7:5,173,7:3,174,7:2,8,9,74,7,73,10,11,12,13,14," +
"15,10,16,17,18,77,5,7,18,5,19,20,21,22,23,24,25,26,27,28,7,29,30,31,-1:60,3" +
"2,33,-1:59,34,-1:61,35,36,37,38,72,-1,39,76,-1,40,79,-1,81,144,-1:2,83,-1:4" +
"2,7:23,-1:8,7:3,-1:2,7:3,-1:10,7,-1:7,42,-1:25,87,-1:31,43,-1:56,88,-1:28,8" +
"9,-1:6,18:3,-1:3,18,-1:52,18:3,-1:3,18,-1:19,45,-1:64,91,92,-1:4,93,-1:53,1" +
"46,-1:57,158,-1:59,94,-1,95,-1:75,85,86,-1:33,7:22,41,-1:8,7:3,-1:2,7:3,-1:" +
"10,7,-1:4,69,-1:75,159,-1:41,90:39,44,90:18,-1:6,7:13,51,7:9,-1:8,7:3,-1:2," +
"7:3,-1:10,7,-1:14,96,-1:53,7:13,52,7:9,-1:8,7:3,-1:2,7:3,-1:10,7,-1:14,145," +
"-1:7,97,98,-1:44,7:13,53,7:9,-1:8,7:3,-1:2,7:3,-1:10,7,-1:13,100,-1:54,7:15" +
",58,7:7,-1:8,7:3,-1:2,7:3,-1:10,7,-1:34,86,-1:31,46,-1:25,101,-1:32,42,-1:8" +
"4,102,-1:8,47,-1:20,88,-1:63,103,-1:107,162,-1:18,148,-1:55,147,-1:53,105,-" +
"1:68,48,-1:54,160,-1:6,108,-1:54,163,-1:57,49,-1:54,166,-1:51,46,-1:93,47,-" +
"1:38,109,-1:52,150,-1:61,50,-1:57,112,-1:50,113,-1:67,115,-1:53,117,-1:55,1" +
"19,-1:63,121,-1:66,153,-1:57,122,-1:50,54,-1:57,55,-1:62,123,-1:53,125,-1:6" +
"4,126,-1:51,127,-1:65,56,-1:56,128,-1:60,57,-1:50,130,-1:70,131,-1:49,132,-" +
"1:70,133,-1:48,134,-1:56,59,-1:70,60,-1:59,155,-1:57,61,-1:51,136,-1:54,62," +
"-1:64,137,-1:52,63,-1:61,138,-1:54,64,-1:59,65,-1:66,140,-1:46,66,-1:50,1,6" +
"7,75,67:53,-1,68,67,1,70:55,-1,71,70,-1:6,7:13,78,7:9,-1:8,7:3,-1:2,7:3,-1:" +
"10,7,-1:14,99,-1:102,149,-1:18,104,-1:55,111,-1:57,110,-1:62,168,-1:50,120," +
"-1:62,118,-1:68,124,-1:57,129,-1:61,135,-1:53,156,-1:55,139,-1:49,7:13,80,7" +
":9,-1:8,7:3,-1:2,7:3,-1:10,7,-1:14,106,-1:62,107,-1:55,114,-1:52,7:13,82,7:" +
"9,-1:8,7:3,-1:2,7:3,-1:10,7,-1:18,151,-1:55,116,-1:52,7:12,84,7:10,-1:8,7:3" +
",-1:2,7:3,-1:10,7,-1:9,7:7,143,7:15,-1:8,7:3,-1:2,7:3,-1:10,7,-1:18,152,-1:" +
"59,154,-1:53,167,-1:53,7:7,157,7:15,-1:8,7:3,-1:2,7:3,-1:10,7,-1:9,7:7,161," +
"7:15,-1:8,7:3,-1:2,7:3,-1:10,7,-1:9,7:7,164,7:15,-1:8,7:3,-1:2,7:3,-1:10,7," +
"-1:9,7:12,165,7:10,-1:8,7:3,-1:2,7:3,-1:10,7,-1:9,7:12,169,7:10,-1:8,7:3,-1" +
":2,7:3,-1:10,7,-1:9,7:12,170,7:10,-1:8,7:3,-1:2,7:3,-1:10,7,-1:9,7:5,171,7:" +
"17,-1:8,7:3,-1:2,7:3,-1:10,7,-1:9,7:2,175,7:20,-1:8,7:3,-1:2,7:3,-1:10,7,-1" +
":9,7:14,176,7:8,-1:8,7:3,-1:2,7:3,-1:10,7,-1:3");

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
						{return new Symbol(sym.NUMBER, yytext()); }
					case -19:
						break;
					case 19:
						{return new Symbol(sym.LBR); }
					case -20:
						break;
					case 20:
						{return new Symbol(sym.RBR); }
					case -21:
						break;
					case 21:
						{return new Symbol(sym.LP); }
					case -22:
						break;
					case 22:
						{return new Symbol(sym.RP); }
					case -23:
						break;
					case 23:
						{return new Symbol(sym.LCBR); }
					case -24:
						break;
					case 24:
						{return new Symbol(sym.RCBR); }
					case -25:
						break;
					case 25:
						{return new Symbol(sym.QMARK);}
					case -26:
						break;
					case 26:
						{return new Symbol(sym.COMMA);}
					case -27:
						break;
					case 27:
						{return new Symbol(sym.COLON);}
					case -28:
						break;
					case 28:
						{return new Symbol(sym.SEMICOLON);}
					case -29:
						break;
					case 29:
						{}
					case -30:
						break;
					case 30:
						{ line_count++;}
					case -31:
						break;
					case 31:
						{}
					case -32:
						break;
					case 32:
						{yybegin(LINECOMMENT) ;}
					case -33:
						break;
					case 33:
						{yybegin(COMMENT) ;}
					case -34:
						break;
					case 34:
						{return new Symbol(sym.EQ);}
					case -35:
						break;
					case 35:
						{return new Symbol(sym.PRED); }
					case -36:
						break;
					case 36:
						{return new Symbol(sym.INS_PRED); }
					case -37:
						break;
					case 37:
						{return new Symbol(sym.CONSISTENCY_RULE );}
					case -38:
						break;
					case 38:
						{return new Symbol(sym.FOCUS );}
					case -39:
						break;
					case 39:
						{return new Symbol(sym.SET);}
					case -40:
						break;
					case 40:
						{return new Symbol(sym.TITLE);}
					case -41:
						break;
					case 41:
						{return new Symbol(sym.TC);}
					case -42:
						break;
					case 42:
						{return new Symbol(sym.IMPLIES);}
					case -43:
						break;
					case 43:
						{return new Symbol(sym.NEQ);}
					case -44:
						break;
					case 44:
						{return new Symbol(sym.STRING, yytext().substring(1, yytext().length()-1));}
					case -45:
						break;
					case 45:
						{return new Symbol(sym.IMPLIES_T);}
					case -46:
						break;
					case 46:
						{return new Symbol(sym.IFF);}
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
						{return new Symbol(sym.LET );}
					case -50:
						break;
					case 50:
						{return new Symbol(sym.MAIN );}
					case -51:
						break;
					case 51:
						{return new Symbol(sym.SINVOKE );}
					case -52:
						break;
					case 52:
						{return new Symbol(sym.CINVOKE );}
					case -53:
						break;
					case 53:
						{return new Symbol(sym.VINVOKE );}
					case -54:
						break;
					case 54:
						{return new Symbol(sym.CLASS);}
					case -55:
						break;
					case 55:
						{return new Symbol(sym.CLONE );}
					case -56:
						break;
					case 56:
						{return new Symbol(sym.RETAIN );}
					case -57:
						break;
					case 57:
						{return new Symbol(sym.ACTION );}
					case -58:
						break;
					case 58:
						{return new Symbol(sym.FOREACH );}
					case -59:
						break;
					case 59:
						{return new Symbol(sym.MESSAGE );}
					case -60:
						break;
					case 60:
						{return new Symbol(sym.SMETHOD );}
					case -61:
						break;
					case 61:
						{return new Symbol(sym.VMETHOD );}
					case -62:
						break;
					case 62:
						{return new Symbol(sym.EXTENDSINTERFACE);}
					case -63:
						break;
					case 63:
						{return new Symbol(sym.EXTENDSCLASS);}
					case -64:
						break;
					case 64:
						{return new Symbol(sym.INTERFACE);}
					case -65:
						break;
					case 65:
						{return new Symbol(sym.IMPLEMENTS);}
					case -66:
						break;
					case 66:
						{return new Symbol(sym.CONSTRUCTOR );}
					case -67:
						break;
					case 67:
						{}
					case -68:
						break;
					case 68:
						{line_count++;}
					case -69:
						break;
					case 69:
						{ yybegin(YYINITIAL) ;}
					case -70:
						break;
					case 70:
						{}
					case -71:
						break;
					case 71:
						{line_count++; yybegin(YYINITIAL); }
					case -72:
						break;
					case 73:
						{ Logger.println("Illegal character: "+yytext()); }
					case -73:
						break;
					case 74:
						{return new Symbol(sym.ID, yytext()); }
					case -74:
						break;
					case 75:
						{}
					case -75:
						break;
					case 77:
						{ Logger.println("Illegal character: "+yytext()); }
					case -76:
						break;
					case 78:
						{return new Symbol(sym.ID, yytext()); }
					case -77:
						break;
					case 80:
						{return new Symbol(sym.ID, yytext()); }
					case -78:
						break;
					case 82:
						{return new Symbol(sym.ID, yytext()); }
					case -79:
						break;
					case 84:
						{return new Symbol(sym.ID, yytext()); }
					case -80:
						break;
					case 143:
						{return new Symbol(sym.ID, yytext()); }
					case -81:
						break;
					case 157:
						{return new Symbol(sym.ID, yytext()); }
					case -82:
						break;
					case 161:
						{return new Symbol(sym.ID, yytext()); }
					case -83:
						break;
					case 164:
						{return new Symbol(sym.ID, yytext()); }
					case -84:
						break;
					case 165:
						{return new Symbol(sym.ID, yytext()); }
					case -85:
						break;
					case 169:
						{return new Symbol(sym.ID, yytext()); }
					case -86:
						break;
					case 170:
						{return new Symbol(sym.ID, yytext()); }
					case -87:
						break;
					case 171:
						{return new Symbol(sym.ID, yytext()); }
					case -88:
						break;
					case 172:
						{return new Symbol(sym.ID, yytext()); }
					case -89:
						break;
					case 173:
						{return new Symbol(sym.ID, yytext()); }
					case -90:
						break;
					case 174:
						{return new Symbol(sym.ID, yytext()); }
					case -91:
						break;
					case 175:
						{return new Symbol(sym.ID, yytext()); }
					case -92:
						break;
					case 176:
						{return new Symbol(sym.ID, yytext()); }
					case -93:
						break;
					case 177:
						{return new Symbol(sym.ID, yytext()); }
					case -94:
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
