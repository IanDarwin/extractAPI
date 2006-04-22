This was an attempt to print out a compilable version of an API given only its .class file.

The approach was to use java.lang.Reflect to get all the values, so as not to run afoul of
"reverse engineering" laws (java.lang.Reflect uses only public API so it could be argued 
to be not technically "reverse engineering" as conventionally understood).

This effort has gone about as far as it can; but there are some major areas that don't work.

For example, java.lang.Reflect does not directly allow you
to access the value of package-private fields such as SerialVersionUID. 

And, this effort does not extract inner classes. Values of inner class instances
would be the most effort (run this against java.awt.* and see, e.g, in java.awt.im.InputSubset,
lines like this: 
	final public static java.awt.im.InputSubset LATIN = LATIN;
	
I would recommend to anybody starting on a similar project to use a byte code engineering
library (like, well, BCEL) to get information, instead of using Reflection.