# keepalive

A small and lightweight library to set the keepalive idle time and interval on a `Socket`, in a cross-platform manner (works on Windows, Linux, Android, ...).

This feature has been added to Linux and Mac OS in [Java 11](https://docs.oracle.com/en/java/javase/11/docs/api/jdk.net/jdk/net/ExtendedSocketOptions.html), but not in previous Java versions or to Windows.

keepalive provides a main class, `Keepalive`, with static functions to set TCP keepalive settings on a socket: enabled/disable, idle time, and interval.

For Kotlin users, keepalive also provides a utility class, `KeepaliveUtils`, with extension methods for `Socket`.

*keepalive internally uses JNA and a bit of reflection.*

## Usage

The latest version is: **`0.0.2`**

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

    // if running on android, also add
    implementation 'fr.delthas:keepalive-android:VERSION'
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

    // if running on android, also add
    implementation("fr.delthas:keepalive-android:VERSION")
}
```

## Status

keepalive is experimental, but has been successfully tested on several platforms.

| Platform | Support |
| -- | -- |
| Windows | Yes, tested |
| Linux | Yes, tested |
| Darwin (MacOS) | Yes |

keepalive does access an internal private field which is not part of the official Java API, so it is JDK-implementation-dependent:

| JDK | Support |
| -- | -- |
| OpenJDK | Yes, tested |
| Android | Yes, tested |
| GNU Classpath | No |
