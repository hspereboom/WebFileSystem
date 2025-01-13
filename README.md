### About
`Myriapod` is a web-based file server meant for testing purposes; it retrieves
attributes and contents of directories and files from a remote fileserver such as
`Arachnid`, which serves markup-free metadata.

### Integration
The main package (namespace) is `WFS`; please rebrand at will.
There are no dependencies outside OOTB Java itself.

In order for `Myriapod` to be treated as a valid `java.nio.file.FileSystem`,
please copy the following file into your compilation target (jar or dir):
  `META-INF/services/java.nio.file.spi.FileSystemProvider`

Said file should contain the canonical name of the `WebFileSystemProvider` class.
This will instruct the JRE to associate the `webfs` URI scheme with said class.

e.g. for Maven projects, one might typically use `src/test/resources/META-INF/services`.

### Configuration
The config file `.myriapod` is unused at the moment.

### Licensing
All code is distributed under the MIT license https://opensource.org/license/mit.  
For easy comparison with other licenses, see https://choosealicense.com/licenses.
