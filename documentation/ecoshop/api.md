---
title: "API"
sidebar_position: 8
---

This page is for developers who want to build against EcoShop from their own plugin. EcoShop is open-source, so you can read the implementation and depend on it directly.

## Source code

The source code is on GitHub at [Auxilor/EcoShop](https://github.com/Auxilor/EcoShop).

## Adding the dependency

1. Add the Auxilor repository to your `build.gradle.kts`.
2. Add EcoShop as a `compileOnly` dependency.

```kotlin
repositories {
    maven("https://repo.auxilor.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.willfp:EcoShop:<version>")
}
```

The latest version available on the repo can be found [here](https://github.com/Auxilor/EcoShop/tags).

<hr/>

## Where to go next

- **Shared APIs:** most cross-plugin APIs live in the [eco framework](https://github.com/Auxilor/eco), not in EcoShop itself.
- **Config side:** [How to make a Shop](how-to-make-a-shop) covers the config that most integrations build on.