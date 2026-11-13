Licensing files for KdeJsouMojePrachy

Do not manually copy an outdated license from a different release.
The build-exe.bat script calls collect-licenses.ps1 after jpackage and copies:
- licenses/THIRD-PARTY-NOTICES.txt
- LICENSE/NOTICE/COPYING etc. files embedded in all runtime dependency JARs
- the legal/ folder from the bundled JDK runtime
- a BUNDLED-JARS.txt manifest

Review the output before distributing. If a JAR has no embedded license text,
obtain the matching version's license and NOTICE from the publisher and include
it in the distribution. Check JDK source-code availability obligations as well.

build-portable.bat calls build-exe.bat and zips the complete application folder,
including licenses/.
