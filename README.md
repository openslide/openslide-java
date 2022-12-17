# OpenSlide Java

This is a Java binding to [OpenSlide](https://openslide.org/).


## Building with Meson

This is the new method.

### Build requirements

- JDK
- Meson &ge; 0.62
- OpenSlide &ge; 3.4.0
- pkg-config


### Building

```
meson setup builddir
meson compile -C builddir
meson install -C builddir
```


## Building with Autotools and Ant

This is the old method, and will eventually be removed.


### Build requirements

- JDK
- Apache Ant
- OpenSlide &ge; 3.4.0
- pkg-config


### Building on Linux or Mac OS X

```
./configure
make
make install
```

(If building from the Git repository, you will first need to install
autoconf, automake, libtool, and pkg-config and run `autoreconf -i`.)


### Cross-compiling for Windows with MinGW-w64

```
PKG_CONFIG=pkg-config \
	PKG_CONFIG_PATH=/path/to/cross/compiled/openslide/lib/pkgconfig \
	./configure --host=i686-w64-mingw32 --build=$(build-aux/config.guess)
make
make install
```

For a 64-bit JRE, substitute `--host=x86_64-w64-mingw32`.


### Building on Windows

Ensure that the path to the openslide-java source tree does not contain
whitespace.

Install Cygwin, selecting these additional packages:

- `make`
- `pkg-config`
- `mingw64-i686-gcc-core` and/or `mingw64-x86_64-gcc-core`

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

For a 64-bit JRE, substitute `--host=x86_64-w64-mingw32`.


## License

OpenSlide Java is released under the terms of the [GNU Lesser General Public
License, version 2.1](https://openslide.org/license/).

OpenSlide Java is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
License for more details.
