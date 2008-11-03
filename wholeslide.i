/* -*- c -*- */
/*
 *  Wholeslide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2008 Carnegie Mellon University
 *  All rights reserved.
 *
 *  Wholeslide is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  Wholeslide is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Wholeslide. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking Wholeslide statically or dynamically with other modules is
 *  making a combined work based on Wholeslide. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 */

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

%apply long long[] {int64_t *};
%apply int[] {uint32_t *};

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
