# simple makefile for now

CC = gcc
JAVAC = javac
JAVA_HOME = /usr/lib/jvm/java
JAVA_PLATFORM = linux

all: libopenslidejava.so
	ant

libopenslidejava.so: openslide-jni.c
	$(CC) $(CFLAGS) -fPIC -shared -g -O2 -Wall -o $@ $< -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(JAVA_PLATFORM) $$(pkg-config openslide --cflags --libs)

clean:
	ant clean
	$(RM) libopenslidejava.so openslide_wrap.c *~ bin




.PHONY: all clean
