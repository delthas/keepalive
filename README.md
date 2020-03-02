# keepalive

A small and lightweight library to set the keepalive idle time and interval on a `Socket`, in a cross-platform manner.

This feature has been added to Linux and Mac OS in [Java 11](https://docs.oracle.com/en/java/javase/11/docs/api/jdk.net/jdk/net/ExtendedSocketOptions.html), but not in previous Java versions or to Windows.

keepalive provides a main class, `Keepalive`, with static functions to set TCP keepalive settings on a socket: enabled/disable, idle time, and interval.

For Kotlin users, keepalive also provides a utility class, `KeepaliveUtils`, with extension methods for `Socket`.

*keepalive internally uses JNA and a bit of reflection.*

## Usage

The latest version is: **`0.0.1`**

### Maven

Add to your Maven `pom.xml`:
```xml
<repositories>
  <repository>
    <id>s06r</id>
    <url>https://maven.dille.cc</url>
  </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>fr.delthas</groupId>
        <artifactId>keepalive</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>
```

### Gradle

Add to your `build.gradle`:

```
repositories {
    maven {
        url 'https://maven.dille.cc'
    }
}

dependencies {
    implementation 'fr.delthas:keepalive:VERSION'
}
```

### Gradle (Kotlin build script)

Add to your `build.gradle.kts`:

```
repositories {
    maven {
        url = uri("https://maven.dille.cc")
    }
}

dependencies {
    implementation("fr.delthas:keepalive:VERSION")
}
```

## Status

keepalive is experimental and has been successfully tested on Windows.

| OS | Support |
| -- | -- |
| Windows | Yes, tested |
| Unix | Yes |
| Darwin (MacOS) | Yes |
