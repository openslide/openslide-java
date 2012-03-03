Build requirements
------------------

- JDK
- Apache Ant
- OpenSlide

Building on Linux or Mac OS X
-----------------------------

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

Building on Windows
-------------------

Install a JDK, Apache Ant, MinGW, and MSYS.  Edit the MSYS fstab file
(e.g. C:\MinGW\msys\1.0\etc\fstab) to mount your JDK and Apache Ant
installations within the MSYS directory tree:

C:\Progra~1\Java\jdk1.6.0_29   /java
C:\ant                         /ant

You must use 8.3 short file names for path elements that contain spaces.

Then:

./configure --prefix=/path/to/install/dir JAVA_HOME=/java \
	ANT_HOME=/ant OPENSLIDE_CFLAGS=-I/path/to/openslide/include \
	OPENSLIDE_LIBS="-L/path/to/openslide/lib -lopenslide"
make
make install
