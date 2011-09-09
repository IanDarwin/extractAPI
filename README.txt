This is an attempt to print out a compilable version of an API given only its .class file.

The approach was to use java.lang.Reflect to get all the values, so as not to run afoul of
"reverse engineering" laws (java.lang.Reflect uses only public API so it could be argued 
to be not technically "reverse engineering" as conventionally understood).

That said, the current version produces a "java.lang" package that compiles with about 80 errors,
and a "java.awt" and all subclasses that compiles with only about 300 errors, so it's still
probably less work than typing an API in from scratch, particularly as many of the errors are
either
	- invalid "final" values;
	- unknown implicit superclasses;
	- cascading errors resulting from the above.