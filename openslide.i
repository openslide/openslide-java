/* -*- c -*- */
/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2008 Carnegie Mellon University
 *  All rights reserved.
 *
 *  OpenSlide is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  OpenSlide is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSlide. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking OpenSlide statically or dynamically with other modules is
 *  making a combined work based on OpenSlide. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 */

%module OpenSlideGlue

%include "typemaps.i"
%include "arrays_java.i"
%include "various.i"

typedef unsigned int uint32_t;
typedef int int32_t;
typedef long long int int64_t;

%javaconst(1);

%{
#include "openslide.h"

char *deref_char_p_p(char **c, int index) {
  return c[index];
}

%}

%pragma(java) jniclasscode=%{
  static {
    try {
        System.loadLibrary("openslidejava");
    } catch (UnsatisfiedLinkError e) {
      System.err.println("Native code library failed to load. \n" + e);
    }
  }
%}

%pragma(java) moduleclassmodifiers="class"

%newobject openslide_open;

%apply long long[] {int64_t *};
%apply int[] {uint32_t *};


char *deref_char_p_p(char **c, int index);


typedef struct _openslide openslide_t;

bool openslide_can_open(const char *filename);

openslide_t *openslide_open(const char *filename);

void openslide_get_layer_dimensions(openslide_t *osr, int32_t layer,
				    int64_t *OUTPUT, int64_t *OUTPUT);

int32_t openslide_get_layer_count(openslide_t *osr);

void openslide_close(openslide_t *osr);

const char *openslide_get_comment(openslide_t *osr);

int32_t openslide_get_best_layer_for_downsample(openslide_t *osr,
						double downsample);

double openslide_get_layer_downsample(openslide_t *osr, int32_t layer);

void openslide_read_region(openslide_t *osr,
			   uint32_t *dest,
			   int64_t x, int64_t y,
			   int32_t layer,
			   int64_t w, int64_t h);

const char * const *openslide_get_property_names(openslide_t *osr);

const char *openslide_get_property_value(openslide_t *osr, const char *name);

const char * const *openslide_get_associated_image_names(openslide_t *osr);

void openslide_get_associated_image_dimensions(openslide_t *osr, const char *name,
					       int64_t *w, int64_t *h);

void openslide_read_associated_image(openslide_t *osr,
				     const char *name,
				     uint32_t *dest);
