/* -*- c -*- */
%module Wholeslide

%include "typemaps.i"
%include "arrays_java.i"
%include "various.i"


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

typedef int uint32_t;  // hah
%apply uint32_t[] {uint32_t *};


void ws_get_baseline_dimensions(wholeslide_t *wsd,
				uint32_t *OUTPUT,
				uint32_t *OUTPUT);
void ws_get_layer_dimensions(wholeslide_t *wsd, uint32_t layer,
			     uint32_t *OUTPUT, uint32_t *OUTPUT);

%include "wholeslide.h"
