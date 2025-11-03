### About
`Myriapod` is a http file system implementation, meant for testing purposes;
it retrieves attributes and contents of directories and files from a remote
file server such as `Arachnid`, which serves markup-free metadata.

### Integration
The main package (namespace) is `WFS`; please rebrand at will.
There are no dependencies outside OOTB Java itself.
The code is compatible with Java 8 (1.8).

In order for this implementation to be recognized as a `java.nio.file.FileSystem`,
make sure that your build places the file `java.nio.file.spi.FileSystemProvider`
into the directory `META-INF/services/` of your compilation target.

e.g. for Maven projects, one might typically use `src/test/resources/META-INF/services`.

Said file should contain the canonical name of the `WebFileSystemProvider` class.
This will instruct the JRE to associate the `webfs` URI scheme with said class.

### Configuration
The config file `.myriapod` is unused at the moment and can be omitted.

### Licensing
All code is distributed under the MIT license https://opensource.org/license/mit.
For easy comparison with other licenses, see https://choosealicense.com/licenses.
