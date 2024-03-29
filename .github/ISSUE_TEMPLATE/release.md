# OpenSlide Java release process

- [ ] Run test build and `meson dist`
- [ ] Update `CHANGELOG.md` and versions in `configure.ac` and `meson.build`
- [ ] Create and push signed tag
- [ ] `git clean -dxf && meson setup builddir && meson dist -C builddir`
- [ ] Attach release notes to [GitHub release](https://github.com/openslide/openslide-java/releases/new), set pre-release flag, and upload tarball
- [ ] [Update openslide-bin](https://github.com/openslide/openslide-bin/issues/new?labels=release&template=release.md)
- [ ] Update website: `_data/releases.yaml`, `_includes/news.md`
- [ ] Send mail to -announce and -users
- [ ] Post to [forum.image.sc](https://forum.image.sc/c/announcements/10)
