# OpenSlide Java

This is a Java binding to [OpenSlide](https://openslide.org/).


## Build requirements

- JDK
- Meson &ge; 0.62
- OpenSlide &ge; 3.4.0
- pkg-config


## Building

```
meson setup builddir
meson compile -C builddir
meson install -C builddir
```


## License

OpenSlide Java is released under the terms of the [GNU Lesser General Public
License, version 2.1](https://openslide.org/license/).

OpenSlide Java is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
License for more details.
