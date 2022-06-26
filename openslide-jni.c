/*
 *  OpenSlide, a library for reading whole slide image files
 *
 *  Copyright (c) 2007-2010 Carnegie Mellon University
 *  All rights reserved.
 *
 *  OpenSlide is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation, version 2.1.
 *
 *  OpenSlide is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with OpenSlide. If not, see
 *  <http://www.gnu.org/licenses/>.
 *
 */

#include <jni.h>

#include <openslide.h>

static jstring osj_detect_vendor(JNIEnv *env, jobject obj, jstring filename) {
  const char *filename2 = (*env)->GetStringUTFChars(env, filename, NULL);
  if (filename2 == NULL) {
    return NULL;
  }

  const char *val = openslide_detect_vendor(filename2);

  (*env)->ReleaseStringUTFChars(env, filename, filename2);

  if (val == NULL) {
    return NULL;
  }
  return (*env)->NewStringUTF(env, val);
}

static jlong osj_open(JNIEnv *env, jobject obj, jstring filename) {
  const char *filename2 = (*env)->GetStringUTFChars(env, filename, NULL);
  if (filename2 == NULL) {
    return 0;
  }

  openslide_t *osr = openslide_open(filename2);

  (*env)->ReleaseStringUTFChars(env, filename, filename2);

  return (jlong) osr;
}

static jint osj_get_level_count(JNIEnv *env, jobject obj, jlong osr) {
  return openslide_get_level_count((openslide_t *) osr);
}

static void osj_get_level_dimensions(JNIEnv *env, jobject obj, jlong osr, jint level,
				     jlongArray dim) {
  int64_t dims[2];
  openslide_get_level_dimensions((openslide_t *) osr, level, dims, dims + 1);

  (*env)->SetLongArrayRegion(env, dim, 0, 2, (jlong *) dims);
}

static jdouble osj_get_level_downsample(JNIEnv *env, jobject obj, jlong osr, jint level) {
  return openslide_get_level_downsample((openslide_t *) osr, level);
}

static void osj_close(JNIEnv *env, jobject obj, jlong osr) {
  openslide_close((openslide_t *) osr);
}

static jobjectArray string_array_to_jobjectArray(JNIEnv *env, const char * const *strs) {
  jclass strclazz = (*env)->FindClass(env, "java/lang/String");
  if (strclazz == NULL) {
    return NULL;
  }

  // count length
  int len = 0;
  const char * const *tmp = strs;
  while (*tmp++ != NULL) {
    len++;
  }

  // new array
  jobjectArray arr = (*env)->NewObjectArray(env, len, strclazz, NULL);
  if (arr == NULL) {
    return NULL;
  }

  // set
  int i;
  for (i = 0; i < len; i++) {
    jstring str = (*env)->NewStringUTF(env, strs[i]);
    if (str == NULL) {
      return NULL;
    }
    (*env)->SetObjectArrayElement(env, arr, i, str);
    (*env)->DeleteLocalRef(env, str);
  }

  return arr;
}

static jobjectArray osj_get_property_names(JNIEnv *env, jobject obj, jlong osr) {
  return string_array_to_jobjectArray(env, openslide_get_property_names((openslide_t *) osr));
}

static jstring osj_get_property_value(JNIEnv *env, jobject obj, jlong osr, jstring name) {
  const char *name2 = (*env)->GetStringUTFChars(env, name, NULL);
  if (name2 == NULL) {
    return NULL;
  }

  const char *val = openslide_get_property_value((openslide_t *) osr, name2);

  (*env)->ReleaseStringUTFChars(env, name, name2);

  if (val == NULL) {
    return NULL;
  }
  return (*env)->NewStringUTF(env, val);
}

static jobjectArray osj_get_associated_image_names(JNIEnv *env, jobject obj, jlong osr) {
  return string_array_to_jobjectArray(env, openslide_get_associated_image_names((openslide_t *) osr));
}

