# Third-party components

This application uses unmodified `com.herohan:UVCAndroid:1.0.13` from Maven Central.

- Project: https://github.com/shiyinghan/UVCAndroid
- Tagged source: https://github.com/shiyinghan/UVCAndroid/tree/1.0.13
- Tag commit: `6018c7f20238832929cf61427427bb7828056260`
- Source archive: https://github.com/shiyinghan/UVCAndroid/archive/refs/tags/1.0.13.zip
- AAR: https://repo.maven.apache.org/maven2/com/herohan/UVCAndroid/1.0.13/UVCAndroid-1.0.13.aar

UVCAndroid's Java wrapper uses Apache-2.0 and incorporates work from saki4510t/UVCCamera. Its native components include libusb (LGPL-2.1), libuvc (BSD), libjpeg-turbo (IJG/BSD/zlib terms), libyuv (BSD), and RapidJSON (MIT). Notices and license texts are included in `androidApp/src/main/assets/licenses/` and bundled in the APK. The upstream libyuv subtree omits its root license file; the matching BSD license text is supplied from the libyuv upstream mirror at https://github.com/lemenkov/libyuv/blob/master/LICENSE.

The original UVCAndroid AAR and tagged source archive are also provided alongside the development APK in `artifacts/third-party/` for inspection and rebuilding. There are no local modifications to its Java or native code. This project's source, standard Android packaging and Debug signing allow replacing/rebuilding the native shared libraries and rebuilding the APK; no integrity enforcement prevents replacement. Consult each component's full license for its terms.

Other runtime dependencies include Kotlin, JetBrains Compose Multiplatform, kotlinx.coroutines, AndroidX and Material Components (Apache-2.0), and Hutool (Mulan PSL v2, transitive dependency of UVCAndroid). Apache-2.0 and Mulan PSL v2 texts are included. Original component copyrights remain with their respective authors. Gradle wrapper is distributed under Apache-2.0.

Sources:

- https://github.com/JetBrains/kotlin
- https://github.com/JetBrains/compose-multiplatform
- https://github.com/Kotlin/kotlinx.coroutines
- https://android.googlesource.com/platform/frameworks/support/
- https://github.com/dromara/hutool/tree/5.8.35
