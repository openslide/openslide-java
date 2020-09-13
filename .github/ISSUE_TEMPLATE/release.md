# OpenSlide Java release process

- [ ] Run test build and `make distcheck`
- [ ] Update `CHANGELOG.txt` and version in `configure.ac`
- [ ] Create and push signed tag
- [ ] `git clean -dxf && autoreconf -i && ./configure && make distcheck`
- [ ] Attach release notes to [GitHub release](https://github.com/openslide/openslide-java/releases), set pre-release flag, and upload tarballs
- [ ] Update openslide-winbuild
- [ ] Update website: `_data/releases.yaml`, `_includes/news.markdown`
- [ ] Send mail to -announce and -users
