# Notable Changes in OpenSlide Java

## Version 0.12.4, 2023-11-07

* Allow opening the synthetic test slide
* Add Meson build option to configure embedding of JNI path in JAR
* Fix `meson dist` failure when packaged as subproject


## Version 0.12.3, 2022-12-17

* Add Meson build system and deprecate Autotools+Ant one
* Change build target to Java 8 from 6, fixing build on newer JDK
* Convert README and changelog to Markdown
* Consolidate license files


## Version 0.12.2, 2016-09-11

* Change build target to Java 6 from 5, fixing build on JDK 9
* Properly detect JNI include paths on Mac OS X


## Version 0.12.1, 2015-04-20

* Bundle Classpath JNI headers for Windows cross builds


## Version 0.12.0, 2014-01-25

* Require OpenSlide 3.4.0
* Replace `OpenSlide.fileIsValid()` with `OpenSlide.detectVendor()`
* Fix `NullPointerException` opening slides without a quickhash1


## Version 0.11.0, 2012-09-08

* Require OpenSlide 3.3.0
* Rename `layer` to `level` throughout API
* Add `OpenSlide.getLibraryVersion()`
* Set package `Implementation-Version` to OpenSlide Java version
* Drop `getComment()`
* Properly handle `openslide_open()` errors on OpenSlide 3.3.0
* Many build fixes for Linux, Mac OS X, Cygwin


## Version 0.10.0, 2011-12-16

* Change package namespace to `org.openslide`
* Add wrapper class for associated images
* Have `OpenSlide` implement `Closeable`
* Convert top-level build system to Autotools
* Rename JAR and JNI library (thanks, Mathieu Malaterre)
* Embed JNI library path in JAR on Linux
* Fix translation by large offsets in GUI (thanks, Jan Harkes)
* Fix `IllegalArgumentException` in associated image error cases
* Add build instructions


## Version 0.9.2, 2010-08-10

* Remove some `Annotation` stuff, try to be more extensible and with simple
  defaults


## Version 0.9.1, 2010-06-16

* Fix build on Windows
* Remove some checks for zero dimensions and negative coordinates


## Version 0.9.0, 2010-06-01

* Eliminate swig dependency
* Support new error handling from OpenSlide 3.2.0
* Add new methods for painting
* Add some `Annotation` stuff


## Version 0.8.0, 2010-01-28

* Switch from GPLv2 to LGPLv2
* More selection types
* Bug fixes for `checkDisposed`
* Remove some exported C calls
* Add call to paint a specific layer without scaling


## Version 0.7.2, 2009-12-09

* Add file chooser to demo viewer
* Sort the properties in the demo viewer


## Version 0.7.1, 2009-11-19

* Swing and ImageIO fixes


## Version 0.7.0, 2009-09-15

* Hide SWIG-generated classes
* Remove synchronization


## Version 0.6.1, 2009-08-25

* Add `Main-Class` to the `jar` file for the demo viewer


## Version 0.6.0, 2009-08-17

* Show properties in the demo viewer
* Fix rendering bugs on newer X servers


## Version 0.5.0, 2009-07-15

* Restart version numbers since the Java bindings do not yet have a stable
  API
* Support for properties and associated images
* SWIG fixes
* Threading fixes

---

## Version 1.0.0-openslide, 2008-12-09

* Renamed to "OpenSlide"

## Version 1.0.0, 2008-11-07

* GPLv2 release
* SWIG fixes for 64-bits
* Print slide coordinates
* Zooming all the way in is supported
* Update for 64-bit changes in C API

---

## Version 0.4.1.2, 2008-06-13

* Rename package


## Version 0.4.1.1, 2008-04-02

* Auto-zoom support


## Version 0.4.1, 2008-03-25

* Can start with a zoomed slide


## Version 0.4.0, 2008-03-12

* Threading fixes
* Zoom to fit
* Centering
* More selection stuff
* Bug fixes


## Version 0.3.0, 2008-01-31

* A few optimizations


## Version 0.2.0, 2008-01-19

* Simple viewer and API (called "wholeslide-java")
