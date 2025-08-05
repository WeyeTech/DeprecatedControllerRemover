<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# DeprecatedControllerRemover Changelog

## [Unreleased]
### Added
- Enhanced error handling and validation
- Performance optimizations for large codebases
- Improved user feedback and progress reporting

## [0.0.3]
### Added
- **Enhanced Code Analysis**: Improved PSI-based analysis for more accurate detection
- **Better Error Handling**: More robust error handling with detailed error messages
- **Performance Improvements**: Optimized analysis for large codebases
- **User Experience Enhancements**: Improved progress reporting and feedback

### Changed
- **Analysis Accuracy**: Enhanced detection of unused elements with better validation
- **Tool Window Performance**: Improved responsiveness of tool windows
- **Documentation**: Updated README with current version information

## [0.0.2]
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
- **Field Cleanup**: Removal of any unused fields (excluding public, static, and annotated fields)
- **Class Removal**: Detection and removal of classes with no methods (more aggressive approach)
- **Project Structure**: Organized codebase with separate modules for controller and code cleanup
- **Tool Window Services**: Dedicated services for managing tool window state and communication
- **Action Integration**: Menu actions integrated into IntelliJ's Tools menu
- **Plugin Configuration**: Proper plugin.xml configuration with tool window definitions

### Changed
- **Plugin Name**: Maintained as "Deprecated Controller Remover"
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
