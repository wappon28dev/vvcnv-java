#

## Install

```sh
brew install maven
mise i
```

ターゲットパス (src/01, src/02, ...) が増えた場合, `pom.xml` > project.build.plugins.plugin 内に次の設定を追加する.

```diff
+ <sources>
+   <source>src/01</source>
+   <source>src/02</source>
+   <!-- 必要な分だけ追加 -->
+ </sources>
```

## Reproduce

```sh
brew install maven
mise use java@21
mvn archetype:generate
```

src/01 や src/02 などのディレクトリを追加する必要があるので, `pom.xml` > project.build に次の設定を追加.

```xml
<!-- NOTE: 複数のディレクトリをビルドできるようにするための設定 -->
<!-- https://www.mojohaus.org/build-helper-maven-plugin/add-source-mojo.html -->
<plugins>
  <plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
      <execution>
        <id>add-custom-sources</id>
        <phase>generate-sources</phase>
        <goals>
          <goal>add-source</goal>
        </goals>
        <configuration>
          <sources>
            <source>src/01</source>
            <source>src/02</source>
            <!-- 必要な分だけ追加 -->
          </sources>
        </configuration>
      </execution>
    </executions>
  </plugin>
</plugins>
```
