project(
  'openslide-java',
  'java',
  default_options : [
    'buildtype=debugoptimized',
  ],
  license : 'LGPL-2.1-only',
  meson_version : '>=0.62',
  version : '0.12.4',
)
# for FFM API
java_ver = '22'

# Java setup
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
  '-source', java_ver,
  '-target', java_ver,
  '-Xlint:all,-serial',
  language : 'java',
)

# jar
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
    'org/openslide/OpenSlideFFM.java',
    'org/openslide/OpenSlide.java',
    'org/openslide/TestCLI.java',
  ],
  main_class : 'org.openslide.gui.Demo',
)
# https://github.com/mesonbuild/meson/issues/3070
openslide_jar = custom_target(
  capture : true,
  command : [jar, '-um', manifest_extra],
  feed : true,
  input : built_jar,
  install : true,
  install_dir : get_option('datadir') / 'openslide-java',
  install_tag : 'runtime',
  output : 'openslide.jar',
)
