// @author Tal Lev-Ami
// @since 9.5.2001 Added the characters '$' and '.' to identifiers (Roman).
package tvla.language.TVS;

import tvla.util.*;
import java_cup.runtime.Symbol;

/** A scanner for TVS formatted files.
 * @author Tal Lev-Ami
 */
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
%class TVSLex
%type Symbol

Letter [a-zA-Z_$]
Digit [0-9]
Number {Digit}{Digit}*
Id {Letter}({Letter}|{Digit}|\$|\.)*
String \"[^\"]*\"
%state COMMENT
%state LINECOMMENT

%%

<YYINITIAL>"/*"	{yybegin(COMMENT) ;}
<YYINITIAL>"//"	{yybegin(LINECOMMENT) ;}
<YYINITIAL>"%location" {return new Symbol(sym.LOCATION); }
<YYINITIAL>"%p"	{return new Symbol(sym.PREDICATES); }
<YYINITIAL>"%n"	{return new Symbol(sym.NODES); }
<YYINITIAL>"%message" {return new Symbol(sym.MESSAGE); }
<YYINITIAL>"%t" {return new Symbol(sym.THREADS); }
<YYINITIAL>"%a" {return new Symbol(sym.AUTINIT); }
<YYINITIAL>"%d"	{return new Symbol(sym.DESCRIPTION); }
<YYINITIAL>"->"	{return new Symbol(sym.ARROW);}
<YYINITIAL>[\t ] {}
<YYINITIAL>"1"    {return new Symbol(sym.TRUE);}
<YYINITIAL>"0"   {return new Symbol(sym.FALSE);}
<YYINITIAL>"1/2" {return new Symbol(sym.UNKNOWN );}
<YYINITIAL>{Id}	{return new Symbol(sym.ID, yytext()); }
<YYINITIAL>{String} {return new Symbol(sym.STRING, yytext().substring(1, yytext().length()-1));}
<YYINITIAL>".1"  {return new Symbol(sym.ONE); }
<YYINITIAL>".0"  {return new Symbol(sym.ZERO); }
<YYINITIAL>"["  {return new Symbol(sym.LBR); }
<YYINITIAL>"]"  {return new Symbol(sym.RBR); }
<YYINITIAL>"{"  {return new Symbol(sym.LCBR); }
<YYINITIAL>"}"  {return new Symbol(sym.RCBR); }
<YYINITIAL>"("  {return new Symbol(sym.LP); }
<YYINITIAL>")"  {return new Symbol(sym.RP); }
<YYINITIAL>"="  {return new Symbol(sym.ASSIGN);}
<YYINITIAL>","  {return new Symbol(sym.COMMA);}
<YYINITIAL>":"  {return new Symbol(sym.COLON);}

<YYINITIAL>\r {}
<YYINITIAL>\n	{ line_count++;}
<YYINITIAL>\032 {}
<YYINITIAL>.   { Logger.println("Illegal character: "+yytext()); }
<COMMENT>"*/"	{ yybegin(YYINITIAL) ;}
<COMMENT>\n   {line_count++;}
<COMMENT>.   {}
<LINECOMMENT>\n   {line_count++; yybegin(YYINITIAL); }
<LINECOMMENT>.   {}
