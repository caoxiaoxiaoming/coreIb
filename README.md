# coreIb

`coreIb` is a small Java 8-compatible Maven library. The current release is `1.0.1`.

## Maven coordinates

```xml
<dependency>
    <groupId>com.github.caoxiaoxiaoming</groupId>
    <artifactId>coreib</artifactId>
    <version>1.0.1</version>
</dependency>
```

The package is published to GitHub Packages. Consumers of this private repository need a GitHub token with `read:packages` permission in their Maven `settings.xml` under the server id `github`.

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/caoxiaoxiaoming/coreIb</url>
    </repository>
</repositories>
```

## API

```java
String version = CoreIb.version();
```

## Build

```shell
mvn clean verify
```

A tag matching the Maven version, such as `v1.0.1`, triggers the GitHub Actions publishing workflow.
