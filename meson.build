project(
  'openslide-java',
  'c', 'java',
  default_options : [
    'buildtype=debugoptimized',
  ],
  license : 'LGPL-2.1-only',
  meson_version : '>=0.62',
  version : '0.12.4',
)
if not meson.is_subproject()
  meson.add_dist_script(
    'scripts/dist.py'
  )
endif
java_ver = '1.8'

# options
# must be absolute path for openslide_jni_path
install_dir = get_option('prefix') / get_option('libdir') / 'openslide-java'

# Java setup
jni = dependency(
  'jni',
  version : '>=' + java_ver,
)
if meson.is_cross_build()
  # probably no native JNI headers available; use bundled generic ones
  jni = declare_dependency(
    dependencies : [jni],
    include_directories : ['cross'],
  )
elif host_machine.system() == 'darwin'
  if not meson.get_compiler('c').has_header('jni.h', dependencies : [jni])
    # Meson < 1.0 doesn't know to check /usr/libexec/java_home
    darwin_java_home = run_command(
      '/usr/libexec/java_home',
      check : true,
    ).stdout().strip()
    message('Using JNI headers from ' + darwin_java_home)
    jni = declare_dependency(
      dependencies : [jni],
      include_directories : [
        darwin_java_home / 'include',
        darwin_java_home / 'include/darwin'
      ],
    )
  endif
endif
native_java_home = meson.get_external_property(
  'java_home',
  '',
  native : true
)
jar = find_program(
  'jar',
  dirs : native_java_home != '' ? [native_java_home / 'bin'] : [],
  native : true,
)

# compiler options
add_project_arguments(
  '-Wno-pointer-to-int-cast',
  '-Wno-int-to-pointer-cast',
  language : 'c',
)
add_project_arguments(
  '-source', java_ver,
  '-target', java_ver,
  '-Xlint:-options',
  language : 'java',
)

# library dependencies
openslide = dependency(
  'openslide',
  version : '>=3.4.0',
)

# JNI
jni_link_args = []
jni_prefix = []
jni_suffix = []
if host_machine.system() == 'windows'
  # JNI uses stdcall without @
  jni_link_args += '-Wl,--kill-at'
  # skip "lib" prefix, even when built with MinGW
  jni_prefix = ''
elif host_machine.system() == 'darwin'
  # special file extension for JNI libraries
  jni_suffix = 'jnilib'
endif
openslide_jni = shared_module(
  'openslide-jni',
  'openslide-jni.c',
  dependencies : [jni, openslide],
  install : true,
  install_dir : install_dir,
  link_args : jni_link_args,
  name_prefix : jni_prefix,
  name_suffix : jni_suffix,
)
fs = import('fs')
openslide_jni_path = install_dir / fs.name(openslide_jni.full_path())

# jar
# by default, don't embed JNI library path in jar on Windows
embed_jni_path = get_option('embed_jni_path')
embed_jni_path = (
  embed_jni_path.enabled() or
  embed_jni_path.auto() and host_machine.system() != 'windows'
)
summary(
  'Embed JNI path in JAR', embed_jni_path,
  bool_yn : true
)
jar_props = configure_file(
  input : 'meta/openslide.properties.in',
  output : 'openslide.properties',
  configuration : {
    'jni_path': embed_jni_path ? openslide_jni_path : '',
  }
)
manifest_extra = configure_file(
  input : 'meta/MANIFEST.MF.in',
  output : 'MANIFEST.MF',
  configuration : {
    'version': meson.project_version(),
  }
)
built_jar = jar(
  'built',
  [
    'org/openslide/gui/Annotation.java',
    'org/openslide/gui/DefaultAnnotation.java',
    'org/openslide/gui/DefaultSelectionListModel.java',
    'org/openslide/gui/Demo.java',
    'org/openslide/gui/OpenSlideView.java',
    'org/openslide/gui/SelectionListModel.java',
    'org/openslide/AssociatedImage.java',
    'org/openslide/OpenSlideDisposedException.java',
    'org/openslide/OpenSlide.java',
    'org/openslide/OpenSlideJNI.java',
    'org/openslide/TestCLI.java',
  ],
  java_resources : structured_sources(
    [],
    {
      'resources': jar_props,
    },
  ),
  main_class : 'org.openslide.gui.Demo',
)
# https://github.com/mesonbuild/meson/issues/3070
openslide_jar = custom_target(
  capture : true,
  command : [jar, '-um', manifest_extra],
  feed : true,
  input : built_jar,
  install : true,
  install_dir : install_dir,
  install_tag : 'runtime',
  output : 'openslide.jar',
)
