# simple makefile for now

CC = gcc
JAVAC = javac

all: libopenslidejava.so src/edu/cmu/cs/openslide/glue/OpenSlide.java
	ant

openslide_wrap.c src/edu/cmu/cs/openslide/glue/OpenSlide.java: openslide.i
	mkdir -p src/edu/cmu/cs/openslide/glue
	swig -includeall -Wall -I/usr/include -java $$(pkg-config openslide --cflags-only-I) -package edu.cmu.cs.openslide.glue -outdir src/edu/cmu/cs/openslide/glue $<


libopenslidejava.so: openslide_wrap.c
	$(CC) $(CFLAGS) -fPIC -fno-strict-aliasing -shared -g -O2 -Wall -o $@ $< $$(pkg-config openslide --cflags --libs)

clean:
	ant clean
	$(RM) libopenslidejava.so openslide_wrap.c src/edu/cmu/cs/openslide/glue/*.java *~ bin




.PHONY: all clean
