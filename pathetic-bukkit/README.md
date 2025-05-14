# Bukkit Implementation
This is the Bukkit implementation of Pathetic. It provides a simple API to use the Pathetic engine in your Bukkit plugin.

Currently Paper and Spigot are explicitly supported, every other server software is a lucky hit.

## How to use it?
To integrate pathetic into your bukkit project, follow the following steps:
<br>
*(We advise you to relocate pathetic)*

<h3>Maven</h3>

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.bsommerfeld.pathetic</groupId>
    <artifactId>pathetic-bukkit</artifactId>
    <version>VERSION</version>
</dependency>
```

<h3>Gradle</h3>

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.bsommerfeld.pathetic:pathetic-bukkit:VERSION'
}
```

<h2>Example Usage</h2>

<p>See the <a href="https://github.com/bsommerfeld/pathetic/tree/trunk/pathetic-bukkit/pathetic-example">pathetic-example</a> module for a detailed bukkit example.</p>
