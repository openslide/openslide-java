/* -*- c -*- */
%module Wholeslide

%include "typemaps.i"
%include "arrays_java.i"
%include "various.i"

#include <stdint.h>

%javaconst(1);

%{
#include "wholeslide.h"
%}

%pragma(java) jniclasscode=%{
  static {
    try {
        System.loadLibrary("wholeslidejava");
    } catch (UnsatisfiedLinkError e) {
      System.err.println("Native code library failed to load. \n" + e);
    }
  }
%}

%newobject ws_open;

%apply int64_t[] {int64_t *};
%apply int32_t[] {uint32_t *};

typedef struct _wholeslide wholeslide_t;

bool ws_can_open(const char *filename);

wholeslide_t *ws_open(const char *filename);

void ws_get_layer_dimensions(wholeslide_t *wsd, int32_t layer,
			     int64_t *OUTPUT, int64_t *OUTPUT);

int32_t ws_get_layer_count(wholeslide_t *wsd);

void ws_close(wholeslide_t *wsd);

const char *ws_get_comment(wholeslide_t *wsd);

int32_t ws_get_best_layer_for_downsample(wholeslide_t *wsd, double downsample);

double ws_get_layer_downsample(wholeslide_t *wsd, int32_t layer);

void ws_read_region(wholeslide_t *wsd,
		    uint32_t *dest,
		    int64_t x, int64_t y,
		    int32_t layer,
		    int64_t w, int64_t h);
