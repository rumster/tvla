// @author Noam Rinetzky.
// (based on Eran Yahav's TVM.lex).
// @since 9.5.2001 Added the characters '$' and '.' to identifiers (Roman).
//
// To generate: java JLex.Main PTS.lex 
//              move PTS.lex.java PTSLex.java


package tvla.language.PTS;
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
%class PTSLex
%type Symbol

Letter [a-zA-Z_$]
Digit [0-9]
Number {Digit}{Digit}*
Id ({Letter}|\$)({Letter}|{Digit}|\$|\.)*
String \"[^\"]*\"
%state COMMENT
%state LINECOMMENT

%%

<YYINITIAL>"/*" {yybegin(COMMENT) ;}
<YYINITIAL>"//" {yybegin(LINECOMMENT) ;}
<YYINITIAL>"==>"   {return new Symbol(sym.IMPLIES_T);}
<YYINITIAL>"%p" {return new Symbol(sym.PRED); }
<YYINITIAL>"%i" {return new Symbol(sym.INS_PRED); }
<YYINITIAL>"%r" {return new Symbol(sym.CONSISTENCY_RULE );}
<YYINITIAL>"%f" {return new Symbol(sym.FOCUS );}
<YYINITIAL>"%message"   {return new Symbol(sym.MESSAGE );}
<YYINITIAL>"%t" {return new Symbol(sym.TITLE);}
<YYINITIAL>"%s" {return new Symbol(sym.SET);}
<YYINITIAL>"%new"   {return new Symbol(sym.NEW );}
<YYINITIAL>"%clone"   {return new Symbol(sym.CLONE );}
<YYINITIAL>"%let"   {return new Symbol(sym.LET );}
<YYINITIAL>"%retain"    {return new Symbol(sym.RETAIN );}
<YYINITIAL>"%action"    {return new Symbol(sym.ACTION );}
<YYINITIAL>"foreach"    {return new Symbol(sym.FOREACH );}
<YYINITIAL>"%vmethod"   {return new Symbol(sym.VMETHOD );}
<YYINITIAL>"%smethod"   {return new Symbol(sym.SMETHOD );}
<YYINITIAL>"%constructor"   {return new Symbol(sym.CONSTRUCTOR );}
<YYINITIAL>"vcall"     {return new Symbol(sym.VINVOKE );}
<YYINITIAL>"scall"     {return new Symbol(sym.SINVOKE );}
<YYINITIAL>"ccall"     {return new Symbol(sym.CINVOKE );}
<YYINITIAL>"%main"     {return new Symbol(sym.MAIN );}
<YYINITIAL>E       {return new Symbol(sym.EXISTS);}
<YYINITIAL>A       {return new Symbol(sym.FORALL);}
<YYINITIAL>TC       {return new Symbol(sym.TC);}
<YYINITIAL>\<[ ]?\-[ ]?\>   {return new Symbol(sym.IFF);}
<YYINITIAL>\-[ ]?\> {return new Symbol(sym.IMPLIES);}
<YYINITIAL>"|"  {return new Symbol(sym.OR);}
<YYINITIAL>"&"  {return new Symbol(sym.AND);}
<YYINITIAL>"!"  {return new Symbol(sym.NOT);}
<YYINITIAL>"*"  {return new Symbol(sym.STAR);}
<YYINITIAL>"+"  {return new Symbol(sym.PLUS);}
<YYINITIAL>"-"  {return new Symbol(sym.MINUS);}
<YYINITIAL>"==" {return new Symbol(sym.EQ);}
<YYINITIAL>"!="    {return new Symbol(sym.NEQ);}
<YYINITIAL>[\t ] {}
<YYINITIAL>1    {return new Symbol(sym.TRUE);}
<YYINITIAL>0   {return new Symbol(sym.FALSE);}
<YYINITIAL>1([ ])?/([ ])?2 {return new Symbol(sym.UNKNOWN );}
<YYINITIAL>{String} {return new Symbol(sym.STRING, yytext().substring(1, yytext().length()-1));}
<YYINITIAL>{Id} {return new Symbol(sym.ID, yytext()); }
<YYINITIAL>{Number} {return new Symbol(sym.NUMBER, yytext()); }
<YYINITIAL>"["  {return new Symbol(sym.LBR); }
<YYINITIAL>"]"  {return new Symbol(sym.RBR); }
<YYINITIAL>"("  {return new Symbol(sym.LP); }
<YYINITIAL>")"  {return new Symbol(sym.RP); }
<YYINITIAL>"{"  {return new Symbol(sym.LCBR); }
<YYINITIAL>"}"  {return new Symbol(sym.RCBR); }
<YYINITIAL>"="  {return new Symbol(sym.ASSIGN);}
<YYINITIAL>"?"  {return new Symbol(sym.QMARK);}
<YYINITIAL>","  {return new Symbol(sym.COMMA);}
<YYINITIAL>":"  {return new Symbol(sym.COLON);}
<YYINITIAL>";"  {return new Symbol(sym.SEMICOLON);}
<YYINITIAL>"%"  {return new Symbol(sym.PERCENT);}
<YYINITIAL>"/"  {return new Symbol(sym.COMBINE);}
<YYINITIAL>"->"	{return new Symbol(sym.ARROW);}
<YYINITIAL>"ETC"	{return new Symbol(sym.ETC);}
<YYINITIAL>"%interface"	{return new Symbol(sym.INTERFACE);}
<YYINITIAL>"%class"	{return new Symbol(sym.CLASS);}
<YYINITIAL>"%iextends"	{return new Symbol(sym.EXTENDSINTERFACE);}
<YYINITIAL>"%cextends"	{return new Symbol(sym.EXTENDSCLASS);}
<YYINITIAL>"%implements"	{return new Symbol(sym.IMPLEMENTS);}


<YYINITIAL>\r {}
<YYINITIAL>\n   { line_count++;}
<YYINITIAL>\032 {}
<YYINITIAL>.   { Logger.println("Illegal character: "+yytext()); }
<COMMENT>"*/"   { yybegin(YYINITIAL) ;}
<COMMENT>\n   {line_count++;}
<COMMENT>.   {}
<LINECOMMENT>\n   {line_count++; yybegin(YYINITIAL); }
<LINECOMMENT>.   {}

