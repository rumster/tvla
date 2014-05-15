// @author Alexey Loginov
// This file is TVP.lex with minimal modifications.
package tvla.formulae;

import java_cup.runtime.Symbol;
import tvla.util.*;
%%
%{
int line_count = 1;
%}

%eofval{ 
return (new Symbol(sym.EOF)); 
%eofval} 

%cup
%implements java_cup.runtime.Scanner
%function next_token
%class FormulaLex
%type Symbol

Letter [a-zA-Z_$]
Digit [0-9]
%{
// The silly "#" at the start of Number avoids clashes with 0/1 (FALSE/TRUE).
%}
Number #{Digit}{Digit}*
Id {Letter}({Letter}|{Digit}|\$|\.)*
String \"[^\"]*\"
%state COMMENT
%state LINECOMMENT
%%

<YYINITIAL>"/*"	{yybegin(COMMENT) ;}
<YYINITIAL>"//"	{yybegin(LINECOMMENT) ;}
<YYINITIAL>"==>"   {return new Symbol(sym.IMPLIES_T);}
<YYINITIAL>"%p"	{return new Symbol(sym.PRED); }
<YYINITIAL>"%i"	{return new Symbol(sym.INS_PRED); }
<YYINITIAL>"%r"	{return new Symbol(sym.CONSISTENCY_RULE );}
<YYINITIAL>"%f"	{return new Symbol(sym.FOCUS );}
<YYINITIAL>"%message"	{return new Symbol(sym.MESSAGE );}
<YYINITIAL>"%t"	{return new Symbol(sym.TITLE);}
<YYINITIAL>"%s"	{return new Symbol(sym.SET);}
<YYINITIAL>"%new"	{return new Symbol(sym.NEW );}
<YYINITIAL>"%retain"	{return new Symbol(sym.RETAIN );}
<YYINITIAL>"%action"	{return new Symbol(sym.ACTION );}
<YYINITIAL>"foreach"	{return new Symbol(sym.FOREACH );}
<YYINITIAL>E       {return new Symbol(sym.EXISTS);}
<YYINITIAL>A       {return new Symbol(sym.FORALL);}
<YYINITIAL>TC       {return new Symbol(sym.TC);}
<YYINITIAL>\<[ ]?\-[ ]?\>	{return new Symbol(sym.IFF);}
<YYINITIAL>\-[ ]?\>	{return new Symbol(sym.IMPLIES);}
<YYINITIAL>"|"	{return new Symbol(sym.OR);}
<YYINITIAL>"&"	{return new Symbol(sym.AND);}
<YYINITIAL>"!"	{return new Symbol(sym.NOT);}
<YYINITIAL>"*"	{return new Symbol(sym.STAR);}
<YYINITIAL>"+"	{return new Symbol(sym.PLUS);}
<YYINITIAL>"-"	{return new Symbol(sym.MINUS);}
<YYINITIAL>"=="	{return new Symbol(sym.EQ);}
<YYINITIAL>"!="    {return new Symbol(sym.NEQ);}
<YYINITIAL>[\t ] {}
<YYINITIAL>1    {return new Symbol(sym.TRUE);}
<YYINITIAL>0   {return new Symbol(sym.FALSE);}
<YYINITIAL>1([ ])?/([ ])?2 {return new Symbol(sym.UNKNOWN );}
<YYINITIAL>{Number} {return new Symbol(sym.NUMBER, new Integer(yytext().substring(1)));}
<YYINITIAL>{String} {return new Symbol(sym.STRING, yytext().substring(1, yytext().length()-1));}
<YYINITIAL>{Id}	{return new Symbol(sym.ID, yytext()); }
<YYINITIAL>"["  {return new Symbol(sym.LBR); }
<YYINITIAL>"]"  {return new Symbol(sym.RBR); }
<YYINITIAL>"("  {return new Symbol(sym.LP); }
<YYINITIAL>")"  {return new Symbol(sym.RP); }
<YYINITIAL>"{"  {return new Symbol(sym.LCBR); }
<YYINITIAL>"}"  {return new Symbol(sym.RCBR); }
<YYINITIAL>"="	{return new Symbol(sym.ASSIGN);}
<YYINITIAL>"?"	{return new Symbol(sym.QMARK);}
<YYINITIAL>","	{return new Symbol(sym.COMMA);}
<YYINITIAL>":"	{return new Symbol(sym.COLON);}
<YYINITIAL>"%"	{return new Symbol(sym.PERCENT);}
<YYINITIAL>"/"	{return new Symbol(sym.COMBINE);}
<YYINITIAL>"-->"	{return new Symbol(sym.ARROW);}

<YYINITIAL>\r {}
<YYINITIAL>\n	{ line_count++;}
<YYINITIAL>\032 {}
<YYINITIAL>.   { Logger.println("Illegal character: "+yytext()); }
<COMMENT>"*/"	{ yybegin(YYINITIAL) ;}
<COMMENT>\n   {line_count++;}
<COMMENT>.   {}
<LINECOMMENT>\n   {line_count++; yybegin(YYINITIAL); }
<LINECOMMENT>.   {}

