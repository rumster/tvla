# 3-Valued Logic Analysis Engine

## What is TVLA
TVLA is a tool for implementing shape analysis. TVLA is a highly-parametric abstract interpretation, providing the following features:
* A language to define the concrete semantics via predicates and actions in first-order logic with transitive closure;
* Expressing and tuning abstraction via instrumentation predicates (also called derived predicates);
* Tuning the precision of abstract transformers via Focus (case-splitting) and Coerce (constraint solving) operations; and
* Automatic generation of sound abstract interpreters from the concrete semantics.

TVLA is naturally suited for checking properties of heap allocated data but can also be used for other problem domains.
