# OpenSlide Java release process

- [ ] Run test build and `make distcheck`
- [ ] Update `CHANGELOG.txt` and version in `configure.ac`
- [ ] Create and push signed tag
- [ ] `git clean -dxf && autoreconf -i && ./configure && make distcheck`
- [ ] Attach release notes to [GitHub release](https://github.com/openslide/openslide-java/releases/new), set pre-release flag, and upload tarballs
- [ ] [Update openslide-winbuild](https://github.com/openslide/openslide-winbuild/issues/new?labels=release&template=release.md)
- [ ] Update website: `_data/releases.yaml`, `_includes/news.markdown`
- [ ] Send mail to -announce and -users
