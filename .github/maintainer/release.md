---
link-text: Release checklist
repo: openslide/openslide-java
title: Release X.Y.Z
labels: [release]
---

# OpenSlide Java release process

- [ ] Update `CHANGELOG.md` and version in `pom.xml`
- [ ] Create and push signed tag
- [ ] Verify that GitHub Actions created a [GitHub release](https://github.com/openslide/openslide-java/releases) with release notes and a JAR
- [ ] `cd` into website checkout; `_scripts/sync-releases.py`; update `_includes/news.md`
- [ ] Send mail to -announce and -users
- [ ] Post to [forum.image.sc](https://forum.image.sc/c/announcements/10)
