# simple makefile for now

CC = gcc
JAVAC = javac

all: libopenslidejava.so src/edu/cmu/cs/openslide/OpenSlideGlue.java
	ant

openslide_wrap.c src/edu/cmu/cs/openslide/OpenSlideGlue.java: openslide.i
	mkdir -p src/edu/cmu/cs/openslide
	swig -Wall -I/usr/include -java $$(pkg-config openslide --cflags-only-I) -package edu.cmu.cs.openslide -outdir src/edu/cmu/cs/openslide $<
	sed -i 's/public class SWIGTYPE/class SWIGTYPE/g' src/edu/cmu/cs/openslide/SWIGTYPE*.java


libopenslidejava.so: openslide_wrap.c
	$(CC) $(CFLAGS) -fPIC -fno-strict-aliasing -shared -g -O2 -Wall -o $@ $< $$(pkg-config openslide --cflags --libs)

clean:
	ant clean
	$(RM) libopenslidejava.so openslide_wrap.c *~ bin src/edu/cmu/cs/openslide/SWIGTYPE*.java src/edu/cmu/cs/openslide/*JNI.java src/edu/cmu/cs/openslide/OpenSlideGlue.java




.PHONY: all clean
