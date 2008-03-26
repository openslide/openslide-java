# simple makefile for now

CC = gcc
JAVAC = javac

all: libwholeslidejava.so src/edu/cmu/cs/diamond/wholeslide/glue/Wholeslide.java
	ant

wholeslide_wrap.c src/edu/cmu/cs/diamond/wholeslide/glue/Wholeslide.java: wholeslide.i
	mkdir -p src/edu/cmu/cs/diamond/wholeslide/glue
	swig -Wall -I/usr/include -java $$(pkg-config wholeslide --cflags-only-I) -package edu.cmu.cs.diamond.wholeslide.glue -outdir src/edu/cmu/cs/diamond/wholeslide/glue $<


libwholeslidejava.so: wholeslide_wrap.c
	$(CC) $(CFLAGS) -fPIC -fno-strict-aliasing -shared -g -O2 -Wall -o $@ $< $$(pkg-config wholeslide --cflags --libs)

clean:
	ant clean
	$(RM) libwholeslidejava.so wholeslide_wrap.c src/edu/cmu/cs/diamond/wholeslide/glue/*.java *~ bin




.PHONY: all clean
