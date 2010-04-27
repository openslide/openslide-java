# simple makefile for now

CC = gcc
JAVAC = javac

all: libopenslidejava.so
	ant

libopenslidejava.so: openslide-jni.c
	$(CC) $(CFLAGS) -fPIC -shared -g -O2 -Wall -o $@ $< $$(pkg-config openslide --cflags --libs)

clean:
	ant clean
	$(RM) libopenslidejava.so openslide_wrap.c *~ bin src/edu/cmu/cs/openslide/SWIGTYPE*.java src/edu/cmu/cs/openslide/*JNI.java src/edu/cmu/cs/openslide/OpenSlideGlue.java




.PHONY: all clean
