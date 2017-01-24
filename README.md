Build requirements
------------------

- JDK
- Apache Ant
- OpenSlide >= 3.4.0

Building on Linux or Mac OS X
-----------------------------

```
./configure
make
make install
```

Cross-compiling for Windows with MinGW-w64
------------------------------------------

```
PKG_CONFIG=pkg-config \
	PKG_CONFIG_PATH=/path/to/cross/compiled/openslide/lib/pkgconfig \
	./configure --host=i686-w64-mingw32 --build=$(build-aux/config.guess)
make
make install
```

For a 64-bit JRE, substitute `--host=x86_64-w64-mingw32`.

Building on Windows
-------------------

Ensure that the path to the openslide-java source tree does not contain whitespace.

Install Cygwin, selecting these additional packages:

- make
- pkg-config
- mingw64-i686-gcc-core and/or mingw64-x86_64-gcc-core

(Cygwin is only needed for the build environment; the resulting binaries
do not require Cygwin.)

Also install a JDK and Apache Ant.

Then:

```
./configure --prefix=/path/to/install/dir \
	--host=i686-w64-mingw32 --build=$(build-aux/config.guess) \
	PKG_CONFIG_PATH="/path/to/openslide/lib/pkgconfig" \
	JAVA_HOME="$(cygpath c:/Program\ Files/Java/jdk*)" \
	ANT_HOME="/path/to/ant/directory"
make
make install
```

For a 64-bit JRE, substitute --host=x86_64-w64-mingw32.