static void osj_read_region(JNIEnv *env, jobject obj, jlong osr, jintArray dest,
			    jlong x, jlong y, jint level, jlong w, jlong h) {
  uint32_t *dest2 = (*env)->GetPrimitiveArrayCritical(env, dest, NULL);
  if (dest2 == NULL) {
    return;
  }

  openslide_read_region((openslide_t *) osr, dest2, x, y, level, w, h);

  (*env)->ReleasePrimitiveArrayCritical(env, dest, dest2, 0);
}

static void osj_get_associated_image_dimensions(JNIEnv *env, jobject obj, jlong osr,
						jstring name, jlongArray dim) {
  int64_t dims[2];

  const char *name2 = (*env)->GetStringUTFChars(env, name, NULL);
  if (name2 == NULL) {
    return;
  }

  openslide_get_associated_image_dimensions((openslide_t *) osr, name2, dims, dims + 1);

  (*env)->SetLongArrayRegion(env, dim, 0, 2, (jlong *) dims);
  (*env)->ReleaseStringUTFChars(env, name, name2);
}

static void osj_read_associated_image(JNIEnv *env, jobject obj, jlong osr,
				      jstring name, jintArray dest) {
  const char *name2 = (*env)->GetStringUTFChars(env, name, NULL);
  if (name2 == NULL) {
    return;
  }

  uint32_t *dest2 = (*env)->GetPrimitiveArrayCritical(env, dest, NULL);
  if (dest2 == NULL) {
    return;
  }

  openslide_read_associated_image((openslide_t *) osr, name2, dest2);

  (*env)->ReleasePrimitiveArrayCritical(env, dest, dest2, 0);

  (*env)->ReleaseStringUTFChars(env, name, name2);
}

static jstring osj_get_error(JNIEnv *env, jobject obj, jlong osr) {
  const char *val = openslide_get_error((openslide_t *) osr);

  if (val == NULL) {
    return NULL;
  }

  return (*env)->NewStringUTF(env, val);
}

static jstring osj_get_version(JNIEnv *env, jobject obj) {
  const char *val = openslide_get_version();

  return (*env)->NewStringUTF(env, val);
}

static JNINativeMethod methods[] = {
  { "openslide_detect_vendor", "(Ljava/lang/String;)Ljava/lang/String;", (void *) osj_detect_vendor },
  { "openslide_open", "(Ljava/lang/String;)J", (void *) osj_open },
  { "openslide_get_level_count", "(J)I", (void *) osj_get_level_count },
  { "openslide_get_level_dimensions", "(JI[J)V", (void *) osj_get_level_dimensions },
  { "openslide_get_level_downsample", "(JI)D", (void *) osj_get_level_downsample },
  { "openslide_close", "(J)V", (void *) osj_close },
  { "openslide_get_property_names", "(J)[Ljava/lang/String;", (void *) osj_get_property_names },
  { "openslide_get_property_value", "(JLjava/lang/String;)Ljava/lang/String;", (void *) osj_get_property_value },
  { "openslide_get_associated_image_names", "(J)[Ljava/lang/String;", (void *) osj_get_associated_image_names },
  { "openslide_read_region", "(J[IJJIJJ)V", (void *) osj_read_region },
  { "openslide_get_associated_image_dimensions", "(JLjava/lang/String;[J)V", (void *) osj_get_associated_image_dimensions },
  { "openslide_read_associated_image", "(JLjava/lang/String;[I)V", (void *) osj_read_associated_image },
  { "openslide_get_error", "(J)Ljava/lang/String;", (void *) osj_get_error },
  { "openslide_get_version", "()Ljava/lang/String;", (void *) osj_get_version },
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  JNIEnv* env;
  if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
    return -1;
  }

  jclass clazz = (*env)->FindClass(env, "org/openslide/OpenSlideJNI");
  if (clazz == NULL) {
    return -1;
  }

  if ((*env)->RegisterNatives(env, clazz, methods,
      sizeof(methods) / sizeof(methods[0])) != JNI_OK) {
    return -1;
  }

  return JNI_VERSION_1_4;
}
