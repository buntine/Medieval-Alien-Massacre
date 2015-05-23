MEDIEVAL ALIEN MASSACRE
-----------------------

A sadistic and pro-death text-based adventure for (bastard) children.

A hobby project, written in Clojure (A Lisp for the JVM).

This project is dedicated to the memory of my friend Adam Hillier.

Please see the new and improved version of MAM at: [Moon Dweller](http://github.com/buntine/MoonDweller).


Installation
------------

If you want to make changes:

First, install Leiningen: http://github.com/technomancy/leiningen

And then:

```
  $ lein deps
  $ lein uberjar
```

Also install rlwrap (optional, but highly recommended as it provides tab completion, history, etc):

```
  $ sudo apt-get install rlwrap
```

Usage
-----

If you've installed rlwrap:

```
  $ ./play
```

Otherwise:

```
  $ java -jar target/mam-1.0.1-SNAPSHOT-standalone.jar
```

Or see a list of standalone packages in my downloads: https://github.com/buntine/Medieval-Alien-Massacre/archives/master


## Notes

 - Type the 'help' command in gameplay for some tips and instructions.
 - Inspired by Dunnet, by Rob Schnell ($ emacs -batch -l dunnet).


## License

Copyright (C) 2010-2014 Andrew Buntine

http://www.andrewbuntine.com

Distributed under the Eclipse Public License, the same as Clojure.
