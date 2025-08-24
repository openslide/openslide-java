# OpenSlide Java

This is a Java binding to [OpenSlide](https://openslide.org/).


## Build requirements

- JDK ≥ 22
- Maven


## Runtime requirements

- JRE ≥ 22
- OpenSlide ≥ 4.0.0, in the system's library search path or preloaded with
  `System.load()` or `System.loadLibrary()`


## Building

```
mvn
```

The JAR will be in `target/openslide-java-*.jar`.

If you have multiple JVMs on your system, and Maven defaults to a version
older than 22, you might need to set `JAVA_HOME`.  For example, on Fedora:

```
JAVA_HOME=/usr/lib/jvm/java-22 mvn
```


## License

OpenSlide Java is released under the terms of the [GNU Lesser General Public
License, version 2.1](https://openslide.org/license/).

OpenSlide Java is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
License for more details.
