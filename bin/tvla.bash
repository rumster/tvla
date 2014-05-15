#!/bin/bash
#
# A TVLA activation script for UNIX platforms.
# Note that the memory limitation is set to 800MB, using the -mx
# option.
#
# Changes:
#
# 2004-02-02  Gilad Arnold  <arnold@tau.ac.il>
#
# * Minor adjustments: (1) use bash;
#   (2) correct determination of DOT output filename
#   for proper creation of PostScript file.
#
################################################################

# Launch TVLA
java -Dtvla.home="$TVLA_HOME" -mx800m -jar $TVLA_HOME/lib/tvla.jar $*

# Generate PostScript output
(( $# > 0 )) && [ ${1:0:1} == "-" ] && DOTNAME="" || DOTNAME="$1"
while (( $# > 0 )); do
	[ "$1" == "-dot" ] && [ -n "$2" ] && DOTNAME="${2%.dt}" && break
	shift
done
[ -n "${DOTNAME}" ] && [ -e "${DOTNAME}.dt" ] && dot -Tps -o"$DOTNAME.ps" < "$DOTNAME.dt"
