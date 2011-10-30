Build requirements
------------------

- POSIX shell
- Make
- C compiler
- JDK
- Apache Ant
- OpenSlide

Building on Linux
-----------------

./configure
make
make install

Cross-compiling for Windows with mingw32
----------------------------------------

You will need the GNU Classpath version of jni.h installed.  (On Fedora
this is in the libgcj-devel package.)

PKG_CONFIG=pkg-config \
	PKG_CONFIG_PATH=/path/to/cross/compiled/openslide/lib/pkgconfig \
	./configure --host=i686-pc-mingw32 --build=$(build-aux/config.guess)
make
make install
