<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# DeprecatedControllerRemover Changelog

## [Unreleased]
### Added
- **Dual Tool Window System**: Added separate tool windows for Deprecated Controller Remover and Code Cleanup operations
- **Deprecated Controller Remover Tool Window**: Black console-style interface for monitoring deprecated controller removal operations
- **Code Cleanup Tool Window**: Black console-style interface for monitoring general code cleanup operations
- **Deprecated Controller Remover Service**: Comprehensive analysis and removal of unused deprecated methods in Spring controllers
- **Code Cleanup Service**: Multi-pass cleanup system for unused imports, final private fields, and empty classes
- **File Marking System**: Support for `//Controller Cleaner` comment to mark files for cleanup
- **Multi-pass Cleanup**: Up to 3 cleanup passes to catch newly unused elements after initial cleanup
- **Real-time Progress Tracking**: Live updates and detailed logging in both tool windows
- **Comprehensive Error Handling**: Robust error handling with detailed error messages
- **User Confirmation Dialogs**: Safety confirmation dialogs before performing cleanup operations
- **Spring Controller Detection**: Automatic detection of `@Controller` and `@RestController` annotated classes
- **Deprecated Method Analysis**: PSI-based analysis of deprecated methods with usage validation
- **Import Cleanup**: Removal of unused import statements (excluding java.lang.*)
- **Field Cleanup**: Removal of unused final private fields (excluding annotated fields)
- **Empty Class Removal**: Detection and removal of classes with no methods, fields, or nested classes
- **Project Structure**: Organized codebase with separate modules for controller and code cleanup
- **Tool Window Services**: Dedicated services for managing tool window state and communication
- **Action Integration**: Menu actions integrated into IntelliJ's Tools menu
- **Plugin Configuration**: Proper plugin.xml configuration with tool window definitions

### Changed
- **Plugin Name**: Maintained as "DeprecatedControllerRemover"
- **Project Structure**: Reorganized from template structure to production-ready architecture
- **Package Structure**: Implemented `com.wheelseye` package structure with separate cleanup modules
- **Tool Window Layout**: Configured tool windows to appear on the same side with consistent icons
- **Documentation**: Comprehensive README updates reflecting current functionality

### Technical Details
- **Build System**: Gradle-based build with proper IntelliJ Platform SDK integration
- **Language Support**: Full Java support with PSI-based analysis
- **Framework Support**: Specialized Spring Framework integration
- **IDE Compatibility**: IntelliJ IDEA 2023.1+ compatibility
- **Java Version**: Java 17+ support

## [0.1.0] - Initial Release
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- Basic project structure and build configuration
- Plugin descriptor and metadata
